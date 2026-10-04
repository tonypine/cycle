package com.tonypine.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tonypine.cycle.core.designsystem.CycleTheme

/** Placeholder start screen until the first feature lands. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    BasicText(
        text = stringResource(R.string.app_name),
        modifier = modifier
            .fillMaxSize()
            .background(CycleTheme.colors.surface)
            .safeDrawingPadding()
            .padding(CycleTheme.spacing.extraLarge),
        style = CycleTheme.typography.headline.copy(color = CycleTheme.colors.onSurface)
    )
}
