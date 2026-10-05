package com.tonypine.cycle

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.designsystem.CycleTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The app with no data: the welcome, then, after Skip, Today's empty state, an empty calendar and
 * History's empty state, the navigation bar under each, in light and dark. Records
 * `src/test/screenshots/app_<screen>_<appearance>.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class CycleAppScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun light() = capture(darkTheme = false, "light")

    @Test
    fun dark() = capture(darkTheme = true, "dark")

    private fun capture(darkTheme: Boolean, appearance: String) {
        val data = (composeRule.activity.application as CycleApplication).data
        composeRule.setContent {
            CycleTheme(darkTheme = darkTheme, reduceMotion = true) {
                CycleApp(data, FakeDeviceLock(), today = { LocalDate.of(2027, 3, 20) })
            }
        }
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("Skip for now")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_welcome_$appearance.png")

        composeRule.onNodeWithText("Skip for now").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("No periods logged yet")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_today_$appearance.png")

        composeRule.onNodeWithText("Calendar").performClick()
        // The log loads on Room's and DataStore's threads.
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("March 2027")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_calendar_$appearance.png")

        composeRule.onNodeWithText("History").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodes(hasText("No cycles yet")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_history_$appearance.png")
    }
}
