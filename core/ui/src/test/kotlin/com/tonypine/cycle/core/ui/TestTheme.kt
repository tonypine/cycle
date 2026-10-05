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
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LogCategory
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.SexualActivity
import com.tonypine.cycle.core.model.SleepQuality
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
    onLog: (LocalDate, FlowLevel?, DayFeelings) -> Unit = { _, _, _ -> },
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

    /** How she felt on [medium]'s day, in every category. */
    val everyFeeling = DayFeelings(
        date = medium.date,
        pain = Pain(PainLevel.MODERATE, setOf(PainKind.CRAMPS)),
        body = setOf(BodySymptom.BLOATING),
        moods = setOf(Mood.IRRITABLE),
        energy = EnergyLevel.LOW,
        sleep = SleepQuality.BADLY,
        sex = SexualActivity.PROTECTED,
        note = "A synthetic note."
    )

    /** A period day with flow and every category logged. */
    val feelings = medium.copy(feelings = everyFeeling)

    /** [feelings] with Sex and Notes hidden in "What to log". */
    val hidden = feelings.copy(hiddenCategories = setOf(LogCategory.SEX, LogCategory.NOTES))
}
