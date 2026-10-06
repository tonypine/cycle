package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.StretchMove
import com.tonypine.cycle.core.model.StretchRefusal
import org.junit.Assert.assertEquals
import org.junit.Test

/** Starting, stopping and editing stretches without overlaps (`0006`, Stored: dated stretches). */
class ContraceptionEditsTest {
    private val pill = stretch(ContraceptionMethod.COMBINED_PILL, "2027-05-03", id = 1)
    private val implant = stretch(ContraceptionMethod.IMPLANT, "2026-11-09", "2027-11-03", id = 1)
    private val ring = stretch(ContraceptionMethod.RING, "2027-11-20", "2028-01-10", id = 2)

    private fun start(
        stretches: List<ContraceptionStretch>,
        started: String?,
        today: String,
        method: ContraceptionMethod = ContraceptionMethod.HORMONAL_IUD,
        breaks: Breaks? = null
    ) = ContraceptionEdits.start(stretches, method, breaks, started?.let(::day), day(today))

    private fun refused(reason: StretchRefusal) = StretchPlan.Refused(reason)

    @Test
    fun `starting a method on none adds it, with no stop date`() {
        assertEquals(
            StretchPlan.Ready(listOf(stretch(ContraceptionMethod.HORMONAL_IUD, "2027-09-13"))),
            start(emptyList(), "2027-09-13", today = "2027-09-17")
        )
    }

    @Test
    fun `changing method ends the current one the day before, without asking`() {
        assertEquals(
            StretchPlan.Ready(
                listOf(pill.copy(stopped = day("2027-09-12")), stretch(ContraceptionMethod.HORMONAL_IUD, "2027-09-13"))
            ),
            start(listOf(pill), "2027-09-13", today = "2027-09-17")
        )
    }

    @Test
    fun `a new start on or before the current method's start is refused`() {
        assertEquals(
            refused(StretchRefusal.NotAfterCurrentStart(pill)),
            start(listOf(pill), "2027-05-03", "2027-09-17")
        )
        assertEquals(
            refused(StretchRefusal.NotAfterCurrentStart(pill)),
            start(listOf(pill), "2027-04-01", "2027-09-17")
        )
        assertEquals(refused(StretchRefusal.NotAfterCurrentStart(pill)), start(listOf(pill), null, "2027-09-17"))
    }

    @Test
    fun `a start after today is refused`() {
        assertEquals(refused(StretchRefusal.StartAfterToday), start(emptyList(), "2027-09-18", "2027-09-17"))
    }

    @Test
    fun `a current method with an unknown start ends the day before any new start`() {
        val unknown = stretch(ContraceptionMethod.IMPLANT, started = null, id = 1)

        assertEquals(
            StretchPlan.Ready(
                listOf(
                    unknown.copy(stopped = day("2027-02-28")),
                    stretch(ContraceptionMethod.HORMONAL_IUD, "2027-03-01")
                )
            ),
            start(listOf(unknown), "2027-03-01", today = "2027-03-10")
        )
    }

    @Test
    fun `a new method during the injection's 13 weeks ends it the day before`() {
        val injection = stretch(ContraceptionMethod.INJECTION, "2026-11-09", "2027-11-02", id = 1)

        assertEquals(
            StretchPlan.Ready(
                listOf(injection.copy(stopped = day("2027-08-30")), stretch(ContraceptionMethod.IMPLANT, "2027-08-31"))
            ),
            start(listOf(injection), "2027-08-31", today = "2027-09-01", method = ContraceptionMethod.IMPLANT)
        )
    }

    @Test
    fun `on none, a start inside a stopped stretch asks to move its end`() {
        val pill = stretch(ContraceptionMethod.COMBINED_PILL, "2027-10-20")
        val moved = implant.copy(stopped = day("2027-10-19"))

        assertEquals(
            StretchPlan.Ready(listOf(moved, pill), moves = listOf(StretchMove(implant, moved))),
            start(listOf(implant), "2027-10-20", "2027-11-15", ContraceptionMethod.COMBINED_PILL, Breaks.MONTHLY)
        )
    }

