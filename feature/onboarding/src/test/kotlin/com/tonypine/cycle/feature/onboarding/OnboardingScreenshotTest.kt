package com.tonypine.cycle.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.ui.MethodChoice
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The welcome and the three setup steps in light, dark and at 200% font scale, plus step 2 at the
 * ends of its ranges and step 3's since when and breaks. Each records
 * `src/test/screenshots/onboarding_<screen>_<appearance>.png`. Step 2 is also recorded on a phone on
 * its side, at the top and scrolled to Next, which fails if it stops scrolling. Synthetic dates.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class OnboardingScreenshotTest(private val name: String, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding() {
        if (appearance == Appearance.Landscape) RuntimeEnvironment.setQualifiers(LANDSCAPE)
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed(darkTheme = appearance == Appearance.Dark) { Screens.getValue(name)() }
            }
        }
        val path = "src/test/screenshots/onboarding_${name}_${appearance.fileName}.png"
        composeRule.onRoot().captureRoboImage(path)
        if (appearance == Appearance.Landscape) {
            composeRule.onNodeWithText("Next").performScrollTo()
            composeRule.onRoot().captureRoboImage(path.replace(".png", "_scrolled.png"))
        }
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200"),
        Landscape("landscape")
    }

    companion object {
        private const val LANDSCAPE = "+w800dp-h360dp-land"

        private val Today = LocalDate.of(2027, 3, 20)

        private val Screens: Map<String, @Composable () -> Unit> = mapOf(
            "welcome" to { WelcomeStep(onGetStarted = {}, onRestore = {}, onSkip = {}) },
            "last_period" to { lastPeriod(picked = null) },
            "last_period_picked" to { lastPeriod(picked = LocalDate.of(2027, 3, 2)) },
            "usual_lengths" to { usualLengths(cycle = 28, period = 5) },
            "usual_lengths_ends" to { usualLengths(cycle = 90, period = 1) },
            "contraception" to { contraception(choice = null) },
            "contraception_implant" to { contraception(MethodChoice.Method(ContraceptionMethod.IMPLANT)) },
            "contraception_none" to { contraception(MethodChoice.None) },
            "since_when_implant" to { sinceWhen(ContraceptionMethod.IMPLANT, picked = LocalDate.of(2026, 11, 9)) },
            "since_when_pill" to { sinceWhen(ContraceptionMethod.COMBINED_PILL, picked = null) },
            "breaks_pill" to { breaks(ContraceptionMethod.COMBINED_PILL) },
            "breaks_ring" to { breaks(ContraceptionMethod.RING) }
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
        private fun usualLengths(cycle: Int, period: Int) = UsualLengthsStep(
            cycleLength = cycle,
            onCycleLengthChange = {},
            periodLength = period,
            onPeriodLengthChange = {},
            onNext = {},
            onBack = {}
        )

        @Composable
        private fun contraception(choice: MethodChoice?) =
            ContraceptionStep(choice = choice, onChoose = {}, onNext = {}, onSkip = {}, onBack = {})

        @Composable
        private fun sinceWhen(method: ContraceptionMethod, picked: LocalDate?) = SinceWhenStep(
            method = method,
            today = Today,
            month = YearMonth.from(picked ?: Today),
            picked = picked,
            onPick = {},
            onMonthChange = {},
            onNext = {},
            onDontRemember = {},
            onBack = {}
        )

        @Composable
        private fun breaks(method: ContraceptionMethod) =
            BreaksStep(method = method, breaks = Breaks.MONTHLY, onPick = {}, onDone = {}, onBack = {})

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = Screens.keys.flatMap { name ->
            listOf(Appearance.Light, Appearance.Dark).map { arrayOf<Any>(name, it) }
        } +
            listOf("welcome", "last_period", "usual_lengths", "contraception_implant", "since_when_implant").map {
                arrayOf<Any>(it, Appearance.FontScale200)
            } +
            listOf(arrayOf<Any>("usual_lengths", Appearance.Landscape))
    }
}
