package com.tonypine.cycle.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.model.PeriodChange
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * History's state. It follows her log and her contraception, so an edit anywhere in the app, on
 * Today, in the calendar or to a method's dates in Settings, recomputes every cycle at once.
 *
 * @param today her day, from the phone's clock in its current zone. Read again by [refreshDay].
 */
class HistoryViewModel(private val cycles: CycleRepository, private val today: () -> LocalDate = LocalDate::now) :
    ViewModel() {
    private val day = MutableStateFlow(today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = day
        .flatMapLatest { day -> cycles.observeLog(day) }
        .map { HistoryUiState.from(it.overview, it.logs) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState.Loading)

    /** Reads the day again, such as when she comes back to the app after midnight. */
    fun refreshDay() {
        day.value = today()
    }
}

/**
 * The state of the cycle that starts on [start]. It follows her log and her settings like
 * [HistoryViewModel]: when an edit moves the cycle's first day, the state becomes
 * [CycleDetailUiState.Missing], and a category she hides or shows again in "What to log" leaves or
 * comes back at once. "Delete this period" writes through [dayLogs].
 */
class CycleDetailViewModel(
    private val cycles: CycleRepository,
    private val dayLogs: DayLogRepository,
    private val start: LocalDate,
    private val today: () -> LocalDate = LocalDate::now
) : ViewModel() {
    private val day = MutableStateFlow(today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CycleDetailUiState> = day
        .flatMapLatest { day -> cycles.observeLog(day) }
        .map { CycleDetailUiState.from(it.overview, it.logs, start, it.feelings, it.hiddenCategories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CycleDetailUiState.Loading)

    /** Reads the day again, such as when she comes back to the app after midnight. */
    fun refreshDay() {
        day.value = today()
    }

    /**
     * "Delete this period", once she confirmed: the cycle joins the one before. [onDeleted] runs once
     * it is written, to leave the detail of a cycle that is gone.
     */
    fun deletePeriod(onDeleted: () -> Unit) {
        viewModelScope.launch {
            dayLogs.deletePeriod(start, today())
            onDeleted()
        }
    }
}

/**
 * "Edit period dates" for the period of the cycle that starts on [start]. It follows her log like
 * [CycleDetailViewModel], and keeps what she picked until she saves: [save] writes the new days
 * through [dayLogs], or shows why they cannot be saved.
 */
class EditPeriodViewModel(
    private val cycles: CycleRepository,
    private val dayLogs: DayLogRepository,
    private val start: LocalDate,
    private val today: () -> LocalDate = LocalDate::now
) : ViewModel() {
    private val day = MutableStateFlow(today())
    private val draft = MutableStateFlow<PeriodDraft?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<EditPeriodUiState> = day
        .flatMapLatest { day -> cycles.observeOverview(day) }
        .combine(draft) { overview, draft -> EditPeriodUiState.from(overview, start, draft) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EditPeriodUiState.Loading)

    /** Reads the day again, such as when she comes back to the app after midnight. */
    fun refreshDay() {
        day.value = today()
    }

    /** A tap on [date] in the calendar: see [PeriodDraft.pick]. */
    fun pick(date: LocalDate) = update { it.draft.pick(date, it.today) }

    /** Taps now set [day]. */
    fun choose(day: PeriodDay) = update { it.draft.choose(day) }

    /** "Still going" on or off; off, the last day is the one stored, or today. */
    fun setStillGoing(on: Boolean) = update { it.draft.stillGoing(on, lastDay = it.period.end) }

    /** The calendar shows [month]. */
    fun showMonth(month: YearMonth) = update { it.draft.copy(month = month) }

    /**
     * Saves the picked days. [onSaved] runs with the period's new first day once they are written;
     * when they are refused, the editor says why and nothing changes.
     */
    fun save(onSaved: (LocalDate) -> Unit) {
        val editing = uiState.value as? EditPeriodUiState.Editing ?: return
        val picked = editing.draft
        viewModelScope.launch {
            when (val change = dayLogs.editPeriod(start, picked.start, picked.end, today())) {
                PeriodChange.Saved -> onSaved(picked.start)
                is PeriodChange.Refused -> draft.value = picked.copy(refusal = change.reason)
            }
        }
    }

    private fun update(change: (EditPeriodUiState.Editing) -> PeriodDraft) {
        val editing = uiState.value as? EditPeriodUiState.Editing ?: return
        draft.value = change(editing)
    }
}

// Keeps the log flowing through a configuration change without a reload.
private const val STOP_TIMEOUT_MILLIS = 5_000L
