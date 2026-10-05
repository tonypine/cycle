package com.tonypine.cycle.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsualLengthsTest {
    @Test
    fun `the defaults are lengths she can give`() {
        assertEquals(28, UsualLengths.cycle(CycleRules.DEFAULT_CYCLE_LENGTH))
        assertEquals(5, UsualLengths.period(CycleRules.DEFAULT_PERIOD_LENGTH))
    }

    @Test
    fun `unusual lengths stay as they are`() {
        assertEquals(19, UsualLengths.cycle(19))
        assertEquals(50, UsualLengths.cycle(50))
        assertEquals(10, UsualLengths.period(10))
    }

    @Test
    fun `the ends of each range are lengths she can give`() {
        assertEquals(15, UsualLengths.cycle(15))
        assertEquals(90, UsualLengths.cycle(90))
        assertEquals(1, UsualLengths.period(1))
        assertEquals(14, UsualLengths.period(14))
    }

    @Test
    fun `a length outside the range becomes the nearest end`() {
        assertEquals(15, UsualLengths.cycle(14))
        assertEquals(15, UsualLengths.cycle(0))
        assertEquals(90, UsualLengths.cycle(91))
        assertEquals(1, UsualLengths.period(0))
        assertEquals(1, UsualLengths.period(-3))
        assertEquals(14, UsualLengths.period(15))
    }

    @Test
    fun `the longest period is shorter than the shortest cycle`() {
        assertTrue(CycleRules.USUAL_PERIOD_LENGTHS.last < CycleRules.USUAL_CYCLE_LENGTHS.first)
    }
}
