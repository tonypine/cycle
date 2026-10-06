package com.tonypine.cycle.feature.today

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LogCategory
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.SexualActivity
import com.tonypine.cycle.core.model.SleepQuality
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * Today's states from a real log: the repositories on an in-memory database and a DataStore file.
 * Synthetic dates only: made-up days in 2027, never anyone's real cycle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TodayViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val today = day("2027-03-20")
    private var clock = today

    private val database = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CycleDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dayLogs = DayLogRepository(database)

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `with nothing logged, Today is empty`() = today { viewModel, _ ->
        assertEquals(TodayUiState.Empty(today, periodLength = 5), viewModel.awaitState<TodayUiState.Empty>())
    }

    @Test
    fun `logging a period 19 days ago shows day 19 and a typical estimate`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-03-02"))

        val state = viewModel.awaitState<TodayUiState.Tracking>()
        assertEquals(19, state.cycleDay)
        assertEquals(TodayPhase.BetweenPeriods(daysUntil = 10), state.phase)
        // A typical 5-day period, marked ended, so it is not still going.
        assertEquals(listOf(Period(day("2027-03-02"), day("2027-03-06"))), state.periods)
        assertEquals(
            NextPeriod(
                expectedStart = day("2027-03-30"),
                earliestStart = day("2027-03-26"),
                latestStart = day("2027-04-03"),
                lastStart = day("2027-03-02"),
                cycleLength = 28,
                daysLate = 0,
                basis = EstimateBasis.Typical
            ),
            state.nextPeriod
        )
        assertEquals(CycleDayState.Period, state.dayState(day("2027-03-04")))
        assertEquals(CycleDayState.PredictedPeriod, state.dayState(day("2027-03-31")))
        assertEquals(CycleDayState.Plain, state.dayState(today))
    }

    @Test
    fun `a period that started recently is still going after it is logged`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-03-18"))

        val state = viewModel.awaitState<TodayUiState.Tracking>()
        assertEquals(TodayPhase.OnPeriod(periodDay = 3), state.phase)
        assertTrue(state.periods.single().isOpen)
    }

    @Test
    fun `inside the range, the next period is due`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-02-24"))

        val state = viewModel.awaitState<TodayUiState.Tracking>()
        assertEquals(25, state.cycleDay)
        assertEquals(TodayPhase.Due, state.phase)
    }

    @Test
    fun `one tap starts a period today and Undo takes it back`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-02-24"))
        val before = viewModel.awaitState<TodayUiState.Tracking> { it.phase == TodayPhase.Due }

        viewModel.onPeriodStarted()
        val started = viewModel.awaitState<TodayUiState.Tracking> { it.phase == TodayPhase.PeriodStartedToday }
        assertEquals(1, started.cycleDay)
        assertEquals(CycleDayState.Period, started.dayState(today))
        assertTrue(started.onPeriod)
        // The estimate follows at once: her first full cycle, 24 days.
        assertEquals(EstimateBasis.Logged(1), started.nextPeriod.basis)
        assertEquals(day("2027-04-13"), started.nextPeriod.expectedStart)

        viewModel.onUndoPeriodStarted()
        assertEquals(before, viewModel.awaitState<TodayUiState.Tracking> { it.phase == TodayPhase.Due })
    }

    @Test
    fun `during a period, Today counts its days`() = today { viewModel, _ ->
        logPeriod(day("2027-02-17"), day("2027-02-21"))
        dayLogs.setPeriodStarted(day("2027-03-17"), started = true)

        val state = viewModel.awaitState<TodayUiState.Tracking> { it.periods.size == 2 }
        assertEquals(4, state.cycleDay)
        assertEquals(TodayPhase.OnPeriod(periodDay = 4), state.phase)
        assertNull(state.stillGoing)
    }

    @Test
    fun `one tap ends the period today and Undo takes it back`() = today { viewModel, _ ->
        logPeriod(day("2027-02-16"), day("2027-02-20"))
        dayLogs.setPeriodStarted(day("2027-03-16"), started = true)
        viewModel.awaitState<TodayUiState.Tracking> { it.phase == TodayPhase.OnPeriod(5) }

        viewModel.onPeriodEnded()
        val ended = viewModel.awaitState<TodayUiState.Tracking> { it.phase is TodayPhase.PeriodEndedToday }
        assertEquals(TodayPhase.PeriodEndedToday(length = 5), ended.phase)
        assertEquals(Period(day("2027-03-16"), today), ended.periods.last())
        assertTrue(ended.onPeriod)

        viewModel.onUndoPeriodEnded()
        val undone = viewModel.awaitState<TodayUiState.Tracking> { it.phase == TodayPhase.OnPeriod(5) }
        assertTrue(undone.periods.last().isOpen)
    }

    @Test
    fun `past the expected day, the period is late and expected from today`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-02-18"))

        val state = viewModel.awaitState<TodayUiState.Tracking>()
        assertEquals(31, state.cycleDay)
        assertEquals(TodayPhase.Late(daysLate = 2), state.phase)
        assertEquals(today, state.nextPeriod.expectedStart)
        assertEquals(today, state.nextPeriod.earliestStart)
        assertEquals(28, state.nextPeriod.cycleLength)
        assertEquals(CycleDayState.PredictedPeriod, state.dayState(today))
    }

    @Test
    fun `a long period asks whether it is still going, once`() = today { viewModel, settings ->
        logPeriod(day("2027-02-13"), day("2027-02-17"))
        dayLogs.setPeriodStarted(day("2027-03-13"), started = true)

        val asked = viewModel.awaitState<TodayUiState.Tracking> { it.periods.size == 2 && it.stillGoing != null }
        assertEquals(TodayPhase.OnPeriod(periodDay = 8), asked.phase)
        assertEquals(day("2027-03-13")..today, asked.stillGoing?.days)

        viewModel.onStillGoing()
        val answered = viewModel.awaitState<TodayUiState.Tracking> { it.stillGoing == null }
        assertEquals(TodayPhase.OnPeriod(periodDay = 8), answered.phase)
        assertEquals(setOf(day("2027-03-13")), settings.settings.first().dismissedStillGoing)
    }

    @Test
    fun `it ended earlier marks the day she picks as the last`() = today { viewModel, settings ->
        logPeriod(day("2027-02-13"), day("2027-02-17"))
        dayLogs.setPeriodStarted(day("2027-03-13"), started = true)
        viewModel.awaitState<TodayUiState.Tracking> { it.periods.size == 2 && it.stillGoing != null }

        viewModel.onEndedOn(day("2027-03-17"))

        val state = viewModel.awaitState<TodayUiState.Tracking> { !it.onPeriod }
        assertEquals(Period(day("2027-03-13"), day("2027-03-17")), state.periods.last())
        assertNull(state.stillGoing)
        assertEquals(setOf(day("2027-03-13")), settings.settings.first().dismissedStillGoing)
    }

    @Test
    fun `the day log saves today's flow`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-03-02"))
        viewModel.awaitState<TodayUiState.Tracking>()

        viewModel.onLogDay(today, FlowLevel.SPOTTING, DayFeelings(today))

        val state = viewModel.awaitState<TodayUiState.Tracking> { it.todayLog.flow != null }
        assertEquals(FlowLevel.SPOTTING, state.todayLog.flow)
        assertTrue(state.todayLog.canClear)
    }

    @Test
    fun `the day log saves how she felt, and hiding a category keeps it for when it shows again`() =
        today { viewModel, settings ->
            viewModel.onLogPeriod(day("2027-03-02"))
            viewModel.awaitState<TodayUiState.Tracking>()
            val feelings = DayFeelings(
                today,
                moods = setOf(Mood.IRRITABLE),
                sleep = SleepQuality.BADLY,
                sex = SexualActivity.PROTECTED
            )

            viewModel.onLogDay(today, null, feelings)
            val logged = viewModel.awaitState<TodayUiState.Tracking> { it.todayLog.isLogged }
            assertEquals(feelings, logged.todayLog.shownFeelings)

            settings.setCategoryShown(LogCategory.SEX, shown = false)
            val hidden = viewModel.awaitState<TodayUiState.Tracking> { it.todayLog.hiddenCategories.isNotEmpty() }
            assertEquals(feelings.copy(sex = null), hidden.todayLog.shownFeelings)
            // The sheet starts from everything, so saving it again keeps the hidden category.
            assertEquals(feelings, hidden.todayLog.feelings)
            viewModel.onLogDay(today, null, hidden.todayLog.feelings.copy(energy = EnergyLevel.HIGH))

            settings.setCategoryShown(LogCategory.SEX, shown = true)
            val shown = viewModel.awaitState<TodayUiState.Tracking> {
                it.todayLog.hiddenCategories.isEmpty() && it.todayLog.feelings.energy != null
            }
            assertEquals(feelings.copy(energy = EnergyLevel.HIGH), shown.todayLog.shownFeelings)
        }

    @Test
    fun `the day log never logs a day after today`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-03-02"))
        viewModel.awaitState<TodayUiState.Tracking>()

        viewModel.onLogDay(today.plusDays(1), FlowLevel.HEAVY, DayFeelings(today.plusDays(1)))

        assertEquals(emptyList<DayLog>(), dayLogs.observeDayLogs(today.plusDays(1), today.plusDays(1)).first())
    }

    @Test
    fun `the day log never clears a day after today`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-03-02"))
        // Logged before the phone's clock went back a day.
        dayLogs.setFlow(today.plusDays(1), FlowLevel.LIGHT)
        viewModel.awaitState<TodayUiState.Tracking>()

        viewModel.onClearDay(today.plusDays(1))
        // Written after the clear, in order, so once it shows, the clear has run.
        viewModel.onLogDay(today, FlowLevel.SPOTTING, DayFeelings(today))
        viewModel.awaitState<TodayUiState.Tracking> { it.todayLog.flow != null }

        assertEquals(
            listOf(DayLog(today.plusDays(1), flow = FlowLevel.LIGHT)),
            dayLogs.observeDayLogs(today.plusDays(1), today.plusDays(1)).first()
        )
    }

    @Test
    fun `clearing today takes the day off the period still going`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-03-18"))
        viewModel.awaitState<TodayUiState.Tracking> { it.onPeriod }

        viewModel.onClearDay(today)

        val state = viewModel.awaitState<TodayUiState.Tracking> { !it.onPeriod }
        assertEquals(Period(day("2027-03-18"), day("2027-03-19")), state.periods.last())
        assertEquals(CycleDayState.Plain, state.dayState(today))
    }

    @Test
    fun `missed a period is asked once, at 1_8 times her usual cycle`() = today { viewModel, settings ->
        // Day 50 of a typical 28-day cycle: not yet.
        logPeriod(day("2027-01-30"), day("2027-02-03"))
        assertNull(viewModel.awaitState<TodayUiState.Tracking> { it.cycleDay == 50 }.missedPeriod)

        clock = today.plusDays(1)
        viewModel.refreshDay()
        val asked = viewModel.awaitState<TodayUiState.Tracking> { it.cycleDay == 51 }
        // Her period was due on 27 February: the calendar opens on February.
        assertEquals(YearMonth.of(2027, 2), asked.missedPeriod?.likelyMonth)

        viewModel.onNoMissedPeriod()
        assertNull(viewModel.awaitState<TodayUiState.Tracking> { it.missedPeriod == null }.missedPeriod)
        assertEquals(setOf(day("2027-01-30")), settings.settings.first().dismissedMissedPeriod)

        clock = today.plusDays(2)
        viewModel.refreshDay()
        assertNull(viewModel.awaitState<TodayUiState.Tracking> { it.cycleDay == 52 }.missedPeriod)
    }

    @Test
    fun `adding the missed period answers the question and recomputes the estimate`() = today { viewModel, _ ->
        logPeriod(day("2027-01-29"), day("2027-02-02"))
        viewModel.awaitState<TodayUiState.Tracking> { it.missedPeriod != null }

        viewModel.onFillPeriod(day("2027-02-26"))

        val state = viewModel.awaitState<TodayUiState.Tracking> { it.periods.size == 2 }
        assertNull(state.missedPeriod)
        assertEquals(23, state.cycleDay)
        assertEquals(Period(day("2027-02-26"), day("2027-03-02")), state.periods.last())
        // One cycle of 28 days, from 29 January to 25 February.
        assertEquals(day("2027-03-26"), state.nextPeriod.expectedStart)
    }

    @Test
    fun `a new day moves Today on`() = today { viewModel, _ ->
        viewModel.onLogPeriod(day("2027-03-02"))
        viewModel.awaitState<TodayUiState.Tracking> { it.cycleDay == 19 }

        clock = today.plusDays(1)
        viewModel.refreshDay()

        assertEquals(
            TodayPhase.BetweenPeriods(9),
            viewModel.awaitState<TodayUiState.Tracking> {
                it.cycleDay == 20
            }.phase
        )
    }

    /** Runs [test] with a ViewModel on the test database, its state collected as the screen would. */
    private fun today(test: suspend TestScope.(TodayViewModel, SettingsRepository) -> Unit) = runTest {
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        val viewModel = TodayViewModel(CycleRepository(dayLogs, settings), dayLogs, settings) { clock }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        try {
            test(viewModel, settings)
        } finally {
            // As if the screen were gone: nothing of the ViewModel may still reach Dispatchers.Main while
            // tearDown resets it, or resetMain fails with "used concurrently".
            viewModel.viewModelScope.coroutineContext.job.cancelAndJoin()
        }
    }

    /**
     * The first state of type [T] that [matches]. Room emits on its own threads, and the ViewModel runs
     * there on the unconfined Main, so a state can still be on its way after a call returns: wait for
     * what the call changes, not just any state.
     */
    private suspend inline fun <reified T : TodayUiState> TodayViewModel.awaitState(
        crossinline matches: (T) -> Boolean = { true }
    ): T = uiState.first { it is T && matches(it) } as T

    /** A past period. The end goes first: alone it is ignored, so no state shows the period open. */
    private suspend fun logPeriod(start: LocalDate, end: LocalDate) {
        dayLogs.setPeriodEnded(end, ended = true)
        dayLogs.setPeriodStarted(start, started = true)
    }

    private fun day(iso: String): LocalDate = LocalDate.parse(iso)
}
