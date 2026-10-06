package com.tonypine.cycle.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.repository.ContraceptionRepository
import com.tonypine.cycle.core.domain.ContraceptionEdits
import com.tonypine.cycle.core.domain.StretchPlan
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.StretchChange
import com.tonypine.cycle.core.model.StretchMove
import com.tonypine.cycle.core.model.StretchRefusal
import com.tonypine.cycle.core.ui.MethodChoice
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Settings › Your cycle › Contraception: her method now and every stretch she recorded, from
 * [ContraceptionRepository]. The estimates follow the stretches on their own: the overview reads
 * them from the same repository.
 *
 * @param clock her day, from the phone's clock in its current zone.
 */
class ContraceptionViewModel(contraception: ContraceptionRepository, clock: () -> LocalDate = LocalDate::now) :
    ViewModel() {
    /** Null until the stretches are read. */
    val uiState: StateFlow<ContraceptionUiState?> = contraception.observeStretches()
        .map { stretches ->
            val today = clock()
            ContraceptionUiState(today, ContraceptionEdits.current(stretches, today), stretches.asReversed())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)
}

/**
 * @property current the stretch in force [today], her method "Now"; null on none.
 * @property stretches every stretch, the latest first: "Your methods".
 */
data class ContraceptionUiState(
    val today: LocalDate,
    val current: ContraceptionStretch?,
    val stretches: List<ContraceptionStretch>
)

/**
 * "Add your method" and "Change method": which method, since when and, on a combined pill, patch or
 * ring, its breaks; Save starts it through [ContraceptionRepository.start], ending the current
 * method the day before. On a combined method the date is checked before the breaks, so a refusal or
 * "Move the end of your implant?" comes on the date's step, where she can pick another day.
 */
