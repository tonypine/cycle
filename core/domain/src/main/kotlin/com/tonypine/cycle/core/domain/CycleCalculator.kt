package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.domain.CycleRules.CYCLES_AHEAD
import com.tonypine.cycle.core.domain.CycleRules.DEFAULT_CYCLE_LENGTH
import com.tonypine.cycle.core.domain.CycleRules.DEFAULT_PERIOD_LENGTH
import com.tonypine.cycle.core.domain.CycleRules.FEW_CYCLES
import com.tonypine.cycle.core.domain.CycleRules.FEW_CYCLES_MIN_SPREAD_DAYS
import com.tonypine.cycle.core.domain.CycleRules.MAX_GAP_DAYS
import com.tonypine.cycle.core.domain.CycleRules.MISSED_PERIOD_PERCENT
import com.tonypine.cycle.core.domain.CycleRules.NO_CYCLES_SPREAD_DAYS
import com.tonypine.cycle.core.domain.CycleRules.RECENT_CYCLES
import com.tonypine.cycle.core.domain.CycleRules.STILL_GOING_EXTRA_DAYS
import com.tonypine.cycle.core.model.Cycle
import com.tonypine.cycle.core.model.CycleEstimate
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.EstimatedPeriod
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.TypicalLengths
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Derives periods, cycles, typical lengths, estimates and prompts from her day logs. Pure: the same
 * logs, settings and day always give the same answer, so editing any past day recomputes every
 * cycle after it. The rules are in `docs/decisions/0003-cycle-estimates.md`.
 */
object CycleCalculator {
    /** Everything the app shows about her cycle on [today]. Logs after [today] are ignored. */
    fun overview(logs: List<DayLog>, settings: CycleSettings, today: LocalDate): CycleOverview {
        val periods = periods(logs, today)
        val cycles = cycles(periods)
        val endedPeriods = endedPeriods(logs, periods, today)
        val typical = TypicalLengths(
            cycle = summarize(cycles.mapNotNull { it.length }.takeLast(RECENT_CYCLES)),
            period = summarize(endedPeriods.map { it.length }.takeLast(RECENT_CYCLES))
        )
        val usual = UsualLengths.of(typical, settings)
        return CycleOverview(
            today = today,
            periods = periods,
            cycles = cycles,
            typical = typical,
            estimate = periods.lastOrNull()?.let { estimate(it.start, typical, usual, today) },
            prompts = prompts(periods, usual, settings, today)
        )
    }

    /**
     * Her periods, oldest first. A period is a run of period days (light or heavier flow, or a
     * period marker) with gaps of at most [MAX_GAP_DAYS]; spotting alone never starts one. "Ended"
     * marks a period's last day. "Ended" alone, with no flow and no "started", never starts a
     * period: it closes the period before it, which then runs up to that day, or is ignored when
     * that period has already ended or there is none. A period marked "started" and not yet "ended"
     * stays open: it takes in every later day, and the last one runs up to [today]. A new "started"
     * after a gap starts a new period, and the one before it ends on its last logged day.
     */
    fun periods(logs: List<DayLog>, today: LocalDate): List<Period> {
        val periods = mutableListOf<Period>()
        var run: Run? = null
        for (day in logs.filter { it.isPeriodDay && it.date <= today }.sortedBy { it.date }) {
            val current = run
            run = when {
                current != null && current.continuesTo(day) -> current.copy(
                    last = day.date,
                    started = current.started || day.periodStarted,
                    ended = day.periodEnded
                )

                day.onlyEnds -> current

                else -> {
                    current?.let { periods += Period(it.start, it.last) }
                    Run(start = day.date, last = day.date, started = day.periodStarted, ended = day.periodEnded)
                }
            }
        }
        run?.let {
            val open = it.started && !it.ended
            periods += Period(it.start, if (open) maxOf(it.last, today) else it.last, isOpen = open)
        }
        return periods
    }

    /** Her cycles, oldest first: each from a period's first day to the day before the next one. */
    fun cycles(periods: List<Period>): List<Cycle> = periods.mapIndexed { index, period ->
        Cycle(start = period.start, end = periods.getOrNull(index + 1)?.start?.minusDays(1))
    }

    /** A run of period days being collected into one period. */
    private data class Run(val start: LocalDate, val last: LocalDate, val started: Boolean, val ended: Boolean) {
        fun continuesTo(day: DayLog): Boolean = !ended &&
            (daysBetween(last, day.date) <= MAX_GAP_DAYS + 1 || day.onlyEnds || (started && !day.periodStarted))
    }

    /** She marked the day "ended" and logged no period flow and no "started": it can only close one. */
    private val DayLog.onlyEnds: Boolean
        get() = periodEnded && !periodStarted && flow?.isPeriodFlow != true

    /**
     * The periods that are over, for her typical period length: every period but the last, and the
     * last once she marked it ended or it is too long ago for another day to extend it.
     */
    private fun endedPeriods(logs: List<DayLog>, periods: List<Period>, today: LocalDate): List<Period> {
        val last = periods.lastOrNull() ?: return emptyList()
        val markedEnded = logs.any { it.date == last.end && it.periodEnded }
        val lastIsOver = !last.isOpen && (markedEnded || daysBetween(last.end, today) > MAX_GAP_DAYS + 1)
        return if (lastIsOver) periods else periods.dropLast(1)
    }

