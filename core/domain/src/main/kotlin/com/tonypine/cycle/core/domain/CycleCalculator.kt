package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.domain.CycleRules.BLEEDING_SUMMARY_DAYS
import com.tonypine.cycle.core.domain.CycleRules.BLEED_SPREAD_DAYS
import com.tonypine.cycle.core.domain.CycleRules.COPPER_IUD_NOTE_MONTHS
import com.tonypine.cycle.core.domain.CycleRules.CYCLES_AHEAD
import com.tonypine.cycle.core.domain.CycleRules.DAYS_BEFORE_BREAK
import com.tonypine.cycle.core.domain.CycleRules.DEFAULT_CYCLE_LENGTH
import com.tonypine.cycle.core.domain.CycleRules.DEFAULT_PERIOD_LENGTH
import com.tonypine.cycle.core.domain.CycleRules.FEW_CYCLES
import com.tonypine.cycle.core.domain.CycleRules.FEW_CYCLES_MIN_SPREAD_DAYS
import com.tonypine.cycle.core.domain.CycleRules.FIRST_MONTHS
import com.tonypine.cycle.core.domain.CycleRules.HORMONAL_IUD_FIRST_MONTHS
import com.tonypine.cycle.core.domain.CycleRules.MAX_GAP_DAYS
import com.tonypine.cycle.core.domain.CycleRules.MISSED_PERIOD_PERCENT
import com.tonypine.cycle.core.domain.CycleRules.NO_CYCLES_SPREAD_DAYS
import com.tonypine.cycle.core.domain.CycleRules.PACK_DAYS
import com.tonypine.cycle.core.domain.CycleRules.RECENT_CYCLES
import com.tonypine.cycle.core.domain.CycleRules.SETTLING_CYCLES
import com.tonypine.cycle.core.domain.CycleRules.SETTLING_SPREAD_DAYS
import com.tonypine.cycle.core.domain.CycleRules.STILL_GOING_EXTRA_DAYS
import com.tonypine.cycle.core.domain.CycleRules.WITHDRAWAL_BLEED_DAYS
import com.tonypine.cycle.core.model.BleedBasis
import com.tonypine.cycle.core.model.BleedEstimate
import com.tonypine.cycle.core.model.BleedingSummary
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionOverview
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.Cycle
import com.tonypine.cycle.core.model.CycleEstimate
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.EstimateKind
import com.tonypine.cycle.core.model.EstimatedPeriod
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.MethodBehaviour
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.StoppedMethod
import com.tonypine.cycle.core.model.TypicalLengths
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Derives periods, cycles, typical lengths, estimates and prompts from her day logs and the stretches
 * of contraception she recorded. Pure: the same logs, stretches, settings and day always give the
 * same answer, so editing any past day or stretch recomputes every cycle after it. The rules are in
 * `docs/decisions/0003-cycle-estimates.md` and `0006-contraception.md`.
 */
object CycleCalculator {
    /**
     * Everything the app shows about her cycle on [today]. Logs after [today], and stretches that
     * start after it, are ignored.
     */
    fun overview(
        logs: List<DayLog>,
        settings: CycleSettings,
        today: LocalDate,
        stretches: List<ContraceptionStretch> = emptyList()
    ): CycleOverview {
        val started = stretches.filter { it.startKey <= today }.sortedBy { it.startKey }
        val hormonal = started.filter { it.method.isHormonal }
        val periods = periods(logs, today).map { it.copy(stretch = owner(it, hormonal)) }
        val cycles = cycles(periods.filter { it.stretch == null }, hormonal)
        val endedPeriods = endedPeriods(logs, periods, today)
        val current = started.lastOrNull { today in it }
        val typical = TypicalLengths(
            cycle = summarize(cycles.filterNot { it.cutShort }.mapNotNull { it.length }.takeLast(RECENT_CYCLES)),
            period = summarize(ownPeriodLengths(endedPeriods, current).takeLast(RECENT_CYCLES))
        )
        val usual = UsualLengths.of(typical, settings)
        val stopped = hormonal.lastOrNull()?.takeIf { current?.method?.isHormonal != true }?.let { latest ->
            stoppedMethod(latest, periods, today)
        }
        val estimates = when {
            current?.behaviour == MethodBehaviour.SCHEDULED_BLEED -> EstimateKind.NEXT_BLEED
            current?.behaviour == MethodBehaviour.NO_ESTIMATE -> EstimateKind.NONE
            stopped?.stretch?.method == ContraceptionMethod.INJECTION -> EstimateKind.NONE
            else -> EstimateKind.PERIOD
        }
        val contraception = ContraceptionOverview(
            stretches = started,
            current = current,
            stopped = stopped,
            estimates = estimates,
            nextBleed = current?.takeIf { estimates == EstimateKind.NEXT_BLEED }
                ?.let { nextBleed(it, periods, endedPeriods, usual, today) },
            bleeding = current?.takeIf { it.behaviour == MethodBehaviour.NO_ESTIMATE }
                ?.let { bleedingSummary(logs, it, today) },
            firstMonths = current?.started?.let { today < it.plusMonths(firstMonths(current.method)) } == true
        )
        return CycleOverview(
            today = today,
            periods = periods,
            cycles = cycles,
            typical = typical,
            estimate = if (estimates == EstimateKind.PERIOD) {
                periodEstimate(hormonal, periods, cycles, stopped, typical, usual, today)
            } else {
                null
            },
            prompts = prompts(periods, cycles, usual, settings, today),
            contraception = contraception
        )
    }

