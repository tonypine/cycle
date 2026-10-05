package com.tonypine.cycle.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tonypine.cycle.core.designsystem.ClickableCard
import com.tonypine.cycle.core.designsystem.CycleIcon
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.TopAppBar

/**
 * The Settings tab until MOT-40 builds it: a row that opens What to log ([onWhatToLog]), and a line
 * saying the rest is on its way. TalkBack reads the row as one button.
 */
@Composable
fun SettingsScreen(onWhatToLog: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    val typography = CycleTheme.typography
    Column(modifier.fillMaxSize()) {
        TopAppBar(title = stringResource(R.string.settings_title))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = spacing.large)
                .padding(top = spacing.small, bottom = spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            ClickableCard(onClick = onWhatToLog, modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.large),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CycleIcon(CycleIcons.Tune, contentDescription = null, tint = colors.onSurfaceVariant)
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            stringResource(R.string.what_to_log_title),
                            style = typography.titleSmall.copy(color = colors.onSurface)
                        )
                        BasicText(
                            stringResource(R.string.settings_what_to_log_body),
                            style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
                        )
                    }
                    CycleIcon(CycleIcons.ChevronEnd, contentDescription = null, tint = colors.onSurfaceVariant)
                }
            }
            BasicText(
                stringResource(R.string.settings_more_soon),
                style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
            )
        }
    }
}

@Preview
@Composable
private fun SettingsPreview() {
    CycleTheme { SettingsScreen(onWhatToLog = {}) }
}
