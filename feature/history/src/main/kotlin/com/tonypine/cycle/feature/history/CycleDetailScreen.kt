package com.tonypine.cycle.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.Card
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.DayCell
import com.tonypine.cycle.core.designsystem.EmptyState
import com.tonypine.cycle.core.designsystem.EmptyStateIcon
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.TonalButton
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.ui.label
import com.tonypine.cycle.core.ui.painSummary
import com.tonypine.cycle.core.ui.summaryLabel
import java.time.YearMonth

/** A cycle's details, wired to its [viewModel]. The day is read again whenever the screen resumes. */
@Composable
fun CycleDetailRoute(
    viewModel: CycleDetailViewModel,
    onBack: () -> Unit,
    onSeeInCalendar: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDay()
        onPauseOrDispose {}
    }
    CycleDetailScreen(state, onBack, onSeeInCalendar, modifier)
}

/**
 * One cycle: its length and dates, its period's dates and the flow of each period day, how she felt
 * and her notes, and a button that opens the calendar on the month it started in. Scrolls when the
 * text is large.
 */
@Composable
fun CycleDetailScreen(
    state: CycleDetailUiState,
    onBack: () -> Unit,
    onSeeInCalendar: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCurrent = (state as? CycleDetailUiState.Detail)?.cycle?.isCurrent == true
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = stringResource(if (isCurrent) R.string.history_current_cycle else R.string.cycle_title),
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.cycle_back), onBack)
        )
        val content = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        when (state) {
            CycleDetailUiState.Loading -> LoadingState(content)

            CycleDetailUiState.Missing -> EmptyState(
                title = stringResource(R.string.cycle_missing_title),
                body = stringResource(R.string.cycle_missing_body),
                modifier = content,
                illustration = { EmptyStateIcon(CycleIcons.History) }
            )

            is CycleDetailUiState.Detail -> Detail(state, onSeeInCalendar, content)
        }
    }
}

@Composable
private fun Detail(state: CycleDetailUiState.Detail, onSeeInCalendar: (YearMonth) -> Unit, modifier: Modifier) {
    val spacing = CycleTheme.spacing
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = spacing.large, end = spacing.large, bottom = spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Summary(state, Modifier.fillMaxWidth())
        PeriodCard(state, Modifier.fillMaxWidth())
        if (state.symptoms.isNotEmpty()) SymptomsCard(state.symptoms, Modifier.fillMaxWidth())
        if (state.notes.isNotEmpty()) NotesCard(state.notes, Modifier.fillMaxWidth())
        TonalButton(
            text = stringResource(R.string.cycle_see_in_calendar),
            onClick = { onSeeInCalendar(state.cycle.month) },
            modifier = Modifier.fillMaxWidth(),
            icon = CycleIcons.Calendar
        )
    }
}

/** "28 days, February 2 to March 1", read by TalkBack as one item. */
@Composable
private fun Summary(state: CycleDetailUiState.Detail, modifier: Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    val cycle = state.cycle
    Card(modifier) {
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            BasicText(
                text = if (cycle.isCurrent) {
                    stringResource(R.string.history_day_so_far, cycle.length)
                } else {
                    pluralStringResource(R.plurals.history_days, cycle.length, cycle.length)
                },
                modifier = Modifier.semantics { heading() },
                style = typography.headline.copy(color = colors.onSurface)
            )
            val start = formatDate(cycle.start, state.today)
            BasicText(
                text = when (val end = cycle.end) {
                    null -> stringResource(R.string.history_since, start)
                    else -> stringResource(R.string.history_dates, start, formatDate(end, state.today))
                },
                style = typography.body.copy(color = colors.onSurface)
            )
        }
    }
}

/** The period's dates and length, then each of its days with the flow she logged. */
@Composable
private fun PeriodCard(state: CycleDetailUiState.Detail, modifier: Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    val period = state.cycle.period
    Card(modifier) {
        BasicText(
            text = stringResource(R.string.cycle_period),
            modifier = Modifier.semantics { heading() },
            style = typography.titleSmall.copy(color = colors.onSurface)
        )
        Column(Modifier.semantics(mergeDescendants = true) {}) {
            BasicText(
                text = stringResource(
                    R.string.history_dates,
                    formatDate(period.start, state.today),
                    formatDate(period.end, state.today)
                ),
                style = typography.body.copy(color = colors.onSurface)
            )
            BasicText(
                text = periodLength(state.cycle),
                style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
            )
        }
        BasicText(
            text = stringResource(R.string.cycle_flow),
            modifier = Modifier.padding(top = CycleTheme.spacing.small),
            style = typography.label.copy(color = colors.onSurfaceVariant)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall),
            verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
        ) {
            state.flow.forEach { day -> FlowDayCell(day, isToday = day.date == state.today) }
        }
    }
}