class AddMethodViewModel(
    private val contraception: ContraceptionRepository,
    private val clock: () -> LocalDate = LocalDate::now
) : ViewModel() {
    private val saver = StretchSaver(viewModelScope)
    private val flow = MutableStateFlow(AddMethodFlow())

    /** Null until the stretches are read. */
    val uiState: StateFlow<AddMethodUiState?> =
        combine(contraception.observeStretches(), flow, saver.dialog) { stretches, flow, dialog ->
            val today = clock()
            AddMethodUiState(
                today = today,
                current = ContraceptionEdits.current(stretches, today),
                step = flow.step,
                choice = flow.choice,
                started = flow.started,
                breaks = flow.breaks,
                dialog = dialog,
                saved = flow.saved
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun onChoose(choice: MethodChoice) = flow.update { it.copy(choice = choice) }

    /** Next on a method: since when. */
    fun onMethodNext() = flow.update {
        if (it.choice is MethodChoice.Method) it.copy(step = AddMethodStep.Since) else it
    }

    /** A day picked on since when. A new day asks again about any stretch it overlaps. */
    fun onPickStart(date: LocalDate) = flow.update { it.copy(started = date, movesConfirmed = false) }

    /**
     * Next or Save on since when: on a combined method checks the day and goes on to its breaks,
     * once she agreed to any move; on any other method saves.
     */
    fun onSinceNext() {
        val shown = flow.value
        val method = (shown.choice as? MethodChoice.Method)?.method ?: return
        val started = shown.started ?: return
        if (!method.isCombined) return save(method, breaks = null)
        viewModelScope.launch {
            // Breaks do not change where a stretch fits: any of them checks the day.
            val stretches = contraception.observeStretches().first()
            when (val plan = ContraceptionEdits.start(stretches, method, Breaks.MONTHLY, started, clock())) {
                is StretchPlan.Refused -> saver.refuse(plan.reason)

                is StretchPlan.Ready -> if (plan.moves.isEmpty()) {
                    toBreaks(movesConfirmed = false)
                } else {
                    saver.ask(plan.moves) { toBreaks(movesConfirmed = true) }
                }
            }
        }
    }

    fun onPickBreaks(breaks: Breaks) = flow.update { it.copy(breaks = breaks) }

    /** Save on the breaks: starts the combined method. */
    fun onSave() {
        val method = (flow.value.choice as? MethodChoice.Method)?.method ?: return
        save(method, flow.value.breaks)
    }

    /** Back inside the steps; false on the first, which leaves. */
    fun onBack(): Boolean {
        val step = flow.value.step
        if (step == AddMethodStep.Method) return false
        flow.update { it.copy(step = AddMethodStep.entries[step.ordinal - 1]) }
        return true
    }

    fun onMove() = saver.onMove()

    fun onDismissDialog() = saver.onDismiss()

    private fun toBreaks(movesConfirmed: Boolean) =
        flow.update { it.copy(step = AddMethodStep.Breaks, movesConfirmed = movesConfirmed) }

    private fun save(method: ContraceptionMethod, breaks: Breaks?) {
        val started = flow.value.started ?: return
        val confirmedBefore = flow.value.movesConfirmed
        saver.save(
            write = { confirmed ->
                contraception.start(method, breaks, started, clock(), confirmed = confirmed || confirmedBefore)
            },
            onSaved = { flow.update { it.copy(saved = true) } }
        )
    }
}

enum class AddMethodStep { Method, Since, Breaks }

/** What she chose so far in "Add your method". Breaks start on every month. */
private data class AddMethodFlow(
    val step: AddMethodStep = AddMethodStep.Method,
    val choice: MethodChoice? = null,
    val started: LocalDate? = null,
    val breaks: Breaks = Breaks.MONTHLY,
    val movesConfirmed: Boolean = false,
    val saved: Boolean = false
)

/**
 * @property current her method now: "Change method" when there is one, "Add your method" otherwise.
 * @property saved the method is saved: the page closes.
 */
data class AddMethodUiState(
    val today: LocalDate,
    val current: ContraceptionStretch?,
    val step: AddMethodStep,
    val choice: MethodChoice?,
    val started: LocalDate?,
    val breaks: Breaks?,
    val dialog: StretchDialog? = null,
    val saved: Boolean = false
)

/**
 * One stretch, by its [id]: its edit page (dates, breaks, delete) and "Mark as stopped". Every write
 * goes through [ContraceptionRepository], which recomputes the estimates like an edited day.
 */
class StretchViewModel(
    private val contraception: ContraceptionRepository,
    private val id: Long,
    private val clock: () -> LocalDate = LocalDate::now
) : ViewModel() {
    private val saver = StretchSaver(viewModelScope)
    private val closed = MutableStateFlow(false)

    val uiState: StateFlow<StretchUiState> =
        combine(contraception.observeStretches(), saver.dialog, closed) { stretches, dialog, closed ->
            val stretch = stretches.firstOrNull { it.id == id }
            val today = clock()
            when {
                closed || stretch == null -> StretchUiState.Closed

                else -> StretchUiState.Ready(
                    stretch = stretch,
                    today = today,
                    latestStop = ContraceptionEdits.latestStop(stretch.method, today),
                    dialog = dialog
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StretchUiState.Loading)

    /** "Mark as stopped" with the last day she gives (on the injection, her last one); then it closes. */
    fun onStop(lastDay: LocalDate) = saver.save(
        write = { confirmed -> contraception.stop(id, lastDay, clock(), confirmed) },
        onSaved = { closed.value = true }
    )

    /** Saves [edited]'s dates or breaks over the stretch. The edit page stays open. */
    fun onEdit(edited: ContraceptionStretch) = saver.save(
        write = { confirmed -> contraception.edit(edited, clock(), confirmed) },
        onSaved = {}
    )

    /** "Delete these dates", once she confirmed: Cycle forgets the method for them, and the page closes. */
    fun onDelete() {
        viewModelScope.launch {
            contraception.delete(id)
            closed.value = true
        }
    }

    fun onMove() = saver.onMove()

    fun onDismissDialog() = saver.onDismiss()
}

sealed interface StretchUiState {
    data object Loading : StretchUiState

    /**
     * @property latestStop the latest stop date it can have: today, or 13 weeks ahead on the injection.
     */
    data class Ready(
        val stretch: ContraceptionStretch,
        val today: LocalDate,
        val latestStop: LocalDate,
        val dialog: StretchDialog? = null
    ) : StretchUiState

    /** Stopped or deleted here, or gone: the page closes. */
    data object Closed : StretchUiState
}

/** A question or a refusal after saving a stretch's dates. */
sealed interface StretchDialog {
    /** "Move the end of your pill?": Move it saves, with [moves]. */
    data class ConfirmMoves(val moves: List<StretchMove>) : StretchDialog

    /** The dates can't be saved, and why. */
    data class Refused(val reason: StretchRefusal) : StretchDialog
}

/**
 * Runs a write that may need her to agree to moving another stretch first: [save] writes, and on
 * [StretchChange.ConfirmMoves] shows the question; [onMove] writes again with the moves confirmed.
 */
private class StretchSaver(private val scope: CoroutineScope) {
    private val shown = MutableStateFlow<StretchDialog?>(null)
    val dialog: StateFlow<StretchDialog?> = shown.asStateFlow()

    // What "Move it" does, while the question shows.
    private var onConfirm: (suspend () -> Unit)? = null

    fun save(write: suspend (confirmed: Boolean) -> StretchChange, onSaved: () -> Unit) {
        scope.launch { handle(write(false), write, onSaved) }
    }

    fun ask(moves: List<StretchMove>, then: suspend () -> Unit) {
        onConfirm = then
        shown.value = StretchDialog.ConfirmMoves(moves)
    }

    fun refuse(reason: StretchRefusal) {
        onConfirm = null
        shown.value = StretchDialog.Refused(reason)
    }

    fun onMove() {
        val then = onConfirm ?: return
        onDismiss()
        scope.launch { then() }
    }

    fun onDismiss() {
        onConfirm = null
        shown.value = null
    }

    private suspend fun handle(
        change: StretchChange,
        write: suspend (confirmed: Boolean) -> StretchChange,
        onSaved: () -> Unit
    ) {
        when (change) {
            StretchChange.Saved -> onSaved()
            is StretchChange.ConfirmMoves -> ask(change.moves) { handle(write(true), write, onSaved) }
            is StretchChange.Refused -> refuse(change.reason)
        }
    }
}

// Keeps the stretches flowing through a configuration change without a reload.
private const val STOP_TIMEOUT_MILLIS = 5_000L