    @Test
    fun `on none, a start on or before a stopped stretch's start is refused`() {
        assertEquals(refused(StretchRefusal.CoversWhole(implant)), start(listOf(implant), "2026-11-09", "2027-11-15"))
        assertEquals(refused(StretchRefusal.CoversWhole(implant)), start(listOf(implant), null, "2027-11-15"))
    }

    @Test
    fun `a start after every stopped stretch needs no question`() {
        assertEquals(
            StretchPlan.Ready(listOf(stretch(ContraceptionMethod.HORMONAL_IUD, "2027-11-04"))),
            start(listOf(implant), "2027-11-04", "2027-11-15")
        )
    }

    @Test
    fun `a start that reaches more than one stretch is refused for the latest one it covers`() {
        val both = listOf(implant, ring)

        assertEquals(refused(StretchRefusal.CoversWhole(ring)), start(both, "2027-11-01", "2028-02-15"))
        assertEquals(refused(StretchRefusal.CoversWhole(ring)), start(both, "2027-11-20", "2028-02-15"))
        val moved = ring.copy(stopped = day("2027-11-20"))
        assertEquals(
            StretchPlan.Ready(
                listOf(moved, stretch(ContraceptionMethod.HORMONAL_IUD, "2027-11-21")),
                listOf(StretchMove(ring, moved))
            ),
            start(both, "2027-11-21", "2028-02-15")
        )
        assertEquals(
            StretchPlan.Ready(listOf(stretch(ContraceptionMethod.HORMONAL_IUD, "2028-01-11"))),
            start(both, "2028-01-11", "2028-02-15")
        )
    }

    @Test
    fun `on none, a start inside a stretch with an unknown start asks to move its end`() {
        val unknown = stretch(ContraceptionMethod.IMPLANT, started = null, stopped = "2027-01-20", id = 1)
        val moved = unknown.copy(stopped = day("2027-01-09"))

        assertEquals(
            StretchPlan.Ready(
                listOf(moved, stretch(ContraceptionMethod.HORMONAL_IUD, "2027-01-10")),
                listOf(StretchMove(unknown, moved))
            ),
            start(listOf(unknown), "2027-01-10", "2027-03-01")
        )
    }

    @Test
    fun `stopping ends the stretch on her last day, and the injection 13 weeks after her last one`() {
        val injection = stretch(ContraceptionMethod.INJECTION, "2026-11-09", id = 1)

        assertEquals(
            StretchPlan.Ready(listOf(implant.copy(stopped = day("2027-11-03")))),
            ContraceptionEdits.stop(listOf(implant.copy(stopped = null)), 1, day("2027-11-03"), day("2027-11-15"))
        )
        assertEquals(
            StretchPlan.Ready(listOf(injection.copy(stopped = day("2027-11-02")))),
            ContraceptionEdits.stop(listOf(injection), 1, day("2027-08-03"), day("2027-08-20"))
        )
    }

    @Test
    fun `stopping before the start or after today is refused`() {
        val current = listOf(pill)

        assertEquals(
            refused(StretchRefusal.StopBeforeStart),
            ContraceptionEdits.stop(current, 1, day("2027-05-02"), day("2027-09-17"))
        )
        assertEquals(
            refused(StretchRefusal.StopTooLate(day("2027-09-17"))),
            ContraceptionEdits.stop(current, 1, day("2027-09-18"), day("2027-09-17"))
        )
        assertEquals(
            refused(StretchRefusal.Gone),
            ContraceptionEdits.stop(current, 2, day("2027-09-01"), day("2027-09-17"))
        )
    }

    @Test
    fun `correcting a start into the stretch before asks to move its end`() {
        val pill = pill.copy(stopped = day("2027-09-12"))
        val iud = stretch(ContraceptionMethod.HORMONAL_IUD, "2027-09-13", id = 2)
        val moved = pill.copy(stopped = day("2027-09-05"))
        val corrected = iud.copy(started = day("2027-09-06"))

        assertEquals(
            StretchPlan.Ready(listOf(moved, corrected), listOf(StretchMove(pill, moved))),
            ContraceptionEdits.edit(listOf(pill, iud), corrected, day("2027-09-17"))
        )
    }