    /**
     * Her usual period length, as the estimates use it: the median of her periods that are over, or
     * her setup length, or the typical 5 days before setup. "Fill in N days" logs this many.
     */
    fun usualPeriodLength(typical: TypicalLengths, settings: CycleSettings): Int =
        UsualLengths.of(typical, settings).period

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

    /**
     * Her cycles, oldest first: each from one of her own [periods]' first day to the day before the
     * next one. A cycle running when a [hormonal] stretch starts ends the day before, cut short.
     */
    fun cycles(periods: List<Period>, hormonal: List<ContraceptionStretch> = emptyList()): List<Cycle> =
        periods.mapIndexed { index, period ->
            val next = periods.getOrNull(index + 1)?.start
            val cut = hormonal.firstOrNull { stretch ->
                val start = stretch.started
                start != null && start > period.start && (next == null || start < next)
            }
            when (cut) {
                null -> Cycle(start = period.start, end = next?.minusDays(1))
                else -> Cycle(start = period.start, end = cut.started?.minusDays(1), cutShort = true)
            }
        }

    /**
     * The hormonal stretch [period] belongs to: the one it starts in, or a combined method that
     * stopped up to [WITHDRAWAL_BLEED_DAYS] days before it starts. Null for a period of her own.
     */
    private fun owner(period: Period, hormonal: List<ContraceptionStretch>): ContraceptionStretch? =
        hormonal.firstOrNull { period.start in it } ?: hormonal.lastOrNull { stretch ->
            val stopped = stretch.stopped
            stretch.method.isCombined && stopped != null &&
                daysBetween(stopped, period.start) in 1..WITHDRAWAL_BLEED_DAYS
        }

    /**
     * The lengths of her own periods that are over, oldest first. On a copper IUD, those that started
     * since it was fitted, once one has ended.
     */
    private fun ownPeriodLengths(endedPeriods: List<Period>, current: ContraceptionStretch?): List<Int> {
        val own = endedPeriods.filter { it.stretch == null }
        val fitted = current?.takeIf { it.method == ContraceptionMethod.COPPER_IUD }?.started
        val sinceFitted = fitted?.let { day -> own.filter { it.start >= day } }.orEmpty()
        return sinceFitted.ifEmpty { own }.map { it.length }
    }

    /**
     * The hormonal stretch she stopped most recently, from the day after its stop date until her first
     * period of her own after it. Null while it is still in force.
     */
    private fun stoppedMethod(latest: ContraceptionStretch, periods: List<Period>, today: LocalDate): StoppedMethod? {
        val stopped = latest.stopped?.takeIf { it < today } ?: return null
        if (periods.any { it.stretch == null && it.start > stopped }) return null
        val days = daysBetween(stopped, today)
        return StoppedMethod(latest, days, withdrawalWindow = latest.method.isCombined && days <= WITHDRAWAL_BLEED_DAYS)
    }

