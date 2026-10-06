package com.tonypine.cycle.feature.history

import com.tonypine.cycle.core.model.CycleOverview
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.PeriodRefusal
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Which end of the period a tap on the calendar sets. */
enum class PeriodDay { First, Last }

/**
 * The days she has picked so far: from [start] to [end], or still going when [end] is null. Taps on
 * the calendar set the [picking] end; the calendar shows [month]. [refusal] says why the last save
 * did not go through, until she changes something.
 */
data class PeriodDraft(
    val start: LocalDate,
    val end: LocalDate?,
    val picking: PeriodDay,
    val month: YearMonth,
    val refusal: PeriodRefusal? = null
) {
    /**
     * A tap on [date]. The first day can be any day up to [today], and the last day follows it when
     * it would come before; the calendar then moves on to the last day. The last day can be any day
     * from the first up to [today]. Other taps change nothing.
     */
    fun pick(date: LocalDate, today: LocalDate): PeriodDraft = when {
        date > today -> this

        picking == PeriodDay.First -> copy(
            start = date,
            end = end?.let { maxOf(it, date) },
            picking = if (end == null) PeriodDay.First else PeriodDay.Last,
            refusal = null
        )

        date < start -> this

        else -> copy(end = date, refusal = null)
    }

    /** Taps now set [day], and the calendar turns to its month. */
    fun choose(day: PeriodDay): PeriodDraft {
        val date = if (day == PeriodDay.Last) end ?: start else start
        return copy(picking = day, month = YearMonth.from(date))
    }

    /**
     * "Still going" on: no last day. Off: the last day is [lastDay] (or the first, if that is later),
     * and taps set it.
     */
    fun stillGoing(on: Boolean, lastDay: LocalDate): PeriodDraft = if (on) {
        copy(end = null, picking = PeriodDay.First, month = YearMonth.from(start), refusal = null)
    } else {
        val end = maxOf(start, lastDay)
        copy(end = end, picking = PeriodDay.Last, month = YearMonth.from(end), refusal = null)
    }

    companion object {
        /** The draft of [period] as it is stored, picking its first day. */
        fun of(period: Period): PeriodDraft = PeriodDraft(
            start = period.start,
            end = period.end.takeUnless { period.isOpen },
            picking = PeriodDay.First,
            month = YearMonth.from(period.start)
        )
    }
}

/** What the period editor shows. Built by [EditPeriodUiState.from]. */
sealed interface EditPeriodUiState {
    /** Her log has not been read yet. */
    data object Loading : EditPeriodUiState

    /** No cycle starts on that day any more: an edit elsewhere moved or removed its period. */
    data object Missing : EditPeriodUiState

    /**
     * Her [period] as stored and the [draft] of its new days. [canStillGo] on the current cycle's
     * period, the only one that can run up to [today].
     */
    data class Editing(val today: LocalDate, val period: Period, val canStillGo: Boolean, val draft: PeriodDraft) :
        EditPeriodUiState {
        /** The day the period runs to: its last day, or [today] while still going. */
        val lastDay: LocalDate
            get() = draft.end ?: today

        /** The days from the first to the [lastDay], both included. */
        val length: Int
            get() = ChronoUnit.DAYS.between(draft.start, lastDay).toInt() + 1

        /** The draft differs from what is stored, so there is something to save. */
        val changed: Boolean
            get() = draft.start != period.start || draft.end != period.end.takeUnless { period.isOpen }

        /** [date] is one of the picked days. */
        fun isPeriodDay(date: LocalDate): Boolean = date >= draft.start && date <= lastDay

        /** A tap on [date] can set the end she is picking. */
        fun canPick(date: LocalDate): Boolean =
            date <= today && (draft.picking == PeriodDay.First || date >= draft.start)
    }

    companion object {
        /**
         * The editor for the period of the cycle that starts on [start], from her [overview], with
         * her [draft] so far, or the period as stored when she has not picked anything yet.
         */
        fun from(overview: CycleOverview, start: LocalDate, draft: PeriodDraft?): EditPeriodUiState {
            val cycle = cycleSummaries(overview).firstOrNull { it.start == start } ?: return Missing
            return Editing(
                today = overview.today,
                period = cycle.period,
                canStillGo = cycle.isCurrent,
                draft = draft ?: PeriodDraft.of(cycle.period)
            )
        }
    }
}
