package com.tonypine.cycle.feature.calendar

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.ui.DayLogEntry
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * The calendar from a real log: the repositories on an in-memory database and a DataStore file.
 * Synthetic dates only: made-up days in 2027, never anyone's real cycle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CalendarViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val today = day("2027-03-20")

    private val database = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CycleDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dayLogs = DayLogRepository(database.dayLogDao())

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `the calendar opens on this month, with nothing selected`() = calendar { viewModel, _ ->
        val state = viewModel.awaitState()
        assertEquals(YearMonth.of(2027, 3), state.month)
        assertEquals(today, state.today)
        assertNull(state.selected)
        assertNull(state.selectedLog)
    }

    @Test
    fun `a period across two months shows in both, with three predicted after it`() = calendar { viewModel, _ ->
        logPeriod(day("2027-02-26"), day("2027-03-02"))

        val state = viewModel.awaitState { it.days.periods.isNotEmpty() }
        assertEquals(listOf(Period(day("2027-02-26"), day("2027-03-02"))), state.days.periods)
        listOf("2027-02-26", "2027-02-28", "2027-03-01", "2027-03-02").forEach {
            assertEquals(it, CycleDayState.Period, state.days.stateOf(day(it)))
        }
        assertEquals(CycleDayState.Plain, state.days.stateOf(day("2027-03-03")))
        // A typical 28-day cycle: 26 March, 23 April and 21 May, five days each.
        listOf("2027-03-26", "2027-03-30", "2027-04-23", "2027-05-21", "2027-05-25").forEach {
            assertEquals(it, CycleDayState.PredictedPeriod, state.days.stateOf(day(it)))
        }
        assertEquals(CycleDayState.Plain, state.days.stateOf(day("2027-06-18")))
    }

    @Test
    fun `logging a past day saves its flow, and changing it takes it back off the period`() = calendar { viewModel, _ ->
        viewModel.onDayClick(day("2027-03-10"))
        assertEquals(DayLogEntry(day("2027-03-10"), fillDays = 5), viewModel.awaitState().selectedLog)

        viewModel.onLogDay(day("2027-03-10"), FlowLevel.MEDIUM)
        val logged = viewModel.awaitState { it.days.periods.isNotEmpty() }
        assertEquals(CycleDayState.Period, logged.days.stateOf(day("2027-03-10")))
        assertEquals(FlowLevel.MEDIUM, logged.selectedLog?.flow)
        assertEquals(day("2027-04-07"), logged.days.predicted.first().start)

        viewModel.onLogDay(day("2027-03-10"), FlowLevel.NONE)
        val changed = viewModel.awaitState { it.days.periods.isEmpty() }
        assertEquals(CycleDayState.Plain, changed.days.stateOf(day("2027-03-10")))
        assertEquals(emptyList<ClosedRange<LocalDate>>(), changed.days.predicted)
    }

    @Test
    fun `filling a forgotten period logs N days and recomputes the estimate`() = calendar { viewModel, _ ->
        logPeriod(day("2027-03-02"), day("2027-03-06"))
        viewModel.awaitState { it.days.periods.isNotEmpty() }

        viewModel.onDayClick(day("2027-02-04"))
        assertEquals(5, viewModel.awaitState().selectedLog?.fillDays)
        viewModel.onFillPeriod(day("2027-02-04"))

        val state = viewModel.awaitState { it.days.periods.size == 2 }
        assertEquals(Period(day("2027-02-04"), day("2027-02-08")), state.days.periods.first())
        (4..8).forEach { assertEquals(CycleDayState.Period, state.days.stateOf(day("2027-02-0$it"))) }
        // Her one cycle, 4 February to 1 March, lasted 26 days: the next period moves from 30 to 28 March.
        assertEquals(day("2027-03-28"), state.days.predicted.first().start)
    }

    @Test
    fun `no fill next to a period`() = calendar { viewModel, _ ->
        logPeriod(day("2027-03-02"), day("2027-03-06"))
        viewModel.awaitState { it.days.periods.isNotEmpty() }

        viewModel.onDayClick(day("2027-03-07"))

        assertNull(viewModel.awaitState { it.selected != null }.selectedLog?.fillDays)
    }

    @Test
    fun `clearing a filled day takes only that day off the period`() = calendar { viewModel, _ ->
        viewModel.onFillPeriod(day("2027-02-04"))
        viewModel.awaitState { it.days.periods.isNotEmpty() }

        viewModel.onDayClick(day("2027-02-06"))
        assertEquals(true, viewModel.awaitState { it.selected != null }.selectedLog?.canClear)
        viewModel.onClearDay(day("2027-02-06"))

        val middle = viewModel.awaitState { it.days.stateOf(day("2027-02-06")) == CycleDayState.Plain }
        listOf("2027-02-04", "2027-02-05", "2027-02-07", "2027-02-08").forEach {
            assertEquals(it, CycleDayState.Period, middle.days.stateOf(day(it)))
        }

        viewModel.onClearDay(day("2027-02-04"))
        val first = viewModel.awaitState { it.days.stateOf(day("2027-02-04")) == CycleDayState.Plain }
        assertEquals(Period(day("2027-02-05"), day("2027-02-08")), first.days.periods.single())

        viewModel.onClearDay(day("2027-02-08"))
        val last = viewModel.awaitState { it.days.stateOf(day("2027-02-08")) == CycleDayState.Plain }
        assertEquals(Period(day("2027-02-05"), day("2027-02-07")), last.days.periods.single())
    }

    @Test
    fun `days after today are not selected, logged or cleared`() = calendar { viewModel, _ ->
        // Logged before the phone's clock went back a day.
        dayLogs.setFlow(today.plusDays(2), FlowLevel.LIGHT)
        viewModel.awaitState()

        viewModel.onDayClick(today.plusDays(1))
        viewModel.onLogDay(today.plusDays(1), FlowLevel.HEAVY)
        viewModel.onFillPeriod(today.plusDays(1))
        viewModel.onClearDay(today.plusDays(2))
        // Written after the others, in order, so once it shows, they have run.
        viewModel.onLogDay(today, FlowLevel.SPOTTING)
        viewModel.awaitState { it.days.withoutPeriodFlow.isNotEmpty() }

        assertNull(viewModel.awaitState().selected)
        assertEquals(
            listOf(DayLog(today, flow = FlowLevel.SPOTTING), DayLog(today.plusDays(2), flow = FlowLevel.LIGHT)),
            dayLogs.observeDayLogs().first()
        )
    }

    @Test
    fun `months move back and forth, to a month asked for, and back to today`() = calendar { viewModel, _ ->
        viewModel.onPreviousMonth()
        assertEquals(YearMonth.of(2027, 2), viewModel.awaitState().month)
        viewModel.onNextMonth()
        viewModel.onNextMonth()
        assertEquals(YearMonth.of(2027, 4), viewModel.awaitState().month)
        viewModel.showMonth(YearMonth.of(2026, 12))
        assertEquals(YearMonth.of(2026, 12), viewModel.awaitState().month)
        viewModel.onGoToToday()
        assertEquals(YearMonth.of(2027, 3), viewModel.awaitState().month)
    }

    /** Runs [test] with a ViewModel on the test database, its state collected as the screen would. */
    private fun calendar(test: suspend TestScope.(CalendarViewModel, SettingsRepository) -> Unit) = runTest {
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        val viewModel = CalendarViewModel(CycleRepository(dayLogs, settings), dayLogs) { today }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        test(viewModel, settings)
    }

    private suspend fun CalendarViewModel.awaitState(
        matches: (CalendarUiState.Ready) -> Boolean = { true }
    ): CalendarUiState.Ready = uiState.first { it is CalendarUiState.Ready && matches(it) } as CalendarUiState.Ready

    /** A past period. The end goes first: alone it is ignored, so no state shows the period open. */
    private suspend fun logPeriod(start: LocalDate, end: LocalDate) {
        dayLogs.setPeriodEnded(end, ended = true)
        dayLogs.setPeriodStarted(start, started = true)
    }

    private fun day(iso: String): LocalDate = LocalDate.parse(iso)
}
