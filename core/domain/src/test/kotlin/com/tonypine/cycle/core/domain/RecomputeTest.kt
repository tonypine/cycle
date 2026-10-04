package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class RecomputeTest {
    private val today = day("2027-03-10")
    private val logs = cycles(29, 26, 31, 28, 27, 28, lastStart = day("2027-03-02"))

    private fun cycleLengths(logs: List<DayLog>) =
        CycleCalculator.overview(logs, setUp(), today).cycles.map { it.length }

    private fun List<DayLog>.edit(date: String, transform: (DayLog) -> DayLog): List<DayLog> {
        val day = day(date)
        val edited = transform(firstOrNull { it.date == day } ?: DayLog(day))
        return filterNot { it.date == day } + edited
    }

    @Test
    fun `logging a day before a past period moves its start and recomputes the cycles around it`() {
        // The second period starts on 2026-10-13; she now logs flow the day before.
        val edited = logs.edit("2026-10-12") { it.copy(flow = FlowLevel.LIGHT) }

        assertEquals(listOf(29, 26, 31, 28, 27, 28, null), cycleLengths(logs))
        assertEquals(listOf(28, 27, 31, 28, 27, 28, null), cycleLengths(edited))
    }

    @Test
    fun `removing a past period merges its cycle into the one before and changes the estimate`() {
        // The fifth period, 2027-01-06 to 2027-01-10, turns out to be spotting.
        val edited = (0L until 5L).fold(logs) { acc, offset ->
            acc.edit(day("2027-01-06").plusDays(offset).toString()) { it.copy(flow = FlowLevel.SPOTTING) }
        }

        assertEquals(listOf(29, 26, 31, 55, 28, null), cycleLengths(edited))
        val before = CycleCalculator.overview(logs, setUp(), today).estimate!!
        val after = CycleCalculator.overview(edited, setUp(), today).estimate!!
        assertEquals(day("2027-03-30"), before.next.expectedStart)
        // Median of 26, 28, 29, 31, 55 is 29; the range runs to her new longest cycle.
        assertEquals(day("2027-03-31"), after.next.expectedStart)
        assertEquals(day("2027-04-26"), after.next.latestStart)
    }
}
