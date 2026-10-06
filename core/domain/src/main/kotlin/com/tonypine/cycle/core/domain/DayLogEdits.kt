package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.domain.CycleRules.MAX_GAP_DAYS
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.PeriodRefusal
import java.time.LocalDate

/** What saving a period's new dates would write, or why it cannot be saved. */
sealed interface PeriodPlan {
    /** [writes] replace what is stored for their days; an empty log removes the day. */
    data class Ready(val writes: List<DayLog>) : PeriodPlan

    data class Refused(val reason: PeriodRefusal) : PeriodPlan
}

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
     * logged as no flow and the period keeps its days, so clearing it neither splits the period nor
     * invents a cycle of a few days. A gap of one day stays inside a period on its own; when the
     * cleared day would cut it short (a wider gap in a period logged as flow alone, or the day held
     * its "started"), its first day is marked started and, unless it is still going, its last day
     * ended.
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

            else -> {
                val gap = DayLog(date, flow = FlowLevel.NONE)
                val whole = CycleCalculator.periods(logs.filterNot { it.date == date } + gap, today)
                    .any { it == period }
                if (whole) {
                    listOf(gap)
                } else {
                    val first = (days[period.start] ?: DayLog(period.start)).copy(periodStarted = true)
                    val last = (days[period.end] ?: DayLog(period.end)).copy(periodEnded = true)
                        .takeUnless { period.isOpen }
                    listOfNotNull(first, gap, last)
                }
            }
        }
    }

    /**
     * "Edit period dates" in History: the period that starts on [start] runs from [newStart] to
     * [newEnd], or from [newStart] and is still going when [newEnd] is null. Its first day is marked
     * started and, unless it is still going, its last day ended; a stray marker between them goes, so
     * nothing splits it or ends it early. The days it no longer covers lose their period flow and
     * markers, while none, spotting and how she felt stay. When the period before it would run on
     * into the days this one leaves, its last day is marked ended, so every other period keeps its
     * days.
     *
     * Refused when no period starts on [start] any more, when a day is after [today] or the last
     * comes before the first, and when the new days reach another period or come within
     * [MAX_GAP_DAYS] of it, which would join the two: still going with a later period does.
     */
    fun editPeriod(
        logs: List<DayLog>,
        start: LocalDate,
        newStart: LocalDate,
        newEnd: LocalDate?,
        today: LocalDate
    ): PeriodPlan {
        val periods = CycleCalculator.periods(logs, today)
        val period = periods.firstOrNull { it.start == start } ?: return PeriodPlan.Refused(PeriodRefusal.Gone)
        val end = newEnd ?: today
        if (newStart > today || end > today) return PeriodPlan.Refused(PeriodRefusal.AfterToday)
        if (end < newStart) return PeriodPlan.Refused(PeriodRefusal.EndBeforeStart)
        val reach = MAX_GAP_DAYS + 1L
        periods.firstOrNull {
            it != period && it.start <= end.plusDays(reach) && it.end >= newStart.minusDays(reach)
        }?.let { return PeriodPlan.Refused(PeriodRefusal.TooClose(it)) }

        val days = logs.associateBy { it.date }
        val writes = logs.filter { it.date in period.start..period.end && it.date !in newStart..end }
            .associate { it.date to it.withoutBleeding() }
            .toMutableMap()
        // Inside the new days only the first is started and only the last ended. Still going, a later
        // "ended" on its own would close it, so every one up to today goes.
        logs.filter { it.date in newStart..end }.forEach { log ->
            writes[log.date] = log.copy(
                periodStarted = log.periodStarted && log.date == newStart,
                periodEnded = log.periodEnded && log.date == newEnd
            )
        }
        writes[newStart] = (writes[newStart] ?: DayLog(newStart)).copy(periodStarted = true)
        if (newEnd != null) writes[newEnd] = (writes[newEnd] ?: DayLog(newEnd)).copy(periodEnded = true)
        val changed = writes.values.filter { it != (days[it.date] ?: DayLog(it.date)) }
        return PeriodPlan.Ready(keepBefore(logs, changed, periods, period, today).sortedBy { it.date })
    }

    /**
     * "Delete this period" in History: every day of the period that starts on [start] loses its
     * period flow and markers, so the cycle it began joins the one before. None, spotting and how she
     * felt stay. When the period before it would run on into the days this one leaves, its last day
     * is marked ended, so every other period keeps its days. Nothing, when no period starts on
     * [start].
     */
    fun deletePeriod(logs: List<DayLog>, start: LocalDate, today: LocalDate): List<DayLog> {
        val periods = CycleCalculator.periods(logs, today)
        val period = periods.firstOrNull { it.start == start } ?: return emptyList()
        val cleared = logs.filter { it.date in period.start..period.end && it != it.withoutBleeding() }
            .map { it.withoutBleeding() }
        return keepBefore(logs, cleared, periods, period, today).sortedBy { it.date }
    }

    /**
     * [writes], and the last day of the period before [period] marked ended when [writes] would
     * change that period: one she marked started and never ended, or a lone "ended" after [period]
     * that was ignored, would take in the days [period] gives up.
     */
    private fun keepBefore(
        logs: List<DayLog>,
        writes: List<DayLog>,
        periods: List<Period>,
        period: Period,
        today: LocalDate
    ): List<DayLog> {
        val before = periods.getOrNull(periods.indexOf(period) - 1) ?: return writes
        val written = writes.associateBy { it.date }
        val after = (logs.filterNot { it.date in written } + writes).filterNot { it.isEmpty }
        if (CycleCalculator.periods(after, today).any { it == before }) return writes
        val last = written[before.end] ?: logs.firstOrNull { it.date == before.end } ?: DayLog(before.end)
        return writes.filterNot { it.date == before.end } + last.copy(periodEnded = true)
    }

    /** The day with no period flow and no marker: none and spotting stay. */
    private fun DayLog.withoutBleeding(): DayLog =
        copy(flow = flow?.takeUnless { it.isPeriodFlow }, periodStarted = false, periodEnded = false)

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