    @Test
    fun `a correction that would swallow a neighbour whole is refused`() {
        val pill = pill.copy(stopped = day("2027-09-12"))
        val iud = stretch(ContraceptionMethod.HORMONAL_IUD, "2027-09-13", id = 2)

        assertEquals(
            refused(StretchRefusal.CoversWhole(pill)),
            ContraceptionEdits.edit(listOf(pill, iud), iud.copy(started = day("2027-05-03")), day("2027-09-17"))
        )
    }

    @Test
    fun `moving a stop into the next stretch asks to move its start`() {
        val longer = implant.copy(stopped = day("2027-11-25"))
        val moved = ring.copy(started = day("2027-11-26"))

        assertEquals(
            StretchPlan.Ready(listOf(moved, longer), listOf(StretchMove(ring, moved))),
            ContraceptionEdits.edit(listOf(implant, ring), longer, day("2028-02-15"))
        )
        assertEquals(
            refused(StretchRefusal.CoversWhole(ring)),
            ContraceptionEdits.edit(listOf(implant, ring), implant.copy(stopped = day("2028-01-10")), day("2028-02-15"))
        )
    }

    @Test
    fun `an edit is refused with a stop before its start, a start after today, or a stop too late`() {
        val today = day("2027-08-20")
        val injection = stretch(ContraceptionMethod.INJECTION, "2027-02-01", "2027-11-02", id = 3)
        val all = listOf(implant.copy(stopped = day("2027-01-31")), injection)

        assertEquals(
            refused(StretchRefusal.StopBeforeStart),
            ContraceptionEdits.edit(all, implant.copy(stopped = day("2026-11-08")), today)
        )
        assertEquals(
            refused(StretchRefusal.StartAfterToday),
            ContraceptionEdits.edit(all, injection.copy(started = day("2027-08-21")), today)
        )
        assertEquals(
            refused(StretchRefusal.StopTooLate(today)),
            ContraceptionEdits.edit(all, implant.copy(stopped = day("2027-08-21")), today)
        )
        assertEquals(
            StretchPlan.Ready(listOf(injection.copy(stopped = day("2027-11-19")))),
            ContraceptionEdits.edit(all, injection.copy(stopped = day("2027-11-19")), today)
        )
        assertEquals(
            refused(StretchRefusal.StopTooLate(day("2027-11-19"))),
            ContraceptionEdits.edit(all, injection.copy(stopped = day("2027-11-20")), today)
        )
        assertEquals(refused(StretchRefusal.Gone), ContraceptionEdits.edit(all, implant.copy(id = 9), today))
    }

    @Test
    fun `the current method is the one that covers today`() {
        val injection = stretch(ContraceptionMethod.INJECTION, "2026-11-09", "2027-11-02", id = 3)

        assertEquals(injection, ContraceptionEdits.current(listOf(injection), day("2027-11-02")))
        assertEquals(null, ContraceptionEdits.current(listOf(injection), day("2027-11-03")))
        assertEquals(pill, ContraceptionEdits.current(listOf(pill), day("2027-09-17")))
        assertEquals(day("2027-11-02"), ContraceptionEdits.stopDate(ContraceptionMethod.INJECTION, day("2027-08-03")))
        assertEquals(day("2027-08-03"), ContraceptionEdits.stopDate(ContraceptionMethod.IMPLANT, day("2027-08-03")))
    }

    @Test
    fun `a method she marked as stopped today is no longer current, and a start today asks to move its end`() {
        val iud = stretch(ContraceptionMethod.HORMONAL_IUD, "2027-09-13", "2027-09-17", id = 2)
        val moved = iud.copy(stopped = day("2027-09-16"))

        assertEquals(null, ContraceptionEdits.current(listOf(iud), day("2027-09-17")))
        assertEquals(
            StretchPlan.Ready(
                listOf(moved, stretch(ContraceptionMethod.IMPLANT, "2027-09-17")),
                listOf(StretchMove(iud, moved))
            ),
            start(listOf(iud), "2027-09-17", "2027-09-17", method = ContraceptionMethod.IMPLANT)
        )
    }
}