    /** The lengths every estimate and prompt uses: hers when she has logged them, else her setup. */
    private data class UsualLengths(
        val cycle: Int,
        val period: Int,
        val cycleBasis: EstimateBasis,
        val periodBasis: EstimateBasis
    ) {
        companion object {
            fun of(typical: TypicalLengths, settings: CycleSettings): UsualLengths {
                val fallbackBasis = if (settings.setupDone) EstimateBasis.Setup else EstimateBasis.Typical
                val fallbackCycle = if (settings.setupDone) settings.usualCycleLength else DEFAULT_CYCLE_LENGTH
                val fallbackPeriod = if (settings.setupDone) settings.usualPeriodLength else DEFAULT_PERIOD_LENGTH
                return UsualLengths(
                    cycle = typical.cycle?.median ?: fallbackCycle,
                    period = typical.period?.median ?: fallbackPeriod,
                    cycleBasis = typical.cycle?.let { EstimateBasis.Logged(it.count) } ?: fallbackBasis,
                    periodBasis = typical.period?.let { EstimateBasis.Logged(it.count) } ?: fallbackBasis
                )
            }
        }
    }

    /**
     * The next [CYCLES_AHEAD] periods after the one that started on [lastStart]. The next one is
     * expected [UsualLengths.cycle] days later, within her shortest to longest recent cycle; with
     * no complete cycle, within ± [NO_CYCLES_SPREAD_DAYS]; with up to [FEW_CYCLES], at least ±
     * [FEW_CYCLES_MIN_SPREAD_DAYS]. Nothing is ever in the past: once late, the next period is
     * expected today. Each later one follows a usual cycle after the one before, with the same range.
     */
    private fun estimate(
        lastStart: LocalDate,
        typical: TypicalLengths,
        usual: UsualLengths,
        today: LocalDate
    ): CycleEstimate {
        val median = usual.cycle
        val cycles = typical.cycle
        val (shortest, longest) = when {
            cycles == null -> median - NO_CYCLES_SPREAD_DAYS to median + NO_CYCLES_SPREAD_DAYS

            cycles.count <= FEW_CYCLES ->
                minOf(cycles.shortest, median - FEW_CYCLES_MIN_SPREAD_DAYS) to
                    maxOf(cycles.longest, median + FEW_CYCLES_MIN_SPREAD_DAYS)

            else -> cycles.shortest to cycles.longest
        }
        val expected = lastStart.plusDays(median.toLong())
        val next = EstimatedPeriod(
            expectedStart = maxOf(expected, today),
            earliestStart = maxOf(lastStart.plusDays(shortest.toLong()), today),
            latestStart = maxOf(lastStart.plusDays(longest.toLong()), today),
            expectedLength = usual.period
        )
        val later = (1 until CYCLES_AHEAD).map { ahead ->
            val start = next.expectedStart.plusDays(ahead.toLong() * median)
            EstimatedPeriod(
                expectedStart = start,
                earliestStart = start.minusDays((median - shortest).toLong()),
                latestStart = start.plusDays((longest - median).toLong()),
                expectedLength = usual.period
            )
        }
        return CycleEstimate(
            periods = listOf(next) + later,
            daysLate = maxOf(daysBetween(expected, today), 0),
            cycleBasis = usual.cycleBasis,
            periodBasis = usual.periodBasis
        )
    }

    /**
     * "Still going?" once her open period reaches her usual period length + [STILL_GOING_EXTRA_DAYS];
     * "missed a period?" once her current cycle reaches [MISSED_PERIOD_PERCENT] % of her usual cycle,
     * outside a period. Each is asked once per cycle: not after she dismissed it.
     */
    private fun prompts(
        periods: List<Period>,
        usual: UsualLengths,
        settings: CycleSettings,
        today: LocalDate
    ): List<CyclePrompt> {
        val last = periods.lastOrNull() ?: return emptyList()
        val day = daysBetween(last.start, today) + 1
        val prompt = when {
            last.isOpen -> CyclePrompt.StillGoing(last.start, day)
                .takeIf { day >= usual.period + STILL_GOING_EXTRA_DAYS && last.start !in settings.dismissedStillGoing }

            else -> CyclePrompt.MissedPeriod(last.start, day)
                .takeIf {
                    day * 100 >= usual.cycle * MISSED_PERIOD_PERCENT &&
                        last.start !in settings.dismissedMissedPeriod
                }
        }
        return listOfNotNull(prompt)
    }

    /** Median, shortest and longest of [lengths], or null when there are none. */
    internal fun summarize(lengths: List<Int>): LengthSummary? {
        if (lengths.isEmpty()) return null
        val sorted = lengths.sorted()
        val middle = sorted.size / 2
        // An even count has two middle values: take their mean, a half day rounded up.
        val median = if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle] + 1) / 2
        return LengthSummary(median = median, shortest = sorted.first(), longest = sorted.last(), count = sorted.size)
    }

    private fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
}
