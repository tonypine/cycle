package com.tonypine.cycle.feature.history

import androidx.compose.runtime.Composable
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
 * History's list of cycles and a past cycle's details in German at 100% and 200% font size: nothing
 * clips or overlaps. Each records `src/test/screenshots/<screen>_de_<scale>.png`. The data is synthetic
 * ([HistorySamples]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class HistoryLanguagesScreenshotTest(private val screen: String, private val fontScale: Float) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun history() {
        renderIn(Language("de"))
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed { Screens.getValue(screen)() }
            }
        }
        val scale = if (fontScale == 2f) "font_scale_200" else "light"
        composeRule.onRoot().captureRoboImage("src/test/screenshots/${screen}_de_$scale.png")
    }

    companion object {
        private val Screens: Map<String, @Composable () -> Unit> = mapOf(
            "history_cycles" to {
                HistoryScreen(HistorySamples.all.getValue("cycles"), onCycleClick = {}, onSeeInCalendar = {})
            },
            "cycle_past" to {
                CycleDetailScreen(
                    HistorySamples.allDetails.getValue("past"),
                    onBack = {},
                    onSeeInCalendar = {},
                    onEditPeriod = {},
                    onDeletePeriod = {}
                )
            }
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = Screens.keys.flatMap { screen ->
            listOf(1f, 2f).map { arrayOf<Any>(screen, it) }
        }
    }
}
