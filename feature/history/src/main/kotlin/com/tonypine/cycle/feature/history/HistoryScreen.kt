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
import com.tonypine.cycle.core.designsystem.TextButton
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.ui.stretchDates
import java.time.LocalDate
import java.time.YearMonth

/** History, wired to its [viewModel]. The day is read again whenever the screen resumes. */
@Composable
fun HistoryRoute(
    viewModel: HistoryViewModel,
    onCycleClick: (start: LocalDate) -> Unit,
    onSeeInCalendar: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDay()
        onPauseOrDispose {}
    }
    HistoryScreen(state, onCycleClick, onSeeInCalendar, modifier)
}

/**
 * History: her typical cycle, then her cycles, the current one first and the past ones newest first,
 * each a card that opens its details. Her time on a hormonal method is one card among them, marked
 * with the method, with its bleeding and a button that opens the calendar on it
 * ([onSeeInCalendar]). Before her first complete cycle or method, an empty state.
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onCycleClick: (start: LocalDate) -> Unit,
    onSeeInCalendar: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(title = stringResource(R.string.history_title))
        val content = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        when (state) {
            HistoryUiState.Loading -> LoadingState(content)
            is HistoryUiState.Empty -> EmptyHistory(state, content)
            is HistoryUiState.Cycles -> CycleList(state, onCycleClick, onSeeInCalendar, content)
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
private fun CycleList(
    state: HistoryUiState.Cycles,
    onCycleClick: (start: LocalDate) -> Unit,
    onSeeInCalendar: (YearMonth) -> Unit,
    modifier: Modifier
) {
    val spacing = CycleTheme.spacing
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = spacing.large, end = spacing.large, bottom = spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        state.typicalCycle?.let { cycle ->
            item(key = "typical") { TypicalCard(cycle, state.typicalPeriod, state.leftOut, Modifier.fillMaxWidth()) }
        }
        item(key = "cycles") {
            BasicText(
                text = stringResource(R.string.history_cycles),
                // Apart from the typical cycle above it; at the top when there is none.
                modifier = Modifier
                    .then(if (state.typicalCycle == null) Modifier else Modifier.padding(top = spacing.medium))
                    .semantics { heading() },
                style = CycleTheme.typography.title.copy(color = CycleTheme.colors.onSurface)
            )
        }
        items(state.entries, key = { it.key }) { entry ->
            when (entry) {
                is CycleSummary -> CycleCard(
                    entry,
                    state.today,
                    onClick = { onCycleClick(entry.start) },
                    Modifier.fillMaxWidth()
                )

                is MethodSummary -> MethodCard(
                    entry,
                    state.today,
                    onSeeInCalendar = { onSeeInCalendar(entry.month) },
                    Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * "Your typical cycle": the middle of her recent cycle and period lengths, each with her shortest to
 * longest. TalkBack reads each length as one item.
 */
@Composable
private fun TypicalCard(cycle: LengthSummary, period: LengthSummary?, leftOut: Boolean, modifier: Modifier) {
    Card(modifier) {
        BasicText(
            text = stringResource(R.string.history_typical_title),
            modifier = Modifier.semantics { heading() },
            style = CycleTheme.typography.titleSmall.copy(color = CycleTheme.colors.onSurface)
        )
        TypicalLength(stringResource(R.string.history_typical_cycle), cycle)
        period?.let { TypicalLength(stringResource(R.string.history_typical_period), it) }
        BasicText(
            text = pluralStringResource(
                if (leftOut) R.plurals.history_typical_basis_left_out else R.plurals.history_typical_basis,
                cycle.count,
                cycle.count
            ),
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
 * One cycle: its dates, its length and its period's length, or for a cycle a method cut short, which
 * method and that it is not counted. The whole card is one button: TalkBack reads it all, then
 * "button".
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
        when (val cutShortBy = cycle.cutShortBy) {
            null -> {
                BasicText(
                    text = if (cycle.isCurrent) {
                        stringResource(R.string.history_day_so_far, cycle.length)
                    } else {
                        pluralStringResource(R.plurals.history_days, cycle.length, cycle.length)
                    },
                    style = typography.body.copy(color = colors.onSurface)
                )
                BasicText(
                    text = periodLength(cycle),
                    style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
                )
            }

            else -> BasicText(
                text = pluralStringResource(
                    R.plurals.history_cut_short,
                    cycle.length,
                    cycle.length,
                    cutShort(cutShortBy)
                ),
                style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
            )
        }
    }
}

/**
 * Her time on one hormonal method: its short name, its dates, that it is not part of her typical
 * cycle, and her bleeding on it; then "See it in the calendar". TalkBack reads the text as one item,
 * the method's name first, then the button.
 */
@Composable
private fun MethodCard(method: MethodSummary, today: LocalDate, onSeeInCalendar: () -> Unit, modifier: Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    Card(modifier) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
        ) {
            BasicText(
                text = methodName(method.stretch.method),
                style = typography.label.copy(color = colors.accent)
            )
            BasicText(
                text = stretchDates(method.stretch),
                style = typography.titleSmall.copy(color = colors.onSurface)
            )
            BasicText(
                text = stringResource(R.string.history_method_left_out),
                style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
            )
            BasicText(
                text = bleedingOn(method, today),
                style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
            )
        }
        TextButton(
            text = stringResource(R.string.cycle_see_in_calendar),
            onClick = onSeeInCalendar,
            icon = CycleIcons.Calendar
        )
    }
}

