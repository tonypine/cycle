package com.tonypine.cycle.core.ui

import com.tonypine.cycle.core.designsystem.BleedingWords
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleLegendEntry
import com.tonypine.cycle.core.model.BleedBasis
import com.tonypine.cycle.core.model.BleedEstimate
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionOverview
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.CycleEstimate
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.EstimateKind
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

    @Test
    fun `on a combined pill with monthly breaks the first break is expected whole, in the pill's words`() {
        val pill = ContraceptionStretch(ContraceptionMethod.COMBINED_PILL, day("2027-05-03"), breaks = Breaks.MONTHLY)
        val firstBreak = EstimatedPeriod(day("2027-05-24"), day("2027-05-24"), day("2027-05-30"), 5)
        val onThePill = overview.copy(
            today = day("2027-05-10"),
            estimate = null,
            contraception = ContraceptionOverview(
                stretches = listOf(pill),
                current = pill,
                estimates = EstimateKind.NEXT_BLEED,
                nextBleed = BleedEstimate(listOf(firstBreak), BleedBasis.START_DATE, missedBreak = false)
            )
        )
        val days = CalendarDays.from(onThePill, emptyList())

        assertEquals(CycleDayState.PredictedPeriod, days.stateOf(day("2027-05-30")))
        assertEquals(CycleDayState.Plain, days.stateOf(day("2027-05-31")))
        assertEquals(BleedingWords.Bleed, days.wordsOf(day("2027-05-24")))
        // Her period before the pill keeps its word.
        assertEquals(BleedingWords.Period, days.wordsOf(day("2027-03-01")))
        val may = (1..31).map { day("2027-05-%02d".format(it)) }
        assertEquals(
            CalendarLegend(
                listOf(CycleLegendEntry.Period, CycleLegendEntry.PredictedPeriod, CycleLegendEntry.Today),
                listOf(BleedingWords.Bleed),
                BleedingWords.Bleed
            ),
            days.legend(may, today = day("2027-05-10"))
        )
    }

    @Test
    fun `a month with a period before the pill and an expected bleed names each in its own word`() {
        val pill = ContraceptionStretch(ContraceptionMethod.COMBINED_PILL, day("2027-03-06"), breaks = Breaks.MONTHLY)
        val firstBreak = EstimatedPeriod(day("2027-03-27"), day("2027-03-27"), day("2027-04-02"), 5)
        val onThePill = overview.copy(
            estimate = null,
            contraception = ContraceptionOverview(
                stretches = listOf(pill),
                current = pill,
                estimates = EstimateKind.NEXT_BLEED,
                nextBleed = BleedEstimate(listOf(firstBreak), BleedBasis.START_DATE, missedBreak = false)
            )
        )
        val days = CalendarDays.from(onThePill, emptyList())

        val march = (1..31).map { day("2027-03-%02d".format(it)) }
        // Only 1 and 2 March are logged, a period; 27 to 31 March are expected bleeds.
        assertEquals(
            CalendarLegend(
                listOf(CycleLegendEntry.Period, CycleLegendEntry.PredictedPeriod, CycleLegendEntry.Today),
                listOf(BleedingWords.Period),
                BleedingWords.Bleed
            ),
            days.legend(march, today = day("2027-03-20"))
        )
    }

    @Test
    fun `after a counted bleed each expected bleed is its usual length`() {
        val pill = ContraceptionStretch(ContraceptionMethod.COMBINED_PILL, day("2027-05-03"), breaks = Breaks.MONTHLY)
        val next = EstimatedPeriod(day("2027-06-21"), day("2027-06-19"), day("2027-06-23"), 4)
        val onThePill = overview.copy(
            today = day("2027-06-15"),
            estimate = null,
            contraception = ContraceptionOverview(
                stretches = listOf(pill),
                current = pill,
                estimates = EstimateKind.NEXT_BLEED,
                nextBleed = BleedEstimate(listOf(next), BleedBasis.LAST_BLEED, missedBreak = false)
            )
        )
        val days = CalendarDays.from(onThePill, emptyList())

        assertEquals(CycleDayState.Plain, days.stateOf(day("2027-06-20")))
        assertEquals(CycleDayState.PredictedPeriod, days.stateOf(day("2027-06-24")))
        assertEquals(CycleDayState.Plain, days.stateOf(day("2027-06-25")))
    }

    @Test
    fun `on the implant nothing is predicted and the legend names only what shows`() {
        val implant = ContraceptionStretch(ContraceptionMethod.IMPLANT, day("2027-03-10"))
        val onTheImplant = overview.copy(
            periods = overview.periods + Period(day("2027-03-14"), day("2027-03-15"), stretch = implant),
            estimate = null,
            contraception = ContraceptionOverview(
                stretches = listOf(implant),
                current = implant,
                estimates = EstimateKind.NONE
            )
        )
        val days = CalendarDays.from(onTheImplant, emptyList())

        assertEquals(CycleDayState.Plain, days.stateOf(day("2027-03-30")))
        assertEquals(BleedingWords.Period, days.wordsOf(day("2027-03-01")))
        assertEquals(BleedingWords.Bleeding, days.wordsOf(day("2027-03-14")))
        val march = (1..31).map { day("2027-03-%02d".format(it)) }
        assertEquals(
            CalendarLegend(
                listOf(CycleLegendEntry.Period, CycleLegendEntry.Today),
                listOf(BleedingWords.Period, BleedingWords.Bleeding),
                BleedingWords.Bleeding
            ),
            days.legend(march, today = day("2027-03-20"))
        )
        // A month with nothing logged shows the word she would log in today.
        val april = (1..30).map { day("2027-04-%02d".format(it)) }
        assertEquals(
            CalendarLegend(listOf(CycleLegendEntry.Period), listOf(BleedingWords.Bleeding), BleedingWords.Bleeding),
            days.legend(april, today = day("2027-03-20"))
        )
    }
}
