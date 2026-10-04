package com.tonypine.cycle.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.CycleTheme

@Composable
internal fun ShapesSection() {
    val shapes = CycleTheme.shapes
    listOf(
        "extraSmall · 8dp" to shapes.extraSmall,
        "small · 12dp" to shapes.small,
        "medium · 16dp" to shapes.medium,
        "large · 24dp" to shapes.large,
        "extraLarge · 32dp" to shapes.extraLarge,
        "full · pill" to shapes.full
    ).forEach { (name, shape) ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)
        ) {
            Box(
                Modifier
                    .size(width = 120.dp, height = 72.dp)
                    .background(CycleTheme.colors.accentContainer, shape)
            )
            CatalogText(name, CycleTheme.typography.body)
        }
    }
}
