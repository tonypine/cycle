package com.tonypine.cycle.feature.today

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every Today state in light and dark, and two at 200% font scale. Each records
 * `src/test/screenshots/today_<state>_<appearance>.png`. The data is synthetic ([TodaySamples]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class TodayScreenshotTest(private val name: String, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun today() {
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed(darkTheme = appearance == Appearance.Dark) {
                    TodayScreen(TodaySamples.all.getValue(name), TodayActions())
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/today_${name}_${appearance.fileName}.png")
        // Nothing else is open: a sheet would mean the state opened one on its own.
        composeRule.onAllNodes(isDialog()).assertCountEquals(0)
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = TodaySamples.all.keys.flatMap { name ->
            listOf(Appearance.Light, Appearance.Dark).map { arrayOf<Any>(name, it) }
        } + listOf("mid_cycle", "still_going", "missed_period").map { arrayOf<Any>(it, Appearance.FontScale200) }
    }
}
