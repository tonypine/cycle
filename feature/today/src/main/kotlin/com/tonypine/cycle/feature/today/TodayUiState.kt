package com.tonypine.cycle.feature.today

import com.tonypine.cycle.core.data.repository.LogOverview
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.ui.CalendarDays
import com.tonypine.cycle.core.ui.DayLogEntry
import java.time.LocalDate
import java.time.YearMonth
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
     * @property days her logged and estimated periods, for the day cells.
     * @property todayLog what the day log sheet shows for today.
     * @property nextPeriod the next period card.
     * @property stillGoing the "still going?" question, while it is asked.
     * @property missedPeriod the "missed a period?" question, while it is asked.
     */
    data class Tracking(
        val today: LocalDate,
        val cycleDay: Int,
        val phase: TodayPhase,
        val days: CalendarDays,
        val todayLog: DayLogEntry,
        val nextPeriod: NextPeriod,
        val stillGoing: StillGoing? = null,
        val missedPeriod: MissedPeriod? = null
    ) : TodayUiState {
        /** Her logged periods. */
        val periods: List<Period>
            get() = days.periods

        /** The day cell state of [date]: a logged period wins over a predicted one. */
        fun dayState(date: LocalDate): CycleDayState = days.stateOf(date)

        /** She is on her period today, including the day it ended. */
        val onPeriod: Boolean
            get() = phase is TodayPhase.PeriodStartedToday ||
                phase is TodayPhase.OnPeriod ||
                phase is TodayPhase.PeriodEndedToday
    }

    companion object {
        /** Today's state from her [log] on that day and her [settings]. */
        fun from(log: LogOverview, settings: CycleSettings): TodayUiState {
            val overview = log.overview
            val today = overview.today
            val todayLog = log.log(today)
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
            // Late moves the expected start to today; the period was due that many days before.
            val due = next.expectedStart.minusDays(estimate.daysLate.toLong())
            val missedPeriod = overview.prompts.filterIsInstance<CyclePrompt.MissedPeriod>().firstOrNull()
                ?.let { MissedPeriod(it, likelyMonth = YearMonth.from(due)) }
            val days = CalendarDays.from(overview, log.logs)
            return Tracking(
                today = today,
                cycleDay = cycleDay,
                phase = phase,
                days = days,
                todayLog = log.entry(today, days),
                nextPeriod = NextPeriod(
                    expectedStart = next.expectedStart,
                    earliestStart = next.earliestStart,
                    latestStart = next.latestStart,
                    lastStart = last.start,
                    // Her cycle length is the day it was due.
                    cycleLength = daysBetween(last.start, due),
                    daysLate = estimate.daysLate,
                    basis = estimate.cycleBasis
                ),
                stillGoing = stillGoing,
                missedPeriod = missedPeriod
            )
        }
    }
}

/** What the day log sheet offers on [date], given the [days] as they draw. */
internal fun LogOverview.entry(date: LocalDate, days: CalendarDays): DayLogEntry = DayLogEntry(
    date = date,
    flow = log(date).flow,
    fillDays = usualPeriodLength.takeIf { canFill(date) },
    canClear = canClear(date),
    isPeriodDay = days.isPeriodDay(date)
)

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

/**
 * "Missed a period?": her cycle has run far longer than usual. "Add a past period" opens the
 * calendar on [likelyMonth], the month her next period was due in.
 */
data class MissedPeriod(val prompt: CyclePrompt.MissedPeriod, val likelyMonth: YearMonth)

internal fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
