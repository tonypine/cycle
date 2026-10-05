package com.tonypine.cycle.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsualLengthsTest {
    @Test
    fun `the defaults are valid`() {
        assertEquals(LengthCheck.Valid(28), UsualLengths.checkCycle("28"))
        assertEquals(LengthCheck.Valid(5), UsualLengths.checkPeriod("5"))
    }

    @Test
    fun `unusual lengths are accepted`() {
        assertEquals(LengthCheck.Valid(19), UsualLengths.checkCycle("19"))
        assertEquals(LengthCheck.Valid(50), UsualLengths.checkCycle("50"))
        assertEquals(LengthCheck.Valid(10), UsualLengths.checkPeriod("10"))
    }

    @Test
    fun `the bounds themselves are accepted`() {
        assertEquals(LengthCheck.Valid(15), UsualLengths.checkCycle("15"))
        assertEquals(LengthCheck.Valid(90), UsualLengths.checkCycle("90"))
        assertEquals(LengthCheck.Valid(1), UsualLengths.checkPeriod("1"))
        assertEquals(LengthCheck.Valid(14), UsualLengths.checkPeriod("14"))
    }

    @Test
    fun `impossible lengths are refused with the range to use`() {
        val cycles = LengthCheck.OutOfRange(15..90)
        assertEquals(cycles, UsualLengths.checkCycle("14"))
        assertEquals(cycles, UsualLengths.checkCycle("91"))
        assertEquals(cycles, UsualLengths.checkCycle("0"))

        val periods = LengthCheck.OutOfRange(1..14)
        assertEquals(periods, UsualLengths.checkPeriod("0"))
        assertEquals(periods, UsualLengths.checkPeriod("15"))
    }

    @Test
    fun `text that is not a whole number is out of range`() {
        assertEquals(LengthCheck.OutOfRange(15..90), UsualLengths.checkCycle("2.5"))
        assertEquals(LengthCheck.OutOfRange(1..14), UsualLengths.checkPeriod("-3"))
        assertEquals(LengthCheck.OutOfRange(15..90), UsualLengths.checkCycle("99999999999"))
    }

    @Test
    fun `an empty or blank field is empty`() {
        assertEquals(LengthCheck.Empty, UsualLengths.checkCycle(""))
        assertEquals(LengthCheck.Empty, UsualLengths.checkPeriod("  "))
    }

    @Test
    fun `the longest period is shorter than the shortest cycle`() {
        assertTrue(CycleRules.USUAL_PERIOD_LENGTHS.last < CycleRules.USUAL_CYCLE_LENGTHS.first)
    }
}
