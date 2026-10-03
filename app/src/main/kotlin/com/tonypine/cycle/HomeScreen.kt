package com.tonypine.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.ui.CycleText

/** Placeholder start screen until the first feature lands. */
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CycleTheme.colors.background)
            .safeDrawingPadding()
            .padding(CycleTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
    ) {
        CycleText(stringResource(R.string.app_name), style = CycleTheme.typography.title)
        CycleText(stringResource(R.string.home_empty), color = CycleTheme.colors.muted)
    }
}
