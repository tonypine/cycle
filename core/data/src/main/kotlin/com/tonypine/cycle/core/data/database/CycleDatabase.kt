package com.tonypine.cycle.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration

/**
 * Her log, on the phone. Releases already ship to her, so every schema change raises [version],
 * adds a tested migration to [CycleMigrations.ALL] and commits the exported schema under
 * `core/data/schemas/`. There is no destructive fallback: a missing migration fails loudly instead
 * of erasing her history.
 */
@Database(entities = [DayLogEntity::class], version = CycleDatabase.VERSION, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CycleDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao

    companion object {
        const val VERSION = 1
        const val FILE_NAME = "cycle.db"

        fun build(context: Context): CycleDatabase = Room
            .databaseBuilder(context.applicationContext, CycleDatabase::class.java, FILE_NAME)
            .addMigrations(*CycleMigrations.ALL)
            .build()
    }
}

/** Every migration between schema versions, oldest first. Empty while the schema is at version 1. */
object CycleMigrations {
    val ALL: Array<Migration> = arrayOf()
}
