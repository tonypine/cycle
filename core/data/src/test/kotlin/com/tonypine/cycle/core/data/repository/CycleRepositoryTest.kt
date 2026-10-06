package com.tonypine.cycle.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.inMemoryDatabase
import com.tonypine.cycle.core.data.settingsRepository
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.EstimateKind
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LogCategory
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.SexualActivity
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CycleRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database = inMemoryDatabase()
    private val dayLogs = DayLogRepository(database)
    private val contraception = ContraceptionRepository(database)

    @After
    fun closeDatabase() = database.close()

    private suspend fun logPeriod(start: LocalDate, days: Int = 4) =
        (0L until days).forEach { dayLogs.setFlow(start.plusDays(it), FlowLevel.MEDIUM) }

    @Test
    fun `the overview recomputes every cycle after a past day is edited`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        val cycles = CycleRepository(dayLogs, settings, contraception)
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
    fun `editing or deleting a stretch of contraception recomputes everything after it`() = runTest {
        val cycles = CycleRepository(dayLogs, settingsRepository(folder.root, backgroundScope), contraception)
        listOf("2027-01-05", "2027-02-02", "2027-03-02", "2027-03-30").forEach { logPeriod(day(it)) }
        val today = day("2027-04-10")
        contraception.start(ContraceptionMethod.IMPLANT, null, day("2027-03-10"), today)
        val implant = contraception.observeStretches().first().single()

        val onImplant = cycles.observeOverview(today).first()
        assertEquals(listOf(28, 28, 8), onImplant.cycles.map { it.length })
        assertEquals(implant, onImplant.contraception.current)
        assertEquals(null, onImplant.estimate)

        contraception.edit(implant.copy(started = day("2027-02-20")), today)
        val startedEarlier = cycles.observeOverview(today).first()
        assertEquals(listOf(28, 18), startedEarlier.cycles.map { it.length })
        assertEquals(
            listOf(null, null, implant.copy(started = day("2027-02-20")), implant.copy(started = day("2027-02-20"))),
            startedEarlier.periods.map {
                it.stretch
            }
        )

        contraception.delete(implant.id)
        val deleted = cycles.observeOverview(today).first()
        assertEquals(listOf(28, 28, 28, null), deleted.cycles.map { it.length })
        assertEquals(EstimateKind.PERIOD, deleted.contraception.estimates)
        assertEquals(day("2027-04-27"), deleted.estimate?.next?.expectedStart)
    }

    @Test
    fun `the overview follows her settings and dismissed prompts`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        val cycles = CycleRepository(dayLogs, settings, contraception)
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
        val cycles = CycleRepository(dayLogs, settingsRepository(folder.root, backgroundScope), contraception)
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
        val cycles = CycleRepository(dayLogs, settingsRepository(folder.root, backgroundScope), contraception)
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
        val cycles = CycleRepository(dayLogs, settingsRepository(folder.root, backgroundScope), contraception)
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

    @Test
    fun `the log carries how she felt and what she hid, and a day she can see something on can be cleared`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        val cycles = CycleRepository(dayLogs, settings, contraception)
        val sex = DayFeelings(day("2027-03-05"), sex = SexualActivity.PROTECTED)
        dayLogs.logDay(day("2027-03-05"), null, sex)
        val today = day("2027-03-10")

        val shown = cycles.observeLog(today).first()
        assertEquals(sex, shown.feelings(day("2027-03-05")))
        assertEquals(DayFeelings(day("2027-03-06")), shown.feelings(day("2027-03-06")))
        assertTrue(shown.canClear(day("2027-03-05")))

        settings.setCategoryShown(LogCategory.SEX, shown = false)

        val hidden = cycles.observeLog(today).first()
        assertEquals(setOf(LogCategory.SEX), hidden.hiddenCategories)
        // Hidden, not deleted: the log still has it, and the sheet saves it back as it was.
        assertEquals(sex, hidden.feelings(day("2027-03-05")))
        assertFalse(hidden.canClear(day("2027-03-05")))
    }
}
