package com.tonypine.cycle.core.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import java.time.LocalDate

/**
 * One row per stretch of time on one method (`docs/decisions/0006-contraception.md`). "None" has no
 * row. The method and its breaks are fixed text codes ([Converters]), dates ISO text.
 */
@Entity(tableName = "contraception")
data class ContraceptionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long,
    @ColumnInfo(name = "method")
    val method: ContraceptionMethod,
    @ColumnInfo(name = "started")
    val started: LocalDate?,
    @ColumnInfo(name = "stopped")
    val stopped: LocalDate?,
    @ColumnInfo(name = "breaks")
    val breaks: Breaks?
)

internal fun ContraceptionEntity.toModel() = ContraceptionStretch(method, started, stopped, breaks, id)

internal fun ContraceptionStretch.toEntity() = ContraceptionEntity(id, method, started, stopped, breaks)
