package com.tonypine.cycle.feature.settings

import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.SwitchRow
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.model.LogCategory

/** What to log, wired to its [viewModel]. [onBack] leaves it, from the app bar. */
@Composable
fun WhatToLogRoute(viewModel: WhatToLogViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    WhatToLogScreen(state, onShownChange = viewModel::onShownChange, onBack = onBack, modifier = modifier)
}

/**
 * What to log: a [SwitchRow] per day log category, in the sheet's order. Flow and spotting come
 * first and stay on, because the estimates need them. Turning a category off hides it from the day
 * log and Today's summary; nothing she logged is deleted. Scrolls when the text is large.
 */
@Composable
fun WhatToLogScreen(
    state: WhatToLogUiState,
    onShownChange: (category: LogCategory, shown: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CycleTheme.spacing
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = stringResource(R.string.what_to_log_title),
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.what_to_log_back), onBack)
        )
        when (state) {
            WhatToLogUiState.Loading -> LoadingState(Modifier.fillMaxSize())

            is WhatToLogUiState.Ready -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = spacing.large)
                    .padding(top = spacing.small, bottom = spacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                BasicText(
                    stringResource(R.string.what_to_log_intro),
                    modifier = Modifier.padding(bottom = spacing.small),
                    style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurfaceVariant)
                )
                SwitchRow(
                    title = stringResource(R.string.what_to_log_flow),
                    checked = true,
                    onCheckedChange = {},
                    body = stringResource(R.string.what_to_log_flow_body),
                    enabled = false
                )
                LogCategory.entries.forEach { category ->
                    SwitchRow(
                        title = stringResource(category.title),
                        checked = state.isShown(category),
                        onCheckedChange = { onShownChange(category, it) },
                        body = stringResource(category.body)
                    )
                }
            }
        }
    }
}

private val LogCategory.title: Int
    @StringRes get() = when (this) {
        LogCategory.PAIN -> R.string.what_to_log_pain
        LogCategory.BODY -> R.string.what_to_log_body
        LogCategory.MOOD -> R.string.what_to_log_mood
        LogCategory.ENERGY -> R.string.what_to_log_energy
        LogCategory.SLEEP -> R.string.what_to_log_sleep
        LogCategory.SEX -> R.string.what_to_log_sex
        LogCategory.NOTES -> R.string.what_to_log_notes
    }

private val LogCategory.body: Int
    @StringRes get() = when (this) {
        LogCategory.PAIN -> R.string.what_to_log_pain_body
        LogCategory.BODY -> R.string.what_to_log_body_body
        LogCategory.MOOD -> R.string.what_to_log_mood_body
        LogCategory.ENERGY -> R.string.what_to_log_energy_body
        LogCategory.SLEEP -> R.string.what_to_log_sleep_body
        LogCategory.SEX -> R.string.what_to_log_sex_body
        LogCategory.NOTES -> R.string.what_to_log_notes_body
    }

@Preview
@Composable
private fun WhatToLogPreview() {
    CycleTheme { WhatToLogScreen(WhatToLogUiState.Ready(setOf(LogCategory.SEX)), { _, _ -> }, {}) }
}
