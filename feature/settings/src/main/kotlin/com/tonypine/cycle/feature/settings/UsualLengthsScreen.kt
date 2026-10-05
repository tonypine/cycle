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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.ui.UsualLengthSliders

/** "Usual cycle and period", wired to its [viewModel]. [onBack] leaves it, and Save leaves once saved. */
@Composable
fun UsualLengthsRoute(viewModel: UsualLengthsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val leave by rememberUpdatedState(onBack)
    LaunchedEffect(state) {
        if (state == UsualLengthsUiState.Saved) leave()
    }
    UsualLengthsScreen(state, onSave = viewModel::onSave, onBack = onBack, modifier = modifier)
}

/**
 * Her usual cycle and period lengths, on the same sliders as setup, starting from what is saved.
 * The sliders only offer lengths she can give, so Save saves what they show. What she sets is kept
 * through a configuration change. Scrolls when the text is large or the phone is on its side.
 */
@Composable
fun UsualLengthsScreen(
    state: UsualLengthsUiState,
    onSave: (cycleLength: Int, periodLength: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CycleTheme.spacing
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = stringResource(R.string.usual_lengths_title),
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.settings_back), onBack)
        )
        when (state) {
            is UsualLengthsUiState.Editing -> {
                var cycleLength by rememberSaveable { mutableIntStateOf(state.cycleLength) }
                var periodLength by rememberSaveable { mutableIntStateOf(state.periodLength) }
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
                    BasicText(
                        stringResource(R.string.usual_lengths_intro),
                        modifier = Modifier.padding(horizontal = spacing.large),
                        style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurfaceVariant)
                    )
                    UsualLengthSliders(
                        cycleLength = cycleLength,
                        onCycleLengthChange = { cycleLength = it },
                        periodLength = periodLength,
                        onPeriodLengthChange = { periodLength = it }
                    )
                    FilledButton(
                        text = stringResource(R.string.usual_lengths_save),
                        onClick = { onSave(cycleLength, periodLength) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = spacing.large)
                    )
                }
            }

            UsualLengthsUiState.Loading, UsualLengthsUiState.Saved -> LoadingState(Modifier.fillMaxSize())
        }
    }
}

@Preview
@Composable
private fun UsualLengthsPreview() {
    CycleTheme { UsualLengthsScreen(UsualLengthsUiState.Editing(28, 5), onSave = { _, _ -> }, onBack = {}) }
}
