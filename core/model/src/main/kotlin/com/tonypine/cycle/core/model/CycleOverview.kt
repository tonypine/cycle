package com.tonypine.cycle.core.model

import java.time.LocalDate

/**
 * Everything derived from her log on [today]: her periods and cycles, her typical lengths, the next
 * period estimate and the prompts to ask. Recomputed from the log every time, never stored.
 *
 * @property periods oldest first.
 * @property cycles oldest first. The last one is the current cycle, open until the next period.
 * @property estimate null until she has logged a period.
 */
data class CycleOverview(
    val today: LocalDate,
    val periods: List<Period>,
    val cycles: List<Cycle>,
    val typical: TypicalLengths,
    val estimate: CycleEstimate?,
    val prompts: List<CyclePrompt>
) {
    /** The cycle she is in today. */
    val currentCycle: Cycle?
        get() = cycles.lastOrNull()?.takeIf { !it.isComplete }

    /** Her cycle day today, or null before she logs a period. */
    val cycleDay: Int?
        get() = currentCycle?.dayOf(today)

    /** The period she is on today, if any. */
    val currentPeriod: Period?
        get() = periods.lastOrNull()?.takeIf { today in it.start..it.end }
}

/**
 * Her typical lengths, from her own recent logs. Null until she has logged one.
 *
 * @property cycle from her recent complete cycles.
 * @property period from her recent periods that have ended.
 */
data class TypicalLengths(val cycle: LengthSummary?, val period: LengthSummary?)

/** The median, shortest and longest of [count] lengths, in days. */
data class LengthSummary(val median: Int, val shortest: Int, val longest: Int, val count: Int)

/** Where an estimate's numbers come from, so the UI can say which. */
sealed interface EstimateBasis {
    /** No setup yet: the typical 28-day cycle and 5-day period. */
    data object Typical : EstimateBasis

    /** The usual lengths she gave at setup. */
    data object Setup : EstimateBasis

    /** Her own last [count] logged cycles (or periods, for a period length). */
    data class Logged(val count: Int) : EstimateBasis
}

/**
 * The next periods, as estimates.
 *
 * @property periods the next period first, then the ones after it: at most three.
 * @property daysLate how many days later than expected the next period is; 0 when it is not late.
 *   Once late, the next period is shown starting today.
 * @property cycleBasis where the cycle length, and so each start, comes from.
 * @property periodBasis where each expected period length comes from.
 */
data class CycleEstimate(
    val periods: List<EstimatedPeriod>,
    val daysLate: Int,
    val cycleBasis: EstimateBasis,
    val periodBasis: EstimateBasis
) {
    val next: EstimatedPeriod
        get() = periods.first()
}

/**
 * One estimated period: when it should start, the range it may start in, and how long it should
 * last. Never in the past.
 */
data class EstimatedPeriod(
    val expectedStart: LocalDate,
    val earliestStart: LocalDate,
    val latestStart: LocalDate,
    val expectedLength: Int
) {
    /** The last day of the expected period. */
    val expectedEnd: LocalDate
        get() = expectedStart.plusDays(expectedLength - 1L)
}

/** A question the UI asks once per cycle. [cycleStart] is the key she dismisses it under. */
sealed interface CyclePrompt {
    val cycleStart: LocalDate

    /** Her open period has lasted [periodDay] days, longer than usual: is it still going? */
    data class StillGoing(override val cycleStart: LocalDate, val periodDay: Int) : CyclePrompt

    /** Her cycle has reached day [cycleDay], far longer than usual: did she miss logging a period? */
    data class MissedPeriod(override val cycleStart: LocalDate, val cycleDay: Int) : CyclePrompt
}
