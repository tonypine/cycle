package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class CycleThemeScreenshotTest {
    @Test
    fun light() = captureRoboImage("src/test/screenshots/cycle_theme_light.png") { ThemeSample(darkTheme = false) }

    @Test
    fun dark() = captureRoboImage("src/test/screenshots/cycle_theme_dark.png") { ThemeSample(darkTheme = true) }
}

@Composable
private fun ThemeSample(darkTheme: Boolean) {
    CycleTheme(darkTheme = darkTheme) {
        val colors = CycleTheme.colors
        val typography = CycleTheme.typography
        Column(
            Modifier
                .background(colors.background)
                .padding(CycleTheme.spacing.medium)
        ) {
            BasicText("Title", style = typography.title.copy(color = colors.onBackground))
            BasicText("Body text", style = typography.body.copy(color = colors.onBackground))
            BasicText("Label", style = typography.label.copy(color = colors.muted))
            Spacer(Modifier.size(CycleTheme.spacing.small))
            Row {
                listOf(colors.background, colors.surface, colors.accent, colors.muted, colors.outline).forEach {
                    Swatch(it, colors.outline)
                    Spacer(Modifier.width(CycleTheme.spacing.small))
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, outline: Color) {
    Box(
        Modifier
            .size(32.dp)
            .background(color)
            .border(1.dp, outline)
    )
}
