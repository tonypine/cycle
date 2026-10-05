package com.tonypine.cycle.core.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tonypine.cycle.core.designsystem.AssistChip
import com.tonypine.cycle.core.designsystem.ButtonGroup
import com.tonypine.cycle.core.designsystem.CycleBottomSheet
import com.tonypine.cycle.core.designsystem.CycleBottomSheetState
import com.tonypine.cycle.core.designsystem.CycleDestructiveDialog
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTextField
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.FilterChip
import com.tonypine.cycle.core.designsystem.TextButton
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * What the day log sheet shows for one day.
 *
 * @property flow the flow she logged that day, which the sheet starts from.
 * @property fillDays offers "Period started this day: fill in N days" with this many days, or null
 *   when a period is too near for it.
 * @property canClear offers "Clear this day": clearing would change something.
 * @property isPeriodDay the day draws as a period day, so clearing it says it will no longer count.
 * @property feelings how she felt that day, the categories she hid included, which the sheet keeps
 *   as they are when it saves.
 * @property hiddenCategories the categories she turned off in "What to log": not shown.
 */
data class DayLogEntry(
    val date: LocalDate,
    val flow: FlowLevel? = null,
    val fillDays: Int? = null,
    val canClear: Boolean = false,
    val isPeriodDay: Boolean = false,
    val feelings: DayFeelings = DayFeelings(date),
    val hiddenCategories: Set<LogCategory> = emptySet()
) {
    /** How she felt that day, in the categories she shows. */
    val shownFeelings: DayFeelings
        get() = feelings.without(hiddenCategories)

    /** She logged something she can see that day: a flow, or how she felt. */
    val isLogged: Boolean
        get() = flow != null || !shownFeelings.isEmpty
}

/**
 * The day log sheet for any day up to today, from Today and the calendar: the day as its title
 * ("Tuesday 2 February"), the flow, the fill shortcut when no period is near, then how she feels
 * (Pain, Body, Mood, Energy, Sleep, Sex and Notes, without the categories she hid), "Clear this day"
 * and Nah / Log it. Nothing is required.
 *
 * Everything is a draft until Log it saves the flow and how she felt with [onLog]; Nah and closing
 * the sheet drop it. The categories she hid go back as they were, so hiding one never erases it.
 * The fill chip logs the period with [onFill] at once and closes the sheet. "Clear this day" asks
 * first, in a [CycleDestructiveDialog] that says what goes, then calls [onClear] and closes the
 * sheet.
 *
 * While she types a note, the field with its supporting text stays above the keyboard, and so do
 * Nah / Log it when there is room; otherwise they are a scroll away.
 *
 * TalkBack announces the sheet by its title and reads each section title as a heading. A choice of
 * one reads as a radio button with its group ("Medium, Flow, radio button, selected"), a choice of
 * many as a checkbox ("Cramps, checkbox, selected").
 */
