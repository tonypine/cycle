package com.tonypine.cycle.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Today's state and the one-tap period actions. Every write goes to her log or settings, and the
 * state follows from the log: an Undo is the write taken back, and the estimate recomputes at once.
 *
 * @param today her day, from the phone's clock in its current zone. Read again by [refreshDay].
 */
class TodayViewModel(
    private val cycles: CycleRepository,
    private val dayLogs: DayLogRepository,
    private val settings: SettingsRepository,
    private val today: () -> LocalDate = LocalDate::now
) : ViewModel() {
    private val day = MutableStateFlow(today())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = day
        .flatMapLatest { day ->
            combine(cycles.observeDay(day), settings.settings) { dayOverview, settings ->
                TodayUiState.from(dayOverview.overview, settings, dayOverview.log)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TodayUiState.Loading)

    /** Reads the day again, such as when she comes back to the app after midnight. */
    fun refreshDay() {
        day.value = today()
    }

    /** "My period started": today is day 1. */
    fun onPeriodStarted() = write { dayLogs.setPeriodStarted(day.value, started = true) }

    fun onUndoPeriodStarted() = write { dayLogs.setPeriodStarted(day.value, started = false) }

    /** "My period ended": today is its last day. */
    fun onPeriodEnded() = write { dayLogs.setPeriodEnded(day.value, ended = true) }

    fun onUndoPeriodEnded() = write { dayLogs.setPeriodEnded(day.value, ended = false) }

    /**
     * Logs a period that started on [start], from the empty state. It lasts her usual period length:
     * when that is over before today, its last day is marked ended; otherwise it is still going.
     */
    fun onLogPeriod(start: LocalDate) = write {
        val today = day.value
        if (start > today) return@write
        val end = start.plusDays(settings.settings.first().usualPeriodLength - 1L)
        // The end first: alone it is ignored, so Today never shows the period open in between.
        if (end < today) dayLogs.setPeriodEnded(end, ended = true)
        dayLogs.setPeriodStarted(start, started = true)
    }

    /** The flow she picked in the day log sheet for today, or null to clear it. */
    fun onFlowChange(flow: FlowLevel?) = write { dayLogs.setFlow(day.value, flow) }

    /** "Still going": not asked again for this period. */
    fun onStillGoing() = write { stillGoing()?.let { settings.dismiss(it.prompt) } }

    /** "It ended earlier", on [lastDay]: marks it ended, which also ends the question. */
    fun onEndedOn(lastDay: LocalDate) = write {
        val stillGoing = stillGoing() ?: return@write
        if (lastDay !in stillGoing.days) return@write
        settings.dismiss(stillGoing.prompt)
        dayLogs.setPeriodEnded(lastDay, ended = true)
    }

    private fun stillGoing(): StillGoing? = (uiState.value as? TodayUiState.Tracking)?.stillGoing

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        // Keeps the log flowing through a configuration change without a reload.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
