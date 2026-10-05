package com.tonypine.cycle.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.repository.CycleRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * History's state. It follows her log, so an edit anywhere in the app, on Today or in the calendar,
 * recomputes every cycle at once.
 *
 * @param today her day, from the phone's clock in its current zone. Read again by [refreshDay].
 */
class HistoryViewModel(private val cycles: CycleRepository, private val today: () -> LocalDate = LocalDate::now) :
    ViewModel() {
    private val day = MutableStateFlow(today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = day
        .flatMapLatest { day -> cycles.observeOverview(day) }
        .map(HistoryUiState::from)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState.Loading)

    /** Reads the day again, such as when she comes back to the app after midnight. */
    fun refreshDay() {
        day.value = today()
    }
}

/**
 * The state of the cycle that starts on [start]. It follows her log like [HistoryViewModel]: when an
 * edit moves the cycle's first day, the state becomes [CycleDetailUiState.Missing].
 */
class CycleDetailViewModel(
    private val cycles: CycleRepository,
    private val start: LocalDate,
    private val today: () -> LocalDate = LocalDate::now
) : ViewModel() {
    private val day = MutableStateFlow(today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CycleDetailUiState> = day
        .flatMapLatest { day -> cycles.observeLog(day) }
        .map { CycleDetailUiState.from(it.overview, it.logs, start) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CycleDetailUiState.Loading)

    /** Reads the day again, such as when she comes back to the app after midnight. */
    fun refreshDay() {
        day.value = today()
    }
}

// Keeps the log flowing through a configuration change without a reload.
private const val STOP_TIMEOUT_MILLIS = 5_000L