/**
 * Her bleeding on [method], in plain counts: "Last 90 days: you logged bleeding or spotting on 12
 * days, in 4 episodes. The longest lasted 6 days.", or on a combined method with a break every
 * month, "You logged 6 bleeds on it."
 */
@Composable
private fun bleedingOn(method: MethodSummary, today: LocalDate): String = when (val bleeding = method.bleeding) {
    is MethodBleeding.Bleeds -> when (bleeding.count) {
        0 -> stringResource(R.string.history_bleeds_none)
        else -> pluralStringResource(R.plurals.history_bleeds, bleeding.count, bleeding.count)
    }

    is MethodBleeding.Days -> {
        val summary = bleeding.summary
        val since = formatDate(summary.from, today)
        if (summary.days == 0) {
            when {
                method.isCurrent && summary.sinceStart -> stringResource(R.string.history_bleeding_none_since, since)
                method.isCurrent -> stringResource(R.string.history_bleeding_none_last_90)
                summary.sinceStart -> stringResource(R.string.history_bleeding_none_stopped_all)
                else -> stringResource(R.string.history_bleeding_none_stopped_last_90)
            }
        } else {
            val days = pluralStringResource(R.plurals.history_bleeding_days, summary.days, summary.days)
            val episodes = pluralStringResource(R.plurals.history_bleeding_episodes, summary.episodes, summary.episodes)
            val logged = when {
                method.isCurrent && summary.sinceStart ->
                    stringResource(R.string.history_bleeding_since, days, episodes, since)

                method.isCurrent -> stringResource(R.string.history_bleeding_last_90, days, episodes)

                summary.sinceStart -> stringResource(R.string.history_bleeding_stopped_all, days, episodes)

                else -> stringResource(R.string.history_bleeding_stopped_last_90, days, episodes)
            }
            val longest = pluralStringResource(R.plurals.history_bleeding_longest, summary.longest, summary.longest)
            "$logged $longest"
        }
    }
}

/** The method's short name, as History's card is labelled: "Implant", "Pill". */
@Composable
private fun methodName(method: ContraceptionMethod): String = stringResource(
    when (method) {
        ContraceptionMethod.COMBINED_PILL -> R.string.history_method_combined_pill
        ContraceptionMethod.PROGESTOGEN_PILL -> R.string.history_method_progestogen_pill
        ContraceptionMethod.PATCH -> R.string.history_method_patch
        ContraceptionMethod.RING -> R.string.history_method_ring
        ContraceptionMethod.IMPLANT -> R.string.history_method_implant
        ContraceptionMethod.HORMONAL_IUD -> R.string.history_method_hormonal_iud
        ContraceptionMethod.COPPER_IUD -> R.string.history_method_copper_iud
        ContraceptionMethod.INJECTION -> R.string.history_method_injection
    }
)

/** How [method] cut a cycle short: "when the implant was fitted". */
@Composable
internal fun cutShort(method: ContraceptionMethod): String = stringResource(
    when (method) {
        ContraceptionMethod.COMBINED_PILL -> R.string.history_cut_short_combined_pill

        ContraceptionMethod.PROGESTOGEN_PILL -> R.string.history_cut_short_progestogen_pill

        ContraceptionMethod.PATCH -> R.string.history_cut_short_patch

        ContraceptionMethod.RING -> R.string.history_cut_short_ring

        ContraceptionMethod.IMPLANT -> R.string.history_cut_short_implant

        // A copper IUD never cuts a cycle short: its cycles are her own.
        ContraceptionMethod.HORMONAL_IUD, ContraceptionMethod.COPPER_IUD -> R.string.history_cut_short_iud

        ContraceptionMethod.INJECTION -> R.string.history_cut_short_injection
    }
)

/** A key for the list, unique among cycles and methods. */
private val HistoryEntry.key: String
    get() = when (this) {
        is CycleSummary -> "cycle-$start"
        is MethodSummary -> "method-${stretch.id}-${stretch.startKey}"
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
    CycleTheme { HistoryScreen(HistorySamples.cycles, onCycleClick = {}, onSeeInCalendar = {}) }
}

@Preview
@Composable
private fun HistoryDarkPreview() {
    CycleTheme(darkTheme = true) { HistoryScreen(HistorySamples.cycles, onCycleClick = {}, onSeeInCalendar = {}) }
}

@Preview
@Composable
private fun HistoryFirstCyclePreview() {
    CycleTheme { HistoryScreen(HistorySamples.firstCycle, onCycleClick = {}, onSeeInCalendar = {}) }
}

@Preview
@Composable
private fun HistoryOnImplantPreview() {
    CycleTheme { HistoryScreen(HistorySamples.onImplant, onCycleClick = {}, onSeeInCalendar = {}) }
}
