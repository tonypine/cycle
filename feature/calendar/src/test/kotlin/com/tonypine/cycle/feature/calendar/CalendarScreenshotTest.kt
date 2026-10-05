package com.tonypine.cycle.feature.calendar

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Each sample month in light and dark, and March at 200% font scale and right-to-left. Each records
 * `src/test/screenshots/calendar_<month>_<appearance>.png`. The data is synthetic ([CalendarSamples]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class CalendarScreenshotTest(private val name: String, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun calendar() {
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides
                    if (appearance == Appearance.Rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                Themed(darkTheme = appearance == Appearance.Dark) {
                    CalendarScreen(CalendarSamples.all.getValue(name), CalendarActions())
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/calendar_${name}_${appearance.fileName}.png")
        // Nothing else is open: a selected day opens its sheet only when tapped.
        composeRule.onAllNodes(isDialog()).assertCountEquals(0)
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200"),
        Rtl("rtl")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = CalendarSamples.all.keys.flatMap { name ->
            listOf(Appearance.Light, Appearance.Dark).map { arrayOf<Any>(name, it) }
        } + listOf(Appearance.FontScale200, Appearance.Rtl).map { arrayOf<Any>("march", it) }
    }
}
