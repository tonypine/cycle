package com.tonypine.cycle.core.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The day log sheet empty (with the fill chip), next to a period (no chip), with a flow selected, its
 * clear dialog, and with every category logged, at the top and scrolled to the bottom, in light and
 * dark; the logged day also at 200% font scale. Each records
 * `src/test/screenshots/day_log_<case>_<appearance>.png` of the whole screen. Synthetic days
 * ([DayLogSamples]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class DayLogSheetScreenshotTest(private val case: Case, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sheet() {
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed(appearance == Appearance.Dark) { OpenDayLogSheet(case.entry) }
            }
        }
        composeRule.waitForIdle()
        when (case) {
            Case.ClearDialog -> {
                composeRule.onNodeWithText("Clear this day").performScrollTo().performClick()
                composeRule.waitForIdle()
            }

            Case.FeelingsBottom -> {
                composeRule.onNodeWithText("Log it").performScrollTo()
                composeRule.waitForIdle()
            }

            else -> Unit
        }
        captureScreenRoboImage("src/test/screenshots/day_log_${case.fileName}_${appearance.fileName}.png")
    }

    enum class Case(val fileName: String, val entry: DayLogEntry) {
        Empty("empty", DayLogSamples.nearPeriod),
        Fill("fill", DayLogSamples.empty),
        Flow("flow", DayLogSamples.medium),
        ClearDialog("clear_dialog", DayLogSamples.medium),
        FeelingsTop("feelings_top", DayLogSamples.feelings),
        FeelingsBottom("feelings_bottom", DayLogSamples.feelings)
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = Case.entries.flatMap { case ->
            listOf(Appearance.Light, Appearance.Dark).map { arrayOf<Any>(case, it) }
        } + listOf(Case.FeelingsTop, Case.FeelingsBottom).map { arrayOf<Any>(it, Appearance.FontScale200) }
    }
}
