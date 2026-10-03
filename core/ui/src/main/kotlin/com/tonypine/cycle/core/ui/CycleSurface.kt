package com.tonypine.cycle.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.CycleTheme

/** A bordered, rounded container on the theme's surface colour. */
@Composable
fun CycleSurface(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .background(CycleTheme.colors.surface, shape)
            .border(1.dp, CycleTheme.colors.outline, shape)
            .padding(CycleTheme.spacing.medium),
        content = content
    )
}
