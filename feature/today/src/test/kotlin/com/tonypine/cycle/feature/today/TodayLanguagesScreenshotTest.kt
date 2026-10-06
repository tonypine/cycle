package com.tonypine.cycle.feature.today

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
 * Today mid-cycle in German at 100% and 200% font size, and in Brazilian Portuguese and Spanish:
 * nothing clips or overlaps, and the date line starts with a capital. Each records
 * `src/test/screenshots/today_mid_cycle_<language>_<scale>.png`. The data is synthetic ([TodaySamples]).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class TodayLanguagesScreenshotTest(private val tag: String, private val fontScale: Float) {
    // A tag rather than a Language: the runner cannot pass a value class to the constructor.
    private val language = Language(tag)

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun today() {
        renderIn(language)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed { TodayScreen(TodaySamples.all.getValue("mid_cycle"), TodayActions()) }
            }
        }
        val scale = if (fontScale == 2f) "font_scale_200" else "light"
        composeRule.onRoot().captureRoboImage("src/test/screenshots/today_mid_cycle_${language.tag}_$scale.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = listOf(
            arrayOf("de", 1f),
            arrayOf("de", 2f),
            arrayOf("pt-BR", 1f),
            arrayOf("es", 1f)
        )
    }
}
