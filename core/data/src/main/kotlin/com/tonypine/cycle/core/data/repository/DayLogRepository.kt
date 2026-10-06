package com.tonypine.cycle.core.data.repository

import androidx.room.withTransaction
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.database.toEntity
import com.tonypine.cycle.core.data.database.toModel
import com.tonypine.cycle.core.domain.DayLogEdits
import com.tonypine.cycle.core.domain.PeriodPlan
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LogCategory
import com.tonypine.cycle.core.model.PeriodChange
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * What she logged, day by day: her flow and period markers ([DayLog]) and how she felt
 * ([DayFeelings]). A day with nothing logged has no row.
 */
class DayLogRepository(private val database: CycleDatabase) {
    private val dao = database.dayLogDao()
    private val feelingsDao = database.feelingsDao()

    /** Every logged day, oldest first. */
    fun observeDayLogs(): Flow<List<DayLog>> = dao.observeAll().map { days -> days.map { it.toModel() } }

    /** The logged days from [from] to [to], both included, oldest first. */
    fun observeDayLogs(from: LocalDate, to: LocalDate): Flow<List<DayLog>> =
        dao.observeBetween(from, to).map { days -> days.map { it.toModel() } }

    /** What she logged on [date], or an empty log. */
    suspend fun get(date: LocalDate): DayLog = dao.get(date)?.toModel() ?: DayLog(date)

    /** Every day she logged how she felt, oldest first. */
    fun observeFeelings(): Flow<List<DayFeelings>> = feelingsDao.observeAll()

    /** How she felt on [date], or nothing logged. */
    suspend fun getFeelings(date: LocalDate): DayFeelings = feelingsDao.get(date)

    /**
     * "Log it" in the day log sheet: the [flow] and how she felt on [date], in one write. Every
     * category is replaced, so the caller passes the ones she hid as they were.
     */
    suspend fun logDay(date: LocalDate, flow: FlowLevel?, feelings: DayFeelings) = database.withTransaction {
        update(date) { it.copy(flow = flow) }
        feelingsDao.save(feelings.copy(date = date).normalized())
    }

    /** Replaces the day with [log]; an empty log removes the day. */
    suspend fun save(log: DayLog) = update(log.date) { log }

    suspend fun setFlow(date: LocalDate, flow: FlowLevel?) = update(date) { it.copy(flow = flow) }

    /** The one-tap "period started" on [date], or its undo. */
    suspend fun setPeriodStarted(date: LocalDate, started: Boolean) = update(date) { it.copy(periodStarted = started) }

    /** The one-tap "period ended" on [date], or its undo. */
    suspend fun setPeriodEnded(date: LocalDate, ended: Boolean) = update(date) { it.copy(periodEnded = ended) }

    /**
     * Logs a period that started on [start] and lasts [length] days, such as from setup or Today's
     * empty state. When it is over before [today], its last day is marked ended; otherwise it is
     * still going. A [start] after [today] is ignored.
     */
    suspend fun logPeriod(start: LocalDate, length: Int, today: LocalDate) {
        require(length > 0) { "A period lasts at least a day, got $length" }
        if (start > today) return
        val end = start.plusDays(length - 1L)
        // The end first: alone it is ignored, so no reader sees the period open in between.
        if (end < today) setPeriodEnded(end, ended = true)
        setPeriodStarted(start, started = true)
    }

    /** Removes everything logged on [date], how she felt included. */
    suspend fun clear(date: LocalDate) = database.withTransaction {
        dao.delete(date)
        feelingsDao.delete(date)
    }

    /**
     * "Period started this day: fill in [length] days" on [start], by [DayLogEdits.fill]: nothing
     * when a period is too near. One write, so no screen sees half the period.
     */
    suspend fun fillPeriod(start: LocalDate, length: Int, today: LocalDate) =
        edit { logs -> DayLogEdits.fill(logs, start, length, today) }

    /**
     * "Clear this day" on [date], by [DayLogEdits.clear]: the day no longer counts as a period day
     * and the rest of its period stays, and how she felt that day goes, but for the categories she
     * [hidden]: she can't see them, and hiding a category deletes nothing. One write, with any day
     * next to it the edit moves.
     */
    suspend fun clearDay(date: LocalDate, today: LocalDate, hidden: Set<LogCategory>) = database.withTransaction {
        edit { logs -> DayLogEdits.clear(logs, date, today) }
        feelingsDao.save(feelingsDao.get(date).without(LogCategory.entries.toSet() - hidden))
    }

    /**
     * "Edit period dates" on the period that starts on [start], by [DayLogEdits.editPeriod]: it runs
     * from [newStart] to [newEnd], or is still going when [newEnd] is null. One write, so no screen
     * sees the period half moved; nothing is written when it is refused.
     */
    suspend fun editPeriod(start: LocalDate, newStart: LocalDate, newEnd: LocalDate?, today: LocalDate): PeriodChange {
        var change: PeriodChange = PeriodChange.Saved
        edit { logs ->
            when (val plan = DayLogEdits.editPeriod(logs, start, newStart, newEnd, today)) {
                is PeriodPlan.Ready -> plan.writes

                is PeriodPlan.Refused -> {
                    change = PeriodChange.Refused(plan.reason)
                    emptyList()
                }
            }
        }
        return change
    }

    /**
     * "Delete this period" on the period that starts on [start], by [DayLogEdits.deletePeriod]: its
     * days lose their period flow and markers, in one write. How she felt those days stays.
     */
    suspend fun deletePeriod(start: LocalDate, today: LocalDate) =
        edit { logs -> DayLogEdits.deletePeriod(logs, start, today) }

    private suspend fun edit(edit: (List<DayLog>) -> List<DayLog>) {
        dao.edit { stored ->
            edit(stored.map { it.toModel() }).map { log -> log.date to log.takeUnless { it.isEmpty }?.toEntity() }
        }
    }

    private suspend fun update(date: LocalDate, transform: (DayLog) -> DayLog) {
        dao.update(date) { stored ->
            transform(stored?.toModel() ?: DayLog(date)).takeUnless { it.isEmpty }?.toEntity()
        }
    }
}
