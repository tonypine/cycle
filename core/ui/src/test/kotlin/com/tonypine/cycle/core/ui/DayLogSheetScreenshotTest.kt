package com.tonypine.cycle.core.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The day log sheet empty (with the fill chip), next to a period (no chip), with a flow selected, and
 * its clear dialog, in light and dark. Each records `src/test/screenshots/day_log_<case>_<appearance>.png`
 * of the whole screen. Synthetic days ([DayLogSamples]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class DayLogSheetScreenshotTest(private val case: Case, private val darkTheme: Boolean) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sheet() {
        composeRule.setContent { Themed(darkTheme) { OpenDayLogSheet(case.entry) } }
        composeRule.waitForIdle()
        if (case == Case.ClearDialog) {
            composeRule.onNodeWithText("Clear this day").performClick()
            composeRule.waitForIdle()
        }
        val appearance = if (darkTheme) "dark" else "light"
        captureScreenRoboImage("src/test/screenshots/day_log_${case.fileName}_$appearance.png")
    }

    enum class Case(val fileName: String, val entry: DayLogEntry) {
        Empty("empty", DayLogSamples.nearPeriod),
        Fill("fill", DayLogSamples.empty),
        Flow("flow", DayLogSamples.medium),
        ClearDialog("clear_dialog", DayLogSamples.medium)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = Case.entries.flatMap { case ->
            listOf(false, true).map { arrayOf<Any>(case, it) }
        }
    }
}
