package com.tonypine.cycle.core.data.repository

import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.domain.CycleCalculator
import com.tonypine.cycle.core.domain.DayLogEdits
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.LogCategory
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
    fun observeDay(today: LocalDate): Flow<DayOverview> =
        observeLog(today).map { DayOverview(overview = it.overview, log = it.log(today)) }

    /**
     * The overview on [today] with every day she logged, from the same read of the log, for screens
     * that show and edit any day: the calendar, and Today's day log sheet. How she felt comes along,
     * with the categories she hid; it never changes the overview.
     */
    fun observeLog(today: LocalDate): Flow<LogOverview> =
        combine(dayLogs.observeDayLogs(), dayLogs.observeFeelings(), settings.settings) { logs, feelings, settings ->
            val overview = CycleCalculator.overview(logs, settings, today)
            LogOverview(
                overview = overview,
                logs = logs,
                usualPeriodLength = CycleCalculator.usualPeriodLength(overview.typical, settings),
                feelings = feelings,
                hiddenCategories = settings.hiddenCategories
            )
        }
}

/** The [overview] on a day and the [log] of that day. */
data class DayOverview(val overview: CycleOverview, val log: DayLog)

/**
 * The [overview] on a day with every day she [logged][logs], oldest first, and her usual period
 * length, which "fill in N days" logs. Says what the day log sheet offers on each day.
 *
 * @property feelings every day she logged how she felt, oldest first, hidden categories included.
 * @property hiddenCategories the categories she turned off in "What to log".
 */
data class LogOverview(
    val overview: CycleOverview,
    val logs: List<DayLog>,
    val usualPeriodLength: Int,
    val feelings: List<DayFeelings> = emptyList(),
    val hiddenCategories: Set<LogCategory> = emptySet()
) {
    private val byDate = logs.associateBy { it.date }
    private val feelingsByDate = feelings.associateBy { it.date }

    /** What she logged on [date], or an empty log. */
    fun log(date: LocalDate): DayLog = byDate[date] ?: DayLog(date)

    /** How she felt on [date], hidden categories included, or nothing logged. */
    fun feelings(date: LocalDate): DayFeelings = feelingsByDate[date] ?: DayFeelings(date)

    /** "Period started this day: fill in [usualPeriodLength] days" is offered on [date]. */
    fun canFill(date: LocalDate): Boolean = DayLogEdits.canFill(logs, date, usualPeriodLength, overview.today)

    /** "Clear this day" on [date] would change something: its flow, or how she felt that she can see. */
    fun canClear(date: LocalDate): Boolean = DayLogEdits.canClear(logs, date, overview.today) ||
        !feelings(date).without(hiddenCategories).isEmpty
}
