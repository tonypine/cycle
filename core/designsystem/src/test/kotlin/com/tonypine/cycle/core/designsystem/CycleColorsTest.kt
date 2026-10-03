package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CycleColorsTest {
    @Test
    fun lightAndDarkPalettesReportTheirMode() {
        assertFalse(LightCycleColors.isDark)
        assertTrue(DarkCycleColors.isDark)
    }

    @Test
    fun textMeetsWcagAaContrastInBothPalettes() {
        listOf(LightCycleColors, DarkCycleColors).forEach { colors ->
            assertContrast(colors.onBackground, colors.background)
            assertContrast(colors.onSurface, colors.surface)
            assertContrast(colors.onAccent, colors.accent)
            assertContrast(colors.muted, colors.background)
        }
    }

    private fun assertContrast(foreground: Color, background: Color) {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        val ratio = (lighter + 0.05f) / (darker + 0.05f)
        assertTrue("Contrast $ratio is below 4.5:1 for $foreground on $background", ratio >= 4.5f)
    }
}
