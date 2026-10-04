package com.tonypine.cycle.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.CycleTheme

@Composable
internal fun SpacingSection() {
    val spacing = CycleTheme.spacing
    listOf(
        "extraSmall" to spacing.extraSmall,
        "small" to spacing.small,
        "medium" to spacing.medium,
        "large" to spacing.large,
        "extraLarge" to spacing.extraLarge,
        "extraExtraLarge" to spacing.extraExtraLarge,
        "huge" to spacing.huge
    ).forEach { (name, size) ->
        TokenRow("$name · ${size.label()}") {
            Box(Modifier.width(size).height(24.dp).background(CycleTheme.colors.accent))
        }
    }

    SubsectionTitle("Elevation")
    val elevation = CycleTheme.elevation
    listOf(
        "level0" to elevation.level0,
        "level1" to elevation.level1,
        "level2" to elevation.level2,
        "level3" to elevation.level3
    ).forEach { (name, level) ->
        TokenRow("$name · ${level.label()}") {
            Box(
                Modifier
                    .size(width = 96.dp, height = 56.dp)
                    .shadow(level, CycleTheme.shapes.medium)
                    .background(CycleTheme.colors.surfaceContainerHigh, CycleTheme.shapes.medium)
            )
        }
    }
}

@Composable
private fun TokenRow(name: String, sample: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)
    ) {
        Box(Modifier.width(96.dp), contentAlignment = Alignment.CenterStart) { sample() }
        CatalogText(name, CycleTheme.typography.body)
    }
}

private fun Dp.label(): String = "${value.toInt()}dp"
