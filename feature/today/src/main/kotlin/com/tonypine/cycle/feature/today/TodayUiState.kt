package com.tonypine.cycle.feature.today

import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Period
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What Today shows. Built from her overview by [TodayUiState.from]; the screen draws it as is. */
sealed interface TodayUiState {
    /** Her log has not been read yet. */
    data object Loading : TodayUiState

    /** No period logged yet. A period she logs lasts [periodLength] days, her usual length. */
    data class Empty(val today: LocalDate, val periodLength: Int) : TodayUiState

    /**
     * She has logged a period.
     *
     * @property cycleDay her cycle day today: "Day 19".
     * @property phase where she is in her cycle, which picks the context line, the cards and the
     *   main button.
     * @property todayFlow the flow she logged today, for the day log sheet.
     * @property periods her logged periods, for the day cells.
     * @property predicted the estimated periods, first to last day each, for the day cells.
     * @property nextPeriod the next period card.
     * @property stillGoing the "still going?" question, while it is asked.
     */
    data class Tracking(
        val today: LocalDate,
        val cycleDay: Int,
        val phase: TodayPhase,
        val todayFlow: FlowLevel?,
        val periods: List<Period>,
        val predicted: List<ClosedRange<LocalDate>>,
        val nextPeriod: NextPeriod,
        val stillGoing: StillGoing?
    ) : TodayUiState {
        /** The day cell state of [date]: a logged period wins over a predicted one. */
        fun dayState(date: LocalDate): CycleDayState = when {
            periods.any { date in it.start..it.end } -> CycleDayState.Period
            predicted.any { date in it } -> CycleDayState.PredictedPeriod
            else -> CycleDayState.Plain
        }

        /** She is on her period today, including the day it ended. */
        val onPeriod: Boolean
            get() = phase is TodayPhase.PeriodStartedToday ||
                phase is TodayPhase.OnPeriod ||
                phase is TodayPhase.PeriodEndedToday
    }

    companion object {
        /** Today's state from her [overview], her [settings] and what she logged [today][todayLog]. */
        fun from(overview: CycleOverview, settings: CycleSettings, todayLog: DayLog): TodayUiState {
            val today = overview.today
            val last = overview.periods.lastOrNull()
            val estimate = overview.estimate
            val cycleDay = overview.cycleDay
            if (last == null || estimate == null || cycleDay == null) return Empty(today, settings.usualPeriodLength)

            val current = overview.currentPeriod
            val next = estimate.next
            val phase = when {
                todayLog.periodEnded && last.end == today && !last.isOpen -> TodayPhase.PeriodEndedToday(last.length)
                current != null && todayLog.periodStarted && current.start == today -> TodayPhase.PeriodStartedToday
                current != null -> TodayPhase.OnPeriod(current.length)
                estimate.daysLate > 0 -> TodayPhase.Late(estimate.daysLate)
                today >= next.earliestStart -> TodayPhase.Due
                else -> TodayPhase.BetweenPeriods(daysBetween(today, next.expectedStart))
            }
            val stillGoing = current?.let { period ->
                overview.prompts.filterIsInstance<CyclePrompt.StillGoing>().firstOrNull()
                    ?.let { StillGoing(it, period.start..today) }
            }
            return Tracking(
                today = today,
                cycleDay = cycleDay,
                phase = phase,
                todayFlow = todayLog.flow,
                periods = overview.periods,
                predicted = estimate.periods.map { it.expectedStart..it.expectedEnd },
                nextPeriod = NextPeriod(
                    expectedStart = next.expectedStart,
                    earliestStart = next.earliestStart,
                    latestStart = next.latestStart,
                    lastStart = last.start,
                    // Late moves the expected start to today; her cycle length is the day it was due.
                    cycleLength = daysBetween(last.start, next.expectedStart) - estimate.daysLate,
                    daysLate = estimate.daysLate,
                    basis = estimate.cycleBasis
                ),
                stillGoing = stillGoing
            )
        }
    }
}

/** Where she is in her cycle today. */
sealed interface TodayPhase {
    /** Between periods, with the next one expected in about [daysUntil] days. */
    data class BetweenPeriods(val daysUntil: Int) : TodayPhase

    /** Today is inside the range the next period may start in. */
    data object Due : TodayPhase

    /** The next period was expected [daysLate] days ago and has not been logged. */
    data class Late(val daysLate: Int) : TodayPhase

    /** She tapped "My period started" today: day 1, with Undo. */
    data object PeriodStartedToday : TodayPhase

    /** She is on day [periodDay] of her period. */
    data class OnPeriod(val periodDay: Int) : TodayPhase

    /** She marked today as her period's last day, after [length] days, with Undo. */
    data class PeriodEndedToday(val length: Int) : TodayPhase
}

/**
 * The next period estimate and what the "How is this estimated?" sheet needs to explain it.
 *
 * @property lastStart the first day of her last period, which the estimate counts from.
 * @property cycleLength the cycle length added to [lastStart]: hers, her setup's or a typical one.
 * @property basis where [cycleLength] and the range come from.
 */
data class NextPeriod(
    val expectedStart: LocalDate,
    val earliestStart: LocalDate,
    val latestStart: LocalDate,
    val lastStart: LocalDate,
    val cycleLength: Int,
    val daysLate: Int,
    val basis: EstimateBasis
)

/** "Still going?": her open period has lasted longer than usual. [days] are its days so far. */
data class StillGoing(val prompt: CyclePrompt.StillGoing, val days: ClosedRange<LocalDate>)

internal fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
