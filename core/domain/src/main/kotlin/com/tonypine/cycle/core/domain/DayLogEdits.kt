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
     * period is still going. Whatever else those days hold stays, except a stray "ended" on a day
     * before the last, which would close the period early. Nothing, when [canFill] is false.
     */
    fun fill(logs: List<DayLog>, start: LocalDate, length: Int, today: LocalDate): List<DayLog> {
        if (!canFill(logs, start, length, today)) return emptyList()
        val days = logs.associateBy { it.date }
        val end = start.plusDays(length - 1L)
        val ends = end < today
        val first = (days[start] ?: DayLog(start)).copy(periodStarted = true, periodEnded = ends && end == start)
        val last = (days[end] ?: DayLog(end)).copy(periodEnded = true).takeIf { ends && end != start }
        val stray = logs.filter {
            it.periodEnded && it.date > start && it.date < end
        }.map { it.copy(periodEnded = false) }
        return (listOfNotNull(first, last) + stray).sortedBy { it.date }
    }

    /**
     * "Clear this day": what she logged on [date] goes and the day no longer counts as a period day,
     * while the rest of its period stays. On a period's first day, "started" moves to the next day
     * that still draws as a period day; on its last, the nearest such day before becomes the last,
     * marked ended, which also closes a period still going today. Days she already cleared are
     * skipped, so they stay cleared; when none is left, the period goes. A day in the middle is
     * logged as no flow: a gap of one day stays inside the period, so it neither splits it nor
     * invents a cycle of a few days.
     */
    fun clear(logs: List<DayLog>, date: LocalDate, today: LocalDate): List<DayLog> {
        val days = logs.associateBy { it.date }
        val own = days[date] ?: DayLog(date)
        val period = CycleCalculator.periods(logs, today).firstOrNull { date in it.start..it.end }
        val cleared = DayLog(date)

        // Marks the nearest day from [from] towards [to] that still draws as a period day.
        fun moveMarker(from: LocalDate, to: LocalDate, mark: (DayLog) -> DayLog): List<DayLog> {
            val step = if (to >= from) 1L else -1L
            val target = generateSequence(from) { it.plusDays(step) }
                .takeWhile { if (step > 0) it <= to else it >= to }
                .map { days[it] ?: DayLog(it) }
                .firstOrNull { !it.isCleared }
            return listOfNotNull(cleared, target?.let(mark))
        }

        return when {
            period == null || period.start == period.end -> listOf(cleared)

            date == period.start -> if (own.periodStarted) {
                moveMarker(date.plusDays(1), period.end) { it.copy(periodStarted = true) }
            } else {
                listOf(cleared)
            }

            date == period.end -> if (own.periodEnded || period.isOpen) {
                moveMarker(date.minusDays(1), period.start) { it.copy(periodEnded = true) }
            } else {
                listOf(cleared)
            }

            else -> listOf(DayLog(date, flow = FlowLevel.NONE))
        }
    }

    /** [clear] on [date] would change her log. */
    fun canClear(logs: List<DayLog>, date: LocalDate, today: LocalDate): Boolean {
        val days = logs.associateBy { it.date }
        return clear(logs, date, today).any { it != (days[it.date] ?: DayLog(it.date)) }
    }

    /**
     * Logged with no period flow and no marker, the way [clear] leaves a day in the middle: it draws
     * plain, even inside a period.
     */
    private val DayLog.isCleared: Boolean
        get() = !isEmpty && !isPeriodDay
}
