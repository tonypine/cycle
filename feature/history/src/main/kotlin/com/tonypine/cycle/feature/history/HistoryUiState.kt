package com.tonypine.cycle.feature.history

import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.Period
import java.time.LocalDate
import java.time.YearMonth

/** What History shows. Built from her overview by [HistoryUiState.from]; the screen draws it as is. */
sealed interface HistoryUiState {
    /** Her log has not been read yet. */
    data object Loading : HistoryUiState

    /**
     * No complete cycle yet. [hasPeriod] when she has logged a period: her first cycle is still
     * going, and History fills in once the next period starts.
     */
    data class Empty(val hasPeriod: Boolean) : HistoryUiState

    /**
     * She has at least one complete cycle.
     *
     * @property typicalCycle her typical cycle length, from her last complete cycles.
     * @property typicalPeriod her typical period length, from her last ended periods.
     * @property cycles the current cycle first, then her past cycles, newest first.
     */
    data class Cycles(
        val today: LocalDate,
        val typicalCycle: LengthSummary,
        val typicalPeriod: LengthSummary?,
        val cycles: List<CycleSummary>
    ) : HistoryUiState

    companion object {
        /** History from her [overview]. */
        fun from(overview: CycleOverview): HistoryUiState {
            val typicalCycle = overview.typical.cycle ?: return Empty(hasPeriod = overview.periods.isNotEmpty())
            return Cycles(
                today = overview.today,
                typicalCycle = typicalCycle,
                typicalPeriod = overview.typical.period,
                cycles = cycleSummaries(overview).asReversed()
            )
        }
    }
}

/**
 * One cycle and the period it starts with.
 *
 * @property end the cycle's last day, or null for the current cycle.
 * @property length the days in the cycle; for the current cycle, its days so far, today included.
 */
data class CycleSummary(val start: LocalDate, val end: LocalDate?, val length: Int, val period: Period) {
    val isCurrent: Boolean
        get() = end == null

    /** The month the calendar opens on for this cycle: the one it started in. */
    val month: YearMonth
        get() = YearMonth.from(start)
}

/** What the cycle detail shows. Built by [CycleDetailUiState.from]. */
sealed interface CycleDetailUiState {
    /** Her log has not been read yet. */
    data object Loading : CycleDetailUiState

    /** No cycle starts on that day any more: an edit to her log moved or removed its period. */
    data object Missing : CycleDetailUiState

    /** The [cycle], and the [flow] she logged on each day of its period, first day first. */
    data class Detail(val today: LocalDate, val cycle: CycleSummary, val flow: List<FlowDay>) : CycleDetailUiState

    companion object {
        /** The cycle that starts on [start], from her [overview] and her [logs]. */
        fun from(overview: CycleOverview, logs: List<DayLog>, start: LocalDate): CycleDetailUiState {
            val cycle = cycleSummaries(overview).firstOrNull { it.start == start } ?: return Missing
            val flowByDate = logs.associate { it.date to it.flow }
            val flow = generateSequence(cycle.period.start) { it.plusDays(1) }
                .takeWhile { it <= cycle.period.end }
                .map { FlowDay(it, flowByDate[it]) }
                .toList()
            return Detail(overview.today, cycle, flow)
        }
    }
}

/** A day of a period and its [flow], or null when she logged none that day. */
data class FlowDay(val date: LocalDate, val flow: FlowLevel?)

/** Her cycles, oldest first, each with its period: the overview has one period per cycle. */
private fun cycleSummaries(overview: CycleOverview): List<CycleSummary> =
    overview.cycles.zip(overview.periods) { cycle, period ->
        CycleSummary(cycle.start, cycle.end, cycle.length ?: cycle.dayOf(overview.today), period)
    }
