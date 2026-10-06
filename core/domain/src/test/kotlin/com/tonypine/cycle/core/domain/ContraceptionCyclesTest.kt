package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.BleedingWord
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.Cycle
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.MethodBehaviour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which cycles and periods leave her typical cycle on each method (`0006`, Leaving her estimates). */
class ContraceptionCyclesTest {
    /** Seven periods 29 days apart, the last on 1 January 2027. */
    private val ownCycles = cycles(29, 29, 29, 29, 29, 29, lastStart = day("2027-01-01"))

    /** Every hormonal method and kind of breaks. */
    private val hormonal = listOf(
        stretch(ContraceptionMethod.COMBINED_PILL, "2027-01-15", breaks = Breaks.MONTHLY),
        stretch(ContraceptionMethod.COMBINED_PILL, "2027-01-15", breaks = Breaks.EVERY_FEW_PACKS),
        stretch(ContraceptionMethod.COMBINED_PILL, "2027-01-15", breaks = Breaks.NONE),
        stretch(ContraceptionMethod.PATCH, "2027-01-15", breaks = Breaks.MONTHLY),
        stretch(ContraceptionMethod.RING, "2027-01-15", breaks = Breaks.EVERY_FEW_PACKS),
        stretch(ContraceptionMethod.PROGESTOGEN_PILL, "2027-01-15"),
        stretch(ContraceptionMethod.IMPLANT, "2027-01-15"),
        stretch(ContraceptionMethod.HORMONAL_IUD, "2027-01-15"),
        stretch(ContraceptionMethod.INJECTION, "2027-01-15")
    )

    @Test
    fun `every method is in exactly one behaviour, and only the copper IUD keeps her cycle`() {
        val behaviours = ContraceptionMethod.entries.associateWith { stretch(it, "2027-01-15").behaviour }

        assertEquals(MethodBehaviour.OWN_CYCLE, behaviours[ContraceptionMethod.COPPER_IUD])
        assertEquals(
            ContraceptionMethod.entries - ContraceptionMethod.COPPER_IUD,
            ContraceptionMethod.entries.filter { it.isHormonal }
        )
        assertEquals(
            listOf(ContraceptionMethod.COMBINED_PILL, ContraceptionMethod.PATCH, ContraceptionMethod.RING),
            ContraceptionMethod.entries.filter { behaviours[it] == MethodBehaviour.SCHEDULED_BLEED }
        )
    }

    @Test
    fun `a hormonal method cuts short the cycle she was in and leaves its bleeding out of her lengths`() {
        for (method in hormonal) {
            val logs = ownCycles + bleed(day("2027-02-10")) + bleed(day("2027-03-10"), length = 9)

            val overview = CycleCalculator.overview(logs, setUp(), day("2027-03-20"), listOf(method))

            val message = "on $method"
            assertEquals(message, List(6) { 29 } + 14, overview.cycles.map { it.length })
            assertEquals(message, Cycle(day("2027-01-01"), day("2027-01-14"), cutShort = true), overview.cycles.last())
            assertEquals(message, LengthSummary(29, 29, 29, 6), overview.typical.cycle)
            // The 9-day bleed on the method is not one of her periods.
            assertEquals(message, LengthSummary(5, 5, 5, 6), overview.typical.period)
            assertEquals(message, listOf(method, method), overview.periods.takeLast(2).map { it.stretch })
            assertTrue(message, overview.periods.dropLast(2).all { it.stretch == null })
            assertNull(message, overview.currentCycle)
            assertNull(message, overview.cycleDay)
            assertNull(message, overview.estimate)
            assertEquals(message, emptyList<Any>(), overview.prompts)
        }
    }

    @Test
    fun `on a copper IUD her cycles count, and her period length comes from periods since fitting`() {
        val copper = stretch(ContraceptionMethod.COPPER_IUD, "2027-01-15")
        val logs = ownCycles + bleed(day("2027-01-30"), length = 7) + bleed(day("2027-02-28"), length = 7)

        val overview = CycleCalculator.overview(logs, setUp(), day("2027-03-10"), listOf(copper))

        assertEquals(List(8) { 29 } + null, overview.cycles.map { it.length })
        assertTrue(overview.cycles.none { it.cutShort })
        assertEquals(LengthSummary(29, 29, 29, 6), overview.typical.cycle)
        assertEquals(LengthSummary(7, 7, 7, 2), overview.typical.period)
        assertEquals(day("2027-03-29"), overview.estimate?.next?.expectedStart)
        assertEquals(7, overview.estimate?.next?.expectedLength)
        assertEquals(11, overview.cycleDay)
    }

    @Test
    fun `on a copper IUD her usual period length holds until a period since fitting has ended`() {
        val copper = stretch(ContraceptionMethod.COPPER_IUD, "2027-01-15")
        val logs = ownCycles + bleed(day("2027-01-30"), length = 7)

        val overview = CycleCalculator.overview(logs, setUp(), day("2027-02-01"), listOf(copper))

        assertEquals(LengthSummary(5, 5, 5, 6), overview.typical.period)
    }

