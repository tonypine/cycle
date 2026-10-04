package com.tonypine.cycle.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.ContrastPair
import com.tonypine.cycle.core.designsystem.CycleColors
import com.tonypine.cycle.core.designsystem.CycleContrastPairs
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.DarkCycleColors
import com.tonypine.cycle.core.designsystem.LightCycleColors
import java.util.Locale

@Composable
internal fun ColorsSection() {
    SubsectionTitle("Roles")
    val darkRoles = DarkCycleColors.roles().toMap()
    LightCycleColors.roles().forEach { (name, light) -> RoleRow(name, light, darkRoles.getValue(name)) }

    SubsectionTitle("Contrast")
    CatalogText(
        "WCAG ratio of each pair. Text needs 4.5:1; fills, edges and rings need 3:1.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    CycleContrastPairs.forEach { ContrastRow(it) }
}

@Composable
private fun RoleRow(name: String, light: Color, dark: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)) {
        CatalogText(name, CycleTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
            Swatch("Light", light)
            Swatch("Dark", dark)
        }
    }
}

@Composable
private fun RowScope.Swatch(palette: String, color: Color) {
    Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(color, CycleTheme.shapes.extraSmall)
                .border(1.dp, CycleTheme.colors.outlineVariant, CycleTheme.shapes.extraSmall)
        )
        Column {
            CatalogText(palette, CycleTheme.typography.labelSmall, color = CycleTheme.colors.onSurfaceVariant)
            CatalogText(color.hex(), CycleTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ContrastRow(pair: ContrastPair) {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)) {
        CatalogText(pair.name, CycleTheme.typography.titleSmall)
        CatalogText(
            "${pair.foregroundRole} on ${pair.backgroundRole} · needs ${pair.minimum.format(1)}:1",
            CycleTheme.typography.bodySmall,
            color = CycleTheme.colors.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
            ContrastSample(pair, LightCycleColors)
            ContrastSample(pair, DarkCycleColors)
        }
    }
}

@Composable
private fun RowScope.ContrastSample(pair: ContrastPair, colors: CycleColors) {
    val ratio = pair.ratio(colors)
    val verdict = if (ratio >= pair.minimum) "pass" else "FAIL"
    CatalogText(
        "${ratio.format(2)}:1 $verdict",
        CycleTheme.typography.label,
        color = pair.foreground(colors),
        modifier = Modifier
            .weight(1f)
            .background(pair.background(colors), CycleTheme.shapes.small)
            .border(1.dp, CycleTheme.colors.outlineVariant, CycleTheme.shapes.small)
            .padding(CycleTheme.spacing.medium)
    )
}

/** Every role by name, in the order of the Zest colour table. */
private fun CycleColors.roles(): List<Pair<String, Color>> = listOf(
    "accent" to accent,
    "onAccent" to onAccent,
    "accentContainer" to accentContainer,
    "onAccentContainer" to onAccentContainer,
    "surface" to surface,
    "surfaceContainer" to surfaceContainer,
    "surfaceContainerHigh" to surfaceContainerHigh,
    "onSurface" to onSurface,
    "onSurfaceVariant" to onSurfaceVariant,
    "outline" to outline,
    "outlineVariant" to outlineVariant,
    "error" to error,
    "onError" to onError,
    "errorContainer" to errorContainer,
    "onErrorContainer" to onErrorContainer,
    "period" to period,
    "onPeriod" to onPeriod,
    "predicted" to predicted,
    "predictedEdge" to predictedEdge,
    "fertile" to fertile,
    "onFertile" to onFertile,
    "ovulation" to ovulation,
    "onOvulation" to onOvulation,
    "today" to today,
    "scrim" to scrim
)

/** `#RRGGBB`, or `#AARRGGBB` for a translucent role such as the scrim. */
private fun Color.hex(): String =
    if (alpha < 1f) "#%08X".format(Locale.ROOT, toArgb()) else "#%06X".format(Locale.ROOT, toArgb() and 0xFFFFFF)

private fun Double.format(decimals: Int): String = "%.${decimals}f".format(Locale.ROOT, this)
