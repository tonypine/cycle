package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Period
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayLogEditsTest {
    private val today = day("2027-03-20")

    /** A past period from [start] to [end], marked the way Today's "Log a period" marks it. */
    private fun marked(start: String, end: String) =
        listOf(DayLog(day(start), periodStarted = true), DayLog(day(end), periodEnded = true))

    /** [logs] with [edits] written over them, the way the repository writes them. */
    private fun apply(logs: List<DayLog>, edits: List<DayLog>): List<DayLog> {
        val edited = edits.map { it.date }.toSet()
        return (logs.filterNot { it.date in edited } + edits.filterNot { it.isEmpty }).sortedBy { it.date }
    }

    private fun periods(logs: List<DayLog>) = CycleCalculator.periods(logs, today)

    @Test
    fun `fill is offered on a past day with no period near`() {
        val logs = marked("2027-03-02", "2027-03-06")

        assertTrue(DayLogEdits.canFill(logs, day("2027-02-10"), 5, today))
        assertTrue(DayLogEdits.canFill(emptyList(), today, 5, today))
    }

    @Test
    fun `fill is not offered where the filled days would join a period, or after today`() {
        val logs = marked("2027-03-02", "2027-03-06")

        // Inside it, a day after it, five days that would end the day before it, and a day with
        // period flow, which is a period of its own.
        assertFalse(DayLogEdits.canFill(logs, day("2027-03-04"), 5, today))
        assertFalse(DayLogEdits.canFill(logs, day("2027-03-08"), 5, today))
        assertFalse(DayLogEdits.canFill(logs, day("2027-02-25"), 5, today))
        assertFalse(DayLogEdits.canFill(bleed(day("2027-02-10"), 1), day("2027-02-10"), 5, today))
        // Two clear days between the filled days and the period are enough.
        assertTrue(DayLogEdits.canFill(logs, day("2027-03-09"), 5, today))
        assertTrue(DayLogEdits.canFill(logs, day("2027-02-23"), 5, today))
        assertFalse(DayLogEdits.canFill(logs, today.plusDays(1), 5, today))
    }

    @Test
    fun `fill logs a period of N days and keeps what the days held`() {
        val logs = listOf(DayLog(day("2027-02-10"), flow = FlowLevel.SPOTTING))

        val edits = DayLogEdits.fill(logs, day("2027-02-10"), 5, today)

        assertEquals(
            listOf(
                DayLog(day("2027-02-10"), flow = FlowLevel.SPOTTING, periodStarted = true),
                DayLog(day("2027-02-14"), periodEnded = true)
            ),
            edits
        )
        assertEquals(listOf(Period(day("2027-02-10"), day("2027-02-14"))), periods(apply(logs, edits)))
    }

    @Test
    fun `a period filled across two months spans both`() {
        val edits = DayLogEdits.fill(emptyList(), day("2027-02-26"), 5, today)

        assertEquals(listOf(Period(day("2027-02-26"), day("2027-03-02"))), periods(edits))
    }

    @Test
    fun `a fill that reaches today is still going, and a one-day fill starts and ends`() {
        val going = DayLogEdits.fill(emptyList(), day("2027-03-18"), 5, today)
        assertEquals(listOf(Period(day("2027-03-18"), today, isOpen = true)), periods(going))

        val oneDay = DayLogEdits.fill(emptyList(), day("2027-03-01"), 1, today)
        assertEquals(listOf(DayLog(day("2027-03-01"), periodStarted = true, periodEnded = true)), oneDay)
    }

    @Test
    fun `fill does nothing where it is not offered`() {
        assertEquals(
            emptyList<DayLog>(),
            DayLogEdits.fill(marked("2027-03-02", "2027-03-06"), day("2027-03-04"), 5, today)
        )
    }

    @Test
    fun `clearing a period's first day moves its start to the next day`() {
        val logs = marked("2027-03-02", "2027-03-06")

        val after = apply(logs, DayLogEdits.clear(logs, day("2027-03-02"), today))

        assertEquals(listOf(Period(day("2027-03-03"), day("2027-03-06"))), periods(after))
    }

    @Test
    fun `clearing a period's last day ends it the day before`() {
        val logs = marked("2027-03-02", "2027-03-06")

        val after = apply(logs, DayLogEdits.clear(logs, day("2027-03-06"), today))

        assertEquals(listOf(Period(day("2027-03-02"), day("2027-03-05"))), periods(after))
    }

    @Test
    fun `clearing today on a period still going ends it yesterday`() {
        val logs = listOf(DayLog(day("2027-03-17"), periodStarted = true))

        val after = apply(logs, DayLogEdits.clear(logs, today, today))

        assertEquals(listOf(Period(day("2027-03-17"), day("2027-03-19"))), periods(after))
    }

    @Test
    fun `clearing a day in the middle logs it as no flow and keeps one period`() {
        val logs = bleed(day("2027-03-02"))

        val edits = DayLogEdits.clear(logs, day("2027-03-04"), today)
        val after = apply(logs, edits)

        assertEquals(listOf(DayLog(day("2027-03-04"), flow = FlowLevel.NONE)), edits)
        assertEquals(listOf(Period(day("2027-03-02"), day("2027-03-06"))), periods(after))
        // Already cleared: nothing more to clear.
        assertFalse(DayLogEdits.canClear(after, day("2027-03-04"), today))
    }

    @Test
    fun `clearing a day with no period, or a one-day period, removes it`() {
        val spotting = listOf(DayLog(day("2027-03-12"), flow = FlowLevel.SPOTTING))
        assertEquals(listOf(DayLog(day("2027-03-12"))), DayLogEdits.clear(spotting, day("2027-03-12"), today))

        val oneDay = listOf(DayLog(day("2027-03-01"), periodStarted = true, periodEnded = true))
        assertEquals(emptyList<Period>(), periods(apply(oneDay, DayLogEdits.clear(oneDay, day("2027-03-01"), today))))
    }

    @Test
    fun `a plain day with nothing logged has nothing to clear`() {
        val logs = marked("2027-03-02", "2027-03-06")

        assertFalse(DayLogEdits.canClear(logs, day("2027-03-12"), today))
        assertTrue(DayLogEdits.canClear(logs, day("2027-03-04"), today))
    }

    @Test
    fun `the usual period length is hers once she has one, else her setup's`() {
        val logs = cycles(28, 28, lastStart = day("2027-03-02"), periodLength = 4)
        val typical = CycleCalculator.overview(logs, setUp(periodLength = 6), today).typical

        assertEquals(4, CycleCalculator.usualPeriodLength(typical, setUp(periodLength = 6)))
        val none = CycleCalculator.overview(emptyList(), setUp(periodLength = 6), today).typical
        assertEquals(6, CycleCalculator.usualPeriodLength(none, setUp(periodLength = 6)))
        assertEquals(5, CycleCalculator.usualPeriodLength(none, notSetUp))
    }
}