    @Test
    fun `a stretch with no start covers every day before its stop, and no cycle is cut short`() {
        val implant = stretch(ContraceptionMethod.IMPLANT, started = null, stopped = "2027-01-20")
        val logs = ownCycles + bleed(day("2027-02-10")) + bleed(day("2027-03-11"))

        val overview = CycleCalculator.overview(logs, setUp(), day("2027-03-20"), listOf(implant))

        assertEquals(
            listOf(Cycle(day("2027-02-10"), day("2027-03-10")), Cycle(day("2027-03-11"), null)),
            overview.cycles
        )
        assertEquals(LengthSummary(29, 29, 29, 1), overview.typical.cycle)
        assertTrue(overview.periods.filter { it.start < day("2027-01-20") }.all { it.stretch == implant })
    }

    @Test
    fun `her last six cycles come from before a stretch and after it`() {
        val implant = stretch(ContraceptionMethod.IMPLANT, "2026-12-20", "2027-03-01")
        val logs = cycles(30, 30, 30, lastStart = day("2026-12-01")) +
            bleed(day("2027-03-20")) + bleed(day("2027-04-18")) + bleed(day("2027-05-17"))

        val overview = CycleCalculator.overview(logs, setUp(), day("2027-05-25"), listOf(implant))

        assertEquals(listOf(30, 30, 30, 19, 29, 29, null), overview.cycles.map { it.length })
        assertEquals(listOf(false, false, false, true, false, false, false), overview.cycles.map { it.cutShort })
        assertEquals(LengthSummary(median = 30, shortest = 29, longest = 30, count = 5), overview.typical.cycle)
    }

    @Test
    fun `a bleed in the 7 days after a combined method stops is its withdrawal bleed, a day later a period`() {
        val pill = stretch(ContraceptionMethod.COMBINED_PILL, "2027-05-03", "2027-11-03")

        val withdrawal = CycleCalculator.overview(bleed(day("2027-11-10")), setUp(), day("2027-11-20"), listOf(pill))
        val period = CycleCalculator.overview(bleed(day("2027-11-11")), setUp(), day("2027-11-20"), listOf(pill))

        assertEquals(pill, withdrawal.periods.single().stretch)
        assertEquals(emptyList<Cycle>(), withdrawal.cycles)
        assertNull(period.periods.single().stretch)
        assertEquals(listOf(Cycle(day("2027-11-11"), null)), period.cycles)
    }

    @Test
    fun `only a combined method has a withdrawal bleed after it`() {
        val miniPill = stretch(ContraceptionMethod.PROGESTOGEN_PILL, "2027-05-03", "2027-11-03")

        val overview = CycleCalculator.overview(bleed(day("2027-11-06")), setUp(), day("2027-11-20"), listOf(miniPill))

        assertNull(overview.periods.single().stretch)
        assertEquals(15, overview.cycleDay)
    }

    @Test
    fun `her bleeding is called by the method on the day`() {
        val pill = stretch(ContraceptionMethod.COMBINED_PILL, "2027-05-03", "2027-11-03")
        val implant = stretch(ContraceptionMethod.IMPLANT, "2027-12-01")
        val logs = bleed(day("2027-04-20")) + bleed(day("2027-05-28")) + bleed(day("2027-11-08"), length = 9) +
            bleed(day("2027-12-10"))

        val overview = CycleCalculator.overview(logs, setUp(), day("2027-12-20"), listOf(pill, implant))

        assertEquals(BleedingWord.PERIOD, overview.bleedingWord(day("2027-04-20")))
        assertEquals(BleedingWord.BLEED, overview.bleedingWord(day("2027-05-28")))
        // The withdrawal bleed keeps the pill's word to its last day, past the 7 days.
        assertEquals(BleedingWord.BLEED, overview.bleedingWord(day("2027-11-16")))
        assertEquals(BleedingWord.PERIOD, overview.bleedingWord(day("2027-11-20")))
        assertEquals(BleedingWord.BLEEDING, overview.bleedingWord(day("2027-12-10")))
    }

    @Test
    fun `a period on a copper IUD or a pill with breaks every few packs takes their words`() {
        val copper = stretch(ContraceptionMethod.COPPER_IUD, "2027-01-15", "2027-05-01")
        val ring = stretch(ContraceptionMethod.RING, "2027-05-02", breaks = Breaks.EVERY_FEW_PACKS)
        val logs = bleed(day("2027-02-01")) + bleed(day("2027-06-01"))

        val overview = CycleCalculator.overview(logs, setUp(), day("2027-06-10"), listOf(copper, ring))

        assertEquals(BleedingWord.PERIOD, overview.bleedingWord(day("2027-02-01")))
        assertEquals(BleedingWord.BLEEDING, overview.bleedingWord(day("2027-06-01")))
    }

    @Test
    fun `stretches that start after today are not in force yet`() {
        val implant = stretch(ContraceptionMethod.IMPLANT, "2027-03-21")

        val overview = CycleCalculator.overview(ownCycles, setUp(), day("2027-01-20"), listOf(implant))

        assertEquals(CycleCalculator.overview(ownCycles, setUp(), day("2027-01-20")), overview)
    }
}
