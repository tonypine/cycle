package com.tonypine.cycle.core.data.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
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
 * its hash to [RELEASED_SCHEMAS], and teach [seed] the new shape if `day_log` changed. These tests
 * then migrate every earlier version to it and check her log survives.
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
            } finally {
                database.close()
            }
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
            1 to "e7993613216ac3cdda24866e10380e6a"
        )
    }

    /** Synthetic rows in the shape of schema [version]'s `day_log`. */
    private fun SupportSQLiteDatabase.seed(version: Int) {
        check(version == 1) { "Add the shape of day_log at version $version" }
        execSQL(
            "INSERT INTO day_log (date, flow, period_started, period_ended) VALUES " +
                "('2027-03-02', 'medium', 1, 0), ('2027-03-03', 'spotting', 0, 0), ('2027-03-06', NULL, 0, 1)"
        )
    }
}
