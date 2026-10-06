package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.domain.CycleRules.INJECTION_WEEKS
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.StretchMove
import com.tonypine.cycle.core.model.StretchRefusal
import java.time.LocalDate

/** What saving a stretch would write, or why it cannot be saved. */
sealed interface StretchPlan {
    /**
     * [writes] are the stretches to store, new ones with id 0. [moves] are the changes among them to
     * stretches she has to confirm first.
     */
    data class Ready(val writes: List<ContraceptionStretch>, val moves: List<StretchMove> = emptyList()) : StretchPlan

    data class Refused(val reason: StretchRefusal) : StretchPlan
}

/**
 * Starting, stopping and editing a stretch of contraception so that no two overlap, by
 * `docs/decisions/0006-contraception.md` ("Stored: dated stretches"). Pure: the repository reads the
 * stretches and writes the plan in one transaction. In every comparison an unknown start is earlier
 * than any date and no stop later than any.
 */
object ContraceptionEdits {
    /**
     * Her method now, on [today]: the stretch that covers it, unless she marked it as stopped today.
     * Today is still that stretch's last day, but she is on none from the moment she says so. The
     * injection stays her method until the date Cycle counts it until has passed. At most one is.
     */
    fun current(stretches: List<ContraceptionStretch>, today: LocalDate): ContraceptionStretch? =
        stretches.lastOrNull { today in it && !(it.stopped == today && it.method != ContraceptionMethod.INJECTION) }

    /**
     * The stop date of [method] when the last day she gives is [lastDay]: that day, or for the
     * injection [INJECTION_WEEKS] weeks after her last one.
     */
    fun stopDate(method: ContraceptionMethod, lastDay: LocalDate): LocalDate =
        if (method == ContraceptionMethod.INJECTION) lastDay.plusWeeks(INJECTION_WEEKS) else lastDay

    /** The latest stop date [method] can have on [today]. */
    fun latestStop(method: ContraceptionMethod, today: LocalDate): LocalDate = stopDate(method, today)

    /**
     * Starts [method] on [started] (null: she does not remember), with no stop date. On a current
     * method, that one ends the day before, with no question asked, and a start on or before its
     * start is refused. On none, a start inside a stopped stretch moves its end once she confirms,
     * and a start that would cover any stretch whole is refused, naming the latest one.
     */
    fun start(
        stretches: List<ContraceptionStretch>,
        method: ContraceptionMethod,
        breaks: Breaks?,
        started: LocalDate?,
        today: LocalDate
    ): StretchPlan {
        if (started != null && started > today) return StretchPlan.Refused(StretchRefusal.StartAfterToday)
        val new = ContraceptionStretch(method, started, stopped = null, breaks = breaks)
        val current = current(stretches, today) ?: return place(stretches, new)
        return when (val plan = place(stretches, new)) {
            is StretchPlan.Refused -> StretchPlan.Refused(StretchRefusal.NotAfterCurrentStart(current))
            is StretchPlan.Ready -> plan.copy(moves = plan.moves.filterNot { it.from == current })
        }
    }

    /**
     * "Mark as stopped": stretch [id] stops on the last day she gives, [lastDay] (her last
     * injection: the stretch then runs [INJECTION_WEEKS] weeks more). [lastDay] must lie between
     * its start and [today].
     */
    fun stop(stretches: List<ContraceptionStretch>, id: Long, lastDay: LocalDate, today: LocalDate): StretchPlan {
        val stretch = stretches.firstOrNull { it.id == id } ?: return StretchPlan.Refused(StretchRefusal.Gone)
        if (lastDay > today) return StretchPlan.Refused(StretchRefusal.StopTooLate(today))
        if (lastDay < stretch.startKey) return StretchPlan.Refused(StretchRefusal.StopBeforeStart)
        return place(stretches, stretch.copy(stopped = stopDate(stretch.method, lastDay)))
    }

    /**
     * Saves [edited] over the stretch with its id. A stop before the start, a start after today or a
     * stop after [latestStop] is refused. A change that would cover another stretch whole is refused,
     * naming the latest one; one that overlaps a neighbour moves the neighbour's edge, once she
     * confirms.
     */
    fun edit(stretches: List<ContraceptionStretch>, edited: ContraceptionStretch, today: LocalDate): StretchPlan {
        if (stretches.none { it.id == edited.id }) return StretchPlan.Refused(StretchRefusal.Gone)
        val started = edited.started
        val stopped = edited.stopped
        return when {
            started != null && started > today -> StretchPlan.Refused(StretchRefusal.StartAfterToday)

            started != null && stopped != null && stopped < started ->
                StretchPlan.Refused(StretchRefusal.StopBeforeStart)

            stopped != null && stopped > latestStop(edited.method, today) ->
                StretchPlan.Refused(StretchRefusal.StopTooLate(latestStop(edited.method, today)))

            else -> place(stretches, edited)
        }
    }

    /**
     * Fits [stretch] among the others: refused if it covers one whole (naming the latest), else each
     * one it overlaps gives way, the one before ending the day before it starts, the one after
     * starting the day after it stops.
     */
    private fun place(stretches: List<ContraceptionStretch>, stretch: ContraceptionStretch): StretchPlan {
        val others = stretches.filter { stretch.id == 0L || it.id != stretch.id }.filter { it.overlaps(stretch) }
        others.filter { stretch.coversWhole(it) }.maxByOrNull { it.startKey }?.let {
            return StretchPlan.Refused(StretchRefusal.CoversWhole(it))
        }
        val moves = others.map { other ->
            val moved = if (other.startKey < stretch.startKey) {
                other.copy(stopped = stretch.startKey.minusDays(1))
            } else {
                other.copy(started = stretch.stopKey.plusDays(1))
            }
            StretchMove(other, moved)
        }
        return StretchPlan.Ready(writes = moves.map { it.to } + stretch, moves = moves)
    }
}
