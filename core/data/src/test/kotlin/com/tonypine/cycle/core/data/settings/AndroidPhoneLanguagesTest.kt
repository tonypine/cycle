package com.tonypine.cycle.core.data.settings

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.settingsRepository
import com.tonypine.cycle.core.model.Language
import java.io.File
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Android's side of Cycle's language, on Android 10 (SDK 29) and on 13 and later (the default SDK). */
@RunWith(AndroidJUnit4::class)
class AndroidPhoneLanguagesTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val phone = AndroidPhoneLanguages(context)

    @Test
    fun `on Android 13 and later Cycle's language is Android's per-app setting`() {
        val localeManager = context.getSystemService(LocaleManager::class.java)
        assertTrue(phone.hasAppLanguage)
        assertEquals(null, phone.appLanguage)

        phone.appLanguage = Language("pt-BR")
        assertEquals(LocaleList.forLanguageTags("pt-BR"), localeManager.applicationLocales)
        assertEquals(Language("pt-BR"), phone.appLanguage)

        phone.appLanguage = null
        assertTrue(localeManager.applicationLocales.isEmpty)
    }

    @Test
    fun `on Android 13 and later a language chosen on Android's page is what the repository reports`() = runTest {
        val languages = LanguageRepository(
            settingsRepository(folder.root, backgroundScope),
            phone,
            File(folder.root, "language_handed_over")
        )
        languages.syncWithPhone()

        context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags("de")
        languages.syncWithPhone()

        assertEquals(Language("de"), languages.language.first())
    }

    @Test
    @Config(sdk = [29], qualifiers = "fr-rFR")
    fun `below Android 13 there is no per-app setting, and the phone's languages are the system's`() {
        assertFalse(phone.hasAppLanguage)
        assertEquals(listOf(Locale.FRANCE), phone.locales)

        phone.appLanguage = Language("de")

        assertEquals(null, phone.appLanguage)
    }
}
