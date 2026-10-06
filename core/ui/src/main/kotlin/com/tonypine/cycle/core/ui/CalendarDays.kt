package com.tonypine.cycle.core.ui

import com.tonypine.cycle.core.designsystem.BleedingWords
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleLegendEntry
import com.tonypine.cycle.core.model.BleedBasis
import com.tonypine.cycle.core.model.BleedingWord
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.bleedingWord
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * The cycle state of every calendar day, for Today's week and the calendar's month.
 *
 * @property periods her logged periods, and her bleeding on a method.
 * @property predicted the estimated periods ahead, first to last day each, or on a combined method
 *   with a break every month the expected bleeds. Nothing on a method with no estimate.
 * @property withoutPeriodFlow the days she logged with no period flow and no period mark (none or
 *   spotting): inside a period they are gap days, which still count towards it but draw plain.
 * @property stretches her contraception, which names each day's bleeding ([wordsOf]).
 */
data class CalendarDays(
    val periods: List<Period>,
    val predicted: List<ClosedRange<LocalDate>>,
    val withoutPeriodFlow: Set<LocalDate> = emptySet(),
    val stretches: List<ContraceptionStretch> = emptyList()
) {
    /** The day cell state of [date]: a logged period wins over a predicted one. */
    fun stateOf(date: LocalDate): CycleDayState = when {
        isPeriodDay(date) -> CycleDayState.Period
        predicted.any { date in it } -> CycleDayState.PredictedPeriod
        else -> CycleDayState.Plain
    }

    /** [date] draws as a logged period day. */
    fun isPeriodDay(date: LocalDate): Boolean = date !in withoutPeriodFlow && periods.any { date in it.start..it.end }

    /** What her bleeding on [date] is called: "period", or "bleed" or "bleeding" on a method. */
    fun wordsOf(date: LocalDate): BleedingWords = bleedingWord(date, periods, stretches).words

    /**
     * The legend for [days] on screen, with [today]: the logged days in each word they show in, or when
     * none shows the word of the day on screen nearest [today] (today itself when it shows); the
     * predicted days only when some show, in the first one's word; then today.
     */
    fun legend(days: List<LocalDate>, today: LocalDate): CalendarLegend {
        val nearest = wordsOf(days.minByOrNull { abs(ChronoUnit.DAYS.between(it, today)) } ?: today)
        val words = days.filter(::isPeriodDay).map(::wordsOf).distinct().ifEmpty { listOf(nearest) }
        val predicted = days.firstOrNull { stateOf(it) == CycleDayState.PredictedPeriod }
        val entries = listOfNotNull(
            CycleLegendEntry.Period,
            CycleLegendEntry.PredictedPeriod.takeIf { predicted != null },
            CycleLegendEntry.Today.takeIf { today in days }
        )
        return CalendarLegend(entries, words, predicted?.let(::wordsOf) ?: nearest)
    }

    companion object {
        val Empty = CalendarDays(emptyList(), emptyList())

        /**
         * The days of her [overview], with what she [logged][logs] on each. On a combined method with a
         * break every month the expected bleeds are drawn in place of periods; each first break, before
         * a bleed has counted, as its whole range.
         */
        fun from(overview: CycleOverview, logs: List<DayLog>): CalendarDays {
            val bleeds = overview.contraception.nextBleed
            val predicted = when {
                bleeds == null -> overview.estimate?.periods.orEmpty().map { it.expectedStart..it.expectedEnd }
                bleeds.basis == BleedBasis.START_DATE -> bleeds.bleeds.map { it.earliestStart..it.latestStart }
                else -> bleeds.bleeds.map { it.expectedStart..it.expectedEnd }
            }
            return CalendarDays(
                periods = overview.periods,
                predicted = predicted,
                withoutPeriodFlow = logs.filter { !it.isEmpty && !it.isPeriodDay }.mapTo(mutableSetOf()) { it.date },
                stretches = overview.contraception.stretches
            )
        }
    }
}

/**
 * What [CycleLegend][com.tonypine.cycle.core.designsystem.CycleLegend] shows under a calendar: its
 * [entries], the [words] of the logged days and the [predictedWords] of the predicted ones.
 */
data class CalendarLegend(
    val entries: List<CycleLegendEntry>,
    val words: List<BleedingWords>,
    val predictedWords: BleedingWords
)

/** The design system's words for her bleeding. */
val BleedingWord.words: BleedingWords
    get() = when (this) {
        BleedingWord.PERIOD -> BleedingWords.Period
        BleedingWord.BLEED -> BleedingWords.Bleed
        BleedingWord.BLEEDING -> BleedingWords.Bleeding
    }
