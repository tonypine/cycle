package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.PeriodRefusal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** "Edit period dates" and "Delete this period" in History, by [DayLogEdits]. */
class PeriodEditsTest {
    private val today = day("2027-03-20")

    /** Three periods logged as flow alone: 5 to 9 January, 2 to 6 February and 2 to 6 March. */
    private val threePeriods = bleed(day("2027-01-05")) + bleed(day("2027-02-02")) + bleed(day("2027-03-02"))

    /** [logs] with [edits] written over them, the way the repository writes them. */
    private fun apply(logs: List<DayLog>, edits: List<DayLog>): List<DayLog> {
        val edited = edits.map { it.date }.toSet()
        return (logs.filterNot { it.date in edited } + edits.filterNot { it.isEmpty }).sortedBy { it.date }
    }

    private fun periods(logs: List<DayLog>) = CycleCalculator.periods(logs, today)

    private fun period(start: String, end: String, isOpen: Boolean = false) = Period(day(start), day(end), isOpen)

    private fun edit(logs: List<DayLog>, start: String, newStart: String, newEnd: String?): List<DayLog> {
        val plan = DayLogEdits.editPeriod(logs, day(start), day(newStart), newEnd?.let(::day), today)
        return apply(logs, (plan as PeriodPlan.Ready).writes)
    }

    private fun refusal(logs: List<DayLog>, start: String, newStart: String, newEnd: String?): PeriodRefusal {
        val plan = DayLogEdits.editPeriod(logs, day(start), day(newStart), newEnd?.let(::day), today)
        return (plan as PeriodPlan.Refused).reason
    }

    @Test
    fun `a period moves and shrinks, and the others keep their days`() {
        val after = edit(threePeriods, "2027-02-02", newStart = "2027-02-04", newEnd = "2027-02-05")

        assertEquals(
            listOf(
                period("2027-01-05", "2027-01-09"),
                period("2027-02-04", "2027-02-05"),
                period("2027-03-02", "2027-03-06")
            ),
            periods(after)
        )
        // The days it gave up lose their flow; the rows go, as nothing else was logged on them.
        assertTrue(after.none { it.date in listOf(day("2027-02-02"), day("2027-02-03"), day("2027-02-06")) })
    }

    @Test
    fun `a period grows into days she did not log`() {
        val after = edit(threePeriods, "2027-02-02", newStart = "2027-01-30", newEnd = "2027-02-10")

        assertEquals(period("2027-01-30", "2027-02-10"), periods(after)[1])
        assertEquals(3, periods(after).size)
        // Her flow on the days it kept stays.
        assertEquals(FlowLevel.MEDIUM, after.single { it.date == day("2027-02-04") }.flow)
    }

    @Test
    fun `none and spotting on the days it gives up stay`() {
        val logs = threePeriods + DayLog(day("2027-02-07"), flow = FlowLevel.SPOTTING) +
            DayLog(day("2027-02-08"), flow = FlowLevel.LIGHT)
        val after = edit(logs, "2027-02-02", newStart = "2027-02-02", newEnd = "2027-02-05")

        assertEquals(period("2027-02-02", "2027-02-05"), periods(after)[1])
        assertEquals(FlowLevel.SPOTTING, after.single { it.date == day("2027-02-07") }.flow)
        assertTrue(after.none { it.date == day("2027-02-08") })
    }

    @Test
    fun `a stray marker inside the new days neither splits nor ends it early`() {
        // Started on the 5th and ended on the 9th; a lone "ended" on the 12th was ignored.
        val logs = listOf(DayLog(day("2027-02-05"), FlowLevel.MEDIUM, periodStarted = true)) +
            bleed(day("2027-02-06"), 3) + DayLog(day("2027-02-09"), FlowLevel.MEDIUM, periodEnded = true) +
            DayLog(day("2027-02-12"), periodEnded = true)
        val after = edit(logs, "2027-02-05", newStart = "2027-02-01", newEnd = "2027-02-14")

        assertEquals(listOf(period("2027-02-01", "2027-02-14")), periods(after))
    }

    @Test
    fun `the period before, started and never ended, does not take in the days given up`() {
        // January was marked started only: February's "started" is all that ends it.
        val logs = listOf(DayLog(day("2027-01-05"), FlowLevel.MEDIUM, periodStarted = true)) +
            bleed(day("2027-01-06"), 4) +
            DayLog(day("2027-02-02"), FlowLevel.MEDIUM, periodStarted = true) + bleed(day("2027-02-03"), 4)
        val after = edit(logs, "2027-02-02", newStart = "2027-02-04", newEnd = "2027-02-06")

        assertEquals(listOf(period("2027-01-05", "2027-01-09"), period("2027-02-04", "2027-02-06")), periods(after))
    }

    @Test
    fun `moving a period past a lone ended does not let it stretch the one before`() {
        // A lone "ended" on 10 February was ignored, as February had ended on the 6th.
        val logs = threePeriods.map { if (it.date == day("2027-02-06")) it.copy(periodEnded = true) else it } +
            DayLog(day("2027-02-10"), periodEnded = true)
        val after = edit(logs, "2027-02-02", newStart = "2027-02-12", newEnd = "2027-02-14")

        assertEquals(
            listOf(
                period("2027-01-05", "2027-01-09"),
                period("2027-02-12", "2027-02-14"),
                period("2027-03-02", "2027-03-06")
            ),
            periods(after)
        )
        assertTrue(after.single { it.date == day("2027-01-09") }.periodEnded)
    }

