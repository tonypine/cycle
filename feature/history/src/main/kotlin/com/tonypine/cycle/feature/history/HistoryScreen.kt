package com.tonypine.cycle.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.Card
import com.tonypine.cycle.core.designsystem.ClickableCard
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.EmptyState
import com.tonypine.cycle.core.designsystem.EmptyStateIcon
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.model.LengthSummary
import java.time.LocalDate

/** History, wired to its [viewModel]. The day is read again whenever the screen resumes. */
@Composable
fun HistoryRoute(viewModel: HistoryViewModel, onCycleClick: (start: LocalDate) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDay()
        onPauseOrDispose {}
    }
    HistoryScreen(state, onCycleClick, modifier)
}

/**
 * History: her typical cycle, then her cycles, the current one first and the past ones newest first,
 * each a card that opens its details. Before her first complete cycle, an empty state.
 */
@Composable
fun HistoryScreen(state: HistoryUiState, onCycleClick: (start: LocalDate) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(title = stringResource(R.string.history_title))
        val content = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        when (state) {
            HistoryUiState.Loading -> LoadingState(content)
            is HistoryUiState.Empty -> EmptyHistory(state, content)
            is HistoryUiState.Cycles -> CycleList(state, onCycleClick, content)
        }
    }
}

@Composable
private fun EmptyHistory(state: HistoryUiState.Empty, modifier: Modifier) {
    EmptyState(
        title = stringResource(
            if (state.hasPeriod) R.string.history_first_cycle_title else R.string.history_empty_title
        ),
        body = stringResource(if (state.hasPeriod) R.string.history_first_cycle_body else R.string.history_empty_body),
        modifier = modifier,
        illustration = { EmptyStateIcon(CycleIcons.History) }
    )
}

@Composable
private fun CycleList(state: HistoryUiState.Cycles, onCycleClick: (start: LocalDate) -> Unit, modifier: Modifier) {
    val spacing = CycleTheme.spacing
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = spacing.large, end = spacing.large, bottom = spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        item(key = "typical") { TypicalCard(state.typicalCycle, state.typicalPeriod, Modifier.fillMaxWidth()) }
        item(key = "cycles") {
            BasicText(
                text = stringResource(R.string.history_cycles),
                modifier = Modifier
                    .padding(top = spacing.medium)
                    .semantics { heading() },
                style = CycleTheme.typography.title.copy(color = CycleTheme.colors.onSurface)
            )
        }
        items(state.cycles, key = { it.start }) { cycle ->
            CycleCard(cycle, state.today, onClick = { onCycleClick(cycle.start) }, Modifier.fillMaxWidth())
        }
    }
}

/**
 * "Your typical cycle": the middle of her recent cycle and period lengths, each with her shortest to
 * longest. TalkBack reads each length as one item.
 */
@Composable
private fun TypicalCard(cycle: LengthSummary, period: LengthSummary?, modifier: Modifier) {
    Card(modifier) {
        BasicText(
            text = stringResource(R.string.history_typical_title),
            modifier = Modifier.semantics { heading() },
            style = CycleTheme.typography.titleSmall.copy(color = CycleTheme.colors.onSurface)
        )
        TypicalLength(stringResource(R.string.history_typical_cycle), cycle)
        period?.let { TypicalLength(stringResource(R.string.history_typical_period), it) }
        BasicText(
            text = pluralStringResource(R.plurals.history_typical_basis, cycle.count, cycle.count),
            style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant)
        )
    }
}

@Composable
private fun TypicalLength(label: String, summary: LengthSummary) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
    ) {
        BasicText(text = label, style = typography.label.copy(color = colors.onSurfaceVariant))
        BasicText(
            text = pluralStringResource(R.plurals.history_days, summary.median, summary.median),
            style = typography.headline.copy(color = colors.onSurface)
        )
        if (summary.shortest != summary.longest) {
            BasicText(
                text = pluralStringResource(
                    R.plurals.history_range,
                    summary.longest,
                    summary.shortest,
                    summary.longest
                ),
                style = typography.body.copy(color = colors.onSurface)
            )
        }
    }
}

/**
 * One cycle: its dates, its length and its period's length. The whole card is one button: TalkBack
 * reads it all, then "button".
 */
@Composable
private fun CycleCard(cycle: CycleSummary, today: LocalDate, onClick: () -> Unit, modifier: Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    ClickableCard(onClick = onClick, modifier = modifier, onClickLabel = stringResource(R.string.history_open_cycle)) {
        val start = formatDate(cycle.start, today, short = true)
        if (cycle.isCurrent) {
            BasicText(
                text = stringResource(R.string.history_current_cycle),
                style = typography.label.copy(color = colors.onSurfaceVariant)
            )
        }
        BasicText(
            text = when (val end = cycle.end) {
                null -> stringResource(R.string.history_since, start)
                else -> stringResource(R.string.history_dates, start, formatDate(end, today, short = true))
            },
            style = typography.titleSmall.copy(color = colors.onSurface)
        )
        BasicText(
            text = if (cycle.isCurrent) {
                stringResource(R.string.history_day_so_far, cycle.length)
            } else {
                pluralStringResource(R.plurals.history_days, cycle.length, cycle.length)
            },
            style = typography.body.copy(color = colors.onSurface)
        )
        BasicText(text = periodLength(cycle), style = typography.bodySmall.copy(color = colors.onSurfaceVariant))
    }
}

/** "Period: 5 days", or "Period: 3 days so far" while it is still going. */
@Composable
internal fun periodLength(cycle: CycleSummary): String {
    val length = cycle.period.length
    val plural = if (cycle.period.isOpen) R.plurals.history_period_so_far else R.plurals.history_period_length
    return pluralStringResource(plural, length, length)
}

@Preview
@Composable
private fun HistoryPreview() {
    CycleTheme { HistoryScreen(HistorySamples.cycles, onCycleClick = {}) }
}

@Preview
@Composable
private fun HistoryDarkPreview() {
    CycleTheme(darkTheme = true) { HistoryScreen(HistorySamples.cycles, onCycleClick = {}) }
}

@Preview
@Composable
private fun HistoryFirstCyclePreview() {
    CycleTheme { HistoryScreen(HistorySamples.firstCycle, onCycleClick = {}) }
}
