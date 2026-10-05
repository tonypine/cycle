package com.tonypine.cycle.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import java.time.YearMonth
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
 * The calendar's month, the day she picked, and the day log edits for any day up to today. Every
 * write goes to her log, and periods, cycles and estimates recompute from it at once, here and on
 * Today.
 *
 * @param today her day, from the phone's clock in its current zone. Read again by [refreshDay].
 */
class CalendarViewModel(
    private val cycles: CycleRepository,
    private val dayLogs: DayLogRepository,
    private val today: () -> LocalDate = LocalDate::now
) : ViewModel() {
    private val day = MutableStateFlow(today())
    private val month = MutableStateFlow(YearMonth.from(day.value))
    private val selected = MutableStateFlow<LocalDate?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = day
        .flatMapLatest { day ->
            combine(cycles.observeLog(day), month, selected) { log, month, selected ->
                CalendarUiState.from(log, month, selected)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CalendarUiState.Loading)

    /** Reads the day again, such as when she comes back to the app after midnight. */
    fun refreshDay() {
        day.value = today()
    }

    fun onPreviousMonth() {
        month.value = month.value.minusMonths(1)
    }

    fun onNextMonth() {
        month.value = month.value.plusMonths(1)
    }

    /** "Go to today": this month. */
    fun onGoToToday() {
        month.value = YearMonth.from(day.value)
    }

    /** Shows [month], such as the one "Add a past period" on Today opens. */
    fun showMonth(month: YearMonth) {
        this.month.value = month
    }

    /** She tapped [date]: selected, for its day log sheet. Days after today are not logged. */
    fun onDayClick(date: LocalDate) {
        if (date <= day.value) selected.value = date
    }

    /** "Log it": the flow she picked for [date] (null for none) and how she felt. */
    fun onLogDay(date: LocalDate, flow: FlowLevel?, feelings: DayFeelings) = write {
        if (date <= day.value) dayLogs.logDay(date, flow, feelings)
    }

    /** "Period started this day: fill in N days" on [start], N being her usual period length. */
    fun onFillPeriod(start: LocalDate) = write {
        val today = day.value
        if (start > today) return@write
        dayLogs.fillPeriod(start, cycles.observeLog(today).first().usualPeriodLength, today)
    }

    /** "Clear this day" on [date]: what she logged in the categories she hid stays. */
    fun onClearDay(date: LocalDate) = write {
        val today = day.value
        if (date > today) return@write
        dayLogs.clearDay(date, today, cycles.observeLog(today).first().hiddenCategories)
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        // Keeps the log flowing through a configuration change without a reload.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
