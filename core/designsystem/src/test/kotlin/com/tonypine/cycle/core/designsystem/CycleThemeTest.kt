package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
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

    @Test
    fun providesEveryTokenSet() {
        var tokens: List<Any>? = null
        composeRule.setContent {
            CycleTheme {
                tokens = listOf(
                    CycleTheme.typography,
                    CycleTheme.shapes,
                    CycleTheme.spacing,
                    CycleTheme.elevation,
                    CycleTheme.motion,
                    CycleTheme.stateAlpha
                )
            }
        }
        assertEquals(
            listOf(
                ZestTypography,
                ZestShapes,
                DefaultCycleSpacing,
                DefaultCycleElevation,
                ZestMotion,
                DefaultCycleStateAlpha
            ),
            tokens
        )
    }

    @Test
    fun installsCycleIndicationInsteadOfRipple() {
        var indication: Indication? = null
        composeRule.setContent {
            CycleTheme(darkTheme = true, reduceMotion = false) { indication = LocalIndication.current }
        }
        val cycle = indication as CycleIndication
        assertEquals(DarkCycleColors.onSurface, cycle.color)
        assertEquals(DarkCycleColors.accent, cycle.focusRingColor)
        assertEquals(PRESSED_SCALE, cycle.pressedScale)
        assertEquals(ZestMotion, cycle.motion)
    }
}
