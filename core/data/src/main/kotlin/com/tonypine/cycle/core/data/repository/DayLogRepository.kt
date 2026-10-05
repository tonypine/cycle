package com.tonypine.cycle.core.data.repository

import com.tonypine.cycle.core.data.database.DayLogDao
import com.tonypine.cycle.core.data.database.toEntity
import com.tonypine.cycle.core.data.database.toModel
import com.tonypine.cycle.core.domain.DayLogEdits
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What she logged, day by day. A day with nothing logged has no row. */
class DayLogRepository(private val dao: DayLogDao) {
    /** Every logged day, oldest first. */
    fun observeDayLogs(): Flow<List<DayLog>> = dao.observeAll().map { days -> days.map { it.toModel() } }

    /** The logged days from [from] to [to], both included, oldest first. */
    fun observeDayLogs(from: LocalDate, to: LocalDate): Flow<List<DayLog>> =
        dao.observeBetween(from, to).map { days -> days.map { it.toModel() } }

    /** What she logged on [date], or an empty log. */
    suspend fun get(date: LocalDate): DayLog = dao.get(date)?.toModel() ?: DayLog(date)

    /** Replaces the day with [log]; an empty log removes the day. */
    suspend fun save(log: DayLog) = update(log.date) { log }

    suspend fun setFlow(date: LocalDate, flow: FlowLevel?) = update(date) { it.copy(flow = flow) }

    /** The one-tap "period started" on [date], or its undo. */
    suspend fun setPeriodStarted(date: LocalDate, started: Boolean) = update(date) { it.copy(periodStarted = started) }

    /** The one-tap "period ended" on [date], or its undo. */
    suspend fun setPeriodEnded(date: LocalDate, ended: Boolean) = update(date) { it.copy(periodEnded = ended) }

    /** Removes everything logged on [date]. */
    suspend fun clear(date: LocalDate) = dao.delete(date)

    /**
     * "Period started this day: fill in [length] days" on [start], by [DayLogEdits.fill]: nothing
     * when a period is too near. One write, so no screen sees half the period.
     */
    suspend fun fillPeriod(start: LocalDate, length: Int, today: LocalDate) =
        edit { logs -> DayLogEdits.fill(logs, start, length, today) }

    /**
     * "Clear this day" on [date], by [DayLogEdits.clear]: the day no longer counts as a period day
     * and the rest of its period stays. One write, with any day next to it the edit moves.
     */
    suspend fun clearDay(date: LocalDate, today: LocalDate) = edit { logs -> DayLogEdits.clear(logs, date, today) }

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
