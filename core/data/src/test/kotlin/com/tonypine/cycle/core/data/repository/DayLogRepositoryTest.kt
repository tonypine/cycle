package com.tonypine.cycle.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.inMemoryDatabase
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayLogRepositoryTest {
    private val database = inMemoryDatabase()
    private val repository = DayLogRepository(database.dayLogDao())

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `a day never logged reads as empty`() = runTest {
        assertEquals(DayLog(day("2027-03-02")), repository.get(day("2027-03-02")))
    }

    @Test
    fun `the one-tap markers and flow add up on the same day`() = runTest {
        repository.setPeriodStarted(day("2027-03-02"), started = true)
        repository.setFlow(day("2027-03-02"), FlowLevel.HEAVY)
        repository.setPeriodEnded(day("2027-03-06"), ended = true)

        assertEquals(
            listOf(
                DayLog(day("2027-03-02"), FlowLevel.HEAVY, periodStarted = true),
                DayLog(day("2027-03-06"), periodEnded = true)
            ),
            repository.observeDayLogs().first()
        )
    }

    @Test
    fun `undoing the only thing logged on a day removes the day`() = runTest {
        repository.setPeriodStarted(day("2027-03-02"), started = true)
        repository.setPeriodStarted(day("2027-03-02"), started = false)
        repository.save(DayLog(day("2027-03-03"), FlowLevel.LIGHT))
        repository.save(DayLog(day("2027-03-03")))

        assertEquals(0, database.openHelper.readableDatabase.query("SELECT * FROM day_log").use { it.count })
    }

    @Test
    fun `no flow is kept as a logged answer`() = runTest {
        repository.setFlow(day("2027-03-02"), FlowLevel.NONE)

        assertEquals(listOf(DayLog(day("2027-03-02"), FlowLevel.NONE)), repository.observeDayLogs().first())
    }

    @Test
    fun `a range reads only the days inside it`() = runTest {
        listOf("2027-02-28", "2027-03-01", "2027-03-31", "2027-04-01").forEach {
            repository.setFlow(day(it), FlowLevel.LIGHT)
        }

        assertEquals(
            listOf(day("2027-03-01"), day("2027-03-31")),
            repository.observeDayLogs(day("2027-03-01"), day("2027-03-31")).first().map { it.date }
        )
    }

    @Test
    fun `clear removes everything logged that day`() = runTest {
        repository.save(DayLog(day("2027-03-02"), FlowLevel.MEDIUM, periodStarted = true))

        repository.clear(day("2027-03-02"))

        assertEquals(emptyList<DayLog>(), repository.observeDayLogs().first())
    }
}
