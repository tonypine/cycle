package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.domain.CycleRules.MAX_GAP_DAYS
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate

/**
 * The edits the day log sheet makes to any day up to today, as the days to write: each returned log
 * replaces what is stored for its day, and an empty log removes the day. Pure, like
 * [CycleCalculator], which recomputes everything from the result. The rules are in
 * `docs/decisions/0003-cycle-estimates.md`.
 */
object DayLogEdits {
    /**
     * "Period started this day: fill in [length] days" is offered on [start]: a day up to [today]
     * with no period near enough that the filled days would join it, within a gap of
     * [MAX_GAP_DAYS] on either side.
     */
    fun canFill(logs: List<DayLog>, start: LocalDate, length: Int, today: LocalDate): Boolean {
        if (start > today || length < 1) return false
        val reach = MAX_GAP_DAYS + 1L
        val from = start.minusDays(reach)
        val to = start.plusDays(length - 1 + reach)
        return CycleCalculator.periods(logs, today).none { it.start <= to && it.end >= from }
    }

    /**
     * Logs a period of [length] days from [start], the way "Log a period" does on Today: [start]
     * marked started, and its last day marked ended when that is before [today]; otherwise the
     * period is still going. Whatever else those days hold stays. Nothing, when [canFill] is false.
     */
    fun fill(logs: List<DayLog>, start: LocalDate, length: Int, today: LocalDate): List<DayLog> {
        if (!canFill(logs, start, length, today)) return emptyList()
        val days = logs.associateBy { it.date }
        val end = start.plusDays(length - 1L)
        val first = (days[start] ?: DayLog(start)).copy(periodStarted = true)
        return when {
            end >= today -> listOf(first)
            end == start -> listOf(first.copy(periodEnded = true))
            else -> listOf(first, (days[end] ?: DayLog(end)).copy(periodEnded = true))
        }
    }

    /**
     * "Clear this day": what she logged on [date] goes and the day no longer counts as a period day,
     * while the rest of its period stays. On a period's first day, "started" moves to the next day;
     * on its last, the day before becomes the last, marked ended, which also closes a period still
     * going today. A day in the middle is logged as no flow: a gap of one day stays inside the
     * period, so it neither splits it nor invents a cycle of a few days.
     */
    fun clear(logs: List<DayLog>, date: LocalDate, today: LocalDate): List<DayLog> {
        val days = logs.associateBy { it.date }
        val own = days[date] ?: DayLog(date)
        val period = CycleCalculator.periods(logs, today).firstOrNull { date in it.start..it.end }
        val cleared = DayLog(date)
        return when {
            period == null || period.start == period.end -> listOf(cleared)

            date == period.start -> {
                val next = date.plusDays(1)
                if (own.periodStarted) {
                    listOf(cleared, (days[next] ?: DayLog(next)).copy(periodStarted = true))
                } else {
                    listOf(cleared)
                }
            }

            date == period.end -> {
                val before = date.minusDays(1)
                if (own.periodEnded || period.isOpen) {
                    listOf(cleared, (days[before] ?: DayLog(before)).copy(periodEnded = true))
                } else {
                    listOf(cleared)
                }
            }

            else -> listOf(DayLog(date, flow = FlowLevel.NONE))
        }
    }

    /** [clear] on [date] would change her log. */
    fun canClear(logs: List<DayLog>, date: LocalDate, today: LocalDate): Boolean {
        val days = logs.associateBy { it.date }
        return clear(logs, date, today).any { it != (days[it.date] ?: DayLog(it.date)) }
    }
}
