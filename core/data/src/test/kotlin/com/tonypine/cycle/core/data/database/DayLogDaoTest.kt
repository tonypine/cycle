package com.tonypine.cycle.core.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.inMemoryDatabase
import com.tonypine.cycle.core.model.FlowLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayLogDaoTest {
    private val database = inMemoryDatabase()
    private val dao = database.dayLogDao()

    @After
    fun closeDatabase() = database.close()

    private fun entity(
        date: String,
        flow: FlowLevel? = FlowLevel.MEDIUM,
        started: Boolean = false,
        ended: Boolean = false
    ) = DayLogEntity(day(date), flow, started, ended)

    @Test
    fun `stores and reads back every field`() = runTest {
        val logged = entity("2027-03-02", FlowLevel.HEAVY, started = true, ended = true)

        dao.upsert(logged)

        assertEquals(logged, dao.get(day("2027-03-02")))
        assertNull(dao.get(day("2027-03-03")))
    }

    @Test
    fun `every flow level and no flow survive a round trip`() = runTest {
        val days = (FlowLevel.entries + null).mapIndexed { i, flow -> entity("2027-03-0${i + 1}", flow) }

        days.forEach { dao.upsert(it) }

        assertEquals(days, dao.observeAll().first())
    }

    @Test
    fun `one row per day, the latest write wins`() = runTest {
        dao.upsert(entity("2027-03-02", FlowLevel.LIGHT))
        dao.upsert(entity("2027-03-02", FlowLevel.HEAVY))

        assertEquals(listOf(entity("2027-03-02", FlowLevel.HEAVY)), dao.observeAll().first())
    }

    @Test
    fun `days come back in date order, across months and years`() = runTest {
        listOf("2027-03-02", "2026-12-31", "2027-01-15", "2027-10-01").forEach { dao.upsert(entity(it)) }

        assertEquals(
            listOf("2026-12-31", "2027-01-15", "2027-03-02", "2027-10-01").map(::day),
            dao.observeAll().first().map { it.date }
        )
    }

    @Test
    fun `a range includes both ends`() = runTest {
        listOf("2027-02-28", "2027-03-01", "2027-03-15", "2027-03-31", "2027-04-01").forEach { dao.upsert(entity(it)) }

        assertEquals(
            listOf("2027-03-01", "2027-03-15", "2027-03-31").map(::day),
            dao.observeBetween(day("2027-03-01"), day("2027-03-31")).first().map { it.date }
        )
    }

    @Test
    fun `update transforms what is stored, and a null result deletes the day`() = runTest {
        dao.update(day("2027-03-02")) { stored -> stored ?: entity("2027-03-02", started = true) }
        dao.update(day("2027-03-02")) { stored -> stored?.copy(flow = FlowLevel.HEAVY) }

        assertEquals(entity("2027-03-02", FlowLevel.HEAVY, started = true), dao.get(day("2027-03-02")))

        dao.update(day("2027-03-02")) { null }

        assertEquals(emptyList<DayLogEntity>(), dao.observeAll().first())
    }

    @Test
    fun `delete removes only that day`() = runTest {
        dao.upsert(entity("2027-03-02"))
        dao.upsert(entity("2027-03-03"))

        dao.delete(day("2027-03-02"))

        assertEquals(listOf(day("2027-03-03")), dao.observeAll().first().map { it.date })
    }

    @Test
    fun `dates and flow are stored as plain text`() = runTest {
        dao.upsert(entity("2027-03-02", FlowLevel.SPOTTING))

        database.openHelper.readableDatabase.query("SELECT date, flow FROM day_log").use { cursor ->
            cursor.moveToFirst()
            assertEquals("2027-03-02", cursor.getString(0))
            assertEquals("spotting", cursor.getString(1))
        }
    }
}
