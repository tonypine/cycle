package com.tonypine.cycle.feature.history

import com.tonypine.cycle.core.domain.CycleCalculator
import com.tonypine.cycle.core.model.BleedingSummary
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.LogCategory
import com.tonypine.cycle.core.model.MethodBehaviour
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.SexualActivity
import com.tonypine.cycle.core.model.SleepQuality
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** What History shows. Built from her overview by [HistoryUiState.from]; the screen draws it as is. */
sealed interface HistoryUiState {
    /** Her log has not been read yet. */
    data object Loading : HistoryUiState

    /**
     * No complete cycle and no time on a hormonal method yet. [hasPeriod] when she has logged a
     * period: her first cycle is still going, and History fills in once the next period starts.
     */
    data class Empty(val hasPeriod: Boolean) : HistoryUiState

    /**
     * She has at least one complete cycle of her own, or time on a hormonal method.
     *
     * @property typicalCycle her typical cycle length, from her last complete cycles of her own; null
     *   when she has none, such as on a method since before she installed Cycle: the card is left out.
     * @property typicalPeriod her typical period length, from her last ended periods.
     * @property leftOut some of her time was on a hormonal method, so her typical cycle leaves it out
     *   and says so.
     * @property entries her cycles and her time on each hormonal method, newest first: the current
     *   cycle or method first.
     */
    data class Cycles(
        val today: LocalDate,
        val typicalCycle: LengthSummary?,
        val typicalPeriod: LengthSummary?,
        val entries: List<HistoryEntry>,
        val leftOut: Boolean = false
    ) : HistoryUiState {
        /** Her cycles alone, newest first. */
        val cycles: List<CycleSummary>
            get() = entries.filterIsInstance<CycleSummary>()
    }

    companion object {
        /** History from her [overview] and every day she logged, [logs], for the bleeding counts. */
        fun from(overview: CycleOverview, logs: List<DayLog>): HistoryUiState {
            val typicalCycle = overview.typical.cycle
            val methods = overview.contraception.stretches
                .filter { it.method.isHormonal }
                .map { methodSummary(it, overview, logs) }
            if (typicalCycle == null && methods.isEmpty()) return Empty(hasPeriod = overview.periods.isNotEmpty())
            return Cycles(
                today = overview.today,
                typicalCycle = typicalCycle,
                typicalPeriod = overview.typical.period,
                entries = (cycleSummaries(overview) + methods).sortedByDescending { it.startKey },
                leftOut = methods.isNotEmpty()
            )
        }

        private fun methodSummary(stretch: ContraceptionStretch, overview: CycleOverview, logs: List<DayLog>) =
            MethodSummary(
                stretch = stretch,
                isCurrent = overview.today in stretch,
                lastDay = minOf(overview.today, stretch.stopKey),
                bleeding = when (stretch.behaviour) {
                    MethodBehaviour.SCHEDULED_BLEED -> MethodBleeding.Bleeds(
                        overview.periods.count { it.stretch == stretch }
                    )

                    else -> MethodBleeding.Days(CycleCalculator.bleedingSummary(logs, stretch, overview.today))
                }
            )
    }
}

/** One card in History's list: a cycle, or her time on a hormonal method. */
sealed interface HistoryEntry {
    /** Where it goes in the list, newest first: its first day, or earlier than any for an unknown start. */
    val startKey: LocalDate
}

/**
 * One cycle and the period it starts with.
 *
 * @property end the cycle's last day, or null for the current cycle.
 * @property length the days in the cycle; for the current cycle, its days so far, today included.
 * @property cutShortBy the hormonal method she started the day after [end]: the cycle is shown, and
 *   never counted in her typical cycle.
 */
