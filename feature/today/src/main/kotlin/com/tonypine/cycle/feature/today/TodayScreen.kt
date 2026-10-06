package com.tonypine.cycle.feature.today

import android.text.format.DateFormat
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
import com.tonypine.cycle.core.model.BleedBasis
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.ui.DAY_AND_DATE
import com.tonypine.cycle.core.ui.DAY_AND_MONTH
import com.tonypine.cycle.core.ui.DayLogEntry
import com.tonypine.cycle.core.ui.DayLogSheet
import com.tonypine.cycle.core.ui.calmLine
import com.tonypine.cycle.core.ui.daySummary
import com.tonypine.cycle.core.ui.formatDate
import com.tonypine.cycle.core.ui.methodInSentence
import com.tonypine.cycle.core.ui.methodShortName
import com.tonypine.cycle.core.ui.summaryLine
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

/**
 * Today, wired to its [viewModel]. The day is read again whenever the screen resumes.
 * [onAddPastPeriod] opens the calendar on a month, from the "missed a period?" card.
 */
@Composable
fun TodayRoute(viewModel: TodayViewModel, modifier: Modifier = Modifier, onAddPastPeriod: (YearMonth) -> Unit = {}) {
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
                onLogDay = viewModel::onLogDay,
                onFillPeriod = viewModel::onFillPeriod,
                onClearDay = viewModel::onClearDay,
                onStillGoing = viewModel::onStillGoing,
                onEndedOn = viewModel::onEndedOn,
                onAddPastPeriod = onAddPastPeriod,
                onNoMissedPeriod = viewModel::onNoMissedPeriod,
                onDismissCopperIudNote = viewModel::onDismissCopperIudNote
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
    val onLogDay: (date: LocalDate, flow: FlowLevel?, feelings: DayFeelings) -> Unit = { _, _, _ -> },
    val onFillPeriod: (start: LocalDate) -> Unit = {},
    val onClearDay: (date: LocalDate) -> Unit = {},
    val onStillGoing: () -> Unit = {},
    val onEndedOn: (lastDay: LocalDate) -> Unit = {},
    val onAddPastPeriod: (month: YearMonth) -> Unit = {},
    val onNoMissedPeriod: () -> Unit = {},
    val onDismissCopperIudNote: () -> Unit = {}
)

/**
 * Today: the day and date, her cycle day (or her method, or the days since she stopped one), a line
 * of context, this week, the one-tap period buttons, what she logged today and what is expected next.
 * Scrolls when the text is large, so nothing clips at 200%.
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
    val estimateSheet = rememberCycleBottomSheetState(skipPartiallyExpanded = true)
    val methodSheet = rememberCycleBottomSheetState(skipPartiallyExpanded = true)
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
            isEnabled = { false },
            wordsOf = state.days::wordsOf
        )
        StatusCard(state, actions, onEndedEarlier = { scope.launch { lastDaySheet.show() } }, modifier = margin)
        state.copperIudNote?.let { CopperIudCard(actions.onDismissCopperIudNote, margin) }
        Actions(state, actions, onLog = { scope.launch { dayLogSheet.show() } }, modifier = margin)
        if (state.todayLog.isLogged) {
            LoggedTodayCard(state.todayLog, onEdit = { scope.launch { dayLogSheet.show() } }, modifier = margin)
        }
        OutlookCard(
            state,
            onExplain = { scope.launch { estimateSheet.show() } },
            onWhatChanges = { scope.launch { methodSheet.show() } },
            modifier = margin
        )
    }

    when (val outlook = state.outlook) {
        is NextPeriod -> EstimateSheet(estimateSheet, outlook)
        is NextBleed -> NextBleedSheet(estimateSheet, outlook.method)
        else -> Unit
    }
    state.method?.let { MethodSheet(methodSheet, it.method) }
    DayLogSheet(dayLogSheet, state.todayLog, actions.onLogDay, actions.onFillPeriod, actions.onClearDay)
    state.stillGoing?.let { LastDaySheet(lastDaySheet, state.today, it, state.words, actions.onEndedOn) }
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
            text = when (val display = state.display) {
                is TodayDisplay.CycleDay -> stringResource(R.string.today_cycle_day, display.day)

                is TodayDisplay.Method -> methodShortName(display.method)

                is TodayDisplay.DaysSince -> pluralStringResource(
                    R.plurals.today_days_since,
                    display.days,
                    display.days
                )
            },
            modifier = Modifier.semantics { heading() },
            style = typography.display.copy(color = colors.onSurface)
        )
        BasicText(text = contextLine(state), style = typography.body.copy(color = colors.onSurface))
    }
}

/**
 * The line under the big one: what is expected or how long she has bled, in the day's words; after
 * stopping a method, since when; on a method with no estimate, its calm line.
 */
