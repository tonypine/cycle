package com.tonypine.cycle.core.ui

import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.Period
import java.time.LocalDate

/**
 * The cycle state of every calendar day, for Today's week and the calendar's month.
 *
 * @property periods her logged periods.
 * @property predicted the estimated periods ahead, first to last day each.
 * @property withoutPeriodFlow the days she logged with no period flow and no period mark (none or
 *   spotting): inside a period they are gap days, which still count towards it but draw plain.
 */
data class CalendarDays(
    val periods: List<Period>,
    val predicted: List<ClosedRange<LocalDate>>,
    val withoutPeriodFlow: Set<LocalDate> = emptySet()
) {
    /** The day cell state of [date]: a logged period wins over a predicted one. */
    fun stateOf(date: LocalDate): CycleDayState = when {
        isPeriodDay(date) -> CycleDayState.Period
        predicted.any { date in it } -> CycleDayState.PredictedPeriod
        else -> CycleDayState.Plain
    }

    /** [date] draws as a logged period day. */
    fun isPeriodDay(date: LocalDate): Boolean = date !in withoutPeriodFlow && periods.any { date in it.start..it.end }

    companion object {
        val Empty = CalendarDays(emptyList(), emptyList())

        /** The days of her [overview], with what she [logged][logs] on each. */
        fun from(overview: CycleOverview, logs: List<DayLog>): CalendarDays = CalendarDays(
            periods = overview.periods,
            predicted = overview.estimate?.periods.orEmpty().map { it.expectedStart..it.expectedEnd },
            withoutPeriodFlow = logs.filter { !it.isEmpty && !it.isPeriodDay }.mapTo(mutableSetOf()) { it.date }
        )
    }
}
