package com.tonypine.cycle.feature.history

import com.tonypine.cycle.core.domain.CycleCalculator
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.Period
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** History and the cycle detail from a synthetic log, through the real calculator. */
class HistoryUiStateTest {
    private val today = HistorySamples.today

    private fun history(logs: List<DayLog>, on: LocalDate = today) =
        HistoryUiState.from(CycleCalculator.overview(logs, notSetUp, on))

    private fun detail(logs: List<DayLog>, start: String, on: LocalDate = today) =
        CycleDetailUiState.from(CycleCalculator.overview(logs, notSetUp, on), logs, day(start))

    @Test
    fun `her typical cycle is 28 days, 26 to 31, and her period 5 days, 4 to 6`() {
        val state = history(syntheticHistory) as HistoryUiState.Cycles

        assertEquals(LengthSummary(median = 28, shortest = 26, longest = 31, count = 6), state.typicalCycle)
        assertEquals(LengthSummary(median = 5, shortest = 4, longest = 6, count = 6), state.typicalPeriod)
    }

    @Test
    fun `the current cycle comes first, then the past cycles newest first`() {
        val state = history(syntheticHistory) as HistoryUiState.Cycles

        assertEquals(HistorySamples.cycles, state)
        assertEquals(
            listOf("2027-09-02", "2027-08-05", "2027-07-09", "2027-06-11", "2027-05-11", "2027-04-15", "2027-03-17"),
            state.cycles.map { it.start.toString() }
        )
        assertTrue(state.cycles.first().isCurrent)
        assertEquals(9, state.cycles.first().length)
        assertEquals(listOf(28, 27, 28, 31, 26, 29), state.cycles.drop(1).map { it.length })
    }

    @Test
    fun `with nothing logged, History is empty`() {
        assertEquals(HistoryUiState.Empty(hasPeriod = false), history(emptyList()))
    }

    @Test
    fun `with one period, her first cycle is still going`() {
        assertEquals(HistoryUiState.Empty(hasPeriod = true), history(bleed("2027-09-02", 4)))
    }

    @Test
    fun `one complete cycle gives a typical cycle of that length`() {
        val state = history(bleed("2027-08-05", 5) + bleed("2027-09-02", 4)) as HistoryUiState.Cycles

        assertEquals(LengthSummary(median = 28, shortest = 28, longest = 28, count = 1), state.typicalCycle)
        assertEquals(2, state.cycles.size)
    }

    @Test
    fun `a past cycle's detail has its dates and the flow of each period day`() {
        val state = detail(syntheticHistory, "2027-08-05") as CycleDetailUiState.Detail

        assertEquals(HistorySamples.pastCycle, state)
        assertEquals(day("2027-09-01"), state.cycle.end)
        assertEquals(Period(day("2027-08-05"), day("2027-08-10")), state.cycle.period)
        // She logged nothing on August 9, inside the period.
        assertEquals(FlowDay(day("2027-08-09"), null), state.flow[4])
        assertEquals(YearMonth.of(2027, 8), state.cycle.month)
    }

    @Test
    fun `the current cycle's detail counts its days so far`() {
        assertEquals(HistorySamples.currentCycle, detail(syntheticHistory, "2027-09-02"))
    }

    @Test
    fun `a period still going shows its days up to today`() {
        val logs = syntheticHistory.filter { it.date < day("2027-09-02") } +
            DayLog(day("2027-09-08"), FlowLevel.HEAVY, periodStarted = true)

        val state = detail(logs, "2027-09-08") as CycleDetailUiState.Detail

        assertTrue(state.cycle.period.isOpen)
        assertEquals(
            listOf(
                FlowDay(day("2027-09-08"), FlowLevel.HEAVY),
                FlowDay(day("2027-09-09"), null),
                FlowDay(day("2027-09-10"), null)
            ),
            state.flow
        )
    }

    @Test
    fun `a cycle whose period was edited away is missing`() {
        assertEquals(CycleDetailUiState.Missing, detail(syntheticHistory, "2027-08-06"))
        assertEquals(CycleDetailUiState.Missing, detail(emptyList(), "2027-08-05"))
    }
}