@Composable
private fun contextLine(state: TodayUiState.Tracking): String {
    val display = state.display
    if (display is TodayDisplay.DaysSince) return sinceLine(display.method)
    val bleed = state.outlook as? NextBleed
    val words = state.words
    return when (val phase = state.phase) {
        is TodayPhase.BetweenPeriods -> when {
            bleed == null -> pluralStringResource(R.plurals.today_context_between, phase.daysUntil, phase.daysUntil)

            bleed.basis == BleedBasis.START_DATE && !bleed.missedBreak ->
                pluralStringResource(R.plurals.today_context_bleed_between_first, phase.daysUntil, phase.daysUntil)

            else -> pluralStringResource(R.plurals.today_context_bleed_between, phase.daysUntil, phase.daysUntil)
        }

        TodayPhase.Due -> stringResource(
            if (bleed == null) R.string.today_context_due else R.string.today_context_bleed_due
        )

        is TodayPhase.Late -> pluralStringResource(R.plurals.today_context_late, phase.daysLate, phase.daysLate)

        TodayPhase.NoEstimate -> when (val method = state.method) {
            null -> ""

            else -> if (state.outlook == TodayOutlook.FirstBleedToLog) {
                stringResource(R.string.today_context_first_bleed_to_log)
            } else {
                calmLine(method.method, method.breaks)
            }
        }

        TodayPhase.PeriodStartedToday -> stringResource(words.startedLine())

        is TodayPhase.OnPeriod -> stringResource(words.dayLine(), phase.periodDay)

        is TodayPhase.PeriodEndedToday -> pluralStringResource(words.endedLine(), phase.length, phase.length)
    }
}

/**
 * The card under the week: "Missed a period?", "Still going?", Undo after a one-tap log, the calm
 * late card, or a break that passed with no bleed.
 */
@Composable
private fun StatusCard(
    state: TodayUiState.Tracking,
    actions: TodayActions,
    onEndedEarlier: () -> Unit,
    modifier: Modifier
) {
    val stillGoing = state.stillGoing
    val missedPeriod = state.missedPeriod
    when {
        missedPeriod != null -> Card(modifier.fillMaxWidth()) {
            CardTitle(stringResource(R.string.today_missed_title))
            CardBody(stringResource(R.string.today_missed_body, missedPeriod.prompt.cycleDay))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
                TonalButton(
                    text = stringResource(R.string.today_missed_add),
                    onClick = { actions.onAddPastPeriod(missedPeriod.likelyMonth) },
                    icon = CycleIcons.Calendar
                )
                TextButton(stringResource(R.string.today_missed_no), onClick = actions.onNoMissedPeriod)
            }
        }

        stillGoing != null -> Card(modifier.fillMaxWidth()) {
            CardTitle(stringResource(state.words.stillGoingTitle()))
            val days = stillGoing.prompt.periodDay
            CardBody(pluralStringResource(state.words.stillGoingBody(), days, days))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
                TonalButton(stringResource(R.string.today_still_going_yes), onClick = actions.onStillGoing)
                TextButton(stringResource(R.string.today_still_going_ended_earlier), onClick = onEndedEarlier)
            }
        }

        state.phase == TodayPhase.PeriodStartedToday ->
            UndoCard(stringResource(state.words.startedCard()), actions.onUndoPeriodStarted, modifier)

        state.phase is TodayPhase.PeriodEndedToday ->
            UndoCard(stringResource(state.words.endedCard()), actions.onUndoPeriodEnded, modifier)

        state.phase is TodayPhase.Late -> Card(modifier.fillMaxWidth()) {
            CardTitle(stringResource(R.string.today_late_title))
            CardBody(stringResource(R.string.today_late_body))
        }

        // No "late" on a scheduled bleed, and nothing about why.
        (state.outlook as? NextBleed)?.missedBreak == true && !state.onPeriod -> Card(modifier.fillMaxWidth()) {
            CardTitle(stringResource(R.string.today_missed_break_title))
            CardBody(stringResource(R.string.today_missed_break_body))
        }
    }
}

