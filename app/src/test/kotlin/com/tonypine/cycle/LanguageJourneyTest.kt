package com.tonypine.cycle

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.tonypine.cycle.core.model.Language
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
 * Settings › App › Language in the app, as the ticket's walkthrough: on a phone in English she
 * switches Cycle to German on its Language page, stays on it while it redraws, goes back to Settings
 * in German on the same tab, then follows the phone again. On Android 13 and later, a language picked
 * on Android's own page shows on Cycle's row and page when she comes back. Below Android 13 (SDK 29)
 * Cycle recreates the activity itself; on 13 and later (the default SDK) Android does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w360dp-h800dp")
class LanguageJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app = ApplicationProvider.getApplicationContext<CycleApplication>()
    private val phoneDefault = Locale.getDefault()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @After
    fun restoreDefaultLocale() = Locale.setDefault(phoneDefault)

    @Test
    @Config(sdk = [29])
    fun `she switches Cycle to German on its Language page, then back to the phone's language`() {
        skipWelcome()
        tab("Settings").performClick()

        // 1. The Language row follows the phone, in English.
        row("Language", "Phone's language, English").performClick()

        // 2. Five options, Phone's language selected.
        listOf("Phone's language", "Deutsch", "English", "Español", "Português (Brasil)").forEach {
            option(it)
        }
        option("Phone's language").assertIsSelected()

        // 3. Deutsch: the same page redraws in German at once, with Deutsch selected.
        option("Deutsch").performClick()
        waitFor(hasText("Sprache") and isHeading())
        option("Deutsch").assertIsSelected()
        assertEquals(Language("de"), runBlocking { app.data.languageRepository.language.first() })

        // 4. Back: Settings in German on the same tab, and the other tabs in German too.
        composeRule.onNodeWithContentDescription("Zurück").performClick()
        row("Sprache", "Deutsch")
        tab("Einstellungen").assertIsSelected()
        tab("Kalender").performClick()
        tab("Kalender").assertIsSelected()
        tab("Heute").performClick()
        tab("Heute").assertIsSelected()

        // 5. Phone's language again: Cycle is in English.
        tab("Einstellungen").performClick()
        row("Sprache", "Deutsch").performClick()
        option("Sprache des Telefons").performClick()
        waitFor(hasText("Language") and isHeading())
        option("Phone's language").assertIsSelected()
        assertEquals(null, runBlocking { app.data.languageRepository.language.first() })
        composeRule.onNodeWithContentDescription("Back").performClick()
        row("Language", "Phone's language, English")
        tab("Settings").assertIsSelected()
    }

    @Test
    fun `on Android 13 and later a language picked on Android's page shows on Cycle's row and page`() {
        skipWelcome()
        tab("Settings").performClick()
        row("Language", "Phone's language, English")

        // 7. Android Settings › Apps › Cycle › Language: Español. Back to Cycle, the activity starts again.
        app.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("es")
        composeRule.activityRule.scenario.recreate()

        row("Español").performClick()
        option("Español").assertIsSelected()
    }

    private fun tab(label: String): SemanticsNodeInteraction {
        waitFor(hasText(label) and isTab)
        return composeRule.onNode(hasText(label) and isTab)
    }

    private fun row(vararg texts: String): SemanticsNodeInteraction {
        val matcher = texts.map { hasText(it) }.reduce { all, each -> all and each } and isButton
        waitFor(matcher)
        return composeRule.onNode(matcher).performScrollTo()
    }

    // An option by its title, the first of its texts: "English" is also Phone's language's second line.
    private fun option(title: String): SemanticsNodeInteraction {
        val matcher = SemanticsMatcher("titled $title") {
            it.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text == title
        } and isRadio
        waitFor(matcher)
        return composeRule.onNode(matcher).performScrollTo()
    }

    private fun waitFor(matcher: SemanticsMatcher) =
        composeRule.waitUntil(WAIT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun skipWelcome() {
        waitFor(hasText("Skip for now"))
        composeRule.onNodeWithText("Skip for now").performClick()
        waitFor(hasText("Settings") and isTab)
    }

    private companion object {
        // DataStore writes on its own thread, and the activity is recreated.
        const val WAIT_MILLIS = 5_000L
    }
}
