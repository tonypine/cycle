package com.tonypine.cycle.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayLogTest {
    private val date = LocalDate.of(2027, 3, 2)

    @Test
    fun `light or heavier flow and either marker make a period day, spotting does not`() {
        assertEquals(
            listOf(false, false, true, true, true),
            FlowLevel.entries.map { DayLog(date, it).isPeriodDay }
        )
        assertTrue(DayLog(date, periodStarted = true).isPeriodDay)
        assertTrue(DayLog(date, periodEnded = true).isPeriodDay)
    }

    @Test
    fun `a day with nothing logged is empty, a day logged as no flow is not`() {
        assertTrue(DayLog(date).isEmpty)
        assertFalse(DayLog(date, FlowLevel.NONE).isEmpty)
        assertFalse(DayLog(date, periodEnded = true).isEmpty)
    }

    @Test
    fun `lengths count both the first and the last day`() {
        assertEquals(5, Period(date, date.plusDays(4)).length)
        assertEquals(28, Cycle(date, date.plusDays(27)).length)
        assertEquals(null, Cycle(date, null).length)
        assertEquals(1, Cycle(date, null).dayOf(date))
        assertEquals(31, Cycle(date, null).dayOf(LocalDate.of(2027, 4, 1)))
    }
}
