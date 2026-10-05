package com.tonypine.cycle

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.designsystem.CycleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * "Cycle is locked", in light, dark, at 200% font scale and right to left. Records
 * `src/test/screenshots/app_lock_<appearance>.png`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h640dp-mdpi")
class LockScreenScreenshotTest(private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides
                    if (appearance == Appearance.Rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                CycleTheme(darkTheme = appearance == Appearance.Dark, reduceMotion = true) {
                    LockScreen(onUnlock = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_lock_${appearance.fileName}.png")
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200"),
        Rtl("rtl")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = Appearance.entries.map { arrayOf<Any>(it) }
    }
}
