package com.tonypine.cycle.core.model

import java.time.LocalDate

/**
 * What she logged for one calendar day. The day is a [LocalDate], the day she means, never an
 * instant, so a log does not move to another day when the phone changes time zone.
 *
 * @property flow how much she bled, or null when she logged nothing about it.
 * @property periodStarted she marked this day as the first of a period (the one-tap "started").
 * @property periodEnded she marked this day as the last of a period (the one-tap "ended").
 */
data class DayLog(
    val date: LocalDate,
    val flow: FlowLevel? = null,
    val periodStarted: Boolean = false,
    val periodEnded: Boolean = false
) {
    /** Nothing logged: storage keeps no row for such a day. */
    val isEmpty: Boolean
        get() = flow == null && !periodStarted && !periodEnded

    /** The day belongs to a period: light or heavier flow, or either period marker. */
    val isPeriodDay: Boolean
        get() = flow?.isPeriodFlow == true || periodStarted || periodEnded
}
