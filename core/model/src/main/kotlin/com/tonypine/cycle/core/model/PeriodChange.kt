package com.tonypine.cycle.core.model

/** What saving a period's new dates did. */
sealed interface PeriodChange {
    data object Saved : PeriodChange

    /** Nothing was saved, for [reason]. */
    data class Refused(val reason: PeriodRefusal) : PeriodChange
}

/** Why a period's new dates cannot be saved. */
sealed interface PeriodRefusal {
    /** No period starts on that day any more: an edit elsewhere moved or removed it. */
    data object Gone : PeriodRefusal

    /** A first or last day after today. */
    data object AfterToday : PeriodRefusal

    /** A last day before the first. */
    data object EndBeforeStart : PeriodRefusal

    /**
     * The new days reach [period], or come within a day of it, so the two would join: "That runs
     * into your period from 3 to 7 March."
     */
    data class TooClose(val period: Period) : PeriodRefusal
}
