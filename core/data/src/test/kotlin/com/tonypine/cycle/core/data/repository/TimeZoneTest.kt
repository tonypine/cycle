package com.tonypine.cycle.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.inMemoryDatabase
import com.tonypine.cycle.core.data.settingsRepository
import com.tonypine.cycle.core.model.DayLog
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/** A day she logs stays that day, whatever zone the phone is in when it is read. */
@RunWith(AndroidJUnit4::class)
class TimeZoneTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database = inMemoryDatabase()
    private val dayLogs = DayLogRepository(database)
    private val contraception = ContraceptionRepository(database)
    private val originalZone = TimeZone.getDefault()

    @After
    fun restore() {
        TimeZone.setDefault(originalZone)
        database.close()
    }

    @Test
    fun `a period started at 23_30 stays on that day after the phone moves to a zone already in the next day`() =
        runTest {
            // 23:30 on 2 March 2027 in São Paulo (UTC-3) is already 11:30 on 3 March in Tokyo (UTC+9).
            val instant = Instant.parse("2027-03-03T02:30:00Z")
            TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))
            val loggedOn = LocalDate.now(Clock.fixed(instant, ZoneId.systemDefault()))
            dayLogs.setPeriodStarted(loggedOn, started = true)

            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            val today = LocalDate.now(Clock.fixed(instant, ZoneId.systemDefault()))

            assertEquals(day("2027-03-02"), loggedOn)
            assertEquals(day("2027-03-03"), today)
            assertEquals(listOf(DayLog(day("2027-03-02"), periodStarted = true)), dayLogs.observeDayLogs().first())
            val overview = CycleRepository(dayLogs, settingsRepository(folder.root, backgroundScope), contraception)
                .observeOverview(today)
                .first()
            assertEquals(day("2027-03-02"), overview.currentCycle?.start)
            assertEquals(2, overview.cycleDay)
        }
}
