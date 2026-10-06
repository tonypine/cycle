package com.tonypine.cycle.feature.history

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * The whole cycle detail with how she felt and her notes, in light and dark and at 200% font scale,
 * on a screen tall enough to show it all. Records `src/test/screenshots/cycle_feelings_<appearance>.png`.
 * The data is synthetic ([HistorySamples.pastCycle]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CycleFeelingsScreenshotTest(private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cycleFeelings() {
        RuntimeEnvironment.setQualifiers("w360dp-h${appearance.heightDp}dp-mdpi")
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, appearance.fontScale)) {
                Themed(darkTheme = appearance == Appearance.Dark) {
                    CycleDetailScreen(
                        HistorySamples.pastCycle,
                        onBack = {},
                        onSeeInCalendar = {},
                        onEditPeriod = {},
                        onDeletePeriod = {}
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/cycle_feelings_${appearance.fileName}.png")
    }

    enum class Appearance(val fileName: String, val heightDp: Int, val fontScale: Float = 1f) {
        Light("light", heightDp = 1400),
        Dark("dark", heightDp = 1400),
        FontScale200("font_scale_200", heightDp = 2500, fontScale = 2f)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = Appearance.entries.map { arrayOf<Any>(it) }
    }
}
