package com.tonypine.cycle.feature.calendar

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * March in German at 100% and 200% font size, with German month and weekday names: nothing clips or
 * overlaps. Each records `src/test/screenshots/calendar_march_de_<scale>.png`. The data is synthetic
 * ([CalendarSamples]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class CalendarLanguagesScreenshotTest(private val fontScale: Float) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calendar() {
        renderIn(Language("de"))
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed { CalendarScreen(CalendarSamples.all.getValue("march"), CalendarActions()) }
            }
        }
        val scale = if (fontScale == 2f) "font_scale_200" else "light"
        composeRule.onRoot().captureRoboImage("src/test/screenshots/calendar_march_de_$scale.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = listOf(arrayOf(1f), arrayOf(2f))
    }
}
