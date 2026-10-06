package com.tonypine.cycle.feature.today

import com.tonypine.cycle.core.data.repository.LogOverview
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.model.BleedBasis
import com.tonypine.cycle.core.model.BleedEstimate
import com.tonypine.cycle.core.model.BleedingSummary
import com.tonypine.cycle.core.model.BleedingWord
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.CycleEstimate
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.EstimateKind
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.StoppedMethod
import com.tonypine.cycle.core.ui.CalendarDays
import com.tonypine.cycle.core.ui.DayLogEntry
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** What Today shows. Built from her overview by [TodayUiState.from]; the screen draws it as is. */
sealed interface TodayUiState {
    /** Her log has not been read yet. */
    data object Loading : TodayUiState

    /** No period logged yet, and no method. A period she logs lasts [periodLength] days, her usual length. */
    data class Empty(val today: LocalDate, val periodLength: Int) : TodayUiState

    /**
     * She has logged a period, or is on a hormonal method, or stopped one
     * (`docs/decisions/0006-contraception.md`).
     *
     * @property display the big line: her cycle day, her method, or the days since she stopped one.
     * @property phase where she is today, which picks the context line, the cards and the main button.
     * @property words what her bleeding is called today, for the buttons, cards and lines: "period",
     *   or "bleed" or "bleeding" on a method.
     * @property days her logged and estimated periods or bleeds, for the day cells.
     * @property todayLog what the day log sheet shows for today.
     * @property outlook what is expected next, and its card.
     * @property stillGoing the "still going?" question, while it is asked.
     * @property missedPeriod the "missed a period?" question, while it is asked.
     * @property method her hormonal method today, for its calm line and sheet; null with no method or
     *   a copper IUD.
     * @property copperIudNote the fitting date of her copper IUD while its "Periods can be heavier at
     *   first" card shows.
     */
    data class Tracking(
        val today: LocalDate,
        val display: TodayDisplay,
        val phase: TodayPhase,
        val days: CalendarDays,
        val todayLog: DayLogEntry,
        val outlook: TodayOutlook,
        val words: BleedingWord = BleedingWord.PERIOD,
        val stillGoing: StillGoing? = null,
        val missedPeriod: MissedPeriod? = null,
        val method: TodayMethod? = null,
        val copperIudNote: LocalDate? = null
    ) : TodayUiState {
        /** Her cycle day today: "Day 19". Null on a hormonal method or after stopping one. */
        val cycleDay: Int?
            get() = (display as? TodayDisplay.CycleDay)?.day

        /** The next period estimate, when there is one. */
        val nextPeriod: NextPeriod?
            get() = outlook as? NextPeriod

        /** Her logged periods, and her bleeding on a method. */
        val periods: List<Period>
            get() = days.periods

        /** The day cell state of [date]: a logged period wins over a predicted one. */
        fun dayState(date: LocalDate): CycleDayState = days.stateOf(date)

        /** She is bleeding today, including the day it ended. */
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
            val contraception = overview.contraception
            val current = contraception.current
            val stopped = contraception.stopped
            val estimate = overview.estimate
            val empty = Empty(today, settings.usualPeriodLength)

            val bleeds = contraception.nextBleed
            val outlook: TodayOutlook = when (contraception.estimates) {
                EstimateKind.NEXT_BLEED ->
                    if (bleeds != null && current != null) {
                        NextBleed.from(current.method, bleeds)
                    } else {
                        TodayOutlook.FirstBleedToLog
                    }

                EstimateKind.NONE -> contraception.bleeding?.let(TodayOutlook::Bleeding) ?: TodayOutlook.AfterInjection

                EstimateKind.PERIOD -> estimate?.let { NextPeriod.from(it, overview.periods, stopped) } ?: return empty
            }
            val display = when {
                current != null && current.method.isHormonal -> TodayDisplay.Method(current.method)
                stopped != null -> TodayDisplay.DaysSince(stopped.days, stopped.stretch.method)
                else -> overview.cycleDay?.let(TodayDisplay::CycleDay) ?: return empty
            }

            val last = overview.periods.lastOrNull()
            val bleeding = overview.currentPeriod
            val phase = when {
                last != null && todayLog.periodEnded && last.end == today && !last.isOpen ->
                    TodayPhase.PeriodEndedToday(last.length)

                bleeding != null && todayLog.periodStarted && bleeding.start == today -> TodayPhase.PeriodStartedToday

                bleeding != null -> TodayPhase.OnPeriod(bleeding.length)

                outlook is NextPeriod && outlook.daysLate > 0 -> TodayPhase.Late(outlook.daysLate)

                outlook is NextPeriod && today >= outlook.earliestStart -> TodayPhase.Due

                outlook is NextPeriod -> TodayPhase.BetweenPeriods(daysBetween(today, outlook.expectedStart))

                outlook is NextBleed && today >= outlook.earliestStart -> TodayPhase.Due

                outlook is NextBleed -> TodayPhase.BetweenPeriods(daysBetween(today, outlook.expectedStart))

                else -> TodayPhase.NoEstimate
            }
            // A bleed starting in the days after a combined method stopped is still its bleed.
            val words = if (bleeding == null && stopped?.withdrawalWindow == true) {
                stopped.stretch.behaviour.word
            } else {
                overview.bleedingWord(today)
            }
            val stillGoing = bleeding?.let { period ->
                overview.prompts.filterIsInstance<CyclePrompt.StillGoing>().firstOrNull()
                    ?.let { StillGoing(it, period.start..today) }
            }
            val missedPeriod = (outlook as? NextPeriod)?.let { next ->
                overview.prompts.filterIsInstance<CyclePrompt.MissedPeriod>().firstOrNull()
                    // Late moves the expected start to today; the period was due that many days before.
                    ?.let { MissedPeriod(it, likelyMonth = YearMonth.from(next.due)) }
            }
            val days = CalendarDays.from(overview, log.logs)
            return Tracking(
                today = today,
                display = display,
                phase = phase,
                days = days,
                todayLog = log.entry(today, days),
                outlook = outlook,
                words = words,
                stillGoing = stillGoing,
                missedPeriod = missedPeriod,
                method = current?.takeIf { it.method.isHormonal }
                    ?.let { TodayMethod(it.method, it.breaks, firstMonths = contraception.firstMonths) },
                copperIudNote = current?.started?.takeIf {
                    current.method == ContraceptionMethod.COPPER_IUD &&
                        contraception.firstMonths &&
                        it !in settings.dismissedCopperIudNote
                }
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
    isPeriodDay = days.isPeriodDay(date),
    feelings = feelings(date),
    hiddenCategories = hiddenCategories
)

/** Today's big line. */
sealed interface TodayDisplay {
    /** Her cycle day: "Day 19". */
    data class CycleDay(val day: Int) : TodayDisplay

    /** Her hormonal method's short name, in place of a cycle day: "Implant". */
    data class Method(val method: ContraceptionMethod) : TodayDisplay

    /** The [days] since she stopped [method], until her first period after it: "12 days". */
    data class DaysSince(val days: Int, val method: ContraceptionMethod) : TodayDisplay
}

/**
 * Her hormonal method today.
 *
 * @property breaks on a combined method, how she takes it.
 * @property firstMonths in its first months since the start date: bleeding is often unsettled.
 */
data class TodayMethod(val method: ContraceptionMethod, val breaks: Breaks?, val firstMonths: Boolean)

/** Where she is today. */
sealed interface TodayPhase {
    /** Not bleeding, with the next period or bleed expected in about [daysUntil] days. */
    data class BetweenPeriods(val daysUntil: Int) : TodayPhase

    /** Today is inside the range the next period or bleed may start in. */
    data object Due : TodayPhase

    /** The next period was expected [daysLate] days ago and has not been logged. */
    data class Late(val daysLate: Int) : TodayPhase

    /** Not bleeding, and nothing is expected: a method with no estimate. */
    data object NoEstimate : TodayPhase

    /** She tapped "My period started" (or "Bleeding started") today: day 1, with Undo. */
    data object PeriodStartedToday : TodayPhase

    /** She is on day [periodDay] of her period, or of bleeding. */
    data class OnPeriod(val periodDay: Int) : TodayPhase

    /** She marked today as its last day, after [length] days, with Undo. */
    data class PeriodEndedToday(val length: Int) : TodayPhase
}

/** What is expected next, which picks Today's card. */
sealed interface TodayOutlook {
    /** On a method with no estimate: her bleeding in the last 90 days, or since it started. */
    data class Bleeding(val summary: BleedingSummary) : TodayOutlook

    /** A combined method with a break every month and no start date: nothing until she logs a bleed. */
    data object FirstBleedToLog : TodayOutlook

    /** After the injection, nothing until her first period. */
    data object AfterInjection : TodayOutlook
}

/**
 * The next period estimate and what the "How is this estimated?" sheet needs to explain it.
 *
 * @property lastStart the day the estimate counts from: the first day of her last period, or the day
 *   she stopped a method ([stoppedMethod]).
 * @property cycleLength the cycle length added to [lastStart]: hers, her setup's or a typical one.
 * @property basis where [cycleLength] and the range come from.
 * @property stoppedMethod the method she stopped, counted from its stop date, before her first period.
 * @property settlingAfter the method she stopped, while her cycles may still be settling after it and
 *   the range is wider.
 */
data class NextPeriod(
    val expectedStart: LocalDate,
    val earliestStart: LocalDate,
    val latestStart: LocalDate,
    val lastStart: LocalDate,
    val cycleLength: Int,
    val daysLate: Int,
    val basis: EstimateBasis,
    val stoppedMethod: ContraceptionMethod? = null,
    val settlingAfter: ContraceptionMethod? = null
) : TodayOutlook {
    /** The day it was due: late moves the expected start to today. */
    val due: LocalDate
        get() = expectedStart.minusDays(daysLate.toLong())

    companion object {
        /** The card for [estimate], counted from [stopped]'s stop date or her last period of her own. */
        fun from(estimate: CycleEstimate, periods: List<Period>, stopped: StoppedMethod?): NextPeriod? {
            val next = estimate.next
            val anchor = stopped?.stretch?.stopped ?: periods.lastOrNull { it.stretch == null }?.start ?: return null
            val due = next.expectedStart.minusDays(estimate.daysLate.toLong())
            return NextPeriod(
                expectedStart = next.expectedStart,
                earliestStart = next.earliestStart,
                latestStart = next.latestStart,
                lastStart = anchor,
                // Her cycle length is the day it was due.
                cycleLength = daysBetween(anchor, due),
                daysLate = estimate.daysLate,
                basis = estimate.cycleBasis,
                stoppedMethod = stopped?.stretch?.method,
                settlingAfter = estimate.settlingAfter
            )
        }
    }
}

/**
 * The next bleed on a combined [method] with a break every month.
 *
 * @property basis [BleedBasis.START_DATE] in the first break, shown as a range only:
 *   "24 to 30 May"; [BleedBasis.LAST_BLEED] after a bleed counted: "Around 21 June".
 * @property missedBreak the last break passed with no bleed logged.
 */
data class NextBleed(
    val method: ContraceptionMethod,
    val expectedStart: LocalDate,
    val earliestStart: LocalDate,
    val latestStart: LocalDate,
    val basis: BleedBasis,
    val missedBreak: Boolean
) : TodayOutlook {
    companion object {
        fun from(method: ContraceptionMethod, estimate: BleedEstimate): NextBleed = NextBleed(
            method = method,
            expectedStart = estimate.next.expectedStart,
            earliestStart = estimate.next.earliestStart,
            latestStart = estimate.next.latestStart,
            basis = estimate.basis,
            missedBreak = estimate.missedBreak
        )
    }
}

/** "Still going?": her open period has lasted longer than usual. [days] are its days so far. */
data class StillGoing(val prompt: CyclePrompt.StillGoing, val days: ClosedRange<LocalDate>)

/**
 * "Missed a period?": her cycle has run far longer than usual. "Add a past period" opens the
 * calendar on [likelyMonth], the month her next period was due in.
 */
data class MissedPeriod(val prompt: CyclePrompt.MissedPeriod, val likelyMonth: YearMonth)

internal fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
