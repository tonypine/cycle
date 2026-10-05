package com.tonypine.cycle.feature.calendar

import com.tonypine.cycle.core.data.repository.LogOverview
import com.tonypine.cycle.core.ui.CalendarDays
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
     * @property days her logged periods and the next three estimated ones, for the day cells.
     * @property selectedLog what the day log sheet shows for [selected].
     */
    data class Ready(
        val today: LocalDate,
        val month: YearMonth,
        val selected: LocalDate?,
        val days: CalendarDays,
        val selectedLog: DayLogEntry?
    ) : CalendarUiState {
        /** Days up to today can be logged; the days after show, in full colour, but do nothing. */
        fun canLog(date: LocalDate): Boolean = date <= today
    }

    companion object {
        fun from(log: LogOverview, month: YearMonth, selected: LocalDate?): CalendarUiState {
            val days = CalendarDays.from(log.overview, log.logs)
            val today = log.overview.today
            val day = selected?.takeIf { it <= today }
            return Ready(
                today = today,
                month = month,
                selected = day,
                days = days,
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
