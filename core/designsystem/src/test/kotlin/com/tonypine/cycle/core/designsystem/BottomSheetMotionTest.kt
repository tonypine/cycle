package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
 * [CycleBottomSheet] opens and closes on the slow spatial spring, which overshoots its anchor, and
 * jumps straight there under reduce motion. The test reads the sheet's offset frame by frame.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class BottomSheetMotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var state: CycleBottomSheetState
    private lateinit var scope: CoroutineScope
    private lateinit var density: Density

    @Test
    fun theSheetSpringsOpenPastItsAnchorAndSettles() {
        setSheet(reduceMotion = false)
        val frames = framesAfter { state.show() }

        val partial = partialAnchor()
        assertTrue("The spring overshoots the anchor: ${frames.min()} vs $partial", frames.min() < partial - 1f)
        assertTrue("Opening takes many frames, not a jump: ${frames.size}", frames.count { it != frames.last() } > 10)
        assertEquals(partial, frames.last(), 0.5f)
        assertEquals(CycleSheetValue.PartiallyExpanded, state.currentValue)
    }

    @Test
    fun theSheetSpringsClosed() {
        setSheet(reduceMotion = false)
        framesAfter { state.show() }

        val frames = framesAfter { state.hide() }

        assertTrue("Closing takes many frames, not a jump: ${frames.size}", frames.count { it != frames.last() } > 10)
        assertEquals(CycleSheetValue.Hidden, state.currentValue)
    }

    @Test
    fun underReduceMotionTheSheetJumpsOpenAndClosed() {
        setSheet(reduceMotion = true)
        val opening = framesAfter { state.show() }

        val partial = partialAnchor()
        assertTrue("No frame passes the anchor: ${opening.min()} vs $partial", opening.min() >= partial - 0.5f)
        assertTrue("It reaches the anchor at once: $opening", opening.count { it != partial } <= 1)

        val closing = framesAfter { state.hide() }
        assertTrue("It leaves at once: $closing", closing.count { it != closing.last() } <= 1)
        assertEquals(CycleSheetValue.Hidden, state.currentValue)
    }

    private fun partialAnchor(): Float =
        with(density) { state.anchoredState.anchors.positionOf(CycleSheetValue.PartiallyExpanded).toDp().value }

    private fun setSheet(reduceMotion: Boolean) {
        composeRule.setContent {
            CycleTheme(reduceMotion = reduceMotion) {
                state = rememberCycleBottomSheetState()
                scope = rememberCoroutineScope()
                density = LocalDensity.current
                CycleBottomSheet(state, title = "Log today") {
                    BasicText("Pick anything that fits.", style = CycleTheme.typography.body)
                    Spacer(Modifier.height(500.dp))
                }
            }
        }
    }

    /**
     * Runs [action] with the clock paused and returns the sheet's offset in dp at each frame once it
     * has anchors, until the action and its animation finish.
     */
    private fun framesAfter(action: suspend () -> Unit): List<Float> {
        composeRule.mainClock.autoAdvance = false
        var done = false
        composeRule.runOnIdle { scope.launch { action() }.invokeOnCompletion { done = true } }
        val frames = mutableListOf<Float>()
        var guard = 0
        while (!done && guard++ < MAX_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
            val offset = state.anchoredState.offset
            if (!offset.isNaN()) frames += with(density) { offset.toDp().value }
        }
        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        check(done) { "The sheet was still moving after $MAX_FRAMES frames" }
        return frames
    }

    private companion object {
        const val MAX_FRAMES = 600
    }
}
