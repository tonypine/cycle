package com.tonypine.cycle.feature.onboarding

import android.graphics.Insets
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Step 2 on a small phone, edge to edge, with the keyboard open: the focused field and the line
 * under it stay above the keyboard. Robolectric has no keyboard, so the test applies IME insets
 * itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class OnboardingImeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the focused length field stays above the keyboard`() {
        composeRule.activityRule.scenario.onActivity { it.window.setDecorFitsSystemWindows(false) }
        composeRule.setContent {
            Themed { OnboardingScreen(today = LocalDate.of(2027, 3, 20), onSkip = {}, onDone = { _, _, _ -> }) }
        }
        composeRule.onNodeWithText("Get started").performClick()
        composeRule.onNodeWithText("I don't remember").performScrollTo().performClick()

        val field = composeRule.onNode(hasSetTextAction() and hasText("Period length, in days", substring = true))
        val visibleBottom = composeRule.onRoot().getBoundsInRoot().bottom - IME_HEIGHT
        val before = field.getBoundsInRoot()
        assertTrue(
            "Period length should start below the keyboard's top edge, at $before",
            before.bottom > visibleBottom
        )

        field.requestFocus()
        showKeyboard()

        val after = field.getBoundsInRoot()
        assertTrue("Period length should sit above the keyboard, at $after", after.bottom <= visibleBottom)
        assertTrue("Period length should not scroll off the top, at $after", after.top >= 0.dp)
        field.assertIsFocused()
    }

    /** Applies IME insets to the content view, as the system does when the keyboard opens. */
    private fun showKeyboard() {
        composeRule.activityRule.scenario.onActivity { activity ->
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val height = (IME_HEIGHT.value * activity.resources.displayMetrics.density).toInt()
            val insets = WindowInsets.Builder()
                .setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, height))
                .setVisible(WindowInsets.Type.ime(), true)
                .build()
            content.dispatchApplyWindowInsets(insets)
        }
        composeRule.waitForIdle()
    }

    private companion object {
        val IME_HEIGHT = 300.dp
    }
}
