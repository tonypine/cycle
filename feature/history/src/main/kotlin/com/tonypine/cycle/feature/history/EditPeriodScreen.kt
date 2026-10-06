package com.tonypine.cycle.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.ButtonGroup
import com.tonypine.cycle.core.designsystem.Card
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.EmptyState
import com.tonypine.cycle.core.designsystem.EmptyStateIcon
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.MonthCalendar
import com.tonypine.cycle.core.designsystem.SwitchRow
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.model.PeriodRefusal
import java.time.LocalDate
import java.time.YearMonth

/**
 * The period editor, wired to its [viewModel]. The day is read again whenever the screen resumes.
 * [onSaved] runs with the period's new first day once the new days are written.
 */
@Composable
fun EditPeriodRoute(
    viewModel: EditPeriodViewModel,
    onBack: () -> Unit,
    onSaved: (start: LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDay()
        onPauseOrDispose {}
    }
    EditPeriodScreen(
        state = state,
        onBack = onBack,
        onChoose = viewModel::choose,
        onPick = viewModel::pick,
        onMonthChange = viewModel::showMonth,
        onStillGoingChange = viewModel::setStillGoing,
        onSave = { viewModel.save(onSaved) },
        modifier = modifier
    )
}

/**
 * "Period dates": the days she picked and how many, the choice of the first or the last day, a month
 * calendar that draws the picked days as a period and sets the chosen end on a tap, "Still going" on
 * the current cycle's period, why the last save was refused, and Save, on once something changed.
 * Days after today cannot be picked, nor a last day before the first. Scrolls when the text is large.
 */
@Composable
fun EditPeriodScreen(
    state: EditPeriodUiState,
    onBack: () -> Unit,
    onChoose: (PeriodDay) -> Unit,
    onPick: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onStillGoingChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = stringResource(R.string.period_edit_title),
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.cycle_back), onBack)
        )
        val content = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        when (state) {
            EditPeriodUiState.Loading -> LoadingState(content)

            EditPeriodUiState.Missing -> EmptyState(
                title = stringResource(R.string.cycle_missing_title),
                body = stringResource(R.string.cycle_missing_body),
                modifier = content,
                illustration = { EmptyStateIcon(CycleIcons.History) }
            )

            is EditPeriodUiState.Editing -> Editor(
                state,
                onChoose,
                onPick,
                onMonthChange,
                onStillGoingChange,
                onSave,
                content
            )
        }
    }
}

@Composable
private fun Editor(
    state: EditPeriodUiState.Editing,
    onChoose: (PeriodDay) -> Unit,
    onPick: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onStillGoingChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier
) {
    val spacing = CycleTheme.spacing
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    val draft = state.draft
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = spacing.large, end = spacing.large, bottom = spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        PickedDays(state, Modifier.fillMaxWidth())
        if (state.canStillGo) {
            SwitchRow(
                title = stringResource(R.string.period_edit_still_going),
                checked = draft.end == null,
                onCheckedChange = onStillGoingChange,
                modifier = Modifier.fillMaxWidth(),
                body = stringResource(R.string.period_edit_still_going_body)
            )
        }
        if (draft.end != null) {
            ButtonGroup(
                label = stringResource(R.string.period_edit_picking),
                options = listOf(
                    stringResource(R.string.period_edit_first_day),
                    stringResource(R.string.period_edit_last_day)
                ),
                selectedIndex = draft.picking.ordinal,
                // Tapping the chosen one again keeps it: one end is always being picked.
                onSelectedChange = { index -> index?.let { onChoose(PeriodDay.entries[it]) } },
                modifier = Modifier.fillMaxWidth()
            )
        }
        BasicText(
            text = stringResource(
                if (draft.picking ==
                    PeriodDay.First
                ) {
                    R.string.period_edit_pick_first
                } else {
                    R.string.period_edit_pick_last
                }
            ),
            style = typography.body.copy(color = colors.onSurfaceVariant)
        )
        MonthCalendar(
            month = draft.month,
            stateOf = { if (state.isPeriodDay(it)) CycleDayState.Period else CycleDayState.Plain },
            onDayClick = onPick,
            today = state.today,
            onPreviousMonth = { onMonthChange(draft.month.minusMonths(1)) },
            onNextMonth = { onMonthChange(draft.month.plusMonths(1)) },
            modifier = Modifier.fillMaxWidth(),
            selected = if (draft.picking == PeriodDay.First) draft.start else draft.end,
            isEnabled = state::canPick
        )
        draft.refusal?.let { refusal ->
            BasicText(
                text = refusal.message(state.today),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = typography.body.copy(color = colors.error)
            )
        }
        FilledButton(
            text = stringResource(R.string.period_edit_save),
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.changed
        )
    }
}

/** "August 5 to August 10" and "6 days", or "Since September 2" and "9 days so far", read as one item. */
@Composable
private fun PickedDays(state: EditPeriodUiState.Editing, modifier: Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    val start = formatDate(state.draft.start, state.today)
    Card(modifier) {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            BasicText(
                text = when (val end = state.draft.end) {
                    null -> stringResource(R.string.history_since, start)
                    else -> stringResource(R.string.history_dates, start, formatDate(end, state.today))
                },
                modifier = Modifier.semantics { heading() },
                style = typography.titleSmall.copy(color = colors.onSurface)
            )
            BasicText(
                text = if (state.draft.end == null) {
                    pluralStringResource(R.plurals.history_period_so_far, state.length, state.length)
                } else {
                    pluralStringResource(R.plurals.history_period_length, state.length, state.length)
                },
                style = typography.body.copy(color = colors.onSurfaceVariant)
            )
        }
    }
}

/** Why the new days cannot be saved, in one or two sentences. */
@Composable
private fun PeriodRefusal.message(today: LocalDate): String = when (this) {
    PeriodRefusal.Gone -> stringResource(R.string.period_edit_gone)

    PeriodRefusal.AfterToday -> stringResource(R.string.period_edit_after_today)

    PeriodRefusal.EndBeforeStart -> stringResource(R.string.period_edit_end_before_start)

    is PeriodRefusal.TooClose -> stringResource(
        R.string.period_edit_too_close,
        stringResource(R.string.history_dates, formatDate(period.start, today), formatDate(period.end, today))
    )
}

@Preview
@Composable
private fun EditPeriodPreview() {
    CycleTheme {
        EditPeriodScreen(
            HistorySamples.editPast,
            onBack = {},
            onChoose = {},
            onPick = {},
            onMonthChange = {},
            onStillGoingChange = {},
            onSave = {}
        )
    }
}

@Preview
@Composable
private fun EditPeriodDarkPreview() {
    CycleTheme(darkTheme = true) {
        EditPeriodScreen(
            HistorySamples.editCurrent,
            onBack = {},
            onChoose = {},
            onPick = {},
            onMonthChange = {},
            onStillGoingChange = {},
            onSave = {}
        )
    }
}
