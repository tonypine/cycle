package com.tonypine.cycle.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
class CoreUiScreenshotTest {
    @Test
    fun light() = captureRoboImage("src/test/screenshots/core_ui_light.png") { Sample(darkTheme = false) }

    @Test
    fun dark() = captureRoboImage("src/test/screenshots/core_ui_dark.png") { Sample(darkTheme = true) }
}

@Composable
private fun Sample(darkTheme: Boolean) {
    CycleTheme(darkTheme = darkTheme) {
        Column(
            Modifier
                .background(CycleTheme.colors.background)
                .padding(CycleTheme.spacing.medium)
        ) {
            CycleText("Text on the background")
            CycleSurface(Modifier.padding(top = CycleTheme.spacing.small)) {
                CycleText("Text on a surface", color = CycleTheme.colors.onSurface)
            }
        }
    }
}