/** "Periods can be heavier at first", on a copper IUD for six months after fitting, until Got it. */
@Composable
private fun CopperIudCard(onGotIt: () -> Unit, modifier: Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)
        ) {
            CardTitle(stringResource(R.string.today_copper_iud_title))
            CardBody(stringResource(R.string.today_copper_iud_body))
        }
        TextButton(stringResource(R.string.today_got_it), onClick = onGotIt)
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

/** The one-tap period button, in the day's words, then the day log. */
@Composable
private fun Actions(state: TodayUiState.Tracking, actions: TodayActions, onLog: () -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        when (state.phase) {
            is TodayPhase.BetweenPeriods, TodayPhase.Due, is TodayPhase.Late, TodayPhase.NoEstimate -> FilledButton(
                text = stringResource(state.words.startButton()),
                onClick = actions.onPeriodStarted,
                modifier = Modifier.fillMaxWidth(),
                icon = CycleIcons.WaterDrop
            )

            TodayPhase.PeriodStartedToday, is TodayPhase.OnPeriod -> FilledButton(
                text = stringResource(state.words.endButton()),
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
 * "Logged today: Cramps, moderate · Bloating · Low energy", once she has logged something she shows,
 * with Edit to open the day log again. TalkBack reads the title and the summary as one item. The note
 * stays out of the line; a day with only a note says so.
 */
@Composable
private fun LoggedTodayCard(log: DayLogEntry, onEdit: () -> Unit, modifier: Modifier) {
    val items = daySummary(log.flow, log.shownFeelings)
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)
        ) {
            CardTitle(stringResource(R.string.today_logged_title))
            CardBody(
                if (items.isEmpty()) stringResource(R.string.today_logged_note_only) else summaryLine(items),
                style = CycleTheme.typography.body
            )
        }
        TextButton(stringResource(R.string.today_logged_edit), onClick = onEdit, icon = CycleIcons.Edit)
    }
}

/** The card of what is expected next: the next period or bleed, the last 90 days, or neither. */
@Composable
private fun OutlookCard(
    state: TodayUiState.Tracking,
    onExplain: () -> Unit,
    onWhatChanges: () -> Unit,
    modifier: Modifier
) {
    when (val outlook = state.outlook) {
        is NextPeriod -> NextPeriodCard(outlook, onExplain, modifier)

        is NextBleed -> NextBleedCard(outlook, state.method, onExplain, modifier)

        is TodayOutlook.Bleeding -> state.method?.let { BleedingCard(outlook, it, onWhatChanges, modifier) }

        TodayOutlook.AfterInjection -> EstimateCard(modifier) {
            CardLabel(stringResource(R.string.today_next_period))
            BasicText(
                text = stringResource(R.string.today_after_injection),
                style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurface)
            )
        }

        // Nothing to expect until she logs a bleed: the line under the method says so.
        TodayOutlook.FirstBleedToLog -> Unit
    }
}

/**
 * "Around 23 October, between 19 and 27 October, estimated from ...". TalkBack reads the estimate as
 * one item, "estimated" included, then the button that explains it.
 */
@Composable
private fun NextPeriodCard(next: NextPeriod, onExplain: () -> Unit, modifier: Modifier) {
    EstimateCard(
        modifier,
        button = {
            TextButton(stringResource(R.string.today_how_estimated), onClick = onExplain, icon = CycleIcons.Info)
        }
    ) {
        CardLabel(stringResource(R.string.today_next_period))
        CardHeadline(stringResource(R.string.today_next_around, formatDate(next.expectedStart)))
        if (next.earliestStart != next.latestStart) {
            CardLine(
                stringResource(
                    R.string.today_next_between,
                    formatDate(next.earliestStart),
                    formatDate(next.latestStart)
                )
            )
        }
        CardNote(basisLine(next))
    }
}