    /** How many months after its start a method's first-months line, or the copper IUD's card, shows. */
    private fun firstMonths(method: ContraceptionMethod): Long = when (method) {
        ContraceptionMethod.HORMONAL_IUD -> HORMONAL_IUD_FIRST_MONTHS
        ContraceptionMethod.COPPER_IUD -> COPPER_IUD_NOTE_MONTHS
        else -> FIRST_MONTHS
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
     * Her next periods: from her last period of her own, or, before her first one after stopping a
     * hormonal method, from its stop date. After stopping one, the range stays at least ±
     * [SETTLING_SPREAD_DAYS] until she has logged [SETTLING_CYCLES] complete cycles since.
     */
    private fun periodEstimate(
        hormonal: List<ContraceptionStretch>,
        periods: List<Period>,
        cycles: List<Cycle>,
        stopped: StoppedMethod?,
        typical: TypicalLengths,
        usual: UsualLengths,
        today: LocalDate
    ): CycleEstimate? {
        val anchor = stopped?.stretch?.stopped ?: periods.lastOrNull { it.stretch == null }?.start ?: return null
        val lastStop = hormonal.mapNotNull { it.stopped }.filter { it < today }.maxOrNull()
        val settling = lastStop != null &&
            cycles.count { it.start > lastStop && it.isComplete && !it.cutShort } < SETTLING_CYCLES
        val settlingAfter = if (settling) hormonal.last { it.stopped == lastStop }.method else null
        return estimate(anchor, typical, usual, today, settlingAfter)
    }

    /**
     * The next [CYCLES_AHEAD] periods after [lastStart]. The next one is expected [UsualLengths.cycle]
     * days later, within her shortest to longest recent cycle; with no complete cycle, within ±
     * [NO_CYCLES_SPREAD_DAYS]; with up to [FEW_CYCLES], at least ± [FEW_CYCLES_MIN_SPREAD_DAYS]; while
     * [settlingAfter] a method, at least ± [SETTLING_SPREAD_DAYS]. Nothing is ever in the past: once
     * late, the next period is expected today. Each later one follows a usual cycle after the one
     * before, with the same range.
     */
    private fun estimate(
        lastStart: LocalDate,
        typical: TypicalLengths,
        usual: UsualLengths,
        today: LocalDate,
        settlingAfter: ContraceptionMethod? = null
    ): CycleEstimate {
        val median = usual.cycle
        val cycles = typical.cycle
        val minSpread = when {
            settlingAfter != null -> SETTLING_SPREAD_DAYS
            cycles == null -> NO_CYCLES_SPREAD_DAYS
            cycles.count <= FEW_CYCLES -> FEW_CYCLES_MIN_SPREAD_DAYS
            else -> 0
        }
        val shortest = minOf(cycles?.shortest ?: median, median - minSpread)
        val longest = maxOf(cycles?.longest ?: median, median + minSpread)
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
            periodBasis = usual.periodBasis,
            settlingAfter = settlingAfter
        )
    }

    /**
     * The next bleeds on [stretch], a combined method with a break every month. A bleed counts only
     * [DAYS_BEFORE_BREAK] days or more after the start date (with no start date, the first bleed
     * counts) or after the last bleed that counted; each break that passes with no bleed moves that
     * day [PACK_DAYS] later. Before a bleed counts, the bleed is expected in the first break, from the
     * start date + [DAYS_BEFORE_BREAK] to the end of the pack; after one, [PACK_DAYS] days after it, ±
     * [BLEED_SPREAD_DAYS]. Null with no start date and no bleed yet.
     */
    private fun nextBleed(
        stretch: ContraceptionStretch,
        periods: List<Period>,
        endedPeriods: List<Period>,
        usual: UsualLengths,
        today: LocalDate
    ): BleedEstimate? {
        var window = stretch.started?.let(BreakWindow::afterStart)
        val counted = mutableListOf<Period>()
        for (bleed in periods.filter { it.stretch == stretch }) {
            val passed = window?.passedBefore(bleed.start)
            window = if (passed == null || bleed.start >= passed.counts) {
                counted += bleed
                BreakWindow.afterBleed(bleed.start)
            } else {
                passed
            }
        }
        val next = window?.passedBefore(today) ?: return null
        // As long as her bleeds in the break that are over, or her usual period before one is.
        val length = summarize(counted.filter { it in endedPeriods }.map { it.length })?.median ?: usual.period
        val bleeds = (0 until CYCLES_AHEAD).map { ahead ->
            val shift = ahead.toLong() * PACK_DAYS
            EstimatedPeriod(
                expectedStart = maxOf(next.expected.plusDays(shift), today),
                earliestStart = maxOf(next.earliest.plusDays(shift), today),
                latestStart = next.latest.plusDays(shift),
                expectedLength = length
            )
        }
        return BleedEstimate(
            bleeds = bleeds,
            basis = if (next.fromBleed) BleedBasis.LAST_BLEED else BleedBasis.START_DATE,
            missedBreak = next.missed
        )
    }

