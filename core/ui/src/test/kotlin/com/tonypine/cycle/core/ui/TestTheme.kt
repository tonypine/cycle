package com.tonypine.cycle.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tonypine.cycle.core.designsystem.CycleBottomSheetState
import com.tonypine.cycle.core.designsystem.CycleSheetValue
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.rememberCycleBottomSheetState
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate

/** [content] in [CycleTheme] on the app's `surface`, with motion reduced so every frame is final. */
@Composable
internal fun Themed(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CycleTheme(darkTheme = darkTheme, reduceMotion = true) {
        Box(Modifier.fillMaxSize().background(CycleTheme.colors.surface)) { content() }
    }
}

/** The day log sheet for [entry], open from the start. Returns its state. */
@Composable
internal fun OpenDayLogSheet(
    entry: DayLogEntry,
    onLog: (LocalDate, FlowLevel?) -> Unit = { _, _ -> },
    onFill: (LocalDate) -> Unit = {},
    onClear: (LocalDate) -> Unit = {}
): CycleBottomSheetState {
    val sheet = rememberCycleBottomSheetState(CycleSheetValue.Expanded, skipPartiallyExpanded = true)
    DayLogSheet(sheet, entry, onLog, onFill, onClear)
    return sheet
}

/** Synthetic days in 2027, never anyone's real cycle. */
internal object DayLogSamples {
    /** A past day with nothing logged and no period near: the fill chip shows. */
    val empty = DayLogEntry(LocalDate.of(2027, 3, 9), fillDays = 5)

    /** A day with nothing logged next to a period: no fill chip. */
    val nearPeriod = DayLogEntry(LocalDate.of(2027, 3, 8))

    /** A period day with medium flow: Clear shows. */
    val medium = DayLogEntry(LocalDate.of(2027, 3, 3), FlowLevel.MEDIUM, canClear = true, isPeriodDay = true)
}
