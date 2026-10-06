package com.tonypine.cycle.feature.calendar

import com.tonypine.cycle.core.data.repository.LogOverview
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.EstimateKind
import com.tonypine.cycle.core.ui.CalendarDays
import com.tonypine.cycle.core.ui.CalendarLegend
import com.tonypine.cycle.core.ui.DayLogEntry
import java.time.LocalDate
import java.time.YearMonth

/** What the calendar shows. Built from her log by [CalendarUiState.from]; the screen draws it as is. */
sealed interface CalendarUiState {
    /** Her log has not been read yet. */
    data object Loading : CalendarUiState

    /**
     * @property month the month on screen.
     * @property selected the day she last tapped, framed, whose day log sheet opens.
     * @property days her logged periods and the next three estimated ones (or expected bleeds), for the
     *   day cells; nothing estimated on a method with no estimate.
     * @property selectedLog what the day log sheet shows for [selected].
     * @property hint what the line under the legend says is estimated, if anything.
     */
    data class Ready(
        val today: LocalDate,
        val month: YearMonth,
        val selected: LocalDate?,
        val days: CalendarDays,
        val selectedLog: DayLogEntry?,
        val hint: CalendarHint = CalendarHint.Periods
    ) : CalendarUiState {
        /** Days up to today can be logged; the days after show, in full colour, but do nothing. */
        fun canLog(date: LocalDate): Boolean = date <= today

        /** The legend for the month on screen: only what it shows, in its words. */
        val legend: CalendarLegend
            get() = days.legend((1..month.lengthOfMonth()).map(month::atDay), today)
    }

    companion object {
        fun from(log: LogOverview, month: YearMonth, selected: LocalDate?): CalendarUiState {
            val days = CalendarDays.from(log.overview, log.logs)
            val today = log.overview.today
            val day = selected?.takeIf { it <= today }
            val contraception = log.overview.contraception
            val current = contraception.current
            val hint = when (contraception.estimates) {
                EstimateKind.PERIOD -> CalendarHint.Periods

                EstimateKind.NEXT_BLEED ->
                    if (contraception.nextBleed != null) CalendarHint.Bleeds else CalendarHint.FirstBleed

                EstimateKind.NONE -> current?.let { CalendarHint.NoEstimate(it.method) } ?: CalendarHint.AfterInjection
            }
            return Ready(
                today = today,
                month = month,
                selected = day,
                days = days,
                hint = hint,
                selectedLog = day?.let { date ->
                    DayLogEntry(
                        date = date,
                        flow = log.log(date).flow,
                        fillDays = log.usualPeriodLength.takeIf { log.canFill(date) },
                        canClear = log.canClear(date),
                        isPeriodDay = days.isPeriodDay(date),
                        feelings = log.feelings(date),
                        hiddenCategories = log.hiddenCategories
                    )
                }
            )
        }
    }
}

/** What the line under the calendar's legend says is estimated. */
sealed interface CalendarHint {
    /** "Predicted periods are estimates." */
    data object Periods : CalendarHint

    /** "Expected bleeds are estimates.", on a combined method with a break every month. */
    data object Bleeds : CalendarHint

    /** The same method with no start date and no bleed logged yet: nothing expected until she logs one. */
    data object FirstBleed : CalendarHint

    /** "Cycle doesn't estimate bleeding on the implant." */
    data class NoEstimate(val method: ContraceptionMethod) : CalendarHint

    /** After the injection, until her first period. */
    data object AfterInjection : CalendarHint
}
