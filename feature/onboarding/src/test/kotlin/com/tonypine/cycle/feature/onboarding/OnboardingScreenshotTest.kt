package com.tonypine.cycle.feature.onboarding

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The welcome and both setup steps in light, dark and at 200% font scale, plus step 2 with its
 * errors. Each records `src/test/screenshots/onboarding_<screen>_<appearance>.png`. Synthetic dates.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class OnboardingScreenshotTest(private val name: String, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding() {
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed(darkTheme = appearance == Appearance.Dark) { Screens.getValue(name)() }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/onboarding_${name}_${appearance.fileName}.png")
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200")
    }

    companion object {
        private val Today = LocalDate.of(2027, 3, 20)

        private val Screens: Map<String, @Composable () -> Unit> = mapOf(
            "welcome" to { WelcomeStep(onGetStarted = {}, onSkip = {}) },
            "last_period" to { lastPeriod(picked = null) },
            "last_period_picked" to { lastPeriod(picked = LocalDate.of(2027, 3, 2)) },
            "usual_lengths" to { usualLengths(cycle = "28", period = "5", showErrors = false) },
            "usual_lengths_errors" to { usualLengths(cycle = "12", period = "", showErrors = true) }
        )

        @Composable
        private fun lastPeriod(picked: LocalDate?) = LastPeriodStep(
            today = Today,
            month = YearMonth.from(Today),
            picked = picked,
            onPick = {},
            onMonthChange = {},
            onNext = {},
            onDontRemember = {},
            onBack = {}
        )

        @Composable
        private fun usualLengths(cycle: String, period: String, showErrors: Boolean) = UsualLengthsStep(
            cycleLength = TextFieldState(cycle),
            periodLength = TextFieldState(period),
            showErrors = showErrors,
            onDone = {},
            onBack = {}
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = Screens.keys.flatMap { name ->
            listOf(Appearance.Light, Appearance.Dark).map { arrayOf<Any>(name, it) }
        } + listOf("welcome", "last_period", "usual_lengths").map { arrayOf<Any>(it, Appearance.FontScale200) }
    }
}
