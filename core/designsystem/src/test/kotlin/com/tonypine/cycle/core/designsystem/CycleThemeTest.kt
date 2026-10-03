package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CycleThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun darkThemeProvidesDarkColors() {
        var colors: CycleColors? = null
        composeRule.setContent {
            CycleTheme(darkTheme = true) { colors = CycleTheme.colors }
        }
        assertEquals(DarkCycleColors, colors)
    }

    @Test
    fun lightThemeProvidesLightColors() {
        var colors: CycleColors? = null
        composeRule.setContent {
            CycleTheme(darkTheme = false) { colors = CycleTheme.colors }
        }
        assertEquals(LightCycleColors, colors)
    }
}
