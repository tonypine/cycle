package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.Cycle
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Period
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodsTest {
    private val today = day("2027-03-20")

    @Test
    fun `no logs give no periods and no cycles`() {
        assertEquals(emptyList<Period>(), CycleCalculator.periods(emptyList(), today))
        assertEquals(emptyList<Cycle>(), CycleCalculator.cycles(emptyList()))
    }

    @Test
    fun `a run of light or heavier days is one period`() {
        val logs = listOf(FlowLevel.HEAVY, FlowLevel.MEDIUM, FlowLevel.LIGHT).mapIndexed { i, flow ->
            DayLog(day("2027-03-01").plusDays(i.toLong()), flow)
        }

        assertEquals(listOf(Period(day("2027-03-01"), day("2027-03-03"))), CycleCalculator.periods(logs, today))
    }

    @Test
    fun `spotting never starts a period`() {
        val logs = listOf(
            DayLog(day("2027-03-01"), FlowLevel.SPOTTING),
            DayLog(day("2027-03-02"), FlowLevel.SPOTTING),
            DayLog(day("2027-03-05"), FlowLevel.NONE)
        )

        assertEquals(emptyList<Period>(), CycleCalculator.periods(logs, today))
    }

    @Test
    fun `spotting before a period does not move its first day`() {
        val logs = listOf(DayLog(day("2027-03-01"), FlowLevel.SPOTTING)) + bleed(day("2027-03-02"), 4)

        assertEquals(listOf(Period(day("2027-03-02"), day("2027-03-05"))), CycleCalculator.periods(logs, today))
    }

    @Test
    fun `a one-day gap stays inside the period`() {
        val logs = bleed(day("2027-03-01"), 2) + bleed(day("2027-03-04"), 2)

        assertEquals(listOf(Period(day("2027-03-01"), day("2027-03-05"))), CycleCalculator.periods(logs, today))
    }

    @Test
    fun `a one-day gap logged as none or spotting stays inside the period`() {
        val logs = bleed(day("2027-03-01"), 2) + DayLog(day("2027-03-03"), FlowLevel.SPOTTING) +
            bleed(day("2027-03-04"), 1) + DayLog(day("2027-03-05"), FlowLevel.NONE) + bleed(day("2027-03-06"), 1)

        assertEquals(listOf(Period(day("2027-03-01"), day("2027-03-06"))), CycleCalculator.periods(logs, today))
    }

    @Test
    fun `a two-day gap starts a new period`() {
        val logs = bleed(day("2027-03-01"), 2) + bleed(day("2027-03-05"), 2)

        assertEquals(
            listOf(Period(day("2027-03-01"), day("2027-03-02")), Period(day("2027-03-05"), day("2027-03-06"))),
            CycleCalculator.periods(logs, today)
        )
    }

    @Test
    fun `an open period counts every day up to today`() {
        val logs = listOf(DayLog(day("2027-03-15"), periodStarted = true))

        assertEquals(
            listOf(Period(day("2027-03-15"), today, isOpen = true)),
            CycleCalculator.periods(logs, today)
        )
    }

    @Test
    fun `an open period takes in later flow days, however far apart`() {
        val logs = listOf(DayLog(day("2027-03-10"), FlowLevel.HEAVY, periodStarted = true)) +
            bleed(day("2027-03-16"), 1)

        assertEquals(
            listOf(Period(day("2027-03-10"), today, isOpen = true)),
            CycleCalculator.periods(logs, today)
        )
    }

    @Test
    fun `ended marks the last day, filling the days since started`() {
        val logs = listOf(
            DayLog(day("2027-03-01"), periodStarted = true),
            DayLog(day("2027-03-05"), periodEnded = true)
        )

        assertEquals(listOf(Period(day("2027-03-01"), day("2027-03-05"))), CycleCalculator.periods(logs, today))
    }

    @Test
    fun `flow logged after ended starts a new period`() {
        val logs = bleed(day("2027-03-01"), 3) + DayLog(day("2027-03-04"), periodEnded = true) +
            bleed(day("2027-03-05"), 2)

        assertEquals(
            listOf(Period(day("2027-03-01"), day("2027-03-04")), Period(day("2027-03-05"), day("2027-03-06"))),
            CycleCalculator.periods(logs, today)
        )
    }

    @Test
    fun `a new started after a gap closes the open period on its last logged day`() {
        val logs = bleed(day("2027-02-01"), 3).mapIndexed { i, log -> log.copy(periodStarted = i == 0) } +
            DayLog(day("2027-03-01"), periodStarted = true)

        assertEquals(
            listOf(Period(day("2027-02-01"), day("2027-02-03")), Period(day("2027-03-01"), today, isOpen = true)),
            CycleCalculator.periods(logs, today)
        )
    }

    @Test
    fun `started on a day inside a logged run joins it`() {
        val logs = listOf(
            DayLog(day("2027-03-01"), FlowLevel.MEDIUM),
            DayLog(day("2027-03-02"), FlowLevel.MEDIUM, periodStarted = true)
        )

        assertEquals(listOf(Period(day("2027-03-01"), today, isOpen = true)), CycleCalculator.periods(logs, today))
    }

    @Test
    fun `days logged after today are left out`() {
        val logs = bleed(day("2027-03-18"), 5)

        assertEquals(listOf(Period(day("2027-03-18"), today)), CycleCalculator.periods(logs, today))
    }

    @Test
    fun `a cycle runs from one period's first day to the day before the next`() {
        val periods = CycleCalculator.periods(bleed(day("2027-01-03")) + bleed(day("2027-01-31")), today)

        assertEquals(
            listOf(Cycle(day("2027-01-03"), day("2027-01-30")), Cycle(day("2027-01-31"), null)),
            CycleCalculator.cycles(periods)
        )
        assertEquals(28, CycleCalculator.cycles(periods).first().length)
    }
}
