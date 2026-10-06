package com.tonypine.cycle.core.data.repository

import androidx.room.withTransaction
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.database.toEntity
import com.tonypine.cycle.core.data.database.toModel
import com.tonypine.cycle.core.domain.ContraceptionEdits
import com.tonypine.cycle.core.domain.StretchPlan
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.StretchChange
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Her contraception: the stretches of time she was on each method (`0006-contraception.md`). Each
 * write reads the stretches, works out what changes by [ContraceptionEdits] and stores it in one
 * transaction, so no reader sees one stretch moved and the other not yet saved.
 *
 * A write that would overlap a stretch she has to agree to move returns [StretchChange.ConfirmMoves]
 * and stores nothing; the same call with `confirmed = true` stores it with the moves.
 */
class ContraceptionRepository(private val database: CycleDatabase) {
    private val dao = database.contraceptionDao()

    /** Every stretch, oldest first. */
    fun observeStretches(): Flow<List<ContraceptionStretch>> =
        dao.observeAll().map { stretches -> stretches.map { it.toModel() } }

    /**
     * Starts [method] on [started], or with an unknown start when null; [breaks] for a combined
     * method only. The current method, if any, ends the day before.
     */
    suspend fun start(
        method: ContraceptionMethod,
        breaks: Breaks?,
        started: LocalDate?,
        today: LocalDate,
        confirmed: Boolean = false
    ): StretchChange = write(confirmed) { ContraceptionEdits.start(it, method, breaks, started, today) }

    /** "Mark as stopped" on stretch [id], with the last day she gives: last pill, removal, last injection. */
    suspend fun stop(id: Long, lastDay: LocalDate, today: LocalDate, confirmed: Boolean = false): StretchChange =
        write(confirmed) { ContraceptionEdits.stop(it, id, lastDay, today) }

    /** Saves [edited]'s dates and breaks over the stretch with its id. */
    suspend fun edit(edited: ContraceptionStretch, today: LocalDate, confirmed: Boolean = false): StretchChange =
        write(confirmed) { ContraceptionEdits.edit(it, edited, today) }

    /** Forgets stretch [id]: the days she logged in it count as her own cycle again. */
    suspend fun delete(id: Long) = dao.delete(id)

    private suspend fun write(confirmed: Boolean, plan: (List<ContraceptionStretch>) -> StretchPlan): StretchChange =
        database.withTransaction {
            when (val planned = plan(dao.getAll().map { it.toModel() })) {
                is StretchPlan.Refused -> StretchChange.Refused(planned.reason)

                is StretchPlan.Ready -> if (planned.moves.isNotEmpty() && !confirmed) {
                    StretchChange.ConfirmMoves(planned.moves)
                } else {
                    planned.writes.forEach { dao.upsert(it.toEntity()) }
                    StretchChange.Saved
                }
            }
        }
}
