package com.tonypine.cycle

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tonypine.cycle.core.designsystem.EmptyState
import com.tonypine.cycle.core.designsystem.EmptyStateIcon

/** A tab whose screen has not landed yet: Settings (MOT-40). */
@Composable
fun PlaceholderScreen(destination: TopLevelDestination, modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(destination.label),
        body = stringResource(R.string.placeholder_body),
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        illustration = { EmptyStateIcon(destination.icon) }
    )
}
