package com.tonypine.cycle.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.MonthCalendar
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.ui.BreaksList
import com.tonypine.cycle.core.ui.MethodChoice
import com.tonypine.cycle.core.ui.MethodList
import com.tonypine.cycle.core.ui.R as CoreUiR
import com.tonypine.cycle.core.ui.breaksQuestion
import com.tonypine.cycle.core.ui.sinceWhenTitle
import java.time.LocalDate
import java.time.YearMonth

/**
 * "Add your method" or "Change method", wired to its [viewModel]: [onBack] leaves from the first
 * step, and the page leaves once saved. Choosing None opens "Mark as stopped" through [onStop], or
 * leaves, as [AddMethodScreen] says.
 */
@Composable
fun AddMethodRoute(
    viewModel: AddMethodViewModel,
    onBack: () -> Unit,
    onStop: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leave by rememberUpdatedState(onBack)
    LaunchedEffect(state?.saved) {
        if (state?.saved == true) leave()
    }
    AddMethodScreen(
        state = state,
        onChoose = viewModel::onChoose,
        onMethodNext = viewModel::onMethodNext,
        onStop = onStop,
        onDone = onBack,
        onPickStart = viewModel::onPickStart,
        onSinceNext = viewModel::onSinceNext,
        onPickBreaks = viewModel::onPickBreaks,
        onSave = viewModel::onSave,
        onBack = { if (!viewModel.onBack()) onBack() },
        onMove = viewModel::onMove,
        onDismissDialog = viewModel::onDismissDialog,
        modifier = modifier
    )
}

/**
 * The steps of adding or changing her method, under a top bar titled "Add your method" (on none) or
 * "Change method":
 * - "Which method?": the method list, nothing chosen at first, with the leaflet line under it after
 *   choosing a pill. Next waits for a choice. On None, Next opens "Mark as stopped" for her method
 *   through [onStop]; with no method, or one that already has a stop date (an injection in its 13
 *   weeks, a method stopped today), it reads Done and calls [onDone], as there is nothing to stop.
 * - Since when: "When did you start the pill?", "Roughly is fine.", and a month of days up to today.
 *   Next on a combined method, Save on the others, once a day is picked.
 * - On a combined pill, patch or ring, its breaks, every month to start with, and Save.
 *
 * Back, on the bar or the system's, goes back a step. A day the method can't start on shows why,
 * and one inside another method's dates asks to move that one's end, from [AddMethodUiState.dialog].
 */
@Composable
fun AddMethodScreen(
    state: AddMethodUiState?,
    onChoose: (MethodChoice) -> Unit,
    onMethodNext: () -> Unit,
    onStop: (id: Long) -> Unit,
    onDone: () -> Unit,
    onPickStart: (LocalDate) -> Unit,
    onSinceNext: () -> Unit,
    onPickBreaks: (Breaks) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onMove: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val adding = state?.current == null
    val title = stringResource(if (adding) R.string.contraception_add else R.string.contraception_change)
    if (state == null) {
        Column(modifier.fillMaxSize()) {
            TopAppBar(
                title = title,
                navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.settings_back), onBack)
            )
            LoadingState(Modifier.fillMaxSize())
        }
        return
    }
    BackHandler(enabled = state.step != AddMethodStep.Method, onBack = onBack)
    val method = (state.choice as? MethodChoice.Method)?.method
    val toStop = state.current?.takeIf { it.stopped == null }
    when (state.step) {
        AddMethodStep.Method -> QuestionPage(
            barTitle = title,
            question = stringResource(R.string.add_method_which),
            body = stringResource(R.string.add_method_which_body),
            onBack = onBack,
            modifier = modifier
        ) {
            MethodList(state.choice, onChoose, Modifier.padding(horizontal = CycleTheme.spacing.large))
            if (method == ContraceptionMethod.COMBINED_PILL || method == ContraceptionMethod.PROGESTOGEN_PILL) {
                PageText(stringResource(CoreUiR.string.method_which_pill))
            }
            PageButton(
                text = stringResource(
                    if (state.choice == MethodChoice.None && toStop == null) {
                        R.string.add_method_done
                    } else {
                        R.string.add_method_next
                    }
                ),
                onClick = {
                    when {
                        state.choice != MethodChoice.None -> onMethodNext()
                        toStop != null -> onStop(toStop.id)
                        else -> onDone()
                    }
                },
                enabled = state.choice != null
            )
        }

        AddMethodStep.Since -> QuestionPage(
            barTitle = title,
            question = method?.let { sinceWhenTitle(it) }.orEmpty(),
            body = stringResource(CoreUiR.string.method_since_body),
            onBack = onBack,
            modifier = modifier
        ) {
            DayPicker(
                today = state.today,
                picked = state.started,
                onPick = onPickStart,
                isEnabled = { it <= state.today }
            )
            val combined = method?.isCombined == true
            PageButton(
                text = stringResource(if (combined) R.string.add_method_next else R.string.add_method_save),
                onClick = onSinceNext,
                enabled = state.started != null
            )
        }

        AddMethodStep.Breaks -> QuestionPage(
            barTitle = title,
            question = method?.let { breaksQuestion(it) }.orEmpty(),
            body = null,
            onBack = onBack,
            modifier = modifier
        ) {
            if (method != null) {
                BreaksList(method, state.breaks, onPickBreaks, Modifier.padding(horizontal = CycleTheme.spacing.large))
            }
            PageButton(stringResource(R.string.add_method_save), onSave, enabled = state.breaks != null)
        }
    }
    StretchDialogs(state.dialog, onMove = onMove, onDismiss = onDismissDialog)
}

