package com.tonypine.cycle.catalog

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
class CatalogScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun light() = capture(darkTheme = false, "src/test/screenshots/catalog_light.png")

    @Test
    fun dark() = capture(darkTheme = true, "src/test/screenshots/catalog_dark.png")

    private fun capture(darkTheme: Boolean, path: String) {
        composeRule.setContent { CycleTheme(darkTheme = darkTheme) { Catalog() } }
        composeRule.onRoot().captureRoboImage(path)
    }
}
