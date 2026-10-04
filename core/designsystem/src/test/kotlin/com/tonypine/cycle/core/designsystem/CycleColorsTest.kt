package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleColorsTest {
    @Test
    fun contentMeetsWcagAaContrastInBothPalettes() {
        listOf(LightCycleColors, DarkCycleColors).forEach { colors ->
            assertContrast(colors.content, colors.background)
        }
    }

    private fun assertContrast(foreground: Color, background: Color) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val ratio = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue("Contrast $ratio is below 4.5:1 for $foreground on $background", ratio >= 4.5f)
    }
}
