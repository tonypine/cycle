package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.CycleEstimate
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.EstimatedPeriod
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.TypicalLengths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EstimateTest {
    /** The worked example in `docs/research/predictions.md`. */
    private val workedExample = cycles(29, 26, 31, 28, 27, 28, lastStart = day("2027-03-02"))

    private fun estimate(logs: List<DayLog>, today: String, settings: CycleSettings = setUp()): CycleEstimate? =
        CycleCalculator.overview(logs, settings, day(today)).estimate

    @Test
    fun `worked example expects 30 March, between 28 March and 2 April`() {
        val estimate = estimate(workedExample, today = "2027-03-10")!!

        assertEquals(
            EstimatedPeriod(
                expectedStart = day("2027-03-30"),
                earliestStart = day("2027-03-28"),
                latestStart = day("2027-04-02"),
                expectedLength = 5
            ),
            estimate.next
        )
        assertEquals(0, estimate.daysLate)
        assertEquals(EstimateBasis.Logged(6), estimate.cycleBasis)
        assertEquals(EstimateBasis.Logged(6), estimate.periodBasis)
    }

    @Test
    fun `worked example gives her typical lengths`() {
        val overview = CycleCalculator.overview(workedExample, setUp(), day("2027-03-10"))

        assertEquals(
            TypicalLengths(
                cycle = LengthSummary(median = 28, shortest = 26, longest = 31, count = 6),
                period = LengthSummary(median = 5, shortest = 5, longest = 5, count = 6)
            ),
            overview.typical
        )
    }

    @Test
    fun `only the last six cycles count`() {
        val withAnOlderLongCycle = cycles(45, 29, 26, 31, 28, 27, 28, lastStart = day("2027-03-02"))

        assertEquals(estimate(workedExample, "2027-03-10"), estimate(withAnOlderLongCycle, "2027-03-10"))
    }

    @Test
    fun `no data gives no estimate`() {
        val overview = CycleCalculator.overview(emptyList(), notSetUp, day("2027-03-10"))

        assertNull(overview.estimate)
        assertEquals(emptyList<Any>(), overview.periods)
        assertEquals(emptyList<Any>(), overview.cycles)
        assertEquals(TypicalLengths(cycle = null, period = null), overview.typical)
        assertEquals(emptyList<Any>(), overview.prompts)
        assertNull(overview.cycleDay)
    }

    @Test
    fun `setup only uses her setup lengths with a range of 4 days either side`() {
        val started = listOf(DayLog(day("2027-03-02"), periodStarted = true))

        val estimate = estimate(started, today = "2027-03-05", settings = setUp(cycleLength = 30, periodLength = 4))!!

        assertEquals(
            EstimatedPeriod(
                expectedStart = day("2027-04-01"),
                earliestStart = day("2027-03-28"),
                latestStart = day("2027-04-05"),
                expectedLength = 4
            ),
            estimate.next
        )
        assertEquals(EstimateBasis.Setup, estimate.cycleBasis)
        assertEquals(EstimateBasis.Setup, estimate.periodBasis)
    }

    @Test
    fun `before setup the typical 28-day cycle is used, and said to be typical`() {
        val started = listOf(DayLog(day("2027-03-02"), periodStarted = true))

        val estimate = estimate(started, today = "2027-03-05", settings = notSetUp.copy(usualCycleLength = 35))!!

        assertEquals(day("2027-03-30"), estimate.next.expectedStart)
        assertEquals(day("2027-03-26"), estimate.next.earliestStart)
        assertEquals(day("2027-04-03"), estimate.next.latestStart)
        assertEquals(EstimateBasis.Typical, estimate.cycleBasis)
    }

    @Test
    fun `one cycle keeps a range of at least 3 days either side`() {
        val estimate = estimate(cycles(30, lastStart = day("2027-03-02")), today = "2027-03-10")!!

        assertEquals(day("2027-04-01"), estimate.next.expectedStart)
        assertEquals(day("2027-03-29"), estimate.next.earliestStart)
        assertEquals(day("2027-04-04"), estimate.next.latestStart)
        assertEquals(EstimateBasis.Logged(1), estimate.cycleBasis)
    }

    @Test
    fun `two cycles keep a range of at least 3 days either side, wider where she was`() {
        // Median 28; she was 2 days shorter (widened to 3) and 2 days longer (widened to 3).
        val estimate = estimate(cycles(26, 30, lastStart = day("2027-03-02")), today = "2027-03-10")!!

        assertEquals(day("2027-03-30"), estimate.next.expectedStart)
        assertEquals(day("2027-03-27"), estimate.next.earliestStart)
        assertEquals(day("2027-04-02"), estimate.next.latestStart)
        assertEquals(EstimateBasis.Logged(2), estimate.cycleBasis)
    }

    @Test
    fun `two cycles further apart than 3 days keep her own range`() {
        val estimate = estimate(cycles(24, 32, lastStart = day("2027-03-02")), today = "2027-03-10")!!

        assertEquals(day("2027-03-30"), estimate.next.expectedStart)
        assertEquals(day("2027-03-26"), estimate.next.earliestStart)
        assertEquals(day("2027-04-03"), estimate.next.latestStart)
    }

    @Test
    fun `three cycles use her shortest to longest, however narrow`() {
        val estimate = estimate(cycles(27, 28, 29, lastStart = day("2027-03-02")), today = "2027-03-10")!!

        assertEquals(day("2027-03-30"), estimate.next.expectedStart)
        assertEquals(day("2027-03-29"), estimate.next.earliestStart)
        assertEquals(day("2027-03-31"), estimate.next.latestStart)
    }

    @Test
    fun `on the expected day it is not late, and the range does not reach into the past`() {
        val estimate = estimate(workedExample, today = "2027-03-30")!!

        assertEquals(day("2027-03-30"), estimate.next.expectedStart)
        assertEquals(day("2027-03-30"), estimate.next.earliestStart)
        assertEquals(day("2027-04-02"), estimate.next.latestStart)
        assertEquals(0, estimate.daysLate)
    }

    @Test
    fun `once late, the next period is expected today and the days late count up`() {
        val estimate = estimate(workedExample, today = "2027-04-03")!!

        assertEquals(
            EstimatedPeriod(
                expectedStart = day("2027-04-03"),
                earliestStart = day("2027-04-03"),
                latestStart = day("2027-04-03"),
                expectedLength = 5
            ),
            estimate.next
        )
        assertEquals(4, estimate.daysLate)
        assertEquals(5, estimate(workedExample, today = "2027-04-04")!!.daysLate)
    }

    @Test
    fun `three periods ahead, each a median cycle after the one before, with the same range`() {
        val estimate = estimate(workedExample, today = "2027-03-10")!!

        assertEquals(
            listOf(
                EstimatedPeriod(day("2027-03-30"), day("2027-03-28"), day("2027-04-02"), 5),
                EstimatedPeriod(day("2027-04-27"), day("2027-04-25"), day("2027-04-30"), 5),
                EstimatedPeriod(day("2027-05-25"), day("2027-05-23"), day("2027-05-28"), 5)
            ),
            estimate.periods
        )
    }

    @Test
    fun `once late, the periods after the next one follow from today`() {
        val estimate = estimate(workedExample, today = "2027-04-03")!!

        assertEquals(
            listOf(day("2027-04-03"), day("2027-05-01"), day("2027-05-29")),
            estimate.periods.map {
                it.expectedStart
            }
        )
    }

    @Test
    fun `the expected length is the median of her periods`() {
        val estimate = estimate(
            cycles(28, 28, 28, lastStart = day("2027-03-02"), periodLength = 4),
            today = "2027-03-10"
        )!!

        assertEquals(4, estimate.next.expectedLength)
        assertEquals(day("2027-04-02"), estimate.next.expectedEnd)
        assertEquals(EstimateBasis.Logged(4), estimate.periodBasis)
    }

    @Test
    fun `a period that may still go on does not count towards her typical length yet`() {
        // The last period's last logged day is yesterday: today's log could still extend it.
        val logs = cycles(28, lastStart = day("2027-03-02"), periodLength = 3)

        val overview = CycleCalculator.overview(logs, setUp(), day("2027-03-05"))

        assertEquals(LengthSummary(median = 3, shortest = 3, longest = 3, count = 1), overview.typical.period)
    }

    @Test
    fun `the median of an even count rounds a half day up`() {
        assertEquals(28, CycleCalculator.summarize(listOf(27, 28))!!.median)
        assertEquals(27, CycleCalculator.summarize(listOf(26, 27))!!.median)
        assertEquals(28, CycleCalculator.summarize(listOf(31, 26, 28, 28))!!.median)
    }
}
