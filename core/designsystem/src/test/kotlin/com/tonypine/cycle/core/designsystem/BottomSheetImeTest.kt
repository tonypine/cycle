package com.tonypine.cycle.core.designsystem

import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.showKeyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A text field low in a partially expanded [CycleBottomSheet] stays visible when the keyboard opens.
 * Robolectric has no keyboard, so the test dispatches IME insets to the sheet's window itself.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class BottomSheetImeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var state: CycleBottomSheetState
    private lateinit var scope: CoroutineScope
    private lateinit var sheetView: View

    @Test
    fun theFocusedFieldStaysAboveTheKeyboard() {
        setSheet()
        composeRule.runOnIdle { scope.launch { state.show() } }
        composeRule.waitForIdle()
        assertEquals(CycleSheetValue.PartiallyExpanded, state.currentValue)
        val field = composeRule.onNode(hasSetTextAction() and hasText("Notes"))
        assertTrue("Notes starts below the screen, at ${notesBounds()}", notesBounds().top.value > SCREEN_HEIGHT)

        field.requestFocus()
        composeRule.runOnIdle { sheetView.showKeyboard(IME_HEIGHT) }
        composeRule.waitForIdle()

        assertEquals("The keyboard expands the sheet", CycleSheetValue.Expanded, state.currentValue)
        val visibleBottom = SCREEN_HEIGHT - IME_HEIGHT
        val notes = notesBounds()
        assertTrue(
            "Notes and its supporting text sit above the keyboard, at $notes",
            notes.bottom.value <= visibleBottom
        )
        assertTrue("Notes stays on screen, at $notes", notes.top.value >= 0f)
        field.assertIsFocused()
    }

    private fun setSheet() {
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                state = rememberCycleBottomSheetState()
                scope = rememberCoroutineScope()
                CycleBottomSheet(state, title = "Add a note") {
                    sheetView = LocalView.current
                    BasicText("Anything worth remembering about today?", style = CycleTheme.typography.body)
                    Spacer(Modifier.height(360.dp))
                    CycleTextField(
                        state = TextFieldState(),
                        label = "Notes",
                        modifier = Modifier.fillMaxWidth().testTag(NOTES_TAG),
                        supportingText = "Only on this phone."
                    )
                    Spacer(Modifier.height(200.dp))
                }
            }
        }
    }

    private fun notesBounds() = composeRule.onNode(hasTestTag(NOTES_TAG)).getBoundsInRoot()

    private companion object {
        const val NOTES_TAG = "notes"
        const val SCREEN_HEIGHT = 640f
        const val IME_HEIGHT = 300f
    }
}
