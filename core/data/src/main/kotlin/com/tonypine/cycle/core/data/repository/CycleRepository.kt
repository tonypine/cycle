package com.tonypine.cycle.core.data.repository

import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.domain.CycleCalculator
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.DayLog
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Her periods, cycles, estimates and prompts, recomputed from the log and settings whenever either
 * changes. Nothing derived is stored.
 */
class CycleRepository(private val dayLogs: DayLogRepository, private val settings: SettingsRepository) {
    /**
     * The overview on [today]. The caller passes the day, from its own clock in the phone's current
     * zone, and collects again when the day changes.
     */
    fun observeOverview(today: LocalDate): Flow<CycleOverview> = observeDay(today).map { it.overview }

    /**
     * The overview on [today] with what she logged that day, both from the same read of the log, so
     * they always agree: a screen never sees today's log from before a write next to an overview
     * from after it.
     */
    fun observeDay(today: LocalDate): Flow<DayOverview> = observeLog(today).map { log ->
        DayOverview(overview = log.overview, log = log.logs.firstOrNull { it.date == today } ?: DayLog(today))
    }

    /**
     * The overview on [today] with every day she logged, both from the same read of the log, for a
     * screen that shows the days behind a cycle, such as each period day's flow.
     */
    fun observeLog(today: LocalDate): Flow<LogOverview> =
        combine(dayLogs.observeDayLogs(), settings.settings) { logs, settings ->
            LogOverview(overview = CycleCalculator.overview(logs, settings, today), logs = logs)
        }
}

/** The [overview] on a day and the [log] of that day. */
data class DayOverview(val overview: CycleOverview, val log: DayLog)

/** The [overview] on a day and every logged day, oldest first: [logs]. */
data class LogOverview(val overview: CycleOverview, val logs: List<DayLog>)
