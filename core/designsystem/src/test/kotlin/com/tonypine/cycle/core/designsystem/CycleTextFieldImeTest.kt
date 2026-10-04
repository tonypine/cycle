package com.tonypine.cycle.core.designsystem

import android.graphics.Insets
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [CycleTextField] in an edge-to-edge screen that scrolls and pads itself above the keyboard, the
 * way feature screens use it. Robolectric has no keyboard, so the test dispatches IME insets itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class CycleTextFieldImeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun focusedFieldStaysAboveTheKeyboard() {
        setForm()
        val field = composeRule.onNode(hasSetTextAction() and hasText("Notes"))
        val visibleBottom = composeRule.onRoot().getBoundsInRoot().bottom - IME_HEIGHT

        field.requestFocus()
        composeRule.waitForIdle()
        val before = composeRule.onNodeWithTagBounds(NOTES_TAG)
        assertTrue("Notes should start below the keyboard's top edge, at $before", before.bottom > visibleBottom)

        showKeyboard()

        val after = composeRule.onNodeWithTagBounds(NOTES_TAG)
        assertTrue(
            "Notes and its supporting text should sit above the keyboard, at $after",
            after.bottom <= visibleBottom
        )
        assertTrue("Notes should not scroll off the top, at $after", after.top >= 0.dp)
        field.assertIsFocused()
    }

    @Test
    fun imeNextMovesFocusToTheNextField() {
        setForm()
        val first = composeRule.onNode(hasSetTextAction() and hasText("Period start"))

        first.requestFocus()
        first.performImeAction()

        composeRule.onNode(hasSetTextAction() and hasText("Notes")).assertIsFocused()
    }

    private fun setForm() {
        // Edge to edge: the app draws behind the system bars and the keyboard, and handles insets itself.
        composeRule.activityRule.scenario.onActivity { it.window.setDecorFitsSystemWindows(false) }
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)
                ) {
                    Spacer(Modifier.height(420.dp))
                    CycleTextField(
                        state = TextFieldState(),
                        label = "Period start",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    CycleTextField(
                        state = TextFieldState(),
                        label = "Notes",
                        modifier = Modifier.testTag(NOTES_TAG),
                        supportingText = "Only on this phone.",
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    Spacer(Modifier.height(420.dp))
                }
            }
        }
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

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithTagBounds(tag: String) =
        onNode(androidx.compose.ui.test.hasTestTag(tag)).getBoundsInRoot()

    private companion object {
        const val NOTES_TAG = "notes"
        val IME_HEIGHT = 300.dp
    }
}
