package com.tonypine.cycle.core.designsystem

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Opening, dragging and every way of closing [CycleBottomSheet], and what TalkBack gets from it. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class BottomSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var state: CycleBottomSheetState
    private lateinit var scope: CoroutineScope
    private var backDispatcher: OnBackPressedDispatcher? = null
    private var dismissals = 0
    private lateinit var inputModeManager: InputModeManager

    @Test
    fun opensPartiallyExpandedAndDragsBetweenAnchors() {
        setSheet()
        open()

        assertEquals(CycleSheetValue.PartiallyExpanded, state.currentValue)
        composeRule.onNodeWithText(TITLE).assertIsDisplayed()
        assertEquals(SCREEN_HEIGHT / 2, sheetTop().value, 1f)

        sheet().performTouchInput { swipeUp(startY = centerY, endY = centerY - 300.dp.toPx()) }
        composeRule.waitForIdle()
        assertEquals(CycleSheetValue.Expanded, state.currentValue)
        assertTrue("An expanded sheet shows its top, at ${sheetTop()}", sheetTop().value < SCREEN_HEIGHT / 2)

        sheet().performTouchInput { swipeDown(startY = centerY, endY = centerY + 150.dp.toPx()) }
        composeRule.waitForIdle()
        assertEquals(CycleSheetValue.PartiallyExpanded, state.currentValue)
        assertEquals(0, dismissals)
    }

    @Test
    fun draggingDownDismisses() {
        setSheet()
        open()

        sheet().performTouchInput { swipeDown(startY = top, endY = top + 400.dp.toPx()) }
        composeRule.waitForIdle()

        assertClosed()
    }

    @Test
    fun tappingTheScrimDismisses() {
        setSheet()
        open()

        composeRule.onNode(isDialog()).performTouchInput { click(center.copy(y = 40.dp.toPx())) }
        composeRule.waitForIdle()

        assertClosed()
    }

    @Test
    fun tappingTheSheetDoesNotDismiss() {
        setSheet()
        open()

        composeRule.onNodeWithText(BODY).performTouchInput { click() }
        composeRule.waitForIdle()

        assertTrue(state.isVisible)
        assertEquals(0, dismissals)
    }

    @Test
    fun backDismisses() {
        setSheet()
        open()

        composeRule.runOnUiThread { checkNotNull(backDispatcher).onBackPressed() }
        composeRule.waitForIdle()

        assertClosed()
    }

    @Test
    fun aCancelledPredictiveBackKeepsTheSheetAndACompletedOneDismisses() {
        setSheet()
        open()
        val dispatcher = checkNotNull(backDispatcher)
        val restingWidth = contentWidth()

        composeRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(backEvent(0f))
            dispatcher.dispatchOnBackProgressed(backEvent(0.6f))
        }
        composeRule.waitForIdle()
        assertTrue("The sheet narrows with the gesture: ${contentWidth()}", contentWidth() < restingWidth)
        composeRule.runOnUiThread { dispatcher.dispatchOnBackCancelled() }
        composeRule.waitForIdle()
        assertTrue(state.isVisible)
        assertEquals(CycleSheetValue.PartiallyExpanded, state.currentValue)
        assertEquals(restingWidth, contentWidth(), 0.5f)

        composeRule.runOnUiThread {
            dispatcher.dispatchOnBackStarted(backEvent(0f))
            dispatcher.dispatchOnBackProgressed(backEvent(0.8f))
            dispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        assertClosed()
    }

    @Test
    fun theHandleExpandsCollapsesAndDismissesThroughAccessibilityActions() {
        setSheet()
        open()
        val handle = composeRule.onNodeWithContentDescription(HANDLE)
        handle
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Partially expanded"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        handle.performCustomAccessibilityActionWithLabel("Expand")
        composeRule.waitForIdle()
        assertEquals(CycleSheetValue.Expanded, state.currentValue)
        handle.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))

        handle.performCustomAccessibilityActionWithLabel("Collapse")
        composeRule.waitForIdle()
        assertEquals(CycleSheetValue.PartiallyExpanded, state.currentValue)

        handle.performCustomAccessibilityActionWithLabel("Dismiss")
        composeRule.waitForIdle()
        assertClosed()
    }

    @Test
    fun tappingTheHandleTogglesExpanded() {
        setSheet()
        open()

        composeRule.onNodeWithContentDescription(HANDLE).performClick()
        composeRule.waitForIdle()
        assertEquals(CycleSheetValue.Expanded, state.currentValue)

        composeRule.onNodeWithContentDescription(HANDLE).performClick()
        composeRule.waitForIdle()
        assertEquals(CycleSheetValue.PartiallyExpanded, state.currentValue)
    }

    @Test
    fun theStateHolderShowsExpandsAndHides() {
        setSheet()
        assertFalse(state.isVisible)
        composeRule.onNode(isDialog()).assertDoesNotExist()

        composeRule.runOnIdle { scope.launch { state.show() } }
        composeRule.waitForIdle()
        assertTrue(state.isVisible)
        assertEquals(CycleSheetValue.PartiallyExpanded, state.currentValue)

        composeRule.runOnIdle { scope.launch { state.expand() } }
        composeRule.waitForIdle()
        assertEquals(CycleSheetValue.Expanded, state.currentValue)

        composeRule.runOnIdle { scope.launch { state.hide() } }
        composeRule.waitForIdle()
        assertClosed()

        composeRule.runOnIdle { scope.launch { state.expand() } }
        composeRule.waitForIdle()
        assertEquals("expand() opens a closed sheet expanded", CycleSheetValue.Expanded, state.currentValue)
    }

    @Test
    fun aShortSheetOpensExpanded() {
        setSheet(tall = false)
        open()

        assertEquals(CycleSheetValue.Expanded, state.currentValue)
        assertFalse(state.hasPartiallyExpandedState)
        composeRule.onNodeWithContentDescription(HANDLE)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
            .performCustomAccessibilityActionWithLabel("Dismiss")
        composeRule.waitForIdle()
        assertClosed()
    }

    @Test
    fun talkBackReadsAPaneWithItsTitleThenTheHandleTitleAndContent() {
        setSheet()
        open()

        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, TITLE)).assertExists()
        composeRule.onNodeWithText(TITLE).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val handleTop = composeRule.onNodeWithContentDescription(HANDLE).getBoundsInRoot().top
        val titleTop = composeRule.onNodeWithText(TITLE).getBoundsInRoot().top
        val bodyTop = composeRule.onNodeWithText(BODY).getBoundsInRoot().top
        assertTrue("Reading order is handle, title, content", handleTop < titleTop && titleTop < bodyTop)
    }

    @Test
    fun focusMovesToTheHandleOnOpenAndBackToTheOpenerOnClose() {
        setSheet()
        // A keyboard user moves to the opener and presses it.
        composeRule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val opener = composeRule.onNodeWithText(OPEN).requestFocus().assertIsFocused()
        opener.performKeyInput { pressKey(Key.Enter) }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(HANDLE).assertIsFocused()

        composeRule.runOnUiThread { checkNotNull(backDispatcher).onBackPressed() }
        composeRule.waitForIdle()
        assertClosed()
        opener.assertIsFocused()
    }

    private fun setSheet(tall: Boolean = true) {
        composeRule.setContent {
            CycleTheme(reduceMotion = false) {
                state = rememberCycleBottomSheetState()
                scope = rememberCoroutineScope()
                inputModeManager = LocalInputModeManager.current
                Box(Modifier.fillMaxSize().background(CycleTheme.colors.surface)) {
                    FilledButton(OPEN, onClick = { scope.launch { state.show() } })
                }
                CycleBottomSheet(state, title = TITLE, onDismiss = { dismissals++ }) {
                    backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                    BasicText(BODY, Modifier.fillMaxWidth(), style = CycleTheme.typography.body)
                    if (tall) Spacer(Modifier.height(500.dp))
                    TextButton("Nah", onClick = {})
                }
            }
        }
    }

    private fun open() {
        composeRule.onNodeWithText(OPEN).performClick()
        composeRule.waitForIdle()
    }

    private fun sheet() = composeRule.onNodeWithText(BODY)

    private fun sheetTop() = composeRule.onNodeWithContentDescription(HANDLE).getBoundsInRoot().top

    /** The width of the content column, which the predictive back scale narrows. */
    private fun contentWidth(): Float = composeRule.onNodeWithText(BODY).getBoundsInRoot().let {
        (it.right - it.left).value
    }

    private fun assertClosed() {
        assertFalse(state.isVisible)
        assertEquals(CycleSheetValue.Hidden, state.currentValue)
        composeRule.onNode(isDialog()).assertDoesNotExist()
        assertEquals(1, dismissals)
    }

    private fun backEvent(progress: Float) = BackEventCompat(0f, 320f, progress, BackEventCompat.EDGE_LEFT)

    private companion object {
        const val OPEN = "Log today"
        const val TITLE = "How was today?"
        const val BODY = "Pick anything that fits."
        const val HANDLE = "Drag handle"
        const val SCREEN_HEIGHT = 640f
    }
}
