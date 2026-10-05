package com.tonypine.cycle.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayFeelingsTest {
    private val date = LocalDate.of(2027, 3, 2)

    private val everything = DayFeelings(
        date = date,
        pain = Pain(PainLevel.MODERATE, setOf(PainKind.CRAMPS)),
        body = setOf(BodySymptom.BLOATING),
        moods = setOf(Mood.IRRITABLE),
        energy = EnergyLevel.LOW,
        sleep = SleepQuality.BADLY,
        sex = SexualActivity.PROTECTED,
        note = "Synthetic note"
    )

    @Test
    fun `a day with nothing logged is empty, a day logged as no pain is not`() {
        assertTrue(DayFeelings(date).isEmpty)
        assertFalse(DayFeelings(date, pain = Pain(PainLevel.NONE)).isEmpty)
        assertFalse(DayFeelings(date, note = "Synthetic").isEmpty)
    }

    @Test
    fun `without drops only the hidden categories`() {
        assertEquals(everything.copy(sex = null), everything.without(setOf(LogCategory.SEX)))
        assertEquals(DayFeelings(date), everything.without(LogCategory.entries.toSet()))
        assertEquals(everything, everything.without(emptySet()))
    }

    @Test
    fun `no pain keeps no place where it hurt`() {
        val noPain = DayFeelings(date, pain = Pain(PainLevel.NONE, setOf(PainKind.CRAMPS)))

        assertEquals(DayFeelings(date, pain = Pain(PainLevel.NONE)), noPain.normalized())
        assertEquals(everything, everything.normalized())
    }

    @Test
    fun `the note is trimmed and cut to 500 characters`() {
        assertEquals("Synthetic", DayFeelings(date, note = "  Synthetic \n").normalized().note)
        assertEquals(500, DayFeelings(date, note = "x".repeat(600)).normalized().note.length)
        assertTrue(DayFeelings(date, note = "   ").normalized().isEmpty)
    }
}
