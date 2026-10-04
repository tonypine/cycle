package com.tonypine.cycle.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.Card
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.EmptyState
import com.tonypine.cycle.core.designsystem.EmptyStateAction
import com.tonypine.cycle.core.designsystem.EmptyStateIcon
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.TextButton
import com.tonypine.cycle.core.designsystem.TonalButton
import com.tonypine.cycle.core.designsystem.WeekRow
import com.tonypine.cycle.core.designsystem.rememberCycleBottomSheetState
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import kotlinx.coroutines.launch

/** Today, wired to its [viewModel]. The day is read again whenever the screen resumes. */
@Composable
fun TodayRoute(viewModel: TodayViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDay()
        onPauseOrDispose {}
    }
    TodayScreen(
        state = state,
        actions = remember(viewModel) {
            TodayActions(
                onPeriodStarted = viewModel::onPeriodStarted,
                onUndoPeriodStarted = viewModel::onUndoPeriodStarted,
                onPeriodEnded = viewModel::onPeriodEnded,
                onUndoPeriodEnded = viewModel::onUndoPeriodEnded,
                onLogPeriod = viewModel::onLogPeriod,
                onFlowChange = viewModel::onFlowChange,
                onStillGoing = viewModel::onStillGoing,
                onEndedOn = viewModel::onEndedOn
            )
        },
        modifier = modifier
    )
}

/** What Today's buttons and sheets do. Each defaults to nothing, for previews and screenshots. */
class TodayActions(
    val onPeriodStarted: () -> Unit = {},
    val onUndoPeriodStarted: () -> Unit = {},
    val onPeriodEnded: () -> Unit = {},
    val onUndoPeriodEnded: () -> Unit = {},
    val onLogPeriod: (start: LocalDate) -> Unit = {},
    val onFlowChange: (FlowLevel?) -> Unit = {},
    val onStillGoing: () -> Unit = {},
    val onEndedOn: (lastDay: LocalDate) -> Unit = {}
)

/**
 * Today: the day and date, her cycle day, a line of context, this week, the one-tap period buttons
 * and the next period estimate. Scrolls when the text is large, so nothing clips at 200%.
 */
@Composable
fun TodayScreen(state: TodayUiState, actions: TodayActions, modifier: Modifier = Modifier) {
    when (state) {
        TodayUiState.Loading -> LoadingState(modifier.fillMaxSize())
        is TodayUiState.Empty -> EmptyToday(state, actions, modifier)
        is TodayUiState.Tracking -> TrackingToday(state, actions, modifier)
    }
}

@Composable
private fun EmptyToday(state: TodayUiState.Empty, actions: TodayActions, modifier: Modifier) {
    val sheet = rememberCycleBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    EmptyState(
        title = stringResource(R.string.today_empty_title),
        body = stringResource(R.string.today_empty_body),
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        illustration = { EmptyStateIcon(CycleIcons.Calendar) },
        action = EmptyStateAction(
            label = stringResource(R.string.today_log_period),
            onClick = { scope.launch { sheet.show() } },
            icon = CycleIcons.Add
        )
    )
    LogPeriodSheet(sheet, state.today, state.periodLength, onLog = actions.onLogPeriod)
}

