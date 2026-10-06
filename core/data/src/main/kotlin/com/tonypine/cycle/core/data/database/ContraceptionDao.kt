package com.tonypine.cycle.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ContraceptionDao {
    /** Every stretch, oldest first: an unknown start (null) sorts before any date. */
    @Query("SELECT * FROM contraception ORDER BY started, id")
    abstract fun observeAll(): Flow<List<ContraceptionEntity>>

    @Query("SELECT * FROM contraception ORDER BY started, id")
    abstract suspend fun getAll(): List<ContraceptionEntity>

    /** Stores [stretch]: a new row when its id is 0, else over the row with its id. */
    @Upsert
    abstract suspend fun upsert(stretch: ContraceptionEntity)

    @Query("DELETE FROM contraception WHERE id = :id")
    abstract suspend fun delete(id: Long)
}
