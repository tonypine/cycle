package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.BleedBasis
import com.tonypine.cycle.core.model.BleedEstimate
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateKind
import com.tonypine.cycle.core.model.EstimatedPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The next bleed on a combined method with a break every month (`0006`). */
class NextBleedTest {
    private val pill = stretch(ContraceptionMethod.COMBINED_PILL, "2027-05-03")

    private fun overview(logs: List<DayLog>, today: String, stretch: ContraceptionStretch = pill): CycleOverview =
        CycleCalculator.overview(logs, setUp(), day(today), listOf(stretch))

    private fun nextBleed(logs: List<DayLog>, today: String, stretch: ContraceptionStretch = pill): BleedEstimate =
        checkNotNull(overview(logs, today, stretch).contraception.nextBleed)

    private fun bleed(expected: String, earliest: String, latest: String, length: Int) =
        EstimatedPeriod(day(expected), day(earliest), day(latest), length)

    @Test
    fun `before a bleed counts, it is expected in the first break, from the start date`() {
        val overview = overview(emptyList(), today = "2027-05-10")

        assertEquals(EstimateKind.NEXT_BLEED, overview.contraception.estimates)
        assertNull(overview.estimate)
        assertEquals(
            BleedEstimate(
                bleeds = listOf(
                    bleed("2027-05-24", "2027-05-24", "2027-05-30", 5),
                    bleed("2027-06-21", "2027-06-21", "2027-06-27", 5),
                    bleed("2027-07-19", "2027-07-19", "2027-07-25", 5)
                ),
                basis = BleedBasis.START_DATE,
                missedBreak = false
            ),
            overview.contraception.nextBleed
        )
    }

    @Test
    fun `bleeding between breaks does not move the estimate`() {
        val estimate = nextBleed(bleed(day("2027-05-08"), length = 3), today = "2027-05-20")

        assertEquals(bleed("2027-05-24", "2027-05-24", "2027-05-30", 5), estimate.next)
        assertEquals(BleedBasis.START_DATE, estimate.basis)
    }

    @Test
    fun `after a bleed counts, the next is a pack later, give or take 2 days`() {
        val estimate = nextBleed(bleed(day("2027-05-25"), length = 4), today = "2027-06-01")

        assertEquals(
            BleedEstimate(
                bleeds = listOf(
                    bleed("2027-06-22", "2027-06-20", "2027-06-24", 4),
                    bleed("2027-07-20", "2027-07-18", "2027-07-22", 4),
                    bleed("2027-08-17", "2027-08-15", "2027-08-19", 4)
                ),
                basis = BleedBasis.LAST_BLEED,
                missedBreak = false
            ),
            estimate
        )
    }

    @Test
    fun `a bleed less than 21 days after the last one that counted does not restart the count`() {
        val logs = bleed(day("2027-05-25"), length = 4) + bleed(day("2027-06-05"), length = 4)

        assertEquals(day("2027-06-22"), nextBleed(logs, today = "2027-06-10").next.expectedStart)
    }

    @Test
    fun `a break with no bleed logged moves the next one a pack later`() {
        val first = nextBleed(emptyList(), today = "2027-05-31")
        val later = nextBleed(bleed(day("2027-05-25"), length = 4), today = "2027-06-25")

        assertTrue(first.missedBreak)
        assertEquals(bleed("2027-06-21", "2027-06-21", "2027-06-27", 5), first.next)
        assertTrue(later.missedBreak)
        assertEquals(bleed("2027-07-20", "2027-07-18", "2027-07-22", 4), later.next)
    }

    @Test
    fun `the expected bleed is never in the past`() {
        val lastDay = nextBleed(emptyList(), today = "2027-05-30")
        val inRange = nextBleed(bleed(day("2027-05-25"), length = 4), today = "2027-06-21")

        assertFalse(lastDay.missedBreak)
        assertEquals(bleed("2027-05-30", "2027-05-30", "2027-05-30", 5), lastDay.next)
        assertEquals(bleed("2027-06-22", "2027-06-21", "2027-06-24", 4), inRange.next)
    }

    @Test
    fun `with no start date there is no estimate until a bleed, and the first one counts`() {
        val noStart = stretch(ContraceptionMethod.COMBINED_PILL, started = null)

        val before = overview(emptyList(), today = "2027-05-10", stretch = noStart)
        val after = nextBleed(bleed(day("2027-05-08"), length = 4), today = "2027-05-20", stretch = noStart)

        assertEquals(EstimateKind.NEXT_BLEED, before.contraception.estimates)
        assertNull(before.contraception.nextBleed)
        assertEquals(bleed("2027-06-05", "2027-06-03", "2027-06-07", 4), after.next)
        assertEquals(BleedBasis.LAST_BLEED, after.basis)
    }

    @Test
    fun `the patch and the ring with a break every month are estimated the same way`() {
        for (method in listOf(ContraceptionMethod.PATCH, ContraceptionMethod.RING)) {
            val estimate = nextBleed(emptyList(), today = "2027-05-10", stretch = stretch(method, "2027-05-03"))

            assertEquals("$method", day("2027-05-24"), estimate.next.earliestStart)
        }
    }

    @Test
    fun `a break every few packs, or none, gets no next-bleed estimate`() {
        for (breaks in listOf(Breaks.EVERY_FEW_PACKS, Breaks.NONE)) {
            val overview =
                overview(emptyList(), "2027-06-10", stretch(ContraceptionMethod.RING, "2027-05-03", breaks = breaks))

            assertEquals("$breaks", EstimateKind.NONE, overview.contraception.estimates)
            assertNull("$breaks", overview.contraception.nextBleed)
            assertNull("$breaks", overview.estimate)
        }
    }
}
