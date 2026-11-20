package com.tonypine.cycle.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.LanguageRepository
import com.tonypine.cycle.core.data.settings.PhoneLanguages
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Language
import java.io.File
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * The Language row and page on the real repositories: a DataStore file, and a phone whose languages
 * and per-app language the test sets. Synthetic days and notes only.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LanguageViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val german = Language("de")
    private val spanish = Language("es")
    private val phone = TestPhone()
    private lateinit var settings: SettingsRepository
    private lateinit var languages: LanguageRepository

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    private fun TestScope.languageViewModel(): LanguageViewModel {
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        languages = LanguageRepository(settings, phone, File(folder.root, "no_backup/language_handed_over"))
        val viewModel = LanguageViewModel(languages)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun LanguageViewModel.state(): LanguageUiState = uiState.first { it != null }!!

    @Test
    fun `with nothing chosen she follows the phone`() = runTest {
        val state = languageViewModel().state()

        assertEquals(LanguageUiState(chosen = null, phoneLanguage = Language.English, phoneLocale = Locale.US), state)
        assertEquals(Language.English, state.current)
        assertFalse(state.phoneNotInCycle)
    }

    @Test
    fun `choosing a language applies it at once, and the phone's language follows the phone again`() = runTest {
        val viewModel = languageViewModel()

        viewModel.onSelect(german)
        assertEquals(german, viewModel.uiState.first { it?.chosen == german }!!.current)
        assertEquals(german, settings.language.first())
        // On Android 13 and later Android applies it, and recreates the activity.
        assertEquals(german, phone.appLanguage)

        viewModel.onSelect(null)
        val following = viewModel.uiState.first { it?.chosen == null }!!
        assertEquals(Language.English, following.current)
        assertEquals(null, phone.appLanguage)
    }

    @Test
    fun `a phone in a language Cycle doesn't have uses the first one it has, or English`() = runTest {
        phone.locales = listOf(Locale.FRANCE, Locale.forLanguageTag("es-ES"))
        val state = languageViewModel().state()

        assertEquals(spanish, state.phoneLanguage)
        assertEquals(Locale.FRANCE, state.phoneLocale)
        assertTrue(state.phoneNotInCycle)
    }

    @Test
    fun `Portuguese from Portugal counts as Cycle's Portuguese`() = runTest {
        phone.locales = listOf(Locale.forLanguageTag("pt-PT"))
        val state = languageViewModel().state()

        assertEquals(Language("pt-BR"), state.phoneLanguage)
        assertFalse(state.phoneNotInCycle)
    }

    @Test
    fun `a change to the phone's languages shows when Cycle comes back to the front`() = runTest {
        val viewModel = languageViewModel()
        assertEquals(Language.English, viewModel.state().phoneLanguage)

        phone.locales = listOf(Locale.forLanguageTag("es-ES"), Locale.UK)
        viewModel.onResume()

        assertEquals(spanish, viewModel.uiState.first { it?.phoneLanguage == spanish }!!.current)
    }

    @Test
    fun `on Android 13 and later a language chosen on Android's page shows once Cycle syncs`() = runTest {
        val viewModel = languageViewModel()
        languages.syncWithPhone()

        phone.appLanguage = spanish
        languages.syncWithPhone()

        assertEquals(spanish, viewModel.uiState.first { it?.chosen == spanish }!!.current)
    }

    @Test
    fun `switching languages changes nothing she logged, her notes included`() = runTest {
        val database = Room
            .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CycleDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val days = DayLogRepository(database)
            val day = LocalDate.of(2027, 3, 3)
            val feelings = DayFeelings(day, note = "Synthetic note: long walk, then an early night.")
            days.logDay(day, FlowLevel.MEDIUM, feelings)
            val logged = days.get(day) to days.getFeelings(day)
            val viewModel = languageViewModel()

            viewModel.onSelect(german)
            viewModel.uiState.first { it?.chosen == german }
            viewModel.onSelect(null)
            viewModel.uiState.first { it?.chosen == null }

            assertEquals(DayLog(day, FlowLevel.MEDIUM), logged.first)
            assertEquals(feelings.note, logged.second.note)
            assertEquals(logged, days.get(day) to days.getFeelings(day))
        } finally {
            database.close()
        }
    }

    /** A phone on Android 13 or later, in [locales], with Android's per-app language for Cycle. */
    private class TestPhone : PhoneLanguages {
        override var locales: List<Locale> = listOf(Locale.US)
        override val hasAppLanguage: Boolean = true
        override var appLanguage: Language? = null
    }
}
