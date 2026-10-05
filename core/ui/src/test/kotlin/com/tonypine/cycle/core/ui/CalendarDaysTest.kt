package com.tonypine.cycle.core.ui

import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.model.CycleEstimate
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.EstimatedPeriod
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.TypicalLengths
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Synthetic dates in 2027. */
class CalendarDaysTest {
    private fun day(iso: String) = LocalDate.parse(iso)

    private val overview = CycleOverview(
        today = day("2027-03-20"),
        periods = listOf(Period(day("2027-02-26"), day("2027-03-02"))),
        cycles = emptyList(),
        typical = TypicalLengths(null, null),
        estimate = CycleEstimate(
            periods = listOf(EstimatedPeriod(day("2027-03-26"), day("2027-03-22"), day("2027-03-30"), 5)),
            daysLate = 0,
            cycleBasis = EstimateBasis.Typical,
            periodBasis = EstimateBasis.Typical
        ),
        prompts = emptyList()
    )

    @Test
    fun `a period across two months draws in both, and the estimate is predicted`() {
        val days = CalendarDays.from(overview, emptyList())

        assertEquals(CycleDayState.Period, days.stateOf(day("2027-02-27")))
        assertEquals(CycleDayState.Period, days.stateOf(day("2027-03-02")))
        assertEquals(CycleDayState.Plain, days.stateOf(day("2027-03-03")))
        assertEquals(CycleDayState.PredictedPeriod, days.stateOf(day("2027-03-30")))
        assertEquals(CycleDayState.Plain, days.stateOf(day("2027-03-31")))
    }

    @Test
    fun `a day logged without period flow inside a period draws plain`() {
        val logs = listOf(
            DayLog(day("2027-02-28"), flow = FlowLevel.NONE),
            DayLog(day("2027-03-01"), flow = FlowLevel.MEDIUM)
        )
        val days = CalendarDays.from(overview, logs)

        assertEquals(CycleDayState.Plain, days.stateOf(day("2027-02-28")))
        assertEquals(CycleDayState.Period, days.stateOf(day("2027-03-01")))
    }
}
