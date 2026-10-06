package com.tonypine.cycle.feature.history

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.repository.ContraceptionRepository
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LogCategory
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.PeriodRefusal
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * History and the cycle detail from a real log: the repositories on an in-memory database and a
 * DataStore file. An edit to the log, from Today or the calendar, updates both.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class HistoryViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private var clock = HistorySamples.today

    private val database = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CycleDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dayLogs = DayLogRepository(database)
    private val contraception = ContraceptionRepository(database)
    private lateinit var settings: SettingsRepository

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `with nothing logged, History is empty`() = history { cycles ->
        val viewModel = HistoryViewModel(cycles) { clock }.also { follow(it.uiState) }

        assertEquals(HistoryUiState.Empty(hasPeriod = false), viewModel.uiState.first { it != HistoryUiState.Loading })
    }

    @Test
    fun `her log gives her typical cycle and her cycles, and an edit updates them`() = history { cycles ->
        syntheticHistory.forEach { dayLogs.save(it) }
        val viewModel = HistoryViewModel(cycles) { clock }.also { follow(it.uiState) }
        assertEquals(HistorySamples.cycles, viewModel.uiState.first { it is HistoryUiState.Cycles })

        // A period logged today starts a new cycle: September's is now complete, 8 days long.
        dayLogs.setFlow(clock, FlowLevel.HEAVY)

        val edited = viewModel.uiState.first { it is HistoryUiState.Cycles && it.cycles.size == 8 }
        edited as HistoryUiState.Cycles
        assertEquals(listOf(clock, day("2027-09-02")), edited.cycles.take(2).map { it.start })
        assertEquals(8, edited.cycles[1].length)
        assertEquals(day("2027-09-09"), edited.cycles[1].end)
    }

    @Test
    fun `setting the implant marks its time and leaves it out, and deleting its dates brings it all back`() =
        history { cycles ->
            clock = day("2027-10-14")
            syntheticBeforeAndOnImplant.forEach { dayLogs.save(it) }
            val viewModel = HistoryViewModel(cycles) { clock }.also { follow(it.uiState) }
            val before = viewModel.uiState.first { it is HistoryUiState.Cycles } as HistoryUiState.Cycles
            assertFalse(before.leftOut)

            // Settings › Contraception: the implant, fitted on 9 November 2026.
            contraception.start(ContraceptionMethod.IMPLANT, breaks = null, started = day("2026-11-09"), today = clock)
            val marked = viewModel.uiState.first { (it as? HistoryUiState.Cycles)?.leftOut == true }
            val id = contraception.observeStretches().first().single().id
            val implant = HistorySamples.onImplant.entries.first() as MethodSummary
            assertEquals(
                HistorySamples.onImplant.copy(
                    entries = listOf(implant.copy(stretch = implant.stretch.copy(id = id))) +
                        HistorySamples.onImplant.entries.drop(1)
                ),
                marked
            )

            contraception.delete(id)
            assertEquals(before, viewModel.uiState.first { (it as? HistoryUiState.Cycles)?.leftOut == false })
        }

    @Test
    fun `the detail follows an edit to its period, and goes missing when its start moves`() = history { cycles ->
        logSyntheticHistory()
        val viewModel = CycleDetailViewModel(cycles, dayLogs, day("2027-08-05")) { clock }.also { follow(it.uiState) }
        assertEquals(HistorySamples.pastCycle, viewModel.uiState.first { it is CycleDetailUiState.Detail })

        // She fills in the day she missed.
        dayLogs.setFlow(day("2027-08-09"), FlowLevel.MEDIUM)
        val filled = viewModel.uiState.first {
            it is CycleDetailUiState.Detail && it.flow[4].flow == FlowLevel.MEDIUM
        } as CycleDetailUiState.Detail
        assertEquals(FlowDay(day("2027-08-09"), FlowLevel.MEDIUM), filled.flow[4])

        // She moves the period's start a day earlier: no cycle starts on August 5 any more.
        dayLogs.setFlow(day("2027-08-04"), FlowLevel.LIGHT)
        assertEquals(CycleDetailUiState.Missing, viewModel.uiState.first { it == CycleDetailUiState.Missing })
    }

    @Test
    fun `the detail shows how she felt, and follows what she hides and shows again`() = history { cycles ->
        logSyntheticHistory()
        val viewModel = CycleDetailViewModel(cycles, dayLogs, day("2027-08-05")) { clock }.also { follow(it.uiState) }
        assertEquals(HistorySamples.pastCycle, viewModel.uiState.first { it is CycleDetailUiState.Detail })
        val bloating = Symptom.Body(BodySymptom.BLOATING)

        // In "What to log", she turns Body off: bloating goes, nothing is deleted.
        settings.setCategoryShown(LogCategory.BODY, shown = false)
        val hidden = viewModel.uiState.first {
            it is CycleDetailUiState.Detail && it.symptoms.none { item -> item.symptom == bloating }
        } as CycleDetailUiState.Detail
        assertEquals(8, hidden.symptoms.size)

        // She turns it back on: it comes back as it was.
        settings.setCategoryShown(LogCategory.BODY, shown = true)
        assertEquals(
            HistorySamples.pastCycle,
            viewModel.uiState.first {
                it is CycleDetailUiState.Detail &&
                    it.symptoms.any { item -> item.symptom == bloating }
            }
        )
    }

    @Test
    fun `deleting a period joins its cycle to the one before, and keeps how she felt`() = history { cycles ->
        logSyntheticHistory()
        val history = HistoryViewModel(cycles) { clock }.also { follow(it.uiState) }
        val viewModel = CycleDetailViewModel(cycles, dayLogs, day("2027-08-05")) { clock }.also { follow(it.uiState) }
        viewModel.uiState.first { it is CycleDetailUiState.Detail }
        val deleted = CompletableDeferred<Unit>()

        viewModel.deletePeriod { deleted.complete(Unit) }

        deleted.await()
        val state = history.uiState.first {
            it is HistoryUiState.Cycles && it.cycles.size == 6
        } as HistoryUiState.Cycles
        // July's cycle now runs to the day before September's period: 55 days.
        assertEquals(day("2027-07-09"), state.cycles[1].start)
        assertEquals(55, state.cycles[1].length)
        assertEquals(syntheticFeelings, dayLogs.observeFeelings().first())
    }

    @Test
    fun `saving new period dates moves the period, and History follows`() = history { cycles ->
        logSyntheticHistory()
        val viewModel = EditPeriodViewModel(cycles, dayLogs, day("2027-08-05")) { clock }.also { follow(it.uiState) }
        viewModel.uiState.first { it is EditPeriodUiState.Editing }
        val saved = CompletableDeferred<LocalDate>()

        viewModel.pick(day("2027-08-03"))
        viewModel.pick(day("2027-08-08"))
        viewModel.save { saved.complete(it) }

        assertEquals(day("2027-08-03"), saved.await())
        val detail = CycleDetailViewModel(cycles, dayLogs, day("2027-08-03")) { clock }.also { follow(it.uiState) }
        val state = detail.uiState.first { it is CycleDetailUiState.Detail } as CycleDetailUiState.Detail
        assertEquals(Period(day("2027-08-03"), day("2027-08-08")), state.cycle.period)
        // July's cycle ends the day before: 25 days.
        val history = HistoryViewModel(cycles) { clock }.also { follow(it.uiState) }
        val july = (history.uiState.first { it is HistoryUiState.Cycles } as HistoryUiState.Cycles).cycles[2]
        assertEquals(25, july.length)
    }

    @Test
    fun `new period dates that run into another period are refused, and nothing changes`() = history { cycles ->
        logSyntheticHistory()
        val viewModel = EditPeriodViewModel(cycles, dayLogs, day("2027-08-05")) { clock }.also { follow(it.uiState) }
        viewModel.uiState.first { it is EditPeriodUiState.Editing }
        var saved = false

        viewModel.pick(day("2027-07-12"))
        viewModel.save { saved = true }

        val refused = viewModel.uiState.first {
            it is EditPeriodUiState.Editing && it.draft.refusal != null
        } as EditPeriodUiState.Editing
        assertEquals(PeriodRefusal.TooClose(Period(day("2027-07-09"), day("2027-07-13"))), refused.draft.refusal)
        assertFalse(saved)
        assertEquals(syntheticHistory, dayLogs.observeDayLogs().first())
    }

    @Test
    fun `a new day moves the current cycle on`() = history { cycles ->
        syntheticHistory.forEach { dayLogs.save(it) }
        val viewModel = HistoryViewModel(cycles) { clock }.also { follow(it.uiState) }
        viewModel.uiState.first { it is HistoryUiState.Cycles }

        clock = clock.plusDays(1)
        viewModel.refreshDay()

        val state = viewModel.uiState.first { it is HistoryUiState.Cycles && it.today == clock }
        assertEquals(10, (state as HistoryUiState.Cycles).cycles.first().length)
    }

    /** Runs [test] with a [CycleRepository] on the test database and [settings]. */
    private fun history(test: suspend TestScope.(CycleRepository) -> Unit) = runTest {
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        test(CycleRepository(dayLogs, settings, contraception))
    }

    /** Logs [syntheticHistory] and how she felt, [syntheticFeelings], as the day log sheet does. */
    private suspend fun logSyntheticHistory() {
        syntheticHistory.forEach { dayLogs.save(it) }
        syntheticFeelings.forEach { dayLogs.logDay(it.date, dayLogs.get(it.date).flow, it) }
    }

    /** Collects [state] as the screen would, so it keeps following the log. */
    private fun TestScope.follow(state: StateFlow<*>) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    }
}
