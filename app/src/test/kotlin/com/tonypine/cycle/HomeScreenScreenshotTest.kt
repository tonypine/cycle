package com.tonypine.cycle

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.designsystem.CycleTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class HomeScreenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = capture(darkTheme = false, "src/test/screenshots/home_light.png")

    @Test
    fun dark() = capture(darkTheme = true, "src/test/screenshots/home_dark.png")

    private fun capture(darkTheme: Boolean, path: String) {
        composeRule.setContent { CycleTheme(darkTheme = darkTheme) { HomeScreen() } }
        composeRule.onRoot().captureRoboImage(path)
    }
}
