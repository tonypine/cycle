package com.tonypine.cycle.catalog

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
class CatalogScreenshotTest {
    @Test
    fun light() = captureRoboImage("src/test/screenshots/catalog_light.png") {
        CycleTheme(darkTheme = false) { Catalog() }
    }

    @Test
    fun dark() = captureRoboImage("src/test/screenshots/catalog_dark.png") {
        CycleTheme(darkTheme = true) { Catalog() }
    }
}