/** A period day as a [DayCell] with its flow under it. TalkBack reads both as one item. */
@Composable
private fun FlowDayCell(day: FlowDay, isToday: Boolean) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DayCell(date = day.date, state = CycleDayState.Period, onClick = null, isToday = isToday)
        BasicText(
            text = stringResource(day.flow.label),
            style = CycleTheme.typography.labelSmall.copy(color = CycleTheme.colors.onSurfaceVariant)
        )
    }
}

/**
 * "How you felt": each thing she logged and the days of the cycle it fell on, "Bloating, days
 * 24–27". TalkBack reads each one with its days as one item, a range as "24 to 27".
 */
@Composable
private fun SymptomsCard(symptoms: List<SymptomDays>, modifier: Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    Card(modifier) {
        SectionTitle(stringResource(R.string.cycle_felt))
        symptoms.forEach { item ->
            val label = item.symptom.label()
            val spoken = stringResource(R.string.cycle_symptom_spoken, label, cycleDays(item.days, spoken = true))
            Column(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken }) {
                BasicText(text = label, style = typography.body.copy(color = colors.onSurface))
                BasicText(
                    text = cycleDays(item.days, spoken = false),
                    style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
                )
            }
        }
    }
}

/** Her notes, each under the day of the cycle she wrote it on, read by TalkBack as one item. */
@Composable
private fun NotesCard(notes: List<CycleNote>, modifier: Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    Card(modifier) {
        SectionTitle(stringResource(R.string.cycle_notes))
        notes.forEach { note ->
            Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
                BasicText(
                    text = stringResource(R.string.cycle_note_day, note.day),
                    style = typography.label.copy(color = colors.onSurfaceVariant)
                )
                BasicText(text = note.text, style = typography.body.copy(color = colors.onSurface))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text = text,
        modifier = Modifier.semantics { heading() },
        style = CycleTheme.typography.titleSmall.copy(color = CycleTheme.colors.onSurface)
    )
}

/** "Days 1, 2, 24–27", or with [spoken] "Days 1, 2, 24 to 27" for TalkBack. */
@Composable
private fun cycleDays(days: List<Int>, spoken: Boolean): String {
    val range = if (spoken) R.string.cycle_day_range_spoken else R.string.cycle_day_range
    val list = dayRuns(days).map { run ->
        if (run.first == run.last) run.first.toString() else stringResource(range, run.first, run.last)
    }.joinToString(", ")
    return pluralStringResource(R.plurals.cycle_days, days.size, list)
}

/** The symptom in a few words, as Today's summary says it: "Cramps, moderate", "Low energy". */
@Composable
private fun Symptom.label(): String = when (this) {
    is Symptom.Pain -> painSummary(Pain(level, setOfNotNull(place)))
    is Symptom.Body -> stringResource(symptom.label)
    is Symptom.Feeling -> stringResource(mood.label)
    is Symptom.Energy -> stringResource(level.summaryLabel)
    is Symptom.Sleep -> stringResource(quality.label)
    is Symptom.Sex -> stringResource(activity.summaryLabel)
}

private val FlowLevel?.label: Int
    get() = when (this) {
        null -> R.string.cycle_flow_not_logged
        FlowLevel.NONE -> R.string.cycle_flow_none
        FlowLevel.SPOTTING -> R.string.cycle_flow_spotting
        FlowLevel.LIGHT -> R.string.cycle_flow_light
        FlowLevel.MEDIUM -> R.string.cycle_flow_medium
        FlowLevel.HEAVY -> R.string.cycle_flow_heavy
    }

@Preview
@Composable
private fun CycleDetailPreview() {
    CycleTheme { CycleDetailScreen(HistorySamples.pastCycle, onBack = {}, onSeeInCalendar = {}) }
}

@Preview
@Composable
private fun CycleDetailDarkPreview() {
    CycleTheme(darkTheme = true) { CycleDetailScreen(HistorySamples.currentCycle, onBack = {}, onSeeInCalendar = {}) }
}
