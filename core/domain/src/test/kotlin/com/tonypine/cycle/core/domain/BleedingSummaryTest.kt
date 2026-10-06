package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.BleedingSummary
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Her bleeding over the last 90 days on a method with no estimate (`0006`, The last 90 days). */
class BleedingSummaryTest {
    private val implant = stretch(ContraceptionMethod.IMPLANT, "2027-01-01")

    @Test
    fun `counts bleeding and spotting days and episodes over the last 90 days`() {
        val logs = bleed(day("2027-04-01"), length = 4) +
            bleed(day("2027-05-10"), length = 2, flow = FlowLevel.SPOTTING) +
            bleed(day("2027-06-01"), length = 6)

        val overview = CycleCalculator.overview(logs, setUp(), day("2027-06-30"), listOf(implant))

        // 1 April falls outside the 90 days, so the first episode counts 3 days.
        assertEquals(
            BleedingSummary(
                day("2027-04-02"),
                day("2027-06-30"),
                sinceStart = false,
                days = 11,
                episodes = 3,
                longest = 6
            ),
            overview.contraception.bleeding
        )
    }

    @Test
    fun `a stretch younger than 90 days counts since its start`() {
        val iud = stretch(ContraceptionMethod.HORMONAL_IUD, "2027-09-06")
        val logs = bleed(day("2027-09-06"), length = 4) + bleed(day("2027-09-12"), length = 3)

        val summary = CycleCalculator.overview(logs, setUp(), day("2027-09-17"), listOf(iud)).contraception.bleeding

        assertEquals(
            BleedingSummary(
                day("2027-09-06"),
                day("2027-09-17"),
                sinceStart = true,
                days = 7,
                episodes = 2,
                longest = 4
            ),
            summary
        )
    }

    @Test
    fun `with nothing logged, there is no bleeding`() {
        val summary = CycleCalculator.overview(emptyList(), setUp(), day("2027-06-30"), listOf(implant))
            .contraception.bleeding

        assertEquals(BleedingSummary(day("2027-04-02"), day("2027-06-30"), false, 0, 0, 0), summary)
    }

    @Test
    fun `a day she logged no flow on splits an episode, and an open period counts up to today`() {
        val logs = bleed(day("2027-06-01"), length = 3) + DayLog(day("2027-06-04"), FlowLevel.NONE) +
            bleed(day("2027-06-05"), length = 1) + DayLog(day("2027-06-27"), FlowLevel.MEDIUM, periodStarted = true)

        val summary = CycleCalculator.bleedingSummary(logs, implant, day("2027-06-30"))

        assertEquals(listOf(8, 3, 4), listOf(summary.days, summary.episodes, summary.longest))
    }

    @Test
    fun `spotting next to bleeding joins its episode`() {
        val logs =
            bleed(day("2027-06-01"), length = 2, flow = FlowLevel.SPOTTING) + bleed(day("2027-06-03"), length = 3)

        val summary = CycleCalculator.bleedingSummary(logs, implant, day("2027-06-30"))

        assertEquals(listOf(5, 1, 5), listOf(summary.days, summary.episodes, summary.longest))
    }

    @Test
    fun `a stopped stretch is summed over its own last 90 days`() {
        val stopped = stretch(ContraceptionMethod.IMPLANT, "2026-06-01", "2027-03-31")
        val logs = bleed(day("2027-03-01"), length = 4) + bleed(day("2027-04-10"), length = 4)

        val summary = CycleCalculator.bleedingSummary(logs, stopped, day("2027-06-30"))

        assertEquals(BleedingSummary(day("2027-01-01"), day("2027-03-31"), false, 4, 1, 4), summary)
    }

    @Test
    fun `only a method with no estimate gets the summary`() {
        val pill = stretch(ContraceptionMethod.COMBINED_PILL, "2027-01-01")
        val copper = stretch(ContraceptionMethod.COPPER_IUD, "2027-01-01")

        for (method in listOf(pill, copper)) {
            assertNull(
                CycleCalculator.overview(emptyList(), setUp(), day("2027-06-30"), listOf(method)).contraception.bleeding
            )
        }
    }
}
