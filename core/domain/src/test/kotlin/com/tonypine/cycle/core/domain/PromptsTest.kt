package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.DayLog
import org.junit.Assert.assertEquals
import org.junit.Test

class PromptsTest {
    private val started = listOf(DayLog(day("2027-03-01"), periodStarted = true))

    private fun prompts(logs: List<DayLog>, today: String, settings: CycleSettings = setUp()) =
        CycleCalculator.overview(logs, settings, day(today)).prompts

    @Test
    fun `still going is asked at her usual period length plus 2 days`() {
        assertEquals(emptyList<CyclePrompt>(), prompts(started, today = "2027-03-06"))
        assertEquals(listOf(CyclePrompt.StillGoing(day("2027-03-01"), periodDay = 7)), prompts(started, "2027-03-07"))
    }

    @Test
    fun `still going follows her setup period length`() {
        val settings = setUp(periodLength = 3)

        assertEquals(emptyList<CyclePrompt>(), prompts(started, today = "2027-03-04", settings))
        assertEquals(listOf(CyclePrompt.StillGoing(day("2027-03-01"), 5)), prompts(started, "2027-03-05", settings))
    }

    @Test
    fun `still going follows her own periods once she has them`() {
        val logs = cycles(28, 28, lastStart = day("2027-02-01"), periodLength = 3) +
            DayLog(day("2027-03-01"), periodStarted = true)

        assertEquals(emptyList<CyclePrompt>(), prompts(logs, today = "2027-03-04"))
        assertEquals(listOf(CyclePrompt.StillGoing(day("2027-03-01"), 5)), prompts(logs, "2027-03-05"))
    }

    @Test
    fun `still going is not asked again once dismissed for this period`() {
        val dismissed = setUp().copy(dismissedStillGoing = setOf(day("2027-03-01")))

        assertEquals(emptyList<CyclePrompt>(), prompts(started, today = "2027-03-09", dismissed))
    }

    @Test
    fun `still going is not asked once the period has ended`() {
        val ended = started + DayLog(day("2027-03-09"), periodEnded = true)

        assertEquals(emptyList<CyclePrompt>(), prompts(ended, today = "2027-03-10"))
    }

    @Test
    fun `missed a period is asked at 1_8 times her median cycle`() {
        // Median 28: 1.8 x 28 = 50.4, so cycle day 51.
        val logs = cycles(29, 26, 31, 28, 27, 28, lastStart = day("2027-03-02"))

        assertEquals(emptyList<CyclePrompt>(), prompts(logs, today = "2027-04-20"))
        assertEquals(listOf(CyclePrompt.MissedPeriod(day("2027-03-02"), cycleDay = 51)), prompts(logs, "2027-04-21"))
    }

    @Test
    fun `missed a period follows her setup cycle length before she has cycles`() {
        // 1.8 x 30 = 54.
        val logs = cycles(lastStart = day("2027-03-01"))
        val settings = setUp(cycleLength = 30)

        assertEquals(emptyList<CyclePrompt>(), prompts(logs, today = "2027-04-22", settings))
        assertEquals(listOf(CyclePrompt.MissedPeriod(day("2027-03-01"), 54)), prompts(logs, "2027-04-23", settings))
    }

    @Test
    fun `missed a period is not asked again once dismissed for this cycle`() {
        val logs = cycles(lastStart = day("2027-03-01"))
        val dismissed = setUp().copy(dismissedMissedPeriod = setOf(day("2027-03-01")))

        assertEquals(emptyList<CyclePrompt>(), prompts(logs, today = "2027-05-01", dismissed))
    }

    @Test
    fun `missed a period is not asked during an open period`() {
        assertEquals(listOf(CyclePrompt.StillGoing(day("2027-03-01"), 61)), prompts(started, today = "2027-04-30"))
    }
}
