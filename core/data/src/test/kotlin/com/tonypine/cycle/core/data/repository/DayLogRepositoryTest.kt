package com.tonypine.cycle.core.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.inMemoryDatabase
import com.tonypine.cycle.core.model.BodySymptom
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DayLogRepositoryTest {
    private val database = inMemoryDatabase()
    private val repository = DayLogRepository(database)

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

    @Test
    fun `fill logs a period's first and last day in one write`() = runTest {
        repository.fillPeriod(day("2027-02-26"), length = 5, today = day("2027-03-20"))

        assertEquals(
            listOf(DayLog(day("2027-02-26"), periodStarted = true), DayLog(day("2027-03-02"), periodEnded = true)),
            repository.observeDayLogs().first()
        )
    }

    @Test
    fun `fill near a period writes nothing`() = runTest {
        repository.setFlow(day("2027-03-02"), FlowLevel.MEDIUM)

        repository.fillPeriod(day("2027-03-03"), length = 5, today = day("2027-03-20"))

        assertEquals(listOf(DayLog(day("2027-03-02"), FlowLevel.MEDIUM)), repository.observeDayLogs().first())
    }

    @Test
    fun `clearing a period's first day moves the start to the next day in one write`() = runTest {
        repository.fillPeriod(day("2027-03-02"), length = 5, today = day("2027-03-20"))

        repository.clearDay(day("2027-03-02"), today = day("2027-03-20"))

        assertEquals(
            listOf(DayLog(day("2027-03-03"), periodStarted = true), DayLog(day("2027-03-06"), periodEnded = true)),
            repository.observeDayLogs().first()
        )
    }

    private val feelings = DayFeelings(
        date = day("2027-03-02"),
        pain = Pain(PainLevel.MODERATE, setOf(PainKind.CRAMPS, PainKind.LOWER_BACK)),
        body = setOf(BodySymptom.BLOATING),
        moods = setOf(Mood.IRRITABLE, Mood.LOW),
        energy = EnergyLevel.LOW,
        sleep = SleepQuality.BADLY,
        sex = SexualActivity.PROTECTED,
        note = "Synthetic note"
    )

    @Test
    fun `Log it saves the flow and how she felt, and the day reads back the same`() = runTest {
        repository.logDay(day("2027-03-02"), FlowLevel.MEDIUM, feelings)

        assertEquals(DayLog(day("2027-03-02"), FlowLevel.MEDIUM), repository.get(day("2027-03-02")))
        assertEquals(feelings, repository.getFeelings(day("2027-03-02")))
        assertEquals(listOf(feelings), repository.observeFeelings().first())
    }

    @Test
    fun `logging again replaces every category, and an empty day leaves no row`() = runTest {
        repository.logDay(day("2027-03-02"), FlowLevel.MEDIUM, feelings)

        repository.logDay(day("2027-03-02"), null, DayFeelings(day("2027-03-02"), energy = EnergyLevel.HIGH))
        assertEquals(
            DayFeelings(day("2027-03-02"), energy = EnergyLevel.HIGH),
            repository.getFeelings(day("2027-03-02"))
        )

        repository.logDay(day("2027-03-02"), null, DayFeelings(day("2027-03-02")))
        assertEquals(emptyList<DayFeelings>(), repository.observeFeelings().first())
        assertEquals(emptyList<DayLog>(), repository.observeDayLogs().first())
    }

    @Test
    fun `Log it stores the day it is given, with no pain kinds under no pain and a trimmed note`() = runTest {
        repository.logDay(
            day("2027-03-04"),
            null,
            feelings.copy(pain = Pain(PainLevel.NONE, setOf(PainKind.CRAMPS)), note = "  Synthetic  ")
        )

        assertEquals(
            feelings.copy(date = day("2027-03-04"), pain = Pain(PainLevel.NONE), note = "Synthetic"),
            repository.getFeelings(day("2027-03-04"))
        )
    }

    @Test
    fun `clearing a day removes how she felt too`() = runTest {
        repository.logDay(day("2027-03-02"), FlowLevel.MEDIUM, feelings)
        repository.logDay(day("2027-03-09"), null, feelings.copy(date = day("2027-03-09")))

        repository.clear(day("2027-03-02"))
        repository.clearDay(day("2027-03-09"), today = day("2027-03-20"))

        assertEquals(emptyList<DayFeelings>(), repository.observeFeelings().first())
        assertEquals(emptyList<DayLog>(), repository.observeDayLogs().first())
    }

    @Test
    fun `one-tap period buttons leave how she felt alone`() = runTest {
        repository.logDay(day("2027-03-02"), null, feelings)

        repository.setPeriodStarted(day("2027-03-02"), started = true)
        repository.setPeriodStarted(day("2027-03-02"), started = false)

        assertEquals(feelings, repository.getFeelings(day("2027-03-02")))
    }
}
