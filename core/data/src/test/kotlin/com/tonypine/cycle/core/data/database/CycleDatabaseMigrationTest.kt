package com.tonypine.cycle.core.data.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.SexualActivity
import com.tonypine.cycle.core.model.SleepQuality
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The harness every schema change goes through. For a new version: raise [CycleDatabase.VERSION],
 * add the migration to [CycleMigrations.ALL], build once to export `schemas/…/<version>.json`, add
 * its hash to [RELEASED_SCHEMAS], and teach [seed] the new shape of any table that changed. These
 * tests then migrate every earlier version to it and check her log survives.
 */
@RunWith(AndroidJUnit4::class)
class CycleDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), CycleDatabase::class.java)

    @After
    fun deleteDatabase() {
        context.deleteDatabase(CycleDatabase.FILE_NAME)
    }

    @Test
    fun `every schema version up to the current one is exported`() {
        val exported = context.assets.list(CycleDatabase::class.java.name).orEmpty().toSet()

        assertEquals((1..CycleDatabase.VERSION).map { "$it.json" }.toSet(), exported)
    }

    @Test
    fun `a released schema never changes`() {
        val hashes = (1..CycleDatabase.VERSION).associateWith { version ->
            context.assets.open("${CycleDatabase::class.java.name}/$version.json").use { stream ->
                JSONObject(stream.reader().readText()).getJSONObject("database").getString("identityHash")
            }
        }

        assertEquals(RELEASED_SCHEMAS, hashes)
    }

    @Test
    fun `every version migrates to the current schema and keeps her log`() = runTest {
        for (version in 1..CycleDatabase.VERSION) {
            context.deleteDatabase(CycleDatabase.FILE_NAME)
            helper.createDatabase(CycleDatabase.FILE_NAME, version).use { it.seed(version) }

            // Checks the migrated tables against the schema the entities declare.
            helper.runMigrationsAndValidate(CycleDatabase.FILE_NAME, CycleDatabase.VERSION, true, *CycleMigrations.ALL)
                .close()

            val database = CycleDatabase.build(context)
            try {
                assertEquals(
                    "from version $version",
                    listOf(
                        DayLog(day("2027-03-02"), FlowLevel.MEDIUM, periodStarted = true),
                        DayLog(day("2027-03-03"), FlowLevel.SPOTTING),
                        DayLog(day("2027-03-06"), periodEnded = true)
                    ),
                    database.dayLogDao().observeAll().first().map { it.toModel() }
                )
                // Versions before 2 had no feelings: they start empty. From 2 on, they are kept.
                assertEquals(
                    "from version $version",
                    if (version >= 2) listOf(SEEDED_FEELINGS) else emptyList(),
                    database.feelingsDao().getAll()
                )
                // Versions before 3 had no contraception: it starts empty. From 3 on, it is kept.
                assertEquals(
                    "from version $version",
                    if (version >= 3) listOf(SEEDED_STRETCH) else emptyList(),
                    database.contraceptionDao().getAll().map { it.toModel() }
                )
            } finally {
                database.close()
            }
        }
    }

    @Test
    fun `after version 1 migrates, how she felt is logged next to her flow`() = runTest {
        helper.createDatabase(CycleDatabase.FILE_NAME, 1).use { it.seed(1) }
        helper.runMigrationsAndValidate(CycleDatabase.FILE_NAME, 2, true, CycleMigrations.MIGRATION_1_2).close()

        val database = CycleDatabase.build(context)
        try {
            database.feelingsDao().save(SEEDED_FEELINGS)

            assertEquals(SEEDED_FEELINGS, database.feelingsDao().get(day("2027-03-02")))
            assertEquals(
                DayLog(day("2027-03-02"), FlowLevel.MEDIUM, periodStarted = true),
                database.dayLogDao().get(day("2027-03-02"))?.toModel()
            )
        } finally {
            database.close()
        }
    }

    @Test
    fun `after version 2 migrates, her contraception is stored and read back`() = runTest {
        helper.createDatabase(CycleDatabase.FILE_NAME, 2).use { it.seed(2) }
        helper.runMigrationsAndValidate(CycleDatabase.FILE_NAME, 3, true, CycleMigrations.MIGRATION_2_3).close()

        val database = CycleDatabase.build(context)
        try {
            database.contraceptionDao().upsert(SEEDED_STRETCH.copy(id = 0).toEntity())

            assertEquals(listOf(SEEDED_STRETCH), database.contraceptionDao().getAll().map { it.toModel() })
            assertEquals(listOf(SEEDED_FEELINGS), database.feelingsDao().getAll())
        } finally {
            database.close()
        }
    }

    @Test
    fun `the database opens without migrations at the current version`() = runTest {
        val database = CycleDatabase.build(context)
        try {
            database.dayLogDao().upsert(DayLogEntity(day("2027-03-02"), FlowLevel.LIGHT, false, false))
            assertNotNull(database.dayLogDao().get(day("2027-03-02")))
        } finally {
            database.close()
        }
    }

    private companion object {
        /**
         * The identity hash of each schema version, from its exported JSON. A version that has
         * shipped is on her phone: changing its entities needs a new version and a migration, not
         * an edit here.
         */
        val RELEASED_SCHEMAS = mapOf(
            1 to "e7993613216ac3cdda24866e10380e6a",
            2 to "3b95ef39f3fdb5ebdf56e043a67fdf99",
            3 to "5fbc0e8838ec8208402068ba4d12476c"
        )

        /** What [seed] stores of her contraception, from version 3 on. */
        val SEEDED_STRETCH = ContraceptionStretch(
            ContraceptionMethod.COMBINED_PILL,
            started = day("2027-01-04"),
            stopped = day("2027-02-28"),
            breaks = Breaks.MONTHLY,
            id = 1
        )

        /** What [seed] logs about how she felt, from version 2 on. */
        val SEEDED_FEELINGS = DayFeelings(
            date = day("2027-03-02"),
            pain = Pain(PainLevel.MODERATE, setOf(PainKind.CRAMPS, PainKind.LOWER_BACK)),
            body = setOf(BodySymptom.BLOATING),
            moods = setOf(Mood.IRRITABLE),
            energy = EnergyLevel.LOW,
            sleep = SleepQuality.BADLY,
            sex = SexualActivity.UNPROTECTED,
            note = "Synthetic note"
        )
    }

    /** Synthetic rows in the shape of schema [version]'s tables. */
    private fun SupportSQLiteDatabase.seed(version: Int) {
        check(version in 1..3) { "Add the shape of the tables at version $version" }
        execSQL(
            "INSERT INTO day_log (date, flow, period_started, period_ended) VALUES " +
                "('2027-03-02', 'medium', 1, 0), ('2027-03-03', 'spotting', 0, 0), ('2027-03-06', NULL, 0, 1)"
        )
        if (version >= 2) {
            execSQL("INSERT INTO pain (date, level, kinds) VALUES ('2027-03-02', 'moderate', 'cramps,lower_back')")
            execSQL("INSERT INTO body_symptoms (date, symptoms) VALUES ('2027-03-02', 'bloating')")
            execSQL("INSERT INTO mood (date, moods) VALUES ('2027-03-02', 'irritable')")
            execSQL("INSERT INTO energy (date, level) VALUES ('2027-03-02', 'low')")
            execSQL("INSERT INTO sleep (date, quality) VALUES ('2027-03-02', 'badly')")
            execSQL("INSERT INTO sex (date, protection) VALUES ('2027-03-02', 'unprotected')")
            execSQL("INSERT INTO note (date, text) VALUES ('2027-03-02', 'Synthetic note')")
        }
        if (version >= 3) {
            execSQL(
                "INSERT INTO contraception (id, method, started, stopped, breaks) VALUES " +
                    "(1, 'combined_pill', '2027-01-04', '2027-02-28', 'monthly')"
            )
        }
    }
}