data class CycleSummary(
    val start: LocalDate,
    val end: LocalDate?,
    val length: Int,
    val period: Period,
    val cutShortBy: ContraceptionMethod? = null
) : HistoryEntry {
    val isCurrent: Boolean
        get() = end == null

    override val startKey: LocalDate
        get() = start

    /** The day of this cycle [date] falls on: its first day is day 1. */
    fun dayOf(date: LocalDate): Int = ChronoUnit.DAYS.between(start, date).toInt() + 1

    /** The month the calendar opens on for this cycle: the one it started in. */
    val month: YearMonth
        get() = YearMonth.from(start)
}

/**
 * Her time on one hormonal method (`docs/decisions/0006-contraception.md`): one card, marked with
 * the method, and left out of her typical cycle.
 *
 * @property isCurrent she is still on it today.
 * @property lastDay the last day it covers so far: today while she is on it, else its stop date.
 */
data class MethodSummary(
    val stretch: ContraceptionStretch,
    val isCurrent: Boolean,
    val lastDay: LocalDate,
    val bleeding: MethodBleeding
) : HistoryEntry {
    override val startKey: LocalDate
        get() = stretch.startKey

    /** The month "See it in the calendar" opens on: the one of its [lastDay]. */
    val month: YearMonth
        get() = YearMonth.from(lastDay)
}

/** What a method's card counts of her bleeding on it. */
sealed interface MethodBleeding {
    /** A method with no estimate: her bleeding or spotting over its last 90 days. */
    data class Days(val summary: BleedingSummary) : MethodBleeding

    /** A combined method with a break every month: the [count] of bleeds she logged on it. */
    data class Bleeds(val count: Int) : MethodBleeding
}

/** What the cycle detail shows. Built by [CycleDetailUiState.from]. */
sealed interface CycleDetailUiState {
    /** Her log has not been read yet. */
    data object Loading : CycleDetailUiState

    /** No cycle starts on that day any more: an edit to her log moved or removed its period. */
    data object Missing : CycleDetailUiState

    /**
     * The [cycle], the [flow] she logged on each day of its period, first day first, and how she
     * felt during it, without the categories she hid.
     *
     * @property symptoms each thing she logged with the cycle days she logged it on, in the day log's
     *   order: pain, body, mood, energy, sleep, then sex.
     * @property notes her notes, first day first.
     */
    data class Detail(
        val today: LocalDate,
        val cycle: CycleSummary,
        val flow: List<FlowDay>,
        val symptoms: List<SymptomDays> = emptyList(),
        val notes: List<CycleNote> = emptyList()
    ) : CycleDetailUiState

    companion object {
        /**
         * The cycle that starts on [start], from her [overview], her [logs] and how she felt each
         * day ([feelings]), leaving out the categories she [hidden].
         */
        fun from(
            overview: CycleOverview,
            logs: List<DayLog>,
            start: LocalDate,
            feelings: List<DayFeelings> = emptyList(),
            hidden: Set<LogCategory> = emptySet()
        ): CycleDetailUiState {
            val cycle = cycleSummaries(overview).firstOrNull { it.start == start } ?: return Missing
            val flowByDate = logs.associate { it.date to it.flow }
            val flow = generateSequence(cycle.period.start) { it.plusDays(1) }
                .takeWhile { it <= cycle.period.end }
                .map { FlowDay(it, flowByDate[it]) }
                .toList()
            val days = feelings
                .filter { it.date >= cycle.start && (cycle.end == null || it.date <= cycle.end) }
                .sortedBy { it.date }
                .map { cycle.dayOf(it.date) to it.without(hidden) }
            val daysBySymptom = days
                .flatMap { (day, felt) -> felt.symptoms().map { it to day } }
                .groupBy({ it.first }, { it.second })
            return Detail(
                today = overview.today,
                cycle = cycle,
                flow = flow,
                symptoms = Symptom.all.mapNotNull { symptom ->
                    daysBySymptom[symptom]?.let { SymptomDays(symptom, it.distinct()) }
                },
                notes = days.filter { (_, felt) ->
                    felt.note.isNotEmpty()
                }.map { (day, felt) -> CycleNote(day, felt.note) }
            )
        }
    }
}

