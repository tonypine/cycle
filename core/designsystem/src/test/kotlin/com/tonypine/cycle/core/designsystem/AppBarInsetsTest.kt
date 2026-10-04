package com.tonypine.cycle.core.designsystem

import android.graphics.Insets
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Both bars in an edge-to-edge screen: the app draws behind the system bars and each bar pads itself.
 * Robolectric draws no system bars, so the test dispatches their insets itself, plus a display cutout
 * on the left as in landscape.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class AppBarInsetsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theTopAppBarSitsBelowTheStatusBarAndClearOfTheCutout() {
        setScreen()
        val bar = composeRule.onNodeWithTag(TOP_BAR).getBoundsInRoot()
        assertEquals("the bar fills behind the status bar", 0.dp, bar.top)

        val back = composeRule.onNodeWithContentDescription("Back").getBoundsInRoot()
        val title = composeRule.onNodeWithText("March 2027").getBoundsInRoot()
        assertTrue("Back at $back should sit below the status bar", back.top >= STATUS_BAR)
        assertTrue("Back at $back should clear the cutout", back.left >= CUTOUT)
        assertTrue("the title at $title should sit below the status bar", title.top >= STATUS_BAR)
    }

    @Test
    fun theNavigationBarSitsAboveTheSystemNavigationBar() {
        setScreen()
        val screen = composeRule.onRoot().getBoundsInRoot()
        listOf("Today", "Calendar", "Log", "Settings").forEach { label ->
            val tab = composeRule.onNodeWithText(label).getBoundsInRoot()
            val clear = tab.bottom <= screen.bottom - NAVIGATION_BAR
            assertTrue("$label at $tab should sit above the navigation bar", clear)
        }
    }

    private fun setScreen() {
        composeRule.activityRule.scenario.onActivity { it.window.setDecorFitsSystemWindows(false) }
        composeRule.setContent {
            CycleTheme(darkTheme = false, reduceMotion = true) {
                Column(Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = "March 2027",
                        modifier = Modifier.testTag(TOP_BAR),
                        navigation = AppBarAction(CycleIcons.Back, "Back", {}),
                        actions = listOf(AppBarAction(CycleIcons.Settings, "Settings", {}))
                    )
                    Box(Modifier.weight(1f).fillMaxWidth())
                    NavigationBar(
                        destinations = listOf(
                            NavigationDestination("Today", CycleIcons.Today),
                            NavigationDestination("Calendar", CycleIcons.Calendar),
                            NavigationDestination("Log", CycleIcons.Add),
                            NavigationDestination("Settings", CycleIcons.Settings)
                        ),
                        selectedIndex = 1,
                        onSelect = {}
                    )
                }
            }
        }
        composeRule.activityRule.scenario.onActivity { activity ->
            val density = activity.resources.displayMetrics.density
            fun Dp.px() = (value * density).toInt()
            val insets = WindowInsets.Builder()
                .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, STATUS_BAR.px(), 0, 0))
                .setInsets(WindowInsets.Type.navigationBars(), Insets.of(0, 0, 0, NAVIGATION_BAR.px()))
                .setInsets(WindowInsets.Type.displayCutout(), Insets.of(CUTOUT.px(), 0, 0, 0))
                .setVisible(WindowInsets.Type.statusBars(), true)
                .setVisible(WindowInsets.Type.navigationBars(), true)
                .build()
            activity.findViewById<ViewGroup>(android.R.id.content).dispatchApplyWindowInsets(insets)
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val TOP_BAR = "top-app-bar"
        val STATUS_BAR = 24.dp
        val NAVIGATION_BAR = 48.dp
        val CUTOUT = 32.dp
    }
}
