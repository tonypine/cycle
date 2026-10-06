package com.tonypine.cycle.feature.history

import androidx.compose.runtime.Composable
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
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every History, cycle detail and period editor state in light and dark, and the list, a detail and
 * the editor at 200% font scale. Each records `src/test/screenshots/history_<state>_<appearance>.png`,
 * `cycle_<state>_<appearance>.png` or `edit_period_<state>_<appearance>.png`. The data is synthetic
 * ([HistorySamples]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class HistoryScreenshotTest(private val name: String, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun history() {
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed(darkTheme = appearance == Appearance.Dark) {
                    screens.getValue(name)()
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/${name}_${appearance.fileName}.png")
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200")
    }

    companion object {
        private val screens: Map<String, @Composable () -> Unit> =
            HistorySamples.all.entries.associate { (name, state) ->
                "history_$name" to @Composable { HistoryScreen(state, onCycleClick = {}) }
            } + HistorySamples.allDetails.entries.associate { (name, state) ->
                "cycle_$name" to @Composable {
                    CycleDetailScreen(state, onBack = {}, onSeeInCalendar = {}, onEditPeriod = {}, onDeletePeriod = {})
                }
            } + HistorySamples.allEdits.entries.associate { (name, state) ->
                "edit_period_$name" to @Composable {
                    EditPeriodScreen(
                        state,
                        onBack = {},
                        onChoose = {},
                        onPick = {},
                        onMonthChange = {},
                        onStillGoingChange = {},
                        onSave = {}
                    )
                }
            }

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = screens.keys.flatMap { name ->
            listOf(Appearance.Light, Appearance.Dark).map { arrayOf<Any>(name, it) }
        } +
            listOf("history_cycles", "cycle_past", "edit_period_refused").map {
                arrayOf<Any>(it, Appearance.FontScale200)
            }
    }
}
