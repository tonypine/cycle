package com.tonypine.cycle.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.export.ImportProblem
import com.tonypine.cycle.core.data.export.YourDataRepository
import com.tonypine.cycle.core.data.repository.ContraceptionRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
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
    private val contraception = ContraceptionRepository(database)
    private val today = LocalDate.of(2027, 3, 20)
    private lateinit var settings: SettingsRepository
    private val deviceLock = FakeDeviceLock()

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
        val viewModel =
            SettingsViewModel(settings, YourDataRepository(database, settings), deviceLock, clock = { today })
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

        assertEquals(DataDialog.NothingToImport(), viewModel.awaitDialog())
    }

    @Test
    fun `a file whose days and methods are all on the phone says so`() = runTest {
        val viewModel = settingsViewModel()
        logTwoDays()
        contraception.start(ContraceptionMethod.IMPLANT, null, LocalDate.of(2026, 11, 9), today = today)
        val export = ByteArrayOutputStream().also { YourDataRepository(database, settings).export(it) }.toByteArray()

        viewModel.onImport { ByteArrayInputStream(export) }

        assertEquals(DataDialog.NothingToImport(hasMethods = true), viewModel.awaitDialog())
        assertEquals(1, contraception.observeStretches().first().size)
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
        assertEquals(DataDialog.ConfirmImport(newDays = 0, restoresLengths = true), viewModel.awaitDialog())

        var restored = false
        viewModel.onConfirmImport(onImported = { restored = true })

        val state = viewModel.uiState.first { it?.usualCycleLength == 31 }
        assertEquals(SettingsUiState(31, 6, lastExported = null), state)
        assertTrue(settings.settings.first().setupDone)
        assertTrue(restored)
    }

    @Test
    fun `a file with only a method not on the phone asks to import it, then adds it and says so`() = runTest {
        val viewModel = settingsViewModel()
        settings.saveSetup(cycleLength = 31, periodLength = 6)
        logTwoDays()
        val implant = ContraceptionStretch(ContraceptionMethod.IMPLANT, LocalDate.of(2026, 11, 9))
        contraception.start(implant.method, null, implant.started, today = today)
        val export = ByteArrayOutputStream().also { YourDataRepository(database, settings).export(it) }.toByteArray()
        contraception.delete(contraception.observeStretches().first().single().id)

        viewModel.onImport { ByteArrayInputStream(export) }
        assertEquals(
            DataDialog.ConfirmImport(newDays = 0, newStretches = 1, hasMethods = true),
            viewModel.awaitDialog()
        )

        viewModel.onConfirmImport()

        val state = viewModel.uiState.first { it?.importedMethods != null }
        assertEquals(1, state?.importedMethods)
        assertEquals(null, state?.importedDays)
        assertEquals(listOf(implant), contraception.observeStretches().first().map { it.copy(id = 0) })
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

    @Test
    fun `lock cycle is off, and turns on only once she unlocked the phone's prompt`() = runTest {
        val viewModel = settingsViewModel()
        assertFalse(viewModel.uiState.first { it != null }!!.appLock)

        viewModel.onAppLockChange(true)

        assertTrue(deviceLock.prompting)
        assertFalse(settings.appLock.first())
        deviceLock.answer(unlocked = true)
        assertTrue(viewModel.uiState.first { it?.appLock == true }!!.appLock)
        assertTrue(settings.appLock.first())
    }

    @Test
    fun `a cancelled prompt leaves lock cycle as it was, on or off`() = runTest {
        val viewModel = settingsViewModel()

        viewModel.onAppLockChange(true)
        deviceLock.answer(unlocked = false)
        assertFalse(settings.appLock.first())

        settings.setAppLock(true)
        viewModel.onAppLockChange(false)
        deviceLock.answer(unlocked = false)
        assertTrue(settings.appLock.first())
        assertTrue(viewModel.uiState.first { it != null }!!.appLock)
        assertEquals(2, deviceLock.prompts)
    }

    @Test
    fun `turning lock cycle off asks for the phone's lock too`() = runTest {
        val viewModel = settingsViewModel()
        settings.setAppLock(true)

        viewModel.onAppLockChange(false)

        assertTrue(settings.appLock.first())
        deviceLock.answer(unlocked = true)
        assertFalse(settings.appLock.first { !it })
    }

    @Test
    fun `a second tap while the prompt shows asks nothing more`() = runTest {
        val viewModel = settingsViewModel()

        viewModel.onAppLockChange(true)
        viewModel.onAppLockChange(true)
        viewModel.onAppLockChange(false)

        assertEquals(1, deviceLock.prompts)
        deviceLock.answer(unlocked = true)
        assertTrue(settings.appLock.first { it })
        // Once it is answered, she can tap again.
        viewModel.onAppLockChange(false)
        assertEquals(2, deviceLock.prompts)
    }

    @Test
    fun `with no screen lock on the phone, lock cycle can't turn on and says why`() = runTest {
        val viewModel = settingsViewModel()
        deviceLock.available = false

        viewModel.onAppLockChange(true)

        assertEquals(DataDialog.NoScreenLock, viewModel.awaitDialog())
        assertEquals(0, deviceLock.prompts)
        assertFalse(settings.appLock.first())
        viewModel.onDismissDialog()
        assertEquals(null, viewModel.uiState.first { it?.dialog == null }!!.dialog)
    }

    @Test
    fun `with no screen lock on the phone, an on lock turns off without a prompt`() = runTest {
        val viewModel = settingsViewModel()
        settings.setAppLock(true)
        deviceLock.available = false

        viewModel.onAppLockChange(false)

        assertFalse(settings.appLock.first { !it })
        assertEquals(0, deviceLock.prompts)
    }

    @Test
    fun `the note that the lock turned itself off shows until she dismisses it`() = runTest {
        val viewModel = settingsViewModel()
        settings.setAppLock(true)

        settings.turnOffAppLockWithoutScreenLock()

        val state = viewModel.uiState.first { it?.appLockTurnedOff == true }!!
        assertFalse(state.appLock)
        viewModel.onDismissLockNote()
        assertFalse(viewModel.uiState.first { it?.appLockTurnedOff == false }!!.appLockTurnedOff)
        assertFalse(settings.appLockTurnedOff.first())
    }
}
