package com.tonypine.cycle.core.data.repository

import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.domain.CycleCalculator
import com.tonypine.cycle.core.model.CycleOverview
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Her periods, cycles, estimates and prompts, recomputed from the log and settings whenever either
 * changes. Nothing derived is stored.
 */
class CycleRepository(private val dayLogs: DayLogRepository, private val settings: SettingsRepository) {
    /**
     * The overview on [today]. The caller passes the day, from its own clock in the phone's current
     * zone, and collects again when the day changes.
     */
    fun observeOverview(today: LocalDate): Flow<CycleOverview> =
        combine(dayLogs.observeDayLogs(), settings.settings) { logs, settings ->
            CycleCalculator.overview(logs, settings, today)
        }
}
