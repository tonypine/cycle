package com.tonypine.cycle.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The welcome and both setup steps in German, Cycle's longest language, at 100% and 200% font size:
 * nothing clips or overlaps. Each records `src/test/screenshots/onboarding_<screen>_de_<scale>.png`.
 * Synthetic dates.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class OnboardingLanguagesScreenshotTest(private val screen: String, private val fontScale: Float) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding() {
        renderIn(German)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed { Screens.getValue(screen)() }
            }
        }
        val scale = if (fontScale == 2f) "font_scale_200" else "light"
        composeRule.onRoot().captureRoboImage("src/test/screenshots/onboarding_${screen}_${German.tag}_$scale.png")
    }

    companion object {
        private val German = Language("de")
        private val Today = LocalDate.of(2027, 10, 14)

        private val Screens: Map<String, @Composable () -> Unit> = mapOf(
            "welcome" to { WelcomeStep(onGetStarted = {}, onRestore = {}, onSkip = {}) },
            "last_period" to {
                LastPeriodStep(
                    today = Today,
                    month = YearMonth.from(Today),
                    picked = LocalDate.of(2027, 10, 1),
                    onPick = {},
                    onMonthChange = {},
                    onNext = {},
                    onDontRemember = {},
                    onBack = {}
                )
            },
            "usual_lengths" to {
                UsualLengthsStep(
                    cycleLength = 28,
                    onCycleLengthChange = {},
                    periodLength = 5,
                    onPeriodLengthChange = {},
                    onNext = {},
                    onBack = {}
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
