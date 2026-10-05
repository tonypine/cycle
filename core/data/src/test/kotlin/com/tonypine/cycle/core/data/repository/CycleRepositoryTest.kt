package com.tonypine.cycle.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.inMemoryDatabase
import com.tonypine.cycle.core.data.settingsRepository
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Period
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CycleRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database = inMemoryDatabase()
    private val dayLogs = DayLogRepository(database.dayLogDao())

    @After
    fun closeDatabase() = database.close()

    private suspend fun logPeriod(start: LocalDate, days: Int = 4) =
        (0L until days).forEach { dayLogs.setFlow(start.plusDays(it), FlowLevel.MEDIUM) }

    @Test
    fun `the overview recomputes every cycle after a past day is edited`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        val cycles = CycleRepository(dayLogs, settings)
        listOf("2027-01-05", "2027-02-02", "2027-03-02").forEach { logPeriod(day(it)) }
        val today = day("2027-03-10")

        assertEquals(listOf(28, 28, null), cycles.observeOverview(today).first().cycles.map { it.length })

        // She forgot a day of flow before February's period.
        dayLogs.setFlow(day("2027-02-01"), FlowLevel.LIGHT)

        val overview = cycles.observeOverview(today).first()
        assertEquals(listOf(27, 29, null), overview.cycles.map { it.length })
        assertEquals(day("2027-03-30"), overview.estimate?.next?.expectedStart)
        assertEquals(EstimateBasis.Logged(2), overview.estimate?.cycleBasis)
    }

    @Test
    fun `the overview follows her settings and dismissed prompts`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        val cycles = CycleRepository(dayLogs, settings)
        dayLogs.setPeriodStarted(day("2027-03-01"), started = true)
        val today = day("2027-03-07")

        val before = cycles.observeOverview(today).first()
        assertEquals(EstimateBasis.Typical, before.estimate?.cycleBasis)
        assertEquals(listOf(CyclePrompt.StillGoing(day("2027-03-01"), 7)), before.prompts)

        settings.setUsualCycleLength(32)
        settings.setSetupDone(true)
        settings.dismiss(before.prompts.single())

        val after = cycles.observeOverview(today).first()
        assertEquals(EstimateBasis.Setup, after.estimate?.cycleBasis)
        assertEquals(day("2027-04-02"), after.estimate?.next?.expectedStart)
        assertEquals(emptyList<CyclePrompt>(), after.prompts)
    }

    @Test
    fun `a day's log and the overview come from the same read`() = runTest {
        val cycles = CycleRepository(dayLogs, settingsRepository(folder.root, backgroundScope))
        val today = day("2027-03-10")
        dayLogs.setPeriodStarted(today, started = true)

        val started = cycles.observeDay(today).first()
        assertEquals(DayLog(today, periodStarted = true), started.log)
        assertEquals(today, started.overview.currentPeriod?.start)

        dayLogs.setPeriodStarted(today, started = false)

        val undone = cycles.observeDay(today).first()
        assertEquals(DayLog(today), undone.log)
        assertEquals(emptyList<Period>(), undone.overview.periods)
    }

    @Test
    fun `every logged day and the overview come from the same read`() = runTest {
        val cycles = CycleRepository(dayLogs, settingsRepository(folder.root, backgroundScope))
        val today = day("2027-03-10")
        logPeriod(day("2027-03-02"), days = 2)

        val log = cycles.observeLog(today).first()
        assertEquals(
            listOf(DayLog(day("2027-03-02"), FlowLevel.MEDIUM), DayLog(day("2027-03-03"), FlowLevel.MEDIUM)),
            log.logs
        )
        assertEquals(listOf(Period(day("2027-03-02"), day("2027-03-03"))), log.overview.periods)

        dayLogs.setFlow(day("2027-03-04"), FlowLevel.LIGHT)

        val edited = cycles.observeLog(today).first()
        assertEquals(day("2027-03-04"), edited.logs.last().date)
        assertEquals(listOf(Period(day("2027-03-02"), day("2027-03-04"))), edited.overview.periods)
    }

    @Test
    fun `the log comes with the overview and says what the day sheet offers`() = runTest {
        val cycles = CycleRepository(dayLogs, settingsRepository(folder.root, backgroundScope))
        val today = day("2027-03-20")
        logPeriod(day("2027-02-02"))
        logPeriod(day("2027-03-02"))

        val log = cycles.observeLog(today).first()

        assertEquals(DayLog(day("2027-03-03"), FlowLevel.MEDIUM), log.log(day("2027-03-03")))
        assertEquals(DayLog(day("2027-03-12")), log.log(day("2027-03-12")))
        // Her two 4-day periods make her usual length 4.
        assertEquals(4, log.usualPeriodLength)
        assertEquals(true, log.canFill(day("2027-03-12")))
        assertEquals(false, log.canFill(day("2027-03-06")))
        assertEquals(true, log.canClear(day("2027-03-03")))
        assertEquals(false, log.canClear(day("2027-03-12")))
    }
}
