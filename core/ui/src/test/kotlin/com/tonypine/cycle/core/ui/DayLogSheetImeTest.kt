package com.tonypine.cycle.core.ui

import android.graphics.Insets
import android.view.WindowInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/**
 * The day log sheet with the keyboard up on "Your note": the whole field, with its supporting text,
 * sits above the keyboard, and so does "Log it" when there is room, or it is a scroll away. In
 * landscape the keyboard leaves about 120dp below the status bar, and the sheet's title scrolls away
 * to make room. Robolectric has no keyboard, so the test applies IME insets to the sheet's window, a
 * frame at a time as the keyboard slides in. Synthetic days and notes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class DayLogSheetImeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the note field and Log it show above the keyboard`() {
        typeANote(keyboard = 300.dp)

        assertAboveTheKeyboard("Log it", composeRule.onNodeWithText("Log it").bounds(), keyboard = 300.dp)
    }

    @Test
    fun `at 200 percent font scale the note field shows and Log it is a scroll away`() {
        typeANote(keyboard = 300.dp, fontScale = 2f)

        assertLogItIsAScrollAway(keyboard = 300.dp)
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun `in landscape the note field shows and Log it is a scroll away`() {
        typeANote(keyboard = LANDSCAPE_KEYBOARD)

        assertLogItIsAScrollAway(keyboard = LANDSCAPE_KEYBOARD)
    }

    @Test
    @Config(qualifiers = LANDSCAPE)
    fun `in landscape at 200 percent font scale the note field shows and the rest is a scroll away`() {
        typeANote(keyboard = LANDSCAPE_KEYBOARD, fontScale = 2f, supportingTextFits = false)

        val supportingText = composeRule.onNodeWithText("Only on this phone.").performScrollTo().bounds()
        assertAboveTheKeyboard("The note's supporting text", supportingText, LANDSCAPE_KEYBOARD)
        assertLogItIsAScrollAway(keyboard = LANDSCAPE_KEYBOARD)
    }

    /**
     * Picks Pain: Moderate and taps "Your note" where it peeks in at the bottom of the sheet. The
     * field still has focus from before she closed the keyboard, so tapping it does not scroll it
     * into view. Then the keyboard slides in. Its supporting text shows below it unless
     * [supportingTextFits] is false: at 200% in landscape the field alone fills the space left.
     */
    private fun typeANote(keyboard: Dp, fontScale: Float = 1f, supportingTextFits: Boolean = true) {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed { OpenDayLogSheet(DayLogSamples.empty) }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNode(hasContentDescription("Moderate, Pain")).performSemanticsAction(SemanticsActions.OnClick)
        note().requestFocus()
        val peekBy = note().bounds().top - (windowBottom() - PEEK)
        sheetContent().performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> scrollBy(0f, peekBy.toPx()) }
        val peeking = note().bounds()
        assertTrue(
            "The note field should start cut by the sheet's bottom, at $peeking",
            peeking.top < windowBottom() && peeking.bottom > windowBottom()
        )

        showKeyboard(keyboard)

        note().assertIsFocused()
        if (supportingTextFits) {
            val supportingText = composeRule.onNodeWithText("Only on this phone.").bounds()
            val field = note().bounds().copy(bottom = supportingText.bottom)
            assertAboveTheKeyboard("The note field and its supporting text", field, keyboard)
        } else {
            assertAboveTheKeyboard("The note field", note().bounds(), keyboard)
        }
    }

    private fun assertLogItIsAScrollAway(keyboard: Dp) {
        val logIt = composeRule.onNodeWithText("Log it").performScrollTo().bounds()
        assertAboveTheKeyboard("Log it", logIt, keyboard)
        note().assertIsFocused()
    }

    private fun assertAboveTheKeyboard(what: String, bounds: DpRect, keyboard: Dp) {
        val keyboardTop = windowBottom() - keyboard
        assertTrue("$what should end above the keyboard at $keyboardTop, at $bounds", bounds.bottom <= keyboardTop)
        val contentTop = sheetContent().getBoundsInRoot().top
        assertTrue(
            "$what should not scroll under the sheet's title at $contentTop, at $bounds",
            bounds.top >= contentTop
        )
    }

    private fun note() = composeRule.onNode(hasSetTextAction())

    /** The sheet's scrolling content, below its title. */
    private fun sheetContent() = composeRule.onNode(hasScrollAction() and hasAnyDescendant(hasSetTextAction()))

    /** Where the node is laid out in its window, including any part scrolled out of the sheet's view. */
    private fun SemanticsNodeInteraction.bounds(): DpRect = with(composeRule.density) {
        val node = fetchSemanticsNode()
        val topLeft = node.positionInRoot
        DpRect(
            left = topLeft.x.toDp(),
            top = topLeft.y.toDp(),
            right = (topLeft.x + node.size.width).toDp(),
            bottom = (topLeft.y + node.size.height).toDp()
        )
    }

    private fun windowBottom() = composeRule.onNode(isDialog()).getBoundsInRoot().bottom

    private fun Dp.toPx() = value * composeRule.density.density

    /**
     * Slides the keyboard in over the sheet's window, [height] tall, the way the system animates it,
     * below a status bar as tall as a phone's.
     */
    private fun showKeyboard(height: Dp) {
        val window = checkNotNull(ShadowDialog.getLatestDialog().window)
        for (step in 1..KEYBOARD_FRAMES) {
            composeRule.runOnIdle {
                val px = (height.toPx() * step / KEYBOARD_FRAMES).toInt()
                val insets = WindowInsets.Builder()
                    .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, STATUS_BAR.toPx().toInt(), 0, 0))
                    .setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, px))
                    .setVisible(WindowInsets.Type.ime(), true)
                    .build()
                window.decorView.dispatchApplyWindowInsets(insets)
            }
            composeRule.mainClock.advanceTimeByFrame()
        }
        composeRule.waitForIdle()
    }

    private companion object {
        const val KEYBOARD_FRAMES = 8

        /** A phone on its side, 392dp tall, with the keyboard it shows there. */
        const val LANDSCAPE = "w872dp-h392dp-land-mdpi"
        val LANDSCAPE_KEYBOARD = 250.dp

        val STATUS_BAR = 24.dp

        /** How much of the note field shows at the bottom of the sheet when she taps it. */
        val PEEK = 40.dp
    }
}
