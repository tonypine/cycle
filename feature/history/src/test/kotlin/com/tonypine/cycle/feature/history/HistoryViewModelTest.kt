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
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LogCategory
import java.io.File
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
    fun `the detail follows an edit to its period, and goes missing when its start moves`() = history { cycles ->
        logSyntheticHistory()
        val viewModel = CycleDetailViewModel(cycles, day("2027-08-05")) { clock }.also { follow(it.uiState) }
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
        val viewModel = CycleDetailViewModel(cycles, day("2027-08-05")) { clock }.also { follow(it.uiState) }
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
