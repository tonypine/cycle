package com.tonypine.cycle.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DayLogDao {
    /** Every logged day, oldest first. */
    @Query("SELECT * FROM day_log ORDER BY date")
    abstract fun observeAll(): Flow<List<DayLogEntity>>

    /** The logged days from [from] to [to], both included, oldest first. */
    @Query("SELECT * FROM day_log WHERE date BETWEEN :from AND :to ORDER BY date")
    abstract fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<DayLogEntity>>

    @Query("SELECT * FROM day_log WHERE date = :date")
    abstract suspend fun get(date: LocalDate): DayLogEntity?

    @Upsert
    abstract suspend fun upsert(day: DayLogEntity)

    @Query("DELETE FROM day_log WHERE date = :date")
    abstract suspend fun delete(date: LocalDate)

    /**
     * Replaces the day at [date] with [transform] of what is stored (null when nothing is), in one
     * transaction. A null result deletes the row.
     */
    @Transaction
    open suspend fun update(date: LocalDate, transform: (DayLogEntity?) -> DayLogEntity?) {
        when (val updated = transform(get(date))) {
            null -> delete(date)
            else -> upsert(updated)
        }
    }
}
