package com.tonypine.cycle.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.export.ImportProblem
import com.tonypine.cycle.core.data.export.YourDataRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDate
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

/** Settings on the real repositories: an in-memory database and a DataStore file. Synthetic days only. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SettingsViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CycleDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dayLogs = DayLogRepository(database)
    private val today = LocalDate.of(2027, 3, 20)
    private lateinit var settings: SettingsRepository

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    private fun TestScope.settingsViewModel(): SettingsViewModel {
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        val viewModel = SettingsViewModel(settings, YourDataRepository(database, settings), clock = { today })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel
    }

    private suspend fun SettingsViewModel.awaitDialog(): DataDialog? = uiState.first { it?.dialog != null }?.dialog

    private suspend fun logTwoDays() {
        dayLogs.setPeriodStarted(LocalDate.of(2027, 3, 2), started = true)
        dayLogs.setFlow(LocalDate.of(2027, 3, 3), FlowLevel.HEAVY)
    }

    @Test
    fun `her usual lengths show, and the export suggests today's file name`() = runTest {
        val viewModel = settingsViewModel()
        settings.saveSetup(cycleLength = 31, periodLength = 6)

        val state = viewModel.uiState.first { it?.usualCycleLength == 31 }
        assertEquals(SettingsUiState(31, 6, lastExported = null), state)
        assertEquals("cycle-export-2027-03-20.csv", viewModel.exportFileName())
    }

    @Test
    fun `export writes her days to the file she picked and remembers today`() = runTest {
        val viewModel = settingsViewModel()
        logTwoDays()
        val file = ByteArrayOutputStream()

        viewModel.onExport { file }

        assertEquals(today, viewModel.uiState.first { it?.lastExported != null }?.lastExported)
        assertEquals(
            listOf(
                "date,flow,period_started,period_ended,pain,pain_where,body,mood,energy,sleep,sex,note",
                "2027-03-02,,yes,,,,,,,,,",
                "2027-03-03,heavy,,,,,,,,,,"
            ),
            file.toString(Charsets.UTF_8.name()).trimEnd().lines().map { it.trimEnd('\r') }
        )
    }

    @Test
    fun `a file that can't be written says so, and the last export stays`() = runTest {
        val viewModel = settingsViewModel()
        val broken = object : OutputStream() {
            override fun write(b: Int) = throw IOException("Synthetic failure")
        }

        viewModel.onExport { broken }
        assertEquals(DataDialog.ExportFailed, viewModel.awaitDialog())
        viewModel.onDismissDialog()

        viewModel.onExport { throw FileNotFoundException("Synthetic failure") }
        assertEquals(DataDialog.ExportFailed, viewModel.awaitDialog())
        assertEquals(null, settings.lastExported.first())
    }

    @Test
    fun `import asks about the new days, then adds them and says how many`() = runTest {
        val viewModel = settingsViewModel()
        logTwoDays()
        val export = ByteArrayOutputStream().also { YourDataRepository(database, settings).export(it) }.toByteArray()
        dayLogs.clear(LocalDate.of(2027, 3, 3))
        dayLogs.setFlow(LocalDate.of(2027, 3, 2), FlowLevel.LIGHT)

        viewModel.onImport { ByteArrayInputStream(export) }
        assertEquals(DataDialog.ConfirmImport(newDays = 1), viewModel.awaitDialog())

        var imported = false
        viewModel.onConfirmImport(onImported = { imported = true })

        assertEquals(1, viewModel.uiState.first { it?.importedDays != null }?.importedDays)
        assertTrue(imported)
        assertEquals(null, viewModel.uiState.value?.dialog)
        assertEquals(
            listOf(
                DayLog(LocalDate.of(2027, 3, 2), FlowLevel.LIGHT, periodStarted = true),
                DayLog(LocalDate.of(2027, 3, 3), FlowLevel.HEAVY)
            ),
            dayLogs.observeDayLogs().first()
        )
    }

    @Test
    fun `cancel imports nothing`() = runTest {
        val viewModel = settingsViewModel()
        logTwoDays()
        val export = ByteArrayOutputStream().also { YourDataRepository(database, settings).export(it) }.toByteArray()
        dayLogs.clear(LocalDate.of(2027, 3, 3))

        viewModel.onImport { ByteArrayInputStream(export) }
        viewModel.awaitDialog()
        viewModel.onDismissDialog()
        viewModel.onConfirmImport()

        assertEquals(listOf(LocalDate.of(2027, 3, 2)), dayLogs.observeDayLogs().first().map { it.date })
        assertEquals(null, viewModel.uiState.value?.importedDays)
    }

    @Test
    fun `a file with nothing new says so`() = runTest {
        val viewModel = settingsViewModel()
        logTwoDays()
        val export = ByteArrayOutputStream().also { YourDataRepository(database, settings).export(it) }.toByteArray()

        viewModel.onImport { ByteArrayInputStream(export) }

        assertEquals(DataDialog.NothingToImport, viewModel.awaitDialog())
    }

    @Test
    fun `a file with only the usual lengths asks to import them and restores them`() = runTest {
        val viewModel = settingsViewModel()
        settings.saveSetup(cycleLength = 31, periodLength = 6)
        settings.setWelcomeDone(true)
        val export = ByteArrayOutputStream().also { YourDataRepository(database, settings).export(it) }.toByteArray()
        viewModel.onDeleteEverything()
        settings.welcomeDone.first { !it }

        viewModel.onImport { ByteArrayInputStream(export) }
        assertEquals(DataDialog.ConfirmImport(newDays = 0), viewModel.awaitDialog())

        var restored = false
        viewModel.onConfirmImport(onImported = { restored = true })

        val state = viewModel.uiState.first { it?.usualCycleLength == 31 }
        assertEquals(SettingsUiState(31, 6, lastExported = null), state)
        assertTrue(settings.settings.first().setupDone)
        assertTrue(restored)
    }

    @Test
    fun `a malformed or unreadable file is refused with its problem`() = runTest {
        val viewModel = settingsViewModel()

        viewModel.onImport { ByteArrayInputStream("Date,Flow\n2027-03-02,light\n".encodeToByteArray()) }
        assertEquals(DataDialog.ImportRefused(ImportProblem.NotAnExport), viewModel.awaitDialog())
        viewModel.onDismissDialog()

        viewModel.onImport { throw FileNotFoundException("Synthetic failure") }
        assertEquals(DataDialog.ImportRefused(ImportProblem.Unreadable), viewModel.awaitDialog())
        viewModel.onDismissDialog()

        viewModel.onImport { null }
        assertEquals(DataDialog.ImportRefused(ImportProblem.Unreadable), viewModel.awaitDialog())
    }

    @Test
    fun `delete everything leaves no days and no settings`() = runTest {
        val viewModel = settingsViewModel()
        logTwoDays()
        settings.saveSetup(cycleLength = 31, periodLength = 6)
        settings.setWelcomeDone(true)
        viewModel.onExport { ByteArrayOutputStream() }
        settings.lastExported.first { it != null }

        viewModel.onDeleteEverything()

        assertFalse(settings.welcomeDone.first { !it })
        assertEquals(emptyList<DayLog>(), dayLogs.observeDayLogs().first())
        assertEquals(
            SettingsUiState(28, 5, lastExported = null),
            viewModel.uiState.first {
                it?.usualCycleLength == 28
            }
        )
    }
}
