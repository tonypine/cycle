package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.EstimateKind
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.MethodBehaviour
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What the overview offers on each method (`0006`, Three behaviours and Per method). */
class ContraceptionOverviewTest {
    private val logs = cycles(29, 29, 29, lastStart = day("2027-01-20"))
    private val today = day("2027-03-01")

    @Test
    fun `each method says which estimates apply`() {
        val expected = listOf(
            stretch(
                ContraceptionMethod.COMBINED_PILL,
                "2027-02-01",
                breaks = Breaks.MONTHLY
            ) to EstimateKind.NEXT_BLEED,
            stretch(ContraceptionMethod.PATCH, "2027-02-01", breaks = Breaks.MONTHLY) to EstimateKind.NEXT_BLEED,
            stretch(ContraceptionMethod.RING, "2027-02-01", breaks = Breaks.MONTHLY) to EstimateKind.NEXT_BLEED,
            stretch(ContraceptionMethod.COMBINED_PILL, "2027-02-01", breaks = Breaks.EVERY_FEW_PACKS) to
                EstimateKind.NONE,
            stretch(ContraceptionMethod.PATCH, "2027-02-01", breaks = Breaks.NONE) to EstimateKind.NONE,
            stretch(ContraceptionMethod.PROGESTOGEN_PILL, "2027-02-01") to EstimateKind.NONE,
            stretch(ContraceptionMethod.IMPLANT, "2027-02-01") to EstimateKind.NONE,
            stretch(ContraceptionMethod.HORMONAL_IUD, "2027-02-01") to EstimateKind.NONE,
            stretch(ContraceptionMethod.INJECTION, "2027-02-01") to EstimateKind.NONE,
            stretch(ContraceptionMethod.COPPER_IUD, "2027-02-01") to EstimateKind.PERIOD
        )

        for ((method, estimates) in expected) {
            val overview = CycleCalculator.overview(logs, setUp(), today, listOf(method)).contraception
            val estimate = CycleCalculator.overview(logs, setUp(), today, listOf(method)).estimate

            val message = "${method.method} ${method.breaks}"
            assertEquals(message, method, overview.current)
            assertEquals(message, method.behaviour, overview.behaviour)
            assertEquals(message, estimates, overview.estimates)
            assertEquals(message, estimates == EstimateKind.PERIOD, estimate != null)
            assertEquals(message, estimates == EstimateKind.NEXT_BLEED, overview.nextBleed != null)
            assertEquals(message, estimates == EstimateKind.NONE, overview.bleeding != null)
            assertTrue(message, overview.firstMonths)
            assertNull(message, overview.stopped)
        }
    }

    @Test
    fun `with no method, everything is as before`() {
        val overview = CycleCalculator.overview(logs, setUp(), today)

        assertEquals(overview, CycleCalculator.overview(logs, setUp(), today, emptyList()))
        assertNull(overview.contraception.current)
        assertEquals(MethodBehaviour.OWN_CYCLE, overview.contraception.behaviour)
        assertEquals(EstimateKind.PERIOD, overview.contraception.estimates)
        assertNotNull(overview.estimate)
    }

    @Test
    fun `the first months last 3 months, 6 on a hormonal IUD and for the copper IUD's card`() {
        fun firstMonths(method: ContraceptionMethod, on: String) =
            CycleCalculator.overview(emptyList(), setUp(), day(on), listOf(stretch(method, "2027-01-01")))
                .contraception.firstMonths

        assertTrue(firstMonths(ContraceptionMethod.IMPLANT, "2027-03-31"))
        assertFalse(firstMonths(ContraceptionMethod.IMPLANT, "2027-04-01"))
        assertTrue(firstMonths(ContraceptionMethod.COMBINED_PILL, "2027-03-31"))
        assertFalse(firstMonths(ContraceptionMethod.COMBINED_PILL, "2027-04-01"))
        assertTrue(firstMonths(ContraceptionMethod.HORMONAL_IUD, "2027-06-30"))
        assertFalse(firstMonths(ContraceptionMethod.HORMONAL_IUD, "2027-07-01"))
        assertTrue(firstMonths(ContraceptionMethod.COPPER_IUD, "2027-06-30"))
        assertFalse(firstMonths(ContraceptionMethod.COPPER_IUD, "2027-07-01"))
    }

    @Test
    fun `with an unknown start, there are no first months`() {
        val implant = stretch(ContraceptionMethod.IMPLANT, started = null)

        assertFalse(CycleCalculator.overview(emptyList(), setUp(), today, listOf(implant)).contraception.firstMonths)
    }

    @Test
    fun `the method ends on its stop date and the next day is none`() {
        val implant = stretch(ContraceptionMethod.IMPLANT, "2026-11-09", "2027-03-01")

        val lastDay = CycleCalculator.overview(logs, setUp(), day("2027-03-01"), listOf(implant))
        val dayAfter = CycleCalculator.overview(logs, setUp(), day("2027-03-02"), listOf(implant))

        assertEquals(implant, lastDay.contraception.current)
        assertNull(lastDay.contraception.stopped)
        assertNull(dayAfter.contraception.current)
        assertEquals(1, dayAfter.contraception.stopped?.days)
        assertEquals(listOf(implant), dayAfter.contraception.stretches)
    }

    @Test
    fun `editing or deleting a stretch recomputes every cycle after it`() {
        val logs = cycles(28, 28, 28, 28, lastStart = day("2027-03-01"))
        val implant = stretch(ContraceptionMethod.IMPLANT, "2027-03-10")
        val today = day("2027-03-20")

        val onImplant = CycleCalculator.overview(logs, setUp(), today, listOf(implant))
        val startedEarlier = CycleCalculator.overview(
            logs,
            setUp(),
            today,
            listOf(implant.copy(started = day("2027-02-10")))
        )
        val deleted = CycleCalculator.overview(logs, setUp(), today, emptyList())

        assertEquals(listOf(28, 28, 28, 28, 9), onImplant.cycles.map { it.length })
        assertEquals(LengthSummary(28, 28, 28, 4), onImplant.typical.cycle)
        assertEquals(listOf(28, 28, 28, 9), startedEarlier.cycles.map { it.length })
        assertEquals(LengthSummary(28, 28, 28, 3), startedEarlier.typical.cycle)
        assertEquals(implant.copy(started = day("2027-02-10")), startedEarlier.periods.last().stretch)
        assertEquals(listOf(28, 28, 28, 28, null), deleted.cycles.map { it.length })
        assertEquals(20, deleted.cycleDay)
        assertNotNull(deleted.estimate)
    }
}