    @Test
    fun `the current period can end, and an ended last period can be still going again`() {
        val going = threePeriods.dropLast(5) + DayLog(day("2027-03-15"), FlowLevel.HEAVY, periodStarted = true)
        val ended = edit(going, "2027-03-15", newStart = "2027-03-15", newEnd = "2027-03-18")
        assertEquals(period("2027-03-15", "2027-03-18"), periods(ended).last())

        val again = edit(ended, "2027-03-15", newStart = "2027-03-14", newEnd = null)
        assertEquals(period("2027-03-14", "2027-03-20", isOpen = true), periods(again).last())
        assertEquals(3, periods(again).size)
    }

    @Test
    fun `still going drops a stray ended later on`() {
        // March ended on the 6th; a lone "ended" on the 12th was ignored.
        val logs = threePeriods + DayLog(day("2027-03-06"), FlowLevel.MEDIUM, periodEnded = true) +
            DayLog(day("2027-03-12"), periodEnded = true)
        val after = edit(logs, "2027-03-02", newStart = "2027-03-02", newEnd = null)

        assertEquals(period("2027-03-02", "2027-03-20", isOpen = true), periods(after).last())
    }

    @Test
    fun `new days that reach another period, or come within a day of it, are refused`() {
        val february = period("2027-02-02", "2027-02-06")
        val march = period("2027-03-02", "2027-03-06")

        // One clear day between them would join them; two keep them apart.
        assertEquals(PeriodRefusal.TooClose(march), refusal(threePeriods, "2027-02-02", "2027-02-02", "2027-02-28"))
        assertEquals(3, periods(edit(threePeriods, "2027-02-02", "2027-02-02", "2027-02-27")).size)
        assertEquals(PeriodRefusal.TooClose(february), refusal(threePeriods, "2027-03-02", "2027-02-08", "2027-03-06"))
        assertEquals(PeriodRefusal.TooClose(march), refusal(threePeriods, "2027-02-02", "2027-02-02", null))
    }

    @Test
    fun `a day after today, a last day before the first, or a period gone are refused`() {
        assertEquals(PeriodRefusal.AfterToday, refusal(threePeriods, "2027-03-02", "2027-03-02", "2027-03-21"))
        assertEquals(PeriodRefusal.AfterToday, refusal(threePeriods, "2027-03-02", "2027-03-21", null))
        assertEquals(PeriodRefusal.EndBeforeStart, refusal(threePeriods, "2027-03-02", "2027-03-05", "2027-03-04"))
        assertEquals(PeriodRefusal.Gone, refusal(threePeriods, "2027-03-03", "2027-03-03", "2027-03-05"))
    }

    @Test
    fun `saving the same days writes nothing`() {
        val logs = threePeriods.dropLast(5) + DayLogEdits.fill(emptyList(), day("2027-03-02"), 5, today)
        val plan = DayLogEdits.editPeriod(logs, day("2027-03-02"), day("2027-03-02"), day("2027-03-06"), today)

        assertEquals(PeriodPlan.Ready(emptyList()), plan)
    }

    @Test
    fun `deleting a period joins its cycle to the one before, and the others keep their days`() {
        val logs = threePeriods.map { if (it.date == day("2027-02-03")) it.copy(flow = FlowLevel.SPOTTING) else it }
        val after = apply(logs, DayLogEdits.deletePeriod(logs, day("2027-02-02"), today))

        assertEquals(listOf(period("2027-01-05", "2027-01-09"), period("2027-03-02", "2027-03-06")), periods(after))
        assertEquals(listOf(DayLog(day("2027-02-03"), FlowLevel.SPOTTING)), after.filter { it.date.monthValue == 2 })
    }

    @Test
    fun `deleting the last period leaves the one before ended, not still going`() {
        val logs =
            listOf(DayLog(day("2027-02-02"), FlowLevel.MEDIUM, periodStarted = true)) + bleed(day("2027-02-03"), 4) +
                DayLog(day("2027-03-02"), FlowLevel.MEDIUM, periodStarted = true)
        val after = apply(logs, DayLogEdits.deletePeriod(logs, day("2027-03-02"), today))

        assertEquals(listOf(period("2027-02-02", "2027-02-06")), periods(after))
    }

    @Test
    fun `deleting a period does not let a lone ended after it stretch the one before`() {
        // February ended on the 6th, and a lone "ended" on the 10th was ignored.
        val logs = threePeriods.map { if (it.date == day("2027-02-06")) it.copy(periodEnded = true) else it } +
            DayLog(day("2027-02-10"), periodEnded = true)
        val after = apply(logs, DayLogEdits.deletePeriod(logs, day("2027-02-02"), today))

        assertEquals(listOf(period("2027-01-05", "2027-01-09"), period("2027-03-02", "2027-03-06")), periods(after))
        assertTrue(after.single { it.date == day("2027-01-09") }.periodEnded)
    }

    @Test
    fun `deleting a period that is gone, or the only one, works`() {
        assertEquals(emptyList<DayLog>(), DayLogEdits.deletePeriod(threePeriods, day("2027-02-03"), today))

        val only = bleed(day("2027-03-02"))
        assertEquals(
            emptyList<Period>(),
            periods(apply(only, DayLogEdits.deletePeriod(only, day("2027-03-02"), today)))
        )
    }
}
