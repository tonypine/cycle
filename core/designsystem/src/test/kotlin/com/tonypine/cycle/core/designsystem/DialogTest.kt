package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

/** How the dialogs open, close, take and give back focus, and what TalkBack gets from them. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class DialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private var visible by mutableStateOf(true)
    private var dismissals = 0

    @Test
    fun theTitleIsAHeadingAndTalkBackReadsTitleTextThenActions() {
        showAlert()
        composeRule.onNodeWithText(TITLE).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val stops = composeRule.onAllNodes(hasAnyAncestor(isDialog()) and hasTextOrClick())
            .fetchSemanticsNodes()
            .map { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() }
        assertEquals(listOf(TITLE, TEXT, DISMISS, CONFIRM), stops)
        listOf(DISMISS, CONFIRM).forEach {
            composeRule.onNodeWithText(it).assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun theActionsCallTheirHandlers() {
        val clicks = mutableListOf<String>()
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                CycleAlertDialog(
                    visible = true,
                    onDismissRequest = { clicks += "dismiss request" },
                    title = TITLE,
                    text = TEXT,
                    confirmText = CONFIRM,
                    onConfirm = { clicks += "confirm" },
                    dismissText = DISMISS,
                    onDismiss = { clicks += "dismiss" }
                )
            }
        }
        composeRule.onNodeWithText(CONFIRM).performClick()
        composeRule.onNodeWithText(DISMISS).performClick()
        assertEquals(listOf("confirm", "dismiss"), clicks)
    }

    @Test
    fun backDismisses() {
        showAlert()
        pressBack()
        assertEquals(1, dismissals)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun backDoesNothingWhenTurnedOff() {
        showAlert(dismissOnBackPress = false)
        pressBack()
        assertEquals(0, dismissals)
        composeRule.onNode(isDialog()).assertExists()
    }

    @Test
    fun aTapOnTheScrimDismisses() {
        showAlert()
        tapScrim()
        assertEquals(1, dismissals)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun aTapOnTheScrimDoesNothingWhenTurnedOff() {
        showAlert(dismissOnScrimTap = false)
        tapScrim()
        assertEquals(0, dismissals)
        composeRule.onNode(isDialog()).assertExists()
    }

    @Test
    fun aTapOnTheDialogItselfDoesNotDismiss() {
        showAlert()
        composeRule.onNodeWithText(TEXT).performClick()
        assertEquals(0, dismissals)
        composeRule.onNode(isDialog()).assertExists()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun keyboardFocusEntersTheDialogAndReturnsToTheOpener() {
        visible = false
        lateinit var inputModeManager: InputModeManager
        composeRule.setContent {
            inputModeManager = LocalInputModeManager.current
            CycleTheme(reduceMotion = true) {
                Box(Modifier.fillMaxSize()) { FilledButton(OPENER, onClick = { visible = true }) }
                alert()
            }
        }
        // In keyboard mode, as with a hardware keyboard or D-pad, buttons take focus. Touch mode is
        // global on a device, so the dialog's window opens in keyboard mode too.
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        composeRule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val opener = composeRule.onNodeWithText(OPENER)
        opener.requestFocus()
        opener.assertIsFocused()

        opener.performKeyInput { pressKey(Key.Enter) }
        composeRule.onNode(isDialog()).assertExists()
        // Focus starts on the first action, the safe one.
        composeRule.onNodeWithText(DISMISS).assertIsFocused()

        pressBack()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        opener.assertIsFocused()
    }

    @Test
    fun aTextFieldInTheDialogTakesFocusWhenItOpens() {
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                CycleDialog(
                    visible = true,
                    onDismissRequest = {},
                    title = "Add a note",
                    confirmText = "Save",
                    onConfirm = {},
                    dismissText = "Cancel",
                    confirmEnabled = false
                ) {
                    CycleTextField(rememberTextFieldState(), label = "Note")
                }
            }
        }
        composeRule.onNode(hasSetTextAction()).assertIsFocused()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun theDestructiveDialogPairsItsErrorColourWithAnIconAndASentence() {
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                CycleDestructiveDialog(
                    visible = true,
                    onDismissRequest = {},
                    title = "Delete this day?",
                    text = "This removes the period and notes logged for 14 March. You can't undo it.",
                    confirmText = "Delete",
                    onConfirm = {},
                    dismissText = "Keep it"
                )
            }
        }
        composeRule.onNodeWithText(
            "Delete this day?"
        ).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeRule.onNodeWithText("Delete").assert(hasClickAction())
        composeRule.onNodeWithText("Keep it").assert(hasClickAction())
        composeRule.onNodeWithText("This removes", substring = true).assertExists()
    }

    @Test
    fun theActionsSitInARowAtTheEnd() {
        showAlert()
        val dismiss = composeRule.onNodeWithText(DISMISS).getBoundsInRoot()
        val confirm = composeRule.onNodeWithText(CONFIRM).getBoundsInRoot()
        assertEquals(dismiss.top, confirm.top)
        assertTrue("Confirm should come after dismiss: $dismiss, $confirm", confirm.left > dismiss.right)
    }

    @Test
    fun theActionsStackWhenTheyDoNotFitAt200Percent() {
        RuntimeEnvironment.setFontScale(2f)
        showAlert()
        val dismiss = composeRule.onNodeWithText(DISMISS).getBoundsInRoot()
        val confirm = composeRule.onNodeWithText(CONFIRM).getBoundsInRoot()
        assertTrue("Confirm should sit above dismiss: $confirm, $dismiss", confirm.bottom <= dismiss.top)
        assertEquals("Both should end at the same edge", confirm.right.value, dismiss.right.value, 1f)
    }

    @Test
    fun theDialogScalesInAndOut() {
        val frames = framesAfterToggle(reduceMotion = false)
        assertTrue("The dialog should still be growing a frame after it opens", frames.openingScale < 0.99f)
        assertTrue(
            "The dialog should take more than $SNAP_FRAMES frames to leave, took ${frames.framesToClose}",
            frames.framesToClose > SNAP_FRAMES
        )
    }

    @Test
    fun theDialogSnapsInAndOutUnderReduceMotion() {
        val frames = framesAfterToggle(reduceMotion = true)
        assertEquals(1f, frames.openingScale, 0.001f)
        assertTrue(
            "The dialog should leave within $SNAP_FRAMES frames, took ${frames.framesToClose}",
            frames.framesToClose <= SNAP_FRAMES
        )
    }

    /** How wide the title is a frame after the dialog opens, relative to its settled width, and how many frames it takes to close. */
    private class ToggleFrames(val openingScale: Float, val framesToClose: Int)

    private fun framesAfterToggle(reduceMotion: Boolean): ToggleFrames {
        visible = false
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { CycleTheme(reduceMotion = reduceMotion) { alert() } }
        composeRule.mainClock.advanceTimeByFrame()

        visible = true
        advanceFramesWhile { composeRule.onAllNodes(hasText(TITLE)).fetchSemanticsNodes().isEmpty() }
        composeRule.mainClock.advanceTimeByFrame()
        val title = composeRule.onNodeWithText(TITLE)
        val early = title.getBoundsInRoot().width
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        val settled = title.getBoundsInRoot().width

        visible = false
        val framesToClose = advanceFramesWhile { composeRule.onAllNodes(isDialog()).fetchSemanticsNodes().isNotEmpty() }
        return ToggleFrames(early / settled, framesToClose)
    }

    /** Advances the clock a frame at a time while [condition] holds, and returns how many frames that took. */
    private fun advanceFramesWhile(condition: () -> Boolean): Int {
        var frames = 0
        while (condition()) {
            check(frames++ < MAX_FRAMES) { "Still waiting after $MAX_FRAMES frames" }
            composeRule.mainClock.advanceTimeByFrame()
        }
        return frames
    }

    private fun showAlert(dismissOnBackPress: Boolean = true, dismissOnScrimTap: Boolean = true) {
        composeRule.setContent {
            CycleTheme(reduceMotion = true) { alert(dismissOnBackPress, dismissOnScrimTap) }
        }
        composeRule.onNode(isDialog()).assertExists()
    }

    @androidx.compose.runtime.Composable
    private fun alert(dismissOnBackPress: Boolean = true, dismissOnScrimTap: Boolean = true) {
        CycleAlertDialog(
            visible = visible,
            onDismissRequest = {
                dismissals++
                visible = false
            },
            title = TITLE,
            text = TEXT,
            confirmText = CONFIRM,
            onConfirm = {},
            dismissText = DISMISS,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnScrimTap = dismissOnScrimTap
        )
    }

    /** Presses back in the dialog window, as the system does. */
    private fun pressBack() {
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            // Dialog.onBackPressed hands the press to the dialog's OnBackPressedDispatcher.
            @Suppress("DEPRECATION")
            ShadowDialog.getLatestDialog().onBackPressed()
        }
        composeRule.waitForIdle()
    }

    /** Taps the scrim near the dialog window's top corner, well away from the dialog. */
    private fun tapScrim() {
        composeRule.onNode(isDialog()).performTouchInput { click(Offset(8f, 8f)) }
        composeRule.waitForIdle()
    }

    private fun hasTextOrClick() = SemanticsMatcher.keyIsDefined(SemanticsProperties.Text) or hasClickAction()

    private companion object {
        const val TITLE = "Turn on reminders?"
        const val TEXT = "Cycle can nudge you a day before your period is due."
        const val CONFIRM = "Remind me"
        const val DISMISS = "Not now"
        const val OPENER = "Reminders"
        const val SETTLE_MILLIS = 2_000L
        const val MAX_FRAMES = 120

        /** A snapped dialog opens or closes within this many frames: one to recompose, one to remove the window. */
        const val SNAP_FRAMES = 3
    }
}
