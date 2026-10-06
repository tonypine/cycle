package com.tonypine.cycle.core.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.FakePhoneLanguages
import com.tonypine.cycle.core.data.settingsRepository
import com.tonypine.cycle.core.model.Language
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/** Cycle's language and its hand-over to Android's per-app setting, as `0008-languages.md` decides. */
@RunWith(AndroidJUnit4::class)
class LanguageRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val german = Language("de")
    private val marker get() = File(folder.root, "no_backup/language_handed_over")

    private fun TestScope.languages(phone: PhoneLanguages, settings: SettingsRepository = settings()) =
        LanguageRepository(settings, phone, marker)

    private fun TestScope.settings() = settingsRepository(folder.root, backgroundScope)

    @Test
    fun `follows the phone until she chooses, and again once she clears it`() = runTest {
        val languages = languages(FakePhoneLanguages(hasAppLanguage = false))
        assertEquals(null, languages.language.first())

        languages.setLanguage(german)
        assertEquals(german, languages.language.first())

        languages.setLanguage(null)
        assertEquals(null, languages.language.first())
    }

    @Test
    fun `her choice survives a process restart`() = runTest {
        val file = File(folder.root, "settings.preferences_pb")
        val firstProcess = CoroutineScope(backgroundScope.coroutineContext + Job())
        SettingsRepository(PreferenceDataStoreFactory.create(scope = firstProcess) { file }).setLanguage(german)
        firstProcess.coroutineContext[Job]!!.cancelAndJoin()

        val restarted = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })

        assertEquals(german, languages(FakePhoneLanguages(), restarted).language.first())
    }

    @Test
    fun `the phone's language is the first of hers that Cycle has, English until Cycle has more`() = runTest {
        val frenchThenSpanish = listOf(Locale.forLanguageTag("fr-FR"), Locale.forLanguageTag("es-ES"))

        assertEquals(Language.English, languages(FakePhoneLanguages(frenchThenSpanish)).phoneLanguage())
    }

    @Test
    fun `below Android 13 the choice stays Cycle's own`() = runTest {
        val phone = FakePhoneLanguages(hasAppLanguage = false)
        val languages = languages(phone)

        languages.setLanguage(german)
        languages.syncWithPhone()

        assertEquals(german, languages.language.first())
        assertEquals(null, phone.appLanguage)
        assertFalse(marker.exists())
    }

    @Test
    fun `on Android 13 and later choosing sets Android's per-app language too`() = runTest {
        val phone = FakePhoneLanguages()
        val languages = languages(phone)

        languages.setLanguage(german)
        assertEquals(german, phone.appLanguage)

        languages.setLanguage(null)
        assertEquals(null, phone.appLanguage)
    }

    @Test
    fun `a language set on Android's per-app page is what the repository reports`() = runTest {
        val phone = FakePhoneLanguages()
        val languages = languages(phone)
        languages.syncWithPhone()

        phone.appLanguage = german
        languages.syncWithPhone()

        assertEquals(german, languages.language.first())
    }

    @Test
    fun `the first start on Android 13 hands her stored choice to Android`() = runTest {
        val phone = FakePhoneLanguages(appLanguage = null)
        val settings = settings()
        settings.setLanguage(german)
        val languages = languages(phone, settings)
        assertFalse(languages.isHandedOver())

        languages.syncWithPhone()

        assertEquals(german, phone.appLanguage)
        assertEquals(german, languages.language.first())
        assertTrue(languages.isHandedOver())
    }

    @Test
    fun `once handed over, System default on Android's page clears Cycle's copy`() = runTest {
        val phone = FakePhoneLanguages(appLanguage = null)
        val settings = settings()
        settings.setLanguage(german)
        marker.parentFile!!.mkdirs()
        marker.createNewFile()

        languages(phone, settings).syncWithPhone()

        assertEquals(null, phone.appLanguage)
        assertEquals(null, settings.language.first())
    }

    @Test
    fun `on the first start, a language Android already has wins and is copied`() = runTest {
        val phone = FakePhoneLanguages(appLanguage = Language("es"))
        val settings = settings()
        settings.setLanguage(german)
        val languages = languages(phone, settings)

        languages.syncWithPhone()

        assertEquals(Language("es"), phone.appLanguage)
        assertEquals(Language("es"), settings.language.first())
        assertTrue(languages.isHandedOver())
    }

    @Test
    fun `delete everything empties Android's per-app language`() = runTest {
        val phone = FakePhoneLanguages(appLanguage = german)

        languages(phone).forget()

        assertEquals(null, phone.appLanguage)
    }
}
