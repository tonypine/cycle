package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** WCAG 2.x minimum for body text and important UI text. */
const val TEXT_CONTRAST = 4.5

/** WCAG 2.x minimum for fills, edges and rings that carry meaning. */
const val GRAPHIC_CONTRAST = 3.0

/**
 * A pair of colour roles that must keep at least [minimum] contrast in both palettes. [CycleContrastPairs]
 * lists every pair from the Zest contrast table; a unit test and the catalog both read it.
 */
class ContrastPair(
    val name: String,
    val foregroundRole: String,
    val backgroundRole: String,
    val minimum: Double,
    val foreground: (CycleColors) -> Color,
    val background: (CycleColors) -> Color
) {
    fun ratio(colors: CycleColors): Double = contrastRatio(foreground(colors), background(colors))
}

val CycleContrastPairs: List<ContrastPair> = listOf(
    ContrastPair("Body text on surface", "onSurface", "surface", TEXT_CONTRAST, { it.onSurface }, { it.surface }),
    ContrastPair(
        "Secondary text on surface",
        "onSurfaceVariant",
        "surface",
        TEXT_CONTRAST,
        { it.onSurfaceVariant },
        { it.surface }
    ),
    ContrastPair(
        "Text on card",
        "onSurface",
        "surfaceContainer",
        TEXT_CONTRAST,
        { it.onSurface },
        { it.surfaceContainer }
    ),
    ContrastPair(
        "Secondary text on card",
        "onSurfaceVariant",
        "surfaceContainer",
        TEXT_CONTRAST,
        { it.onSurfaceVariant },
        { it.surfaceContainer }
    ),
    ContrastPair(
        "Secondary text on raised surface",
        "onSurfaceVariant",
        "surfaceContainerHigh",
        TEXT_CONTRAST,
        { it.onSurfaceVariant },
        { it.surfaceContainerHigh }
    ),
    ContrastPair("Text on accent", "onAccent", "accent", TEXT_CONTRAST, { it.onAccent }, { it.accent }),
    ContrastPair(
        "Text on accent container",
        "onAccentContainer",
        "accentContainer",
        TEXT_CONTRAST,
        { it.onAccentContainer },
        { it.accentContainer }
    ),
    ContrastPair("Accent as text on surface", "accent", "surface", TEXT_CONTRAST, { it.accent }, { it.surface }),
    ContrastPair(
        "Accent as text on card",
        "accent",
        "surfaceContainer",
        TEXT_CONTRAST,
        { it.accent },
        { it.surfaceContainer }
    ),
    ContrastPair("Outline vs surface", "outline", "surface", GRAPHIC_CONTRAST, { it.outline }, { it.surface }),
    ContrastPair(
        "Outline vs card",
        "outline",
        "surfaceContainer",
        GRAPHIC_CONTRAST,
        { it.outline },
        { it.surfaceContainer }
    ),
    ContrastPair("Error text on surface", "error", "surface", TEXT_CONTRAST, { it.error }, { it.surface }),
    ContrastPair(
        "Error text on card",
        "error",
        "surfaceContainer",
        TEXT_CONTRAST,
        { it.error },
        { it.surfaceContainer }
    ),
    ContrastPair("Text on error", "onError", "error", TEXT_CONTRAST, { it.onError }, { it.error }),
    ContrastPair(
        "Text on error container",
        "onErrorContainer",
        "errorContainer",
        TEXT_CONTRAST,
        { it.onErrorContainer },
        { it.errorContainer }
    ),
    ContrastPair("Day number on period", "onPeriod", "period", TEXT_CONTRAST, { it.onPeriod }, { it.period }),
    ContrastPair("Period fill vs surface", "period", "surface", GRAPHIC_CONTRAST, { it.period }, { it.surface }),
    ContrastPair(
        "Day number on predicted period",
        "onSurface",
        "predicted",
        TEXT_CONTRAST,
        { it.onSurface },
        { it.predicted }
    ),
    ContrastPair(
        "Predicted dashed edge vs surface",
        "predictedEdge",
        "surface",
        GRAPHIC_CONTRAST,
        { it.predictedEdge },
        { it.surface }
    ),
    ContrastPair(
        "Day number on fertile window",
        "onFertile",
        "fertile",
        TEXT_CONTRAST,
        { it.onFertile },
        { it.fertile }
    ),
    ContrastPair(
        "Day number on ovulation",
        "onOvulation",
        "ovulation",
        TEXT_CONTRAST,
        { it.onOvulation },
        { it.ovulation }
    ),
    ContrastPair(
        "Ovulation fill vs surface",
        "ovulation",
        "surface",
        GRAPHIC_CONTRAST,
        { it.ovulation },
        { it.surface }
    ),
    ContrastPair(
        "Fertile marker dot on fertile fill",
        "ovulation",
        "fertile",
        GRAPHIC_CONTRAST,
        { it.ovulation },
        { it.fertile }
    ),
    ContrastPair("Today ring vs surface", "today", "surface", GRAPHIC_CONTRAST, { it.today }, { it.surface })
)

/** WCAG 2.x contrast ratio between two opaque colours, from 1.0 to 21.0. */
fun contrastRatio(first: Color, second: Color): Double {
    val lighter = maxOf(first.relativeLuminance(), second.relativeLuminance())
    val darker = minOf(first.relativeLuminance(), second.relativeLuminance())
    return (lighter + 0.05) / (darker + 0.05)
}

private fun Color.relativeLuminance(): Double = 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)

private fun linear(channel: Float): Double {
    val c = channel.toDouble()
    return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}
