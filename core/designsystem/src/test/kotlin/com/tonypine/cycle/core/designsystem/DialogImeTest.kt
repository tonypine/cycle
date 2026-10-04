package com.tonypine.cycle.core.designsystem

import android.graphics.Insets
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/**
 * A [CycleDialog] holding a text field, with the keyboard open. Robolectric has no keyboard, so the
 * test dispatches IME insets to the dialog's window itself, as the system does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class DialogImeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theFocusedFieldAndTheActionsStayAboveTheKeyboard() {
        composeRule.setContent { CycleTheme(reduceMotion = true) { NoteDialog() } }
        val field = composeRule.onNode(hasSetTextAction())
        field.assertIsFocused()
        val screenBottom = composeRule.onNode(isDialog()).getBoundsInRoot().bottom
        val keyboardTop = screenBottom - IME_HEIGHT
        val saveBefore = composeRule.onNodeWithText("Save").getBoundsInRoot()
        assertTrue(
            "Save should start where the keyboard will cover it, at $saveBefore",
            saveBefore.bottom > keyboardTop
        )

        showKeyboard()

        field.assertIsFocused()
        val note = composeRule.onNode(hasTestTag(NOTE_TAG)).getBoundsInRoot()
        assertAbove("The note field, with its supporting text,", note, keyboardTop)
        listOf("Save", "Cancel").forEach {
            assertAbove("The $it action", composeRule.onNodeWithText(it).getBoundsInRoot(), keyboardTop)
        }
        assertTrue(
            "The title should not scroll off the top",
            composeRule.onNodeWithText(TITLE).getBoundsInRoot().top >= 0.dp
        )
    }

    @Test
    fun theDialogShrinksAboveTheKeyboardAndItsContentScrolls() {
        composeRule.setContent { CycleTheme(reduceMotion = true) { NoteDialog() } }
        val introBefore = composeRule.onNodeWithText(INTRO).getBoundsInRoot()

        showKeyboard()

        // The intro sits above the field in the scrolling content: it moves up out of the
        // shrunken dialog so the focused field is visible.
        val introAfter = composeRule.onNodeWithText(INTRO).getBoundsInRoot()
        assertTrue("The content should scroll, from $introBefore to $introAfter", introAfter.top < introBefore.top)
    }

    private fun assertAbove(what: String, bounds: DpRect, keyboardTop: Dp) {
        assertTrue("$what should sit above the keyboard ($keyboardTop), at $bounds", bounds.bottom <= keyboardTop)
    }

    /** Applies IME insets to the dialog's window, as the system does when the keyboard opens. */
    private fun showKeyboard() {
        composeRule.runOnUiThread {
            val window = ShadowDialog.getLatestDialog().window!!
            val height = (IME_HEIGHT.value * window.context.resources.displayMetrics.density).toInt()
            val insets = WindowInsets.Builder()
                .setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, height))
                .setVisible(WindowInsets.Type.ime(), true)
                .build()
            window.decorView.dispatchApplyWindowInsets(insets)
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun NoteDialog() {
        CycleDialog(
            visible = true,
            onDismissRequest = {},
            title = TITLE,
            confirmText = "Save",
            onConfirm = {},
            dismissText = "Cancel",
            text = INTRO
        ) {
            CycleTextField(
                rememberTextFieldState(),
                label = "Note",
                modifier = Modifier.testTag(NOTE_TAG),
                supportingText = "Only on this phone."
            )
        }
    }

    private companion object {
        const val TITLE = "Add a note"
        const val INTRO = "A few words about the day: how you slept, what you ate, anything that stood out. " +
            "Notes stay on this phone and never leave it. They show on the day in the calendar, under " +
            "the symptoms you logged, and you can change them at any time."
        const val NOTE_TAG = "note"
        val IME_HEIGHT = 300.dp
    }
}
