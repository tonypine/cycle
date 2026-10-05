package com.tonypine.cycle

import android.graphics.Insets
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The app draws edge to edge, so Today scrolls under the status bar: the strip behind the clock and
 * system icons stays `surface`, at 100% and 200% font, and Today's content clips below it. Robolectric
 * draws no system bars, so the test dispatches a status bar inset itself. Records
 * `src/test/screenshots/app_today_scrolled_<appearance>.png`, at 200% font. Synthetic data only: a
 * made-up period in 2027.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class StatusBarScrimTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `at 100% font, nothing shows under the status bar once Today scrolls`() =
        assertStatusBarClear(darkTheme = false, fontScale = 1f)

    @Test
    fun `at 200% font, nothing shows under the status bar once Today scrolls`() =
        assertStatusBarClear(darkTheme = false, fontScale = 2f)

    @Test
    fun `in dark, nothing shows under the status bar once Today scrolls`() =
        assertStatusBarClear(darkTheme = true, fontScale = 2f)

    @Test
    fun light() = capture(darkTheme = false, "light")

    @Test
    fun dark() = capture(darkTheme = true, "dark")

    private fun assertStatusBarClear(darkTheme: Boolean, fontScale: Float) {
        val surface = showScrolledToday(darkTheme, fontScale)
        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        val statusBar = (STATUS_BAR.value * composeRule.activity.resources.displayMetrics.density).toInt()
        for (y in 0 until statusBar) {
            for (x in 0 until pixels.width) {
                assertEquals("the status bar at ($x, $y) should show only surface", surface, pixels[x, y])
            }
        }
    }

    private fun capture(darkTheme: Boolean, appearance: String) {
        showScrolledToday(darkTheme, fontScale = 2f)
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_today_scrolled_$appearance.png")
    }

    /** Shows Today with a logged period under a status bar, scrolls it, and returns `surface`. */
    private fun showScrolledToday(darkTheme: Boolean, fontScale: Float): Color {
        val data = (composeRule.activity.application as CycleApplication).data
        runBlocking {
            (0 until PERIOD_DAYS).forEach {
                data.dayLogRepository.save(DayLog(PERIOD_START.plusDays(it.toLong()), FlowLevel.MEDIUM))
            }
        }
        composeRule.activityRule.scenario.onActivity { it.window.setDecorFitsSystemWindows(false) }
        var surface = Color.Unspecified
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                CycleTheme(darkTheme = darkTheme, reduceMotion = true) {
                    surface = CycleTheme.colors.surface
                    CycleApp(data, FakeDeviceLock(), today = { TODAY })
                }
            }
        }
        // Her logs skip the first-run welcome; Today shows once the log is read.
        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodes(hasText("Day 17")).fetchSemanticsNodes().isNotEmpty()
        }
        showStatusBar()

        val heading = composeRule.onNodeWithText("Day 17")
        val before = heading.getBoundsInRoot()
        // Today's column, not the week row inside it.
        val today = composeRule.onNode(hasScrollAction() and hasAnyDescendant(hasText("Day 17")))
        today.performSemanticsAction(SemanticsActions.ScrollBy) {
            it(0f, SCROLL.value * composeRule.activity.resources.displayMetrics.density)
        }
        composeRule.waitForIdle()
        val after = heading.getBoundsInRoot()
        assertTrue("Today should scroll under the status bar, from $before to $after", after.top < before.top)
        assertTrue("the heading at $after should reach under the status bar", after.top < STATUS_BAR)
        return surface
    }

    /** Applies a status bar inset to the content view, as the system does on a phone. */
    private fun showStatusBar() {
        composeRule.activityRule.scenario.onActivity { activity ->
            val height = (STATUS_BAR.value * activity.resources.displayMetrics.density).toInt()
            val insets = WindowInsets.Builder()
                .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, height, 0, 0))
                .setVisible(WindowInsets.Type.statusBars(), true)
                .build()
            activity.findViewById<ViewGroup>(android.R.id.content).dispatchApplyWindowInsets(insets)
        }
        composeRule.waitForIdle()
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2027, 3, 20)
        val PERIOD_START: LocalDate = LocalDate.of(2027, 3, 4)
        const val PERIOD_DAYS = 5
        val STATUS_BAR = 24.dp
        val SCROLL = 64.dp
        const val WAIT_MILLIS = 5_000L
    }
}
