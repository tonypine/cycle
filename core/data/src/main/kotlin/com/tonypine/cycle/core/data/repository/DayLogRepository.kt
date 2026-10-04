package com.tonypine.cycle.core.data.repository

import com.tonypine.cycle.core.data.database.DayLogDao
import com.tonypine.cycle.core.data.database.toEntity
import com.tonypine.cycle.core.data.database.toModel
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

    private suspend fun update(date: LocalDate, transform: (DayLog) -> DayLog) {
        dao.update(date) { stored ->
            transform(stored?.toModel() ?: DayLog(date)).takeUnless { it.isEmpty }?.toEntity()
        }
    }
}
