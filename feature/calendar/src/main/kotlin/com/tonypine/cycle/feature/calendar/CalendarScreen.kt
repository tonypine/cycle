package com.tonypine.cycle.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleLegend
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.MonthCalendar
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.designsystem.rememberCycleBottomSheetState
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.ui.DayLogSheet
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

/**
 * The calendar, wired to its [viewModel]. The day is read again whenever the screen resumes. A
 * [requestedMonth], from Today's "Add a past period", is shown once and then handed back through
 * [onMonthShown].
 */
@Composable
fun CalendarRoute(
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier,
    requestedMonth: YearMonth? = null,
    onMonthShown: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDay()
        onPauseOrDispose {}
    }
    LaunchedEffect(requestedMonth) {
        if (requestedMonth != null) {
            viewModel.showMonth(requestedMonth)
            onMonthShown()
        }
    }
    CalendarScreen(
        state = state,
        actions = remember(viewModel) {
            CalendarActions(
                onPreviousMonth = viewModel::onPreviousMonth,
                onNextMonth = viewModel::onNextMonth,
                onGoToToday = viewModel::onGoToToday,
                onDayClick = viewModel::onDayClick,
                onLogDay = viewModel::onLogDay,
                onFillPeriod = viewModel::onFillPeriod,
                onClearDay = viewModel::onClearDay
            )
        },
        modifier = modifier
    )
}

/** What the calendar's buttons, days and sheet do. Each defaults to nothing, for previews and screenshots. */
class CalendarActions(
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onGoToToday: () -> Unit = {},
    val onDayClick: (date: LocalDate) -> Unit = {},
    val onLogDay: (date: LocalDate, flow: FlowLevel?, feelings: DayFeelings) -> Unit = { _, _, _ -> },
    val onFillPeriod: (start: LocalDate) -> Unit = {},
    val onClearDay: (date: LocalDate) -> Unit = {}
)

/**
 * The calendar: a top bar with "Go to today", the month with her logged periods, the next three
 * estimated ones (expected bleeds on a combined method with monthly breaks, nothing on a method with no
 * estimate), today and the selected day, the legend of what the month shows, in her method's words,
 * and a line saying the predictions are estimates, or why there are none. Tapping a day up to today selects it and opens its day log sheet; the days after today
 * show their estimates in full colour and do nothing. Scrolls when the text is large.
 */
@Composable
fun CalendarScreen(state: CalendarUiState, actions: CalendarActions, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(CycleTheme.colors.surface)) {
        TopAppBar(
            title = stringResource(R.string.calendar_title),
            actions = listOf(
                AppBarAction(CycleIcons.Today, stringResource(R.string.calendar_go_to_today), actions.onGoToToday)
            )
        )
        when (state) {
            CalendarUiState.Loading -> LoadingState(Modifier.fillMaxSize())
            is CalendarUiState.Ready -> Month(state, actions)
        }
    }
}

@Composable
private fun Month(state: CalendarUiState.Ready, actions: CalendarActions) {
    val sheet = rememberCycleBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val spacing = CycleTheme.spacing
    val margin = Modifier.padding(horizontal = spacing.large)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(vertical = spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.large)
    ) {
        // Calendars sit 12dp in, so seven 48dp days fit a 360dp screen.
        MonthCalendar(
            month = state.month,
            stateOf = state.days::stateOf,
            onDayClick = { date ->
                actions.onDayClick(date)
                scope.launch { sheet.show() }
            },
            today = state.today,
            onPreviousMonth = actions.onPreviousMonth,
            onNextMonth = actions.onNextMonth,
            modifier = Modifier.padding(horizontal = spacing.medium),
            selected = state.selected,
            isEnabled = state::canLog,
            wordsOf = state.days::wordsOf
        )
        val legend = state.legend
        CycleLegend(margin, entries = legend.entries, words = legend.words, predictedWords = legend.predictedWords)
        BasicText(
            text = hint(state.hint),
            modifier = margin,
            style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant)
        )
    }
    state.selectedLog?.let { entry ->
        DayLogSheet(sheet, entry, actions.onLogDay, actions.onFillPeriod, actions.onClearDay)
    }
}

/** The line under the legend: what is estimated, or why nothing is, and that past days can be logged. */
@Composable
private fun hint(hint: CalendarHint): String = stringResource(
    when (hint) {
        CalendarHint.Periods -> R.string.calendar_hint

        CalendarHint.Bleeds -> R.string.calendar_hint_bleeds

        CalendarHint.FirstBleed -> R.string.calendar_hint_first_bleed

        CalendarHint.AfterInjection -> R.string.calendar_hint_after_injection

        is CalendarHint.NoEstimate -> when (hint.method) {
            ContraceptionMethod.COMBINED_PILL -> R.string.calendar_hint_none_pill

            ContraceptionMethod.PROGESTOGEN_PILL -> R.string.calendar_hint_none_mini_pill

            ContraceptionMethod.PATCH -> R.string.calendar_hint_none_patch

            ContraceptionMethod.RING -> R.string.calendar_hint_none_ring

            ContraceptionMethod.IMPLANT -> R.string.calendar_hint_none_implant

            ContraceptionMethod.HORMONAL_IUD -> R.string.calendar_hint_none_hormonal_iud

            ContraceptionMethod.INJECTION -> R.string.calendar_hint_none_injection

            // A copper IUD keeps her estimates, so never comes here.
            ContraceptionMethod.COPPER_IUD -> R.string.calendar_hint
        }
    }
)

@Preview
@Composable
private fun CalendarPreview() {
    CycleTheme { CalendarScreen(CalendarSamples.march, CalendarActions()) }
}

@Preview
@Composable
private fun CalendarDarkPreview() {
    CycleTheme(darkTheme = true) { CalendarScreen(CalendarSamples.march, CalendarActions()) }
}
