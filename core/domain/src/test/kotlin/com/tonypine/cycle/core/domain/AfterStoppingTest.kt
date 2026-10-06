package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.BleedingWord
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.CycleEstimate
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.EstimateKind
import com.tonypine.cycle.core.model.EstimatedPeriod
import com.tonypine.cycle.core.model.StoppedMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Estimates after she stops a method (`0006`, Stopping; `docs/design/contraception.md`, journey D). */
class AfterStoppingTest {
    private val implant = stretch(ContraceptionMethod.IMPLANT, "2026-11-09", "2027-11-03")

    /** Her own cycles of 29 days before the implant, the last one cut short when it went in. */
    private val before = cycles(29, 29, 29, 29, 29, 29, lastStart = day("2026-10-20"))

    @Test
    fun `the next period is expected her usual cycle after the stop date, give or take 7 days`() {
        val overview = CycleCalculator.overview(
            bleed(day("2027-10-01"), 3),
            setUp(29),
            day("2027-11-15"),
            listOf(implant)
        )

        assertEquals(EstimateKind.PERIOD, overview.contraception.estimates)
        assertEquals(StoppedMethod(implant, days = 12, withdrawalWindow = false), overview.contraception.stopped)
        assertEquals(
            CycleEstimate(
                periods = listOf(
                    EstimatedPeriod(day("2027-12-02"), day("2027-11-25"), day("2027-12-09"), 5),
                    EstimatedPeriod(day("2027-12-31"), day("2027-12-24"), day("2028-01-07"), 5),
                    EstimatedPeriod(day("2028-01-29"), day("2028-01-22"), day("2028-02-05"), 5)
                ),
                daysLate = 0,
                cycleBasis = EstimateBasis.Setup,
                periodBasis = EstimateBasis.Setup,
                settlingAfter = ContraceptionMethod.IMPLANT
            ),
            overview.estimate
        )
        assertNull(overview.currentCycle)
    }

    @Test
    fun `her own cycles from before the method set her usual cycle, with the wider range`() {
        val overview = CycleCalculator.overview(before, setUp(), day("2027-11-15"), listOf(implant))

        assertEquals(
            EstimatedPeriod(day("2027-12-02"), day("2027-11-25"), day("2027-12-09"), 5),
            overview.estimate?.next
        )
        assertEquals(EstimateBasis.Logged(6), overview.estimate?.cycleBasis)
    }

    @Test
    fun `once she logs a period, cycles count from it, with the wider range until three complete cycles`() {
        val after = listOf("2027-11-20", "2027-12-19", "2028-01-17", "2028-02-15").map { bleed(day(it)) }

        val firstPeriod = CycleCalculator.overview(before + after[0], setUp(), day("2027-11-25"), listOf(implant))
        val twoCycles = CycleCalculator.overview(
            before + after.take(3).flatten(),
            setUp(),
            day("2028-01-20"),
            listOf(implant)
        )
        val threeCycles = CycleCalculator.overview(
            before + after.flatten(),
            setUp(),
            day("2028-02-20"),
            listOf(implant)
        )

        assertNull(firstPeriod.contraception.stopped)
        assertEquals(6, firstPeriod.cycleDay)
        assertEquals(
            EstimatedPeriod(day("2027-12-19"), day("2027-12-12"), day("2027-12-26"), 5),
            firstPeriod.estimate?.next
        )
        assertEquals(
            EstimatedPeriod(day("2028-02-15"), day("2028-02-08"), day("2028-02-22"), 5),
            twoCycles.estimate?.next
        )
        assertEquals(ContraceptionMethod.IMPLANT, twoCycles.estimate?.settlingAfter)
        assertEquals(
            EstimatedPeriod(day("2028-03-15"), day("2028-03-15"), day("2028-03-15"), 5),
            threeCycles.estimate?.next
        )
        assertNull(threeCycles.estimate?.settlingAfter)
    }

    @Test
    fun `missed a period is not asked before her first period after stopping`() {
        val overview = CycleCalculator.overview(before, setUp(), day("2028-02-01"), listOf(implant))

        assertEquals(emptyList<Any>(), overview.prompts)
        assertEquals(61, overview.estimate?.daysLate)
    }

    @Test
    fun `the withdrawal bleed after the pill does not end the days since, nor anchor the estimate`() {
        val pill = stretch(ContraceptionMethod.COMBINED_PILL, "2027-05-03", "2027-11-03")
        val logs = bleed(day("2027-11-06"), length = 4)

        val during = CycleCalculator.overview(logs, setUp(29), day("2027-11-08"), listOf(pill))
        val after = CycleCalculator.overview(logs, setUp(29), day("2027-11-11"), listOf(pill))

        assertEquals(StoppedMethod(pill, days = 5, withdrawalWindow = true), during.contraception.stopped)
        assertEquals(pill, during.currentPeriod?.stretch)
        assertEquals(BleedingWord.BLEED, during.bleedingWord(day("2027-11-08")))
        assertEquals(day("2027-12-02"), during.estimate?.next?.expectedStart)
        assertNull(during.cycleDay)
        assertEquals(StoppedMethod(pill, days = 8, withdrawalWindow = false), after.contraception.stopped)
    }

    @Test
    fun `after the injection there is no estimate until she logs a period`() {
        val injection = stretch(ContraceptionMethod.INJECTION, "2026-11-09", "2027-11-02")

        val waiting = CycleCalculator.overview(emptyList(), setUp(29), day("2027-11-14"), listOf(injection))
        val period = CycleCalculator.overview(bleed(day("2027-11-20")), setUp(29), day("2027-11-25"), listOf(injection))

        assertEquals(EstimateKind.NONE, waiting.contraception.estimates)
        assertEquals(12, waiting.contraception.stopped?.days)
        assertNull(waiting.estimate)
        assertNull(waiting.contraception.bleeding)
        assertEquals(EstimateKind.PERIOD, period.contraception.estimates)
        assertEquals(EstimatedPeriod(day("2027-12-19"), day("2027-12-12"), day("2027-12-26"), 5), period.estimate?.next)
        assertEquals(ContraceptionMethod.INJECTION, period.estimate?.settlingAfter)
    }

    @Test
    fun `while the injection's 13 weeks run, she is still on it`() {
        val injection = stretch(ContraceptionMethod.INJECTION, "2026-11-09", "2027-11-02")

        val overview = CycleCalculator.overview(emptyList(), setUp(), day("2027-08-20"), listOf(injection))

        assertEquals(injection, overview.contraception.current)
        assertNull(overview.contraception.stopped)
        assertEquals(EstimateKind.NONE, overview.contraception.estimates)
    }

    @Test
    fun `stopping a copper IUD changes nothing about her estimates`() {
        val copper = stretch(ContraceptionMethod.COPPER_IUD, "2027-01-15", "2027-06-01")
        val logs = cycles(29, 29, 29, lastStart = day("2027-05-20"))

        val overview = CycleCalculator.overview(logs, setUp(), day("2027-06-10"), listOf(copper))

        assertNull(overview.contraception.stopped)
        assertNull(overview.estimate?.settlingAfter)
        assertEquals(CycleCalculator.overview(logs, setUp(), day("2027-06-10")).estimate, overview.estimate)
    }
}