@Composable
private fun TrackingToday(state: TodayUiState.Tracking, actions: TodayActions, modifier: Modifier) {
    val estimateSheet = rememberCycleBottomSheetState()
    val dayLogSheet = rememberCycleBottomSheetState(skipPartiallyExpanded = true)
    val lastDaySheet = rememberCycleBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val spacing = CycleTheme.spacing
    val margin = Modifier.padding(horizontal = spacing.large)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(vertical = spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        Header(state, margin)
        // Calendars sit 12dp in, so seven 48dp days fit a 360dp screen.
        WeekRow(
            weekOf = state.today,
            stateOf = state::dayState,
            onDayClick = {},
            today = state.today,
            modifier = Modifier.padding(horizontal = spacing.medium),
            isEnabled = { false }
        )
        StatusCard(state, actions, onEndedEarlier = { scope.launch { lastDaySheet.show() } }, modifier = margin)
        Actions(state, actions, onLog = { scope.launch { dayLogSheet.show() } }, modifier = margin)
        NextPeriodCard(state.nextPeriod, onExplain = { scope.launch { estimateSheet.show() } }, modifier = margin)
    }

    EstimateSheet(estimateSheet, state.nextPeriod)
    DayLogSheet(dayLogSheet, state.todayFlow, actions.onFlowChange)
    state.stillGoing?.let { LastDaySheet(lastDaySheet, state.today, it, actions.onEndedOn) }
}

@Composable
private fun Header(state: TodayUiState.Tracking, modifier: Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)) {
        BasicText(
            text = formatDate(state.today, DAY_AND_DATE),
            style = typography.label.copy(color = colors.onSurfaceVariant)
        )
        BasicText(
            text = stringResource(R.string.today_cycle_day, state.cycleDay),
            modifier = Modifier.semantics { heading() },
            style = typography.display.copy(color = colors.onSurface)
        )
        BasicText(text = contextLine(state.phase), style = typography.body.copy(color = colors.onSurface))
    }
}

@Composable
private fun contextLine(phase: TodayPhase): String = when (phase) {
    is TodayPhase.BetweenPeriods ->
        pluralStringResource(R.plurals.today_context_between, phase.daysUntil, phase.daysUntil)

    TodayPhase.Due -> stringResource(R.string.today_context_due)

    is TodayPhase.Late -> pluralStringResource(R.plurals.today_context_late, phase.daysLate, phase.daysLate)

    TodayPhase.PeriodStartedToday -> stringResource(R.string.today_context_started)

    is TodayPhase.OnPeriod -> stringResource(R.string.today_context_on_period, phase.periodDay)

    is TodayPhase.PeriodEndedToday -> pluralStringResource(R.plurals.today_context_ended, phase.length, phase.length)
}

/** The card under the week: Undo after a one-tap log, the calm late card, or "Still going?". */
@Composable
private fun StatusCard(
    state: TodayUiState.Tracking,
    actions: TodayActions,
    onEndedEarlier: () -> Unit,
    modifier: Modifier
) {
    val stillGoing = state.stillGoing
    when {
        stillGoing != null -> Card(modifier.fillMaxWidth()) {
            CardTitle(stringResource(R.string.today_still_going_title))
            val days = stillGoing.prompt.periodDay
            CardBody(pluralStringResource(R.plurals.today_still_going_body, days, days))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
                TonalButton(stringResource(R.string.today_still_going_yes), onClick = actions.onStillGoing)
                TextButton(stringResource(R.string.today_still_going_ended_earlier), onClick = onEndedEarlier)
            }
        }

        state.phase == TodayPhase.PeriodStartedToday ->
            UndoCard(stringResource(R.string.today_started_card), actions.onUndoPeriodStarted, modifier)

        state.phase is TodayPhase.PeriodEndedToday ->
            UndoCard(stringResource(R.string.today_ended_card), actions.onUndoPeriodEnded, modifier)

        state.phase is TodayPhase.Late -> Card(modifier.fillMaxWidth()) {
            CardTitle(stringResource(R.string.today_late_title))
            CardBody(stringResource(R.string.today_late_body))
        }
    }
}

@Composable
private fun UndoCard(text: String, onUndo: () -> Unit, modifier: Modifier) {
    Card(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = text,
                modifier = Modifier.weight(1f),
                style = CycleTheme.typography.titleSmall.copy(color = CycleTheme.colors.onSurface)
            )
            TextButton(stringResource(R.string.today_undo), onClick = onUndo)
        }
    }
}

