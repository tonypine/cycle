package com.tonypine.cycle

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
 * Setup from the welcome to Today on the app's own storage, with a synthetic day: on 20 March 2027,
 * a last period on 2 March, 28 and 5. Records `src/test/screenshots/app_today_after_setup.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class CycleAppSetupTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `setup gives day 19 and an estimate from the lengths she gave`() {
        val data = (composeRule.activity.application as CycleApplication).data
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                CycleApp(data, FakeDeviceLock(), today = { LocalDate.of(2027, 3, 20) })
            }
        }
        waitForText("Get started")
        composeRule.onNodeWithText("Get started").performClick()
        composeRule.onNodeWithContentDescription("2 March").performClick()
        composeRule.onNodeWithText("Next").performScrollTo().performClick()
        composeRule.onNodeWithText("Done").performScrollTo().performClick()

        waitForText("Day 19")
        composeRule.onNodeWithText("Day 19").assertIsDisplayed()
        composeRule.onNodeWithText("Around 30 March", substring = true).performScrollTo()
        composeRule.onNodeWithText("Between 26 March and 3 April", substring = true).assertExists()
        composeRule.onNodeWithText("Estimated from the lengths you gave", substring = true).assertExists()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_today_after_setup.png")
    }

    private fun waitForText(text: String) = composeRule.waitUntil(WAIT_MILLIS) {
        composeRule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private companion object {
        // Room and DataStore write on their own threads.
        const val WAIT_MILLIS = 5_000L
    }
}
