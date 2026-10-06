package com.tonypine.cycle.feature.settings

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.Card
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.OutlinedButton
import com.tonypine.cycle.core.designsystem.TonalButton
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.ui.DAY_MONTH_AND_YEAR
import com.tonypine.cycle.core.ui.DAY_SHORT_MONTH_AND_YEAR
import com.tonypine.cycle.core.ui.calmLine
import com.tonypine.cycle.core.ui.formatDate
import com.tonypine.cycle.core.ui.methodTitle
import com.tonypine.cycle.core.ui.stretchDates
import java.time.LocalDate

/**
 * Settings › Your cycle › Contraception, wired to its [viewModel]. [onAdd] opens "Add your method"
 * or "Change method", [onStop] "Mark as stopped" for a stretch, and [onOpen] a stretch's page.
 */
@Composable
fun ContraceptionRoute(
    viewModel: ContraceptionViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onStop: (id: Long) -> Unit,
    onOpen: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ContraceptionScreen(state, onBack = onBack, onAdd = onAdd, onStop = onStop, onOpen = onOpen, modifier = modifier)
}

/**
 * Her contraception: "Now", a card with her method today, since when (with its breaks on a combined
 * method) and its calm line, then "Mark as stopped" and "Change method", or on none "Add your method".
 * A method whose stop date is still ahead, such as the injection's 13 weeks, says until when Cycle
 * counts it and has no "Mark as stopped". On the combined pill, patch or ring, or an IUD, "When to get
 * help" follows, in full. Then "Your methods", every stretch, the latest first, each a row with the
 * method and its dates that opens its page, and a note that Cycle doesn't advise on methods. Section
 * titles are headings, and each row is one button for TalkBack. Scrolls when the text is large.
 */
@Composable
fun ContraceptionScreen(
    state: ContraceptionUiState?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onStop: (id: Long) -> Unit,
    onOpen: (id: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CycleTheme.spacing
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = stringResource(R.string.contraception_title),
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.settings_back), onBack)
        )
        if (state == null) {
            LoadingState(Modifier.fillMaxSize())
            return
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = spacing.large)
                .padding(top = spacing.small, bottom = spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            SectionTitle(stringResource(R.string.contraception_now))
            NowCard(state.current)
            val current = state.current
            if (current == null) {
                FilledButton(stringResource(R.string.contraception_add), onAdd, Modifier.fillMaxWidth())
            } else {
                TonalButton(stringResource(R.string.contraception_change), onAdd, Modifier.fillMaxWidth())
                if (current.stopped == null) {
                    OutlinedButton(
                        stringResource(R.string.contraception_stop),
                        { onStop(current.id) },
                        Modifier.fillMaxWidth()
                    )
                }
            }
            getHelp(current?.method)?.let { WhenToGetHelp(it) }
            if (state.stretches.isNotEmpty()) {
                SectionTitle(stringResource(R.string.contraception_your_methods))
                state.stretches.forEach { stretch ->
                    SettingsRow(
                        icon = CycleIcons.Medication,
                        title = methodTitle(stretch.method),
                        body = stretchDates(stretch),
                        onClick = { onOpen(stretch.id) },
                        opensPage = true
                    )
                }
            }
            BasicText(
                stringResource(R.string.contraception_note),
                modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
                style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant)
            )
        }
    }
}

/** Her method today with since when and its calm line, or "None" and that Cycle uses her own cycle. */
@Composable
private fun NowCard(current: ContraceptionStretch?) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    Card(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)) {
            BasicText(
                if (current == null) stringResource(R.string.contraception_none) else methodTitle(current.method),
                style = typography.title.copy(color = colors.onSurface)
            )
            if (current != null) {
                BasicText(sinceLine(current), style = typography.bodySmall.copy(color = colors.onSurfaceVariant))
            }
            BasicText(
                calmLine(current?.method, current?.breaks),
                style = typography.body.copy(color = colors.onSurface)
            )
            val stopped = current?.stopped
            if (stopped != null) {
                val until = formatDate(stopped, DAY_MONTH_AND_YEAR)
                BasicText(
                    if (current.method == ContraceptionMethod.INJECTION) {
                        stringResource(R.string.contraception_counts_until_injection, until)
                    } else {
                        stringResource(R.string.contraception_counts_until, until)
                    },
                    style = typography.body.copy(color = colors.onSurface)
                )
            }
        }
    }
}

/** "Since 3 May 2027 · a break every month", or "Start not known". */
@Composable
private fun sinceLine(stretch: ContraceptionStretch): String {
    val since = stretch.started
        ?.let { stringResource(R.string.contraception_since, formatDate(it, DAY_MONTH_AND_YEAR)) }
        ?: stringResource(R.string.contraception_start_not_known)
    val breaks = stretch.breaks ?: return since
    return stringResource(
        R.string.contraception_with_breaks,
        since,
        stringResource(
            when (breaks) {
                Breaks.MONTHLY -> R.string.contraception_breaks_monthly
                Breaks.EVERY_FEW_PACKS -> R.string.contraception_breaks_every_few_packs
                Breaks.NONE -> R.string.contraception_breaks_none
            }
        )
    )
}

/** The Contraception row's line in Settings: "Implant, since 9 Nov 2026", or "None". */
@Composable
internal fun contraceptionSummary(current: ContraceptionStretch?): String {
    if (current == null) return stringResource(R.string.contraception_none)
    val title = methodTitle(current.method)
    return current.started
        ?.let { stringResource(R.string.contraception_row_since, title, formatDate(it, DAY_SHORT_MONTH_AND_YEAR)) }
        ?: stringResource(R.string.contraception_row_start_not_known, title)
}

@Preview
@Composable
private fun ContraceptionPreview() {
    val pill = ContraceptionStretch(
        ContraceptionMethod.COMBINED_PILL,
        started = LocalDate.of(2027, 5, 3),
        stopped = LocalDate.of(2027, 9, 12),
        breaks = Breaks.MONTHLY,
        id = 1
    )
    val iud = ContraceptionStretch(ContraceptionMethod.HORMONAL_IUD, started = LocalDate.of(2027, 9, 13), id = 2)
    CycleTheme {
        ContraceptionScreen(
            ContraceptionUiState(LocalDate.of(2027, 9, 17), current = iud, stretches = listOf(iud, pill)),
            onBack = {},
            onAdd = {},
            onStop = {},
            onOpen = {}
        )
    }
}
