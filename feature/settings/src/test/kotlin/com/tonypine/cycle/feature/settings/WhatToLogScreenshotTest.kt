package com.tonypine.cycle.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.model.LogCategory
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What to log, with Sex hidden, and the Settings placeholder, in light, dark and at 200% font scale.
 * Each records `src/test/screenshots/<screen>_<appearance>.png`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class WhatToLogScreenshotTest(private val screen: String, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed(darkTheme = appearance == Appearance.Dark) {
                    when (screen) {
                        "what_to_log" -> WhatToLogScreen(
                            WhatToLogUiState.Ready(setOf(LogCategory.SEX)),
                            onShownChange = { _, _ -> },
                            onBack = {}
                        )

                        else -> SettingsScreen(onWhatToLog = {})
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/${screen}_${appearance.fileName}.png")
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = listOf("what_to_log", "settings").flatMap { screen ->
            Appearance.entries.map { arrayOf<Any>(screen, it) }
        }
    }
}