    /**
     * The break a bleed is expected in, from [earliest] to [latest] ([expected] the likeliest day),
     * and the first day a bleed [counts] toward the next estimate.
     */
    private data class BreakWindow(
        val counts: LocalDate,
        val earliest: LocalDate,
        val expected: LocalDate,
        val latest: LocalDate,
        val fromBleed: Boolean,
        val missed: Boolean = false
    ) {
        /** The window, moved a pack later for each break that ended before [date] with no bleed. */
        fun passedBefore(date: LocalDate): BreakWindow {
            var window = this
            while (window.latest < date) {
                window = window.copy(
                    counts = window.counts.plusDays(PACK_DAYS.toLong()),
                    earliest = window.earliest.plusDays(PACK_DAYS.toLong()),
                    expected = window.expected.plusDays(PACK_DAYS.toLong()),
                    latest = window.latest.plusDays(PACK_DAYS.toLong()),
                    missed = true
                )
            }
            return window
        }

        companion object {
            /** The first break: the last days of the first pack, shown as a range only. */
            fun afterStart(start: LocalDate) = BreakWindow(
                counts = start.plusDays(DAYS_BEFORE_BREAK.toLong()),
                earliest = start.plusDays(DAYS_BEFORE_BREAK.toLong()),
                expected = start.plusDays(DAYS_BEFORE_BREAK.toLong()),
                latest = start.plusDays(PACK_DAYS - 1L),
                fromBleed = false
            )

            /** The break after a bleed that counted on [start]: a pack later. */
            fun afterBleed(start: LocalDate): BreakWindow {
                val expected = start.plusDays(PACK_DAYS.toLong())
                return BreakWindow(
                    counts = start.plusDays(DAYS_BEFORE_BREAK.toLong()),
                    earliest = expected.minusDays(BLEED_SPREAD_DAYS.toLong()),
                    expected = expected,
                    latest = expected.plusDays(BLEED_SPREAD_DAYS.toLong()),
                    fromBleed = true
                )
            }
        }
    }

    /**
     * Her bleeding over [stretch]'s last [BLEEDING_SUMMARY_DAYS] days up to [today] (or its stop
     * date), or since its start when that is later. A bleeding day is a period day by `0003`'s
     * rules or a spotting day; an episode is a run of them in a row.
     */
    fun bleedingSummary(logs: List<DayLog>, stretch: ContraceptionStretch, today: LocalDate): BleedingSummary {
        val to = minOf(today, stretch.stopKey)
        val ninetyDays = to.minusDays(BLEEDING_SUMMARY_DAYS - 1L)
        val from = stretch.started?.takeIf { it > ninetyDays } ?: ninetyDays
        val periods = periods(logs, today)
        val byDate = logs.associateBy { it.date }
        val bleeding = generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }.map { date ->
            val log = byDate[date]
            log?.flow == FlowLevel.SPOTTING ||
                (periods.any { date in it.start..it.end } && log?.let { it.isPeriodDay || it.flow == null } != false)
        }.toList()
        val episodes = mutableListOf<Int>()
        var run = 0
        for (day in bleeding + false) {
            if (day) {
                run++
            } else if (run > 0) {
                episodes += run
                run = 0
            }
        }
        return BleedingSummary(
            from = from,
            to = to,
            sinceStart = from != ninetyDays,
            days = episodes.sum(),
            episodes = episodes.size,
            longest = episodes.maxOrNull() ?: 0
        )
    }

    /**
     * "Still going?" once her open period (or bleeding) reaches her usual period length +
     * [STILL_GOING_EXTRA_DAYS]; "missed a period?" once her current cycle of her own reaches
     * [MISSED_PERIOD_PERCENT] % of her usual cycle, outside a period: never on a hormonal method or
     * before her first period after stopping one, where there is no current cycle. Each is asked once
     * per cycle: not after she dismissed it.
     */
    private fun prompts(
        periods: List<Period>,
        cycles: List<Cycle>,
        usual: UsualLengths,
        settings: CycleSettings,
        today: LocalDate
    ): List<CyclePrompt> {
        val last = periods.lastOrNull() ?: return emptyList()
        val prompt = when {
            last.isOpen -> {
                val day = daysBetween(last.start, today) + 1
                CyclePrompt.StillGoing(last.start, day)
                    .takeIf {
                        day >= usual.period + STILL_GOING_EXTRA_DAYS &&
                            last.start !in settings.dismissedStillGoing
                    }
            }

            else -> cycles.lastOrNull()?.takeIf { !it.isComplete }?.let { cycle ->
                val day = cycle.dayOf(today)
                CyclePrompt.MissedPeriod(cycle.start, day)
                    .takeIf {
                        day * 100 >= usual.cycle * MISSED_PERIOD_PERCENT &&
                            cycle.start !in settings.dismissedMissedPeriod
                    }
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
