package com.tonypine.cycle.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Her log, on the phone. Releases already ship to her, so every schema change raises [version],
 * adds a tested migration to [CycleMigrations.ALL] and commits the exported schema under
 * `core/data/schemas/`. There is no destructive fallback: a missing migration fails loudly instead
 * of erasing her history.
 */
@Database(
    entities = [
        DayLogEntity::class,
        PainEntity::class,
        BodySymptomsEntity::class,
        MoodEntity::class,
        EnergyEntity::class,
        SleepEntity::class,
        SexEntity::class,
        NoteEntity::class,
        ContraceptionEntity::class
    ],
    version = CycleDatabase.VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CycleDatabase : RoomDatabase() {
    abstract fun dayLogDao(): DayLogDao

    abstract fun feelingsDao(): FeelingsDao

    abstract fun contraceptionDao(): ContraceptionDao

    companion object {
        const val VERSION = 3
        const val FILE_NAME = "cycle.db"

        fun build(context: Context): CycleDatabase = Room
            .databaseBuilder(context.applicationContext, CycleDatabase::class.java, FILE_NAME)
            .addMigrations(*CycleMigrations.ALL)
            .build()
    }
}

/** Every migration between schema versions, oldest first. */
object CycleMigrations {
    /** Version 2 adds how she felt: one table per category, keyed by the day. `day_log` is untouched. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf(
                "pain" to "`level` TEXT NOT NULL, `kinds` TEXT NOT NULL",
                "body_symptoms" to "`symptoms` TEXT NOT NULL",
                "mood" to "`moods` TEXT NOT NULL",
                "energy" to "`level` TEXT NOT NULL",
                "sleep" to "`quality` TEXT NOT NULL",
                "sex" to "`protection` TEXT NOT NULL",
                "note" to "`text` TEXT NOT NULL"
            ).forEach { (table, columns) ->
                db.execSQL("CREATE TABLE IF NOT EXISTS `$table` (`date` TEXT NOT NULL, $columns, PRIMARY KEY(`date`))")
            }
        }
    }

    /**
     * Version 3 adds her contraception: one row per stretch on a method
     * (`docs/decisions/0006-contraception.md`). Every other table is untouched.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `contraception` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`method` TEXT NOT NULL, `started` TEXT, `stopped` TEXT, `breaks` TEXT)"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