@Composable
fun DayLogSheet(
    sheet: CycleBottomSheetState,
    entry: DayLogEntry,
    onLog: (date: LocalDate, flow: FlowLevel?, feelings: DayFeelings) -> Unit,
    onFill: (start: LocalDate) -> Unit,
    onClear: (date: LocalDate) -> Unit
) {
    val scope = rememberCoroutineScope()
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val date = entry.date
    CycleBottomSheet(sheet, title = formatDate(date, DAY_AND_DATE), onDismiss = { confirmClear = false }) {
        var flow by rememberSaveable(date) { mutableStateOf(entry.flow) }
        var feelings by rememberSaveable(date) { mutableStateOf(entry.feelings) }
        val note = rememberSaveable(date, saver = TextFieldState.Saver) { TextFieldState(entry.feelings.note) }
        val shown = { category: LogCategory -> category !in entry.hiddenCategories }
        Section(stringResource(R.string.day_log_flow)) { label ->
            Choice(label, FlowLevel.entries, flow, { stringResource(it.label) }) { flow = it }
        }
        entry.fillDays?.let { days ->
            AssistChip(
                label = pluralStringResource(R.plurals.day_log_fill, days, days),
                onClick = {
                    onFill(date)
                    scope.launch { sheet.hide() }
                },
                icon = CycleIcons.WaterDrop
            )
        }
        if (shown(LogCategory.PAIN)) {
            Section(stringResource(R.string.day_log_pain)) { label ->
                val pain = feelings.pain
                Choice(label, PainLevel.entries, pain?.level, { stringResource(it.label) }) { level ->
                    feelings = feelings.copy(pain = level?.let { Pain(it, pain?.kinds.orEmpty()) })
                }
                if (pain != null && pain.level > PainLevel.NONE) {
                    BasicText(
                        stringResource(R.string.day_log_pain_where),
                        style = CycleTheme.typography.label.copy(color = CycleTheme.colors.onSurfaceVariant)
                    )
                    Chips(PainKind.entries, pain.kinds, { stringResource(it.label) }) { kinds ->
                        feelings = feelings.copy(pain = pain.copy(kinds = kinds))
                    }
                }
            }
        }
        if (shown(LogCategory.BODY)) {
            Section(stringResource(R.string.day_log_body)) {
                Chips(BodySymptom.entries, feelings.body, { stringResource(it.label) }) {
                    feelings = feelings.copy(body = it)
                }
            }
        }
        if (shown(LogCategory.MOOD)) {
            Section(stringResource(R.string.day_log_mood)) {
                Chips(Mood.entries, feelings.moods, {
                    stringResource(it.label)
                }) { feelings = feelings.copy(moods = it) }
            }
        }
        if (shown(LogCategory.ENERGY)) {
            Section(stringResource(R.string.day_log_energy)) { label ->
                Choice(label, EnergyLevel.entries, feelings.energy, { stringResource(it.label) }) {
                    feelings = feelings.copy(energy = it)
                }
            }
        }
        if (shown(LogCategory.SLEEP)) {
            Section(stringResource(R.string.day_log_sleep)) { label ->
                Choice(label, SleepQuality.entries, feelings.sleep, { stringResource(it.label) }) {
                    feelings = feelings.copy(sleep = it)
                }
            }
        }
        if (shown(LogCategory.SEX)) {
            Section(stringResource(R.string.day_log_sex)) { label ->
                Choice(label, SexualActivity.entries, feelings.sex, { stringResource(it.label) }) {
                    feelings = feelings.copy(sex = it)
                }
            }
        }
        // The end of the sheet: the note, Clear and Nah / Log it, in one column so it can be brought
        // into view above the keyboard together.
        val noteSource = remember { MutableInteractionSource() }
        val endOfSheet = remember { BringIntoViewRequester() }
        val noteField = remember { BringIntoViewRequester() }
        KeepAboveKeyboard(noteSource, endOfSheet, noteField)
        Column(
            modifier = Modifier.bringIntoViewRequester(endOfSheet),
            verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)
        ) {
            if (shown(LogCategory.NOTES)) {
                Section(stringResource(R.string.day_log_notes)) {
                    CycleTextField(
                        state = note,
                        label = stringResource(R.string.day_log_note_label),
                        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(noteField),
                        placeholder = stringResource(R.string.day_log_note_placeholder),
                        supportingText = stringResource(R.string.day_log_note_supporting),
                        maxLength = DayFeelings.NOTE_MAX_LENGTH,
                        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 2, maxHeightInLines = 6),
                        interactionSource = noteSource
                    )
                }
            }
            if (entry.canClear) {
                TextButton(
                    text = stringResource(R.string.day_log_clear),
                    onClick = { confirmClear = true },
                    icon = CycleIcons.Delete
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
            ) {
                TextButton(stringResource(R.string.day_log_dismiss), onClick = { scope.launch { sheet.hide() } })
                FilledButton(
                    text = stringResource(R.string.day_log_save),
                    onClick = {
                        onLog(date, flow, feelings.copy(note = note.text.toString()))
                        scope.launch { sheet.hide() }
                    }
                )
            }
        }
    }
    val day = formatDate(date)
    CycleDestructiveDialog(
        visible = confirmClear && sheet.isVisible,
        onDismissRequest = { confirmClear = false },
        title = stringResource(R.string.day_log_clear_title, day),
        text = stringResource(
            if (entry.isPeriodDay) R.string.day_log_clear_period_body else R.string.day_log_clear_body,
            day
        ),
        confirmText = stringResource(R.string.day_log_clear_confirm),
        onConfirm = {
            confirmClear = false
            onClear(date)
            scope.launch { sheet.hide() }
        },
        dismissText = stringResource(R.string.day_log_clear_keep)
    )
}

/**
 * Keeps the note and Log it above the keyboard while the field in [noteSource] has focus. Each time
 * the keyboard opens or changes height, brings [endOfSheet] into view, Log it included when there is
 * room, then [noteField], so the whole field and its supporting text show even when the end of the
 * sheet is taller than the space left. Foundation does this only for a field that was fully in view
 * before the keyboard opened.
 */
@Composable
private fun KeepAboveKeyboard(
    noteSource: MutableInteractionSource,
    endOfSheet: BringIntoViewRequester,
    noteField: BringIntoViewRequester
) {
    val focused by noteSource.collectIsFocusedAsState()
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    LaunchedEffect(focused, ime, density) {
        if (!focused) return@LaunchedEffect
        snapshotFlow { ime.getBottom(density) }.collectLatest {
            // Wait a frame, so the sheet has laid out above the keyboard's new height first.
            withFrameNanos {}
            endOfSheet.bringIntoView()
            noteField.bringIntoView()
        }
    }
}

/** A titled part of the sheet: the title, read as a heading, then [content], given the title. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.(title: String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        BasicText(
            title,
            modifier = Modifier.semantics { heading() },
            style = CycleTheme.typography.titleSmall.copy(color = CycleTheme.colors.onSurface)
        )
        content(title)
    }
}

/** One of [options], or none: a [ButtonGroup] that TalkBack reads with [label]. */
@Composable
private fun <T> Choice(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T?) -> Unit
) {
    ButtonGroup(
        label = label,
        options = options.map { optionLabel(it) },
        selectedIndex = selected?.let { options.indexOf(it) },
        onSelectedChange = { index -> onSelect(index?.let { options[it] }) }
    )
}

/** Any of [options]: a [FilterChip] each, in a row that wraps. */
@Composable
private fun <T> Chips(
    options: List<T>,
    selected: Set<T>,
    optionLabel: @Composable (T) -> String,
    onChange: (Set<T>) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
    ) {
        options.forEach { option ->
            FilterChip(
                label = optionLabel(option),
                selected = option in selected,
                onClick = { onChange(if (option in selected) selected - option else selected + option) }
            )
        }
    }
}

private val FlowLevel.label: Int
    get() = when (this) {
        FlowLevel.NONE -> R.string.day_log_flow_none
        FlowLevel.SPOTTING -> R.string.day_log_flow_spotting
        FlowLevel.LIGHT -> R.string.day_log_flow_light
        FlowLevel.MEDIUM -> R.string.day_log_flow_medium
        FlowLevel.HEAVY -> R.string.day_log_flow_heavy
    }