/** The one-tap period button, then the day log. */
@Composable
private fun Actions(state: TodayUiState.Tracking, actions: TodayActions, onLog: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        when (state.phase) {
            is TodayPhase.BetweenPeriods, TodayPhase.Due, is TodayPhase.Late -> FilledButton(
                text = stringResource(R.string.today_period_started),
                onClick = actions.onPeriodStarted,
                modifier = Modifier.fillMaxWidth(),
                icon = CycleIcons.WaterDrop
            )

            TodayPhase.PeriodStartedToday, is TodayPhase.OnPeriod -> FilledButton(
                text = stringResource(R.string.today_period_ended),
                onClick = actions.onPeriodEnded,
                modifier = Modifier.fillMaxWidth(),
                icon = CycleIcons.Check
            )

            // Ended today: Undo on the card above takes it back.
            is TodayPhase.PeriodEndedToday -> Unit
        }
        TonalButton(
            text = stringResource(
                if (state.onPeriod) R.string.today_log_flow_and_feelings else R.string.today_log_feelings
            ),
            onClick = onLog,
            modifier = Modifier.fillMaxWidth(),
            icon = CycleIcons.EditNote
        )
    }
}

/**
 * "Around 23 October, between 19 and 27 October, estimated from ...". TalkBack reads the estimate as
 * one item, "estimated" included, then the button that explains it.
 */
@Composable
private fun NextPeriodCard(next: NextPeriod, onExplain: () -> Unit, modifier: Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)
        ) {
            BasicText(
                text = stringResource(R.string.today_next_period),
                style = typography.label.copy(color = colors.onSurfaceVariant)
            )
            BasicText(
                text = stringResource(R.string.today_next_around, formatDate(next.expectedStart)),
                style = typography.headline.copy(color = colors.onSurface)
            )
            if (next.earliestStart != next.latestStart) {
                BasicText(
                    text = stringResource(
                        R.string.today_next_between,
                        formatDate(next.earliestStart),
                        formatDate(next.latestStart)
                    ),
                    style = typography.body.copy(color = colors.onSurface)
                )
            }
            BasicText(text = basisLine(next), style = typography.bodySmall.copy(color = colors.onSurfaceVariant))
        }
        TextButton(stringResource(R.string.today_how_estimated), onClick = onExplain, icon = CycleIcons.Info)
    }
}

@Composable
private fun basisLine(next: NextPeriod): String = when (val basis = next.basis) {
    EstimateBasis.Typical -> stringResource(R.string.today_basis_typical, next.cycleLength)
    EstimateBasis.Setup -> stringResource(R.string.today_basis_setup)
    is EstimateBasis.Logged -> pluralStringResource(R.plurals.today_basis_logged, basis.count, basis.count)
}

@Composable
internal fun CardTitle(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier,
        style = CycleTheme.typography.titleSmall.copy(color = CycleTheme.colors.onSurface)
    )
}

@Composable
internal fun CardBody(text: String, modifier: Modifier = Modifier, style: TextStyle = CycleTheme.typography.bodySmall) {
    BasicText(text = text, modifier = modifier, style = style.copy(color = CycleTheme.colors.onSurfaceVariant))
}

/** [date] in the phone's locale, by default as day and month: "23 October". */
@Composable
internal fun formatDate(date: LocalDate, skeleton: String = DAY_AND_MONTH): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale, skeleton) { dateFormatter(locale, skeleton) }
    return formatter.format(date)
}

@Preview
@Composable
private fun TodayMidCyclePreview() {
    CycleTheme { TodayScreen(TodaySamples.midCycle, TodayActions()) }
}

@Preview
@Composable
private fun TodayPeriodDarkPreview() {
    CycleTheme(darkTheme = true) { TodayScreen(TodaySamples.periodStarted, TodayActions()) }
}

@Preview
@Composable
private fun TodayEmptyPreview() {
    CycleTheme { TodayScreen(TodaySamples.empty, TodayActions()) }
}
