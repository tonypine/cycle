package com.tonypine.cycle.core.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The day log sheet with every category logged, in German at 100% and 200% font size: the chips and
 * section titles wrap, nothing clips or overlaps. Each records
 * `src/test/screenshots/day_log_feelings_top_de_<scale>.png` of the whole screen. Synthetic days
 * ([DayLogSamples]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class DayLogSheetLanguagesScreenshotTest(private val fontScale: Float) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sheet() {
        renderIn(Language("de"))
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed { OpenDayLogSheet(DayLogSamples.feelings) }
            }
        }
        composeRule.waitForIdle()
        val scale = if (fontScale == 2f) "font_scale_200" else "light"
        captureScreenRoboImage("src/test/screenshots/day_log_feelings_top_de_$scale.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(1f), arrayOf(2f))
    }
}