/**
 * The next bleed: "24 to 30 May" in the first break, then "Around 21 June, between 19 and 23 June",
 * with where it comes from and, in the first months, that bleeding between breaks is common.
 */
@Composable
private fun NextBleedCard(next: NextBleed, method: TodayMethod?, onExplain: () -> Unit, modifier: Modifier) {
    EstimateCard(
        modifier,
        button = {
            TextButton(stringResource(R.string.today_how_estimated), onClick = onExplain, icon = CycleIcons.Info)
        }
    ) {
        CardLabel(stringResource(R.string.today_next_bleed))
        when (next.basis) {
            BleedBasis.START_DATE -> CardHeadline(dateRange(next.earliestStart, next.latestStart))

            BleedBasis.LAST_BLEED -> {
                CardHeadline(stringResource(R.string.today_next_around, formatDate(next.expectedStart)))
                CardLine(
                    stringResource(
                        R.string.today_next_between,
                        formatDate(next.earliestStart),
                        formatDate(next.latestStart)
                    )
                )
            }
        }
        CardNote(bleedBasisLine(next))
        if (method?.firstMonths == true) firstMonthsLine(method.method, method.breaks)?.let { CardNote(it) }
    }
}

/**
 * "Last 90 days" on a method with no estimate: her bleeding in plain counts, the first-months line
 * while it applies, and what changes on the method.
 */
@Composable
private fun BleedingCard(
    bleeding: TodayOutlook.Bleeding,
    method: TodayMethod,
    onWhatChanges: () -> Unit,
    modifier: Modifier
) {
    EstimateCard(
        modifier,
        button = { TextButton(methodSheetButton(method.method), onClick = onWhatChanges, icon = CycleIcons.Info) }
    ) {
        CardLabel(bleedingTitle(bleeding.summary))
        CardLine(bleedingCounts(bleeding.summary))
        if (method.firstMonths) firstMonthsLine(method.method, method.breaks)?.let { CardNote(it) }
    }
}

/** A card whose [content] TalkBack reads as one item, then its [button]. */
@Composable
private fun EstimateCard(modifier: Modifier, button: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    Card(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)
        ) { content() }
        button()
    }
}

@Composable
private fun CardLabel(text: String) {
    BasicText(text, style = CycleTheme.typography.label.copy(color = CycleTheme.colors.onSurfaceVariant))
}

@Composable
private fun CardHeadline(text: String) {
    BasicText(text, style = CycleTheme.typography.headline.copy(color = CycleTheme.colors.onSurface))
}

@Composable
private fun CardLine(text: String) {
    BasicText(text, style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurface))
}

@Composable
private fun CardNote(text: String) {
    BasicText(text, style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant))
}

/**
 * Where the estimate comes from. After stopping a method, her usual cycle, and that cycles can take a
 * few months to settle, so the range is wider.
 */
@Composable
private fun basisLine(next: NextPeriod): String {
    val basis = when {
        next.stoppedMethod != null -> stringResource(R.string.today_basis_usual, next.cycleLength)

        else -> when (val basis = next.basis) {
            EstimateBasis.Typical -> stringResource(R.string.today_basis_typical, next.cycleLength)
            EstimateBasis.Setup -> stringResource(R.string.today_basis_setup)
            is EstimateBasis.Logged -> pluralStringResource(R.plurals.today_basis_logged, basis.count, basis.count)
        }
    }
    val settling = next.settlingAfter ?: return basis
    return basis + " " + stringResource(R.string.today_basis_settling, methodInSentence(settling))
}

/**
 * "24 to 30 May" ("May 24 to 30" where the month comes first), or "28 May to 3 June" across two
 * months.
 */
@Composable
private fun dateRange(from: LocalDate, to: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val dayFirst = remember(locale) { DateFormat.getBestDateTimePattern(locale, DAY_AND_MONTH).startsWith("d") }
    val sameMonth = from.month == to.month && from.year == to.year
    return stringResource(
        R.string.today_next_bleed_range,
        if (sameMonth && dayFirst) formatDate(from, DAY) else formatDate(from),
        if (sameMonth && !dayFirst) formatDate(to, DAY) else formatDate(to)
    )
}

private const val DAY = "d"

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
