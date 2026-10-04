package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * The loading indicator morphs and turns on its own, and holds a still frame under reduce motion.
 * The clock is driven by hand, so the infinite loop runs (an auto-advancing test clock holds it).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoadingMotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theLoadingIndicatorAnimates() {
        val (start, later) = frames(reduceMotion = false)
        assertFalse(start.samePixels(later))
    }

    @Test
    fun theLoadingIndicatorHoldsStillUnderReduceMotion() {
        val (start, later) = frames(reduceMotion = true)
        assertTrue(start.samePixels(later))
    }

    /** The indicator on its first frame, and [LATER_MILLIS] later. */
    private fun frames(reduceMotion: Boolean): Pair<ImageBitmap, ImageBitmap> {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CycleTheme(reduceMotion = reduceMotion) {
                Box(Modifier.testTag(TAG).background(CycleTheme.colors.surface)) { LoadingIndicator() }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        val start = composeRule.onNodeWithTag(TAG).captureToImage()
        composeRule.mainClock.advanceTimeBy(LATER_MILLIS)
        return start to composeRule.onNodeWithTag(TAG).captureToImage()
    }

    private fun ImageBitmap.samePixels(other: ImageBitmap): Boolean {
        if (width != other.width || height != other.height) return false
        val mine = toPixelMap()
        val theirs = other.toPixelMap()
        return (0 until width).all { x -> (0 until height).all { y -> mine[x, y] == theirs[x, y] } }
    }

    private companion object {
        const val TAG = "loading"

        /** Part way through the first step of the slow spatial spring. */
        const val LATER_MILLIS = 300L
    }
}
