package com.tonypine.cycle.catalog

import androidx.compose.runtime.Composable
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/** Every catalog page in light, dark, 200% font scale and right-to-left. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CatalogScreenshotTest(private val page: Page, private val variant: Variant) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        // Tall enough to show the whole page, so the screenshot covers every token.
        val height = if (variant == Variant.FontScale200) page.largeFontHeight else page.height
        RuntimeEnvironment.setQualifiers("w360dp-h${height}dp-mdpi")
        composeRule.setContent {
            CycleTheme(darkTheme = variant == Variant.Dark) {
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, if (variant == Variant.FontScale200) 2f else 1f),
                    LocalLayoutDirection provides
                        if (variant == Variant.Rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    page.content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/catalog_${page.slug}_${variant.slug}.png")
    }

    class Page(val slug: String, val height: Int, val largeFontHeight: Int, val content: @Composable () -> Unit) {
        override fun toString() = slug
    }

    enum class Variant(val slug: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font200"),
        Rtl("rtl")
    }

    companion object {
        // Page heights in dp at 100% and 200% font scale. Raise them when a section grows.
        private val heights = mapOf(
            "Colours" to (4300 to 8600),
            "Typography" to (1200 to 2240),
            "Shapes" to (720 to 840),
            "Spacing" to (860 to 1120)
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun parameters(): List<Array<Any>> {
            val pages = listOf(Page("list", 540, 840) { SectionList(onOpen = {}) }) +
                CatalogSections.map { section ->
                    val (height, largeFontHeight) = heights.getValue(section.title)
                    Page(section.title.lowercase(), height, largeFontHeight) {
                        SectionPage(section, onBack = {})
                    }
                }
            return pages.flatMap { page -> Variant.entries.map { arrayOf<Any>(page, it) } }
        }
    }
}
