package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The navigation bar's selection pill slides to the new destination on the spatial springs and
 * stretches on the way, because its leading edge moves on the fast spring and its trailing edge on
 * the default one. Under reduce motion it jumps. The tests find the pill in a screenshot by its
 * `accentContainer` pixels along its middle row: the first and last are its edges.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class NavigationBarMotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun thePillStretchesAsItSlidesToTheRight() {
        val (resting, early, settled) = pillSpans(reduceMotion = false, from = 0, to = 3)
        assertEquals(PILL_WIDTH, resting.width, EDGE_TOLERANCE)
        assertTrue("mid-spring pill $early should be wider than at rest", early.width > PILL_WIDTH + 16)
        assertTrue("mid-spring pill $early should have left the first slot", early.end > resting.end)
        assertTrue("mid-spring pill $early should not have reached the last slot", early.start < settled.start)
        assertEquals(PILL_WIDTH, settled.width, EDGE_TOLERANCE)
    }

    @Test
    fun thePillStretchesAsItSlidesToTheLeft() {
        val (resting, early, settled) = pillSpans(reduceMotion = false, from = 3, to = 0)
        assertTrue("mid-spring pill $early should be wider than at rest", early.width > PILL_WIDTH + 16)
        assertTrue("mid-spring pill $early should have left the last slot", early.start < resting.start)
        assertEquals(PILL_WIDTH, settled.width, EDGE_TOLERANCE)
    }

    @Test
    fun thePillJumpsUnderReduceMotion() {
        val (resting, early, settled) = pillSpans(reduceMotion = true, from = 0, to = 3)
        assertNotEquals(resting, settled)
        assertEquals(settled, early)
    }

    @Test
    fun theWholeBarSnapsUnderReduceMotion() {
        val (early, settled) = frames(reduceMotion = true, from = 0, to = 3)
        assertTrue(early.samePixels(settled))
    }

    @Test
    fun theWholeBarAnimates() {
        val (early, settled) = frames(reduceMotion = false, from = 0, to = 3)
        assertTrue(!early.samePixels(settled))
    }

    /** The pill's span in pixels (1px is 1dp): at rest, [EARLY_FRAMES] frames after the change, and settled. */
    private fun pillSpans(reduceMotion: Boolean, from: Int, to: Int): Triple<Span, Span, Span> {
        val (resting, early, settled) = images(reduceMotion, from, to)
        return Triple(resting.pillSpan(), early.pillSpan(), settled.pillSpan())
    }

    private fun frames(reduceMotion: Boolean, from: Int, to: Int): Pair<ImageBitmap, ImageBitmap> {
        val (_, early, settled) = images(reduceMotion, from, to)
        return early to settled
    }

    private fun images(reduceMotion: Boolean, from: Int, to: Int): Triple<ImageBitmap, ImageBitmap, ImageBitmap> {
        var selected by mutableIntStateOf(from)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CycleTheme(darkTheme = false, reduceMotion = reduceMotion) {
                Box(Modifier.testTag(TAG).background(CycleTheme.colors.surface)) {
                    NavigationBar(Destinations, selected, onSelect = {})
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        val resting = composeRule.onNodeWithTag(TAG).captureToImage()
        composeRule.runOnUiThread {
            selected = to
            Snapshot.sendApplyNotifications()
        }
        repeat(EARLY_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }
        val early = composeRule.onNodeWithTag(TAG).captureToImage()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        return Triple(resting, early, composeRule.onNodeWithTag(TAG).captureToImage())
    }

    private data class Span(val start: Int, val end: Int) {
        val width: Float get() = (end - start + 1).toFloat()
    }

    private fun ImageBitmap.pillSpan(): Span {
        val pixels = toPixelMap()
        val pill = (0 until width).filter { x -> pixels[x, PILL_ROW].isCloseTo(LightCycleColors.accentContainer) }
        check(pill.isNotEmpty()) { "no pill on row $PILL_ROW" }
        return Span(pill.first(), pill.last())
    }

    private fun Color.isCloseTo(other: Color) =
        abs(red - other.red) < 0.02f && abs(green - other.green) < 0.02f && abs(blue - other.blue) < 0.02f

    private fun ImageBitmap.samePixels(other: ImageBitmap): Boolean {
        if (width != other.width || height != other.height) return false
        val mine = toPixelMap()
        val theirs = other.toPixelMap()
        return (0 until width).all { x -> (0 until height).all { y -> mine[x, y] == theirs[x, y] } }
    }

    private companion object {
        const val TAG = "navigation-bar"

        /** Enough frames to recompose, start the animation and run it a little. */
        const val EARLY_FRAMES = 4
        const val SETTLE_MILLIS = 2_000L

        /** The bar's 12dp inner padding, plus half the 32dp pill: its full width, either side of the icon. */
        const val PILL_ROW = 28
        const val PILL_WIDTH = 56f
        const val EDGE_TOLERANCE = 2f

        val Destinations = listOf(
            NavigationDestination("Today", CycleIcons.Today),
            NavigationDestination("Calendar", CycleIcons.Calendar),
            NavigationDestination("Log", CycleIcons.Add),
            NavigationDestination("Settings", CycleIcons.Settings)
        )
    }
}
