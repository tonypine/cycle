package com.tonypine.cycle.core.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate

/**
 * One row per day she logged something, keyed by the day itself (an ISO date, no time, no zone).
 * The "how she feels" categories arrive later as their own tables keyed by the same date, so adding
 * one is a migration that adds a table, not a rewrite of this one.
 */
@Entity(tableName = "day_log")
data class DayLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "date")
    val date: LocalDate,
    @ColumnInfo(name = "flow")
    val flow: FlowLevel?,
    @ColumnInfo(name = "period_started", defaultValue = "0")
    val periodStarted: Boolean,
    @ColumnInfo(name = "period_ended", defaultValue = "0")
    val periodEnded: Boolean
)

internal fun DayLogEntity.toModel() = DayLog(date, flow, periodStarted, periodEnded)

internal fun DayLog.toEntity() = DayLogEntity(date, flow, periodStarted, periodEnded)