/**
 * One thing she can log about how she felt, as the cycle detail lists it: pain counts once per
 * place, with the day's level, so "Cramps, moderate" and "Lower back, moderate" are two.
 */
sealed interface Symptom {
    /** Pain at [level] in one [place], or with nowhere said when [place] is null. */
    data class Pain(val level: PainLevel, val place: PainKind?) : Symptom

    data class Body(val symptom: BodySymptom) : Symptom

    data class Feeling(val mood: Mood) : Symptom

    data class Energy(val level: EnergyLevel) : Symptom

    data class Sleep(val quality: SleepQuality) : Symptom

    /** Sensitive: in the app only, never in a notification or widget. */
    data class Sex(val activity: SexualActivity) : Symptom

    companion object {
        /**
         * Every symptom in the day log's order: pain with nowhere said, then by place, each from
         * mild to severe; then body, mood, energy, sleep and sex.
         */
        val all: List<Symptom> =
            PainLevel.entries.map { Pain(it, place = null) } +
                PainKind.entries.flatMap { place ->
                    PainLevel.entries.filter { it != PainLevel.NONE }.map { Pain(it, place) }
                } +
                BodySymptom.entries.map(::Body) +
                Mood.entries.map(::Feeling) +
                EnergyLevel.entries.map(::Energy) +
                SleepQuality.entries.map(::Sleep) +
                SexualActivity.entries.map(::Sex)
    }
}

/** A [symptom] and the [days] of the cycle she logged it on, day 1 first. */
data class SymptomDays(val symptom: Symptom, val days: List<Int>)

/** Her note on [day] of the cycle. */
data class CycleNote(val day: Int, val text: String)

/** What she logged on a day, one [Symptom] per thing. */
private fun DayFeelings.symptoms(): List<Symptom> = buildList {
    pain?.let { pain ->
        if (pain.kinds.isEmpty() || pain.level == PainLevel.NONE) {
            add(Symptom.Pain(pain.level, place = null))
        } else {
            pain.kinds.forEach { add(Symptom.Pain(pain.level, it)) }
        }
    }
    body.forEach { add(Symptom.Body(it)) }
    moods.forEach { add(Symptom.Feeling(it)) }
    energy?.let { add(Symptom.Energy(it)) }
    sleep?.let { add(Symptom.Sleep(it)) }
    sex?.let { add(Symptom.Sex(it)) }
}

/**
 * [days], first day first, as the detail writes them: three or more days in a row become one range,
 * "24–27"; one or two stay single, "1, 2".
 */
internal fun dayRuns(days: List<Int>): List<IntRange> {
    val runs = mutableListOf<IntRange>()
    days.sorted().distinct().forEach { day ->
        val last = runs.lastOrNull()
        if (last != null && last.last == day - 1) runs[runs.lastIndex] = last.first..day else runs += day..day
    }
    return runs.flatMap { run -> if (run.count() >= 3) listOf(run) else run.map { it..it } }
}

/** A day of a period and its [flow], or null when she logged none that day. */
data class FlowDay(val date: LocalDate, val flow: FlowLevel?)

/**
 * Her cycles, oldest first, each with its period: the overview has one period of her own per cycle,
 * and the bleeding on a hormonal method belongs to no cycle. A cycle cut short says which method cut
 * it.
 */
internal fun cycleSummaries(overview: CycleOverview): List<CycleSummary> =
    overview.cycles.zip(overview.periods.filter { it.stretch == null }) { cycle, period ->
        CycleSummary(
            start = cycle.start,
            end = cycle.end,
            length = cycle.length ?: cycle.dayOf(overview.today),
            period = period,
            cutShortBy = cycle.end?.takeIf { cycle.cutShort }?.let { end ->
                overview.contraception.stretches.firstOrNull { it.method.isHormonal && it.started == end.plusDays(1) }
                    ?.method
            }
        )
    }
