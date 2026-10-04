package com.tonypine.cycle.catalog

import android.graphics.Insets
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.designsystem.CycleTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The App bars demo in an edge-to-edge window. Robolectric draws no system bars, so the test
 * dispatches their insets; the demo shades where they would be. The screenshot shows both bars clear
 * of the shaded strips, and the bounds checks prove it. `CatalogNavigationTest` opens the demo from
 * the catalog.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h720dp-mdpi")
class AppBarsDemoScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theBarsSitClearOfTheSystemBars() {
        composeRule.activityRule.scenario.onActivity { it.window.setDecorFitsSystemWindows(false) }
        composeRule.setContent { CycleTheme(darkTheme = false, reduceMotion = true) { AppBarsDemo(onClose = {}) } }
        dispatchSystemBars()

        val screen = composeRule.onRoot().getBoundsInRoot()
        val back = composeRule.onNodeWithContentDescription("Back to the catalog").getBoundsInRoot()
        assertTrue("Back at $back should sit below the status bar", back.top >= STATUS_BAR)
        listOf("Today", "Log", "Settings").forEach { label ->
            val tab = composeRule.onNodeWithText(label).getBoundsInRoot()
            val clear = tab.bottom <= screen.bottom - NAVIGATION_BAR
            assertTrue("$label at $tab should sit above the navigation bar", clear)
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/catalog_app-bars_edge-to-edge.png")
    }

    /** Applies status and navigation bar insets to the content view, as the system does. */
    private fun dispatchSystemBars() {
        composeRule.activityRule.scenario.onActivity { activity ->
            val density = activity.resources.displayMetrics.density
            val insets = WindowInsets.Builder()
                .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, (STATUS_BAR.value * density).toInt(), 0, 0))
                .setInsets(
                    WindowInsets.Type.navigationBars(),
                    Insets.of(0, 0, 0, (NAVIGATION_BAR.value * density).toInt())
                )
                .setVisible(WindowInsets.Type.statusBars(), true)
                .setVisible(WindowInsets.Type.navigationBars(), true)
                .build()
            activity.findViewById<ViewGroup>(android.R.id.content).dispatchApplyWindowInsets(insets)
        }
        composeRule.waitForIdle()
    }

    private companion object {
        val STATUS_BAR = 24.dp
        val NAVIGATION_BAR = 48.dp
    }
}
