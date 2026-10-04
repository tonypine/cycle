package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleContrastTest {
    @Test
    fun everyPairMeetsItsWcagThresholdInBothPalettes() {
        val failures = palettes.flatMap { (palette, colors) ->
            CycleContrastPairs
                .filter { it.ratio(colors) < it.minimum }
                .map { "${it.name} ($palette): %.2f < %.1f".format(it.ratio(colors), it.minimum) }
        }
        assertTrue("Pairs below their threshold:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun ratiosMatchTheZestTable() {
        CycleContrastPairs.forEach { pair ->
            val (light, dark) = documentedRatios.getValue(pair.name)
            assertEquals("${pair.name} (light)", light, pair.ratio(LightCycleColors), ROUNDING_TOLERANCE)
            assertEquals("${pair.name} (dark)", dark, pair.ratio(DarkCycleColors), ROUNDING_TOLERANCE)
        }
        assertEquals(documentedRatios.keys, CycleContrastPairs.map { it.name }.toSet())
    }

    @Test
    fun contrastRatioSpansOneToTwentyOne() {
        assertEquals(21.0, contrastRatio(LightCycleColors.onAccent, Color.Black), 0.001)
        assertEquals(1.0, contrastRatio(LightCycleColors.surface, LightCycleColors.surface), 0.001)
    }

    private companion object {
        const val ROUNDING_TOLERANCE = 0.006
    }

    private val palettes = listOf("light" to LightCycleColors, "dark" to DarkCycleColors)

    // The table in docs/design/visual-directions.md (Zest), rounded to two decimals.
    private val documentedRatios = mapOf(
        "Body text on surface" to (16.88 to 15.23),
        "Secondary text on surface" to (8.20 to 10.73),
        "Text on card" to (15.62 to 13.90),
        "Secondary text on raised surface" to (6.89 to 8.57),
        "Secondary text on card" to (7.59 to 9.80),
        "Text on accent" to (7.17 to 7.72),
        "Text on accent container" to (13.31 to 7.25),
        "Accent as text on surface" to (6.99 to 10.86),
        "Accent as text on card" to (6.47 to 9.91),
        "Outline vs surface" to (4.47 to 5.66),
        "Error text on surface" to (6.38 to 10.90),
        "Error text on card" to (5.90 to 9.95),
        "Text on error" to (6.54 to 7.72),
        "Text on error container" to (12.77 to 7.24),
        "Day number on period" to (4.96 to 7.48),
        "Period fill vs surface" to (4.84 to 8.13),
        "Day number on predicted period" to (13.65 to 11.60),
        "Predicted dashed edge vs surface" to (4.84 to 8.13),
        "Day number on fertile window" to (10.46 to 7.96),
        "Day number on ovulation" to (4.80 to 10.06),
        "Ovulation fill vs surface" to (4.68 to 11.51),
        "Fertile marker dot on fertile fill" to (3.83 to 6.29),
        "Today ring vs surface" to (16.88 to 15.23)
    )
}
