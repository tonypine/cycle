package com.tonypine.cycle.core.domain

/** A usual length as she typed it, checked against the lengths she can give. */
sealed interface LengthCheck {
    /** A whole number of days she can give. */
    data class Valid(val days: Int) : LengthCheck

    /** Nothing typed yet. */
    data object Empty : LengthCheck

    /** A number outside [range], or not a number at all. */
    data class OutOfRange(val range: IntRange) : LengthCheck
}

/** Checks the usual cycle and period lengths she types at setup, in days. */
object UsualLengths {
    fun checkCycle(text: String): LengthCheck = check(text, CycleRules.USUAL_CYCLE_LENGTHS)

    fun checkPeriod(text: String): LengthCheck = check(text, CycleRules.USUAL_PERIOD_LENGTHS)

    private fun check(text: String, range: IntRange): LengthCheck {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return LengthCheck.Empty
        val days = trimmed.toIntOrNull()?.takeIf { it in range } ?: return LengthCheck.OutOfRange(range)
        return LengthCheck.Valid(days)
    }
}
