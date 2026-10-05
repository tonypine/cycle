package com.tonypine.cycle.feature.history

import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.Period
import java.time.LocalDate

/**
 * One synthetic History per state, for previews and screenshots. Made-up dates in 2027, never anyone's
 * real cycle: six cycles of 29, 26, 31, 28, 27 and 28 days, with periods of 5, 4, 6, 5, 5, 6 and 4
 * days. `HistoryUiStateTest` checks the same states come out of a real log.
 */
internal object HistorySamples {
    val today: LocalDate = LocalDate.of(2027, 9, 10)

    private val current = CycleSummary(
        start = day("2027-09-02"),
        end = null,
        length = 9,
        period = Period(day("2027-09-02"), day("2027-09-05"))
    )

    private val past = listOf(
        cycle("2027-08-05", length = 28, periodLength = 6),
        cycle("2027-07-09", length = 27, periodLength = 5),
        cycle("2027-06-11", length = 28, periodLength = 5),
        cycle("2027-05-11", length = 31, periodLength = 6),
        cycle("2027-04-15", length = 26, periodLength = 4),
        cycle("2027-03-17", length = 29, periodLength = 5)
    )

    val loading = HistoryUiState.Loading

    val empty = HistoryUiState.Empty(hasPeriod = false)

    val firstCycle = HistoryUiState.Empty(hasPeriod = true)

    /** Her typical cycle, then the current cycle and six past ones. */
    val cycles = HistoryUiState.Cycles(
        today = today,
        typicalCycle = LengthSummary(median = 28, shortest = 26, longest = 31, count = 6),
        typicalPeriod = LengthSummary(median = 5, shortest = 4, longest = 6, count = 6),
        cycles = listOf(current) + past
    )

    /** August's cycle, with a day she logged no flow on. */
    val pastCycle = CycleDetailUiState.Detail(
        today = today,
        cycle = past.first(),
        flow = flow(
            "2027-08-05",
            FlowLevel.MEDIUM,
            FlowLevel.HEAVY,
            FlowLevel.HEAVY,
            FlowLevel.MEDIUM,
            null,
            FlowLevel.LIGHT
        )
    )

    /** The cycle she is in: day 9, after a 4-day period. */
    val currentCycle = CycleDetailUiState.Detail(
        today = today,
        cycle = current,
        flow = flow("2027-09-02", FlowLevel.MEDIUM, FlowLevel.HEAVY, FlowLevel.MEDIUM, FlowLevel.LIGHT)
    )

    val missing = CycleDetailUiState.Missing

    /** Every list state by name, for the screenshot tests. */
    val all: Map<String, HistoryUiState> = mapOf(
        "loading" to loading,
        "empty" to empty,
        "first_cycle" to firstCycle,
        "cycles" to cycles
    )

    /** Every detail state by name, for the screenshot tests. */
    val allDetails: Map<String, CycleDetailUiState> = mapOf(
        "past" to pastCycle,
        "current" to currentCycle,
        "missing" to missing
    )

    private fun cycle(start: String, length: Int, periodLength: Int): CycleSummary {
        val first = day(start)
        return CycleSummary(
            start = first,
            end = first.plusDays(length - 1L),
            length = length,
            period = Period(first, first.plusDays(periodLength - 1L))
        )
    }

    private fun flow(start: String, vararg levels: FlowLevel?): List<FlowDay> =
        levels.mapIndexed { index, level -> FlowDay(day(start).plusDays(index.toLong()), level) }

    private fun day(iso: String): LocalDate = LocalDate.parse(iso)
}