/**
 * A page that asks one thing: a top bar with Back and [barTitle], the [question] as a heading with an
 * optional [body] under it, then [content]. Scrolls, as at 200% font scale or on a phone on its side.
 */
@Composable
internal fun QuestionPage(
    barTitle: String,
    question: String,
    body: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val spacing = CycleTheme.spacing
    val typography = CycleTheme.typography
    val colors = CycleTheme.colors
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = barTitle,
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.settings_back), onBack)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
                .padding(vertical = spacing.large),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            Column(
                Modifier.padding(horizontal = spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                BasicText(
                    question,
                    modifier = Modifier.semantics { heading() },
                    style = typography.headline.copy(color = colors.onSurface)
                )
                if (body != null) BasicText(body, style = typography.body.copy(color = colors.onSurfaceVariant))
            }
            content()
        }
    }
}

/** A line of text on a [QuestionPage], in its margins. */
@Composable
internal fun PageText(text: String) {
    BasicText(
        text,
        modifier = Modifier.padding(horizontal = CycleTheme.spacing.large),
        style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant)
    )
}

/** A [QuestionPage]'s button, full width under its content. */
@Composable
internal fun PageButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    FilledButton(
        text = text,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CycleTheme.spacing.large),
        enabled = enabled
    )
}

/**
 * A month of days to pick one from, opening on the month of [picked] or else [today]'s. Days where
 * [isEnabled] is false ignore taps. The month shown is kept through a configuration change. On a
 * page it sits in 12dp margins; in a sheet, pass [sheetCalendarMargins].
 */
@Composable
internal fun DayPicker(
    today: LocalDate,
    picked: LocalDate?,
    onPick: (LocalDate) -> Unit,
    isEnabled: (LocalDate) -> Boolean,
    modifier: Modifier = Modifier.padding(horizontal = CycleTheme.spacing.medium)
) {
    var month by rememberSaveable { mutableStateOf(YearMonth.from(picked ?: today)) }
    MonthCalendar(
        month = month,
        stateOf = { CycleDayState.Plain },
        onDayClick = onPick,
        today = today,
        onPreviousMonth = { month = month.minusMonths(1) },
        onNextMonth = { month = month.plusMonths(1) },
        modifier = modifier,
        selected = picked,
        isEnabled = isEnabled
    )
}

/**
 * Widens a calendar into a sheet's 24dp padding, to the 12dp margins calendars use, so seven 48dp
 * days fit a 360dp screen without scrolling sideways, as on Today's sheets.
 */
@Composable
internal fun Modifier.sheetCalendarMargins(): Modifier {
    val bleed = CycleTheme.spacing.extraLarge - CycleTheme.spacing.medium
    return layout { measurable, constraints ->
        val extra = bleed.roundToPx() * 2
        val wider = if (constraints.hasBoundedWidth) {
            constraints.copy(
                maxWidth = constraints.maxWidth + extra
            )
        } else {
            constraints
        }
        val placeable = measurable.measure(wider.copy(minWidth = minOf(wider.minWidth + extra, wider.maxWidth)))
        layout((placeable.width - extra).coerceAtLeast(0), placeable.height) { placeable.place(-extra / 2, 0) }
    }
}
