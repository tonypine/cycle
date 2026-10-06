package com.tonypine.cycle

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.tonypine.cycle.core.model.Language
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Cycle's language applied to the app (`docs/decisions/0008-languages.md`): by Cycle below Android 13
 * (SDK 29), by Android on 13 and later (the default SDK). Cycle has only English strings so far, so the
 * screens stay in English; the activity's configuration shows what Android would draw in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w360dp-h800dp")
class AppLanguageTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app = ApplicationProvider.getApplicationContext<CycleApplication>()
    private val languages = app.data.languageRepository
    private val german = Language("de")
    private val phoneDefault = Locale.getDefault()

    @After
    fun restoreDefaultLocale() = Locale.setDefault(phoneDefault)

    private fun activityLocale() = composeRule.activity.resources.configuration.locales.get(0)

    private fun waitForActivityIn(locale: Locale) = composeRule.waitUntil(WAIT_MILLIS) {
        composeRule.runOnIdle { activityLocale() } == locale
    }

    @Test
    @Config(sdk = [29])
    fun `below Android 13 choosing a language recreates the app in it, on the same tab`() {
        skipWelcome()
        composeRule.onNode(hasText("Calendar") and isTab).performClick()

        runBlocking { languages.setLanguage(german) }
        waitForActivityIn(Locale.GERMAN)

        assertEquals(Locale.GERMAN, Locale.getDefault())
        composeRule.onNode(hasText("Calendar") and isTab).assertIsSelected()

        // The phone's language again.
        runBlocking { languages.setLanguage(null) }
        waitForActivityIn(Locale.US)
        assertEquals(phoneDefault, Locale.getDefault())
        composeRule.onNode(hasText("Calendar") and isTab).assertIsSelected()
    }

    @Test
    @Config(sdk = [29])
    fun `below Android 13 a cold start opens in the stored language before the first frame`() {
        runBlocking { app.data.settingsRepository.setLanguage(german) }
        // What a new process does: the Application reads the settings once, and attachBaseContext waits
        // for that read, before onCreate sets the content. The first composition is already in German.
        app.language.onCreate()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(Locale.GERMAN, activity.resources.configuration.locales.get(0))
            }
        }
    }

    @Test
    fun `on Android 13 and later choosing a language hands it to Android`() {
        runBlocking { languages.setLanguage(german) }

        assertEquals(LocaleList.forLanguageTags("de"), localeManager().applicationLocales)
        assertEquals(german, runBlocking { languages.language.first() })
    }

    @Test
    fun `on Android 13 and later a language set on Android's page is what the repository reports`() {
        localeManager().applicationLocales = LocaleList.forLanguageTags("de")

        // Back to the front: the activity starts again.
        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(WAIT_MILLIS) { runBlocking { languages.language.first() } == german }
    }

    @Test
    fun `the first start on Android 13 hands a choice made below 13 to Android`() {
        // A phone updated from Android 12, or a backup restored from one: her choice in the settings,
        // none on Android's side, and no record of a hand-over on this install.
        File(app.noBackupFilesDir, "language_handed_over").delete()
        runBlocking { app.data.settingsRepository.setLanguage(german) }

        app.language.onCreate()

        assertEquals(LocaleList.forLanguageTags("de"), localeManager().applicationLocales)
        assertEquals(german, runBlocking { languages.language.first() })
    }

    private fun localeManager() = app.getSystemService(LocaleManager::class.java)

    private fun skipWelcome() {
        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodes(hasText("Skip for now")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Skip for now").performClick()
        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodes(hasText("Calendar") and isTab).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private companion object {
        val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

        // DataStore writes on its own thread.
        const val WAIT_MILLIS = 5_000L
    }
}
