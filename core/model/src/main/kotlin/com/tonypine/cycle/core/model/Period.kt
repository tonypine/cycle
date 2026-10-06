package com.tonypine.cycle.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * A period, derived from her day logs and never stored.
 *
 * @property start the first day: cycle day 1.
 * @property end the last day. For an open period, today.
 * @property isOpen she marked it started and not yet ended, so it runs up to today.
 * @property stretch the hormonal method this bleeding belongs to: it started inside the stretch, or
 *   in the 7 days after a combined method stopped. Null for a period of her own cycle, the only kind
 *   that counts toward her typical lengths.
 */
data class Period(
    val start: LocalDate,
    val end: LocalDate,
    val isOpen: Boolean = false,
    val stretch: ContraceptionStretch? = null
) {
    /** Days from [start] to [end], both included. */
    val length: Int
        get() = daysBetween(start, end) + 1
}

/**
 * A cycle of her own: from one period's first day to the day before the next period starts. Derived,
 * never stored. There is no cycle on a hormonal method.
 *
 * @property end the last day, or null for the current cycle, which is open until the next period.
 * @property cutShort it ended the day before she started a hormonal method: shown, and never counted.
 */
data class Cycle(val start: LocalDate, val end: LocalDate?, val cutShort: Boolean = false) {
    val isComplete: Boolean
        get() = end != null

    /** Days from [start] to [end], both included, or null while the cycle is open. */
    val length: Int?
        get() = end?.let { daysBetween(start, it) + 1 }

    /** The cycle day of [date]: 1 on [start]. */
    fun dayOf(date: LocalDate): Int = daysBetween(start, date) + 1
}

internal fun daysBetween(from: LocalDate, to: LocalDate): Int = ChronoUnit.DAYS.between(from, to).toInt()
