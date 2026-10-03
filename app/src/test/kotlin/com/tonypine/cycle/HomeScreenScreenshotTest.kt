package com.tonypine.cycle

import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.designsystem.CycleTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class HomeScreenScreenshotTest {
    @Test
    fun light() = captureRoboImage("src/test/screenshots/home_light.png") {
        CycleTheme(darkTheme = false) { HomeScreen() }
    }

    @Test
    fun dark() = captureRoboImage("src/test/screenshots/home_dark.png") {
        CycleTheme(darkTheme = true) { HomeScreen() }
    }
}
