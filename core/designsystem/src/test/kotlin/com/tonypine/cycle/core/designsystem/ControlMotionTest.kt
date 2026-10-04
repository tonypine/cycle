package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Buttons squash from a pill to 14dp corners while pressed, filter chips grow from 12dp corners into a
 * pill when selected, button group segments grow from 8dp corners into a pill when selected, and the
 * switch thumb slides across when checked, all on a spring that snaps under reduce motion. The shape
 * and thumb tests read the corner radius or thumb position frame by frame; the component tests compare
 * a few frames in with the settled component, so the colours, press scale and check icon must snap too.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ControlMotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aPressedButtonSpringsFromAPillTo14dp() {
        val radii = cornerRadii(reduceMotion = false, size = ButtonSize) { buttonShape(pressed = it) }
        assertEquals(ButtonSize.height / 2, radii.resting, 0.01f)
        assertTrue("mid-spring radius ${radii.early}", radii.early > 14f && radii.early < ButtonSize.height / 2)
        assertEquals(14f, radii.settled, 0.01f)
    }

    @Test
    fun aPressedButtonSnapsTo14dpUnderReduceMotion() {
        val radii = cornerRadii(reduceMotion = true, size = ButtonSize) { buttonShape(pressed = it) }
        assertEquals(ButtonSize.height / 2, radii.resting, 0.01f)
        assertEquals(14f, radii.early, 0.01f)
        assertEquals(14f, radii.settled, 0.01f)
    }

    @Test
    fun aSelectedChipSpringsFrom12dpToAPill() {
        val radii = cornerRadii(reduceMotion = false, size = ChipSize) { chipShape(selected = it) }
        assertEquals(12f, radii.resting, 0.01f)
        assertTrue("mid-spring radius ${radii.early}", radii.early > 12f && radii.early < ChipSize.height / 2)
        assertEquals(ChipSize.height / 2, radii.settled, 0.01f)
    }

    @Test
    fun aSelectedChipSnapsToAPillUnderReduceMotion() {
        val radii = cornerRadii(reduceMotion = true, size = ChipSize) { chipShape(selected = it) }
        assertEquals(12f, radii.resting, 0.01f)
        assertEquals(ChipSize.height / 2, radii.early, 0.01f)
        assertEquals(ChipSize.height / 2, radii.settled, 0.01f)
    }

    @Test
    fun aPressedButtonAnimates() {
        assertFalse(pressedButtonFrames(reduceMotion = false).let { (early, settled) -> early.samePixels(settled) })
    }

    @Test
    fun aPressedButtonSnapsWholeUnderReduceMotion() {
        assertTrue(pressedButtonFrames(reduceMotion = true).let { (early, settled) -> early.samePixels(settled) })
    }

    @Test
    fun aSelectedChipAnimates() {
        assertFalse(selectedChipFrames(reduceMotion = false).let { (early, settled) -> early.samePixels(settled) })
    }

    @Test
    fun aSelectedChipSnapsWholeUnderReduceMotion() {
        assertTrue(selectedChipFrames(reduceMotion = true).let { (early, settled) -> early.samePixels(settled) })
    }

    @Test
    fun aSelectedSegmentSpringsFrom8dpToAPill() {
        val radii = cornerRadii(reduceMotion = false, size = SegmentSize) {
            segmentShape(first = false, last = false, selected = it)
        }
        assertEquals(8f, radii.resting, 0.01f)
        assertTrue("mid-spring radius ${radii.early}", radii.early > 8f && radii.early < SegmentSize.height / 2)
        assertEquals(SegmentSize.height / 2, radii.settled, 0.01f)
    }

    @Test
    fun aSelectedSegmentSnapsToAPillUnderReduceMotion() {
        val radii = cornerRadii(reduceMotion = true, size = SegmentSize) {
            segmentShape(first = false, last = false, selected = it)
        }
        assertEquals(8f, radii.resting, 0.01f)
        assertEquals(SegmentSize.height / 2, radii.early, 0.01f)
        assertEquals(SegmentSize.height / 2, radii.settled, 0.01f)
    }

    @Test
    fun theOuterEndsOfAGroupArePillsOnTheirStartAndEndSides() {
        val first = SegmentShape(CornerSize(50), CornerSize(8.dp)) { 0f }
        val ltr = first.createOutline(SegmentSize, LayoutDirection.Ltr, Density(1f)) as Outline.Rounded
        assertEquals(SegmentSize.height / 2, ltr.roundRect.topLeftCornerRadius.x, 0.01f)
        assertEquals(8f, ltr.roundRect.topRightCornerRadius.x, 0.01f)
        val rtl = first.createOutline(SegmentSize, LayoutDirection.Rtl, Density(1f)) as Outline.Rounded
        assertEquals(8f, rtl.roundRect.topLeftCornerRadius.x, 0.01f)
        assertEquals(SegmentSize.height / 2, rtl.roundRect.topRightCornerRadius.x, 0.01f)
    }

    @Test
    fun aSwitchThumbSpringsAcross() {
        val positions = thumbPositions(reduceMotion = false)
        assertEquals(0f, positions.resting, 0.001f)
        assertTrue("mid-spring position ${positions.early}", positions.early > 0f && positions.early < 1f)
        assertEquals(1f, positions.settled, 0.001f)
    }

    @Test
    fun aSwitchThumbSnapsAcrossUnderReduceMotion() {
        val positions = thumbPositions(reduceMotion = true)
        assertEquals(0f, positions.resting, 0.001f)
        assertEquals(1f, positions.early, 0.001f)
        assertEquals(1f, positions.settled, 0.001f)
    }

    @Test
    fun aButtonGroupSelectionAnimates() {
        assertFalse(selectedSegmentFrames(reduceMotion = false).let { (early, settled) -> early.samePixels(settled) })
    }

    @Test
    fun aButtonGroupSelectionSnapsWholeUnderReduceMotion() {
        assertTrue(selectedSegmentFrames(reduceMotion = true).let { (early, settled) -> early.samePixels(settled) })
    }

    @Test
    fun aSwitchRowAnimates() {
        assertFalse(checkedSwitchRowFrames(reduceMotion = false).let { (early, settled) -> early.samePixels(settled) })
    }

    @Test
    fun aSwitchRowSnapsWholeUnderReduceMotion() {
        assertTrue(checkedSwitchRowFrames(reduceMotion = true).let { (early, settled) -> early.samePixels(settled) })
    }

    private class Radii(val resting: Float, val early: Float, val settled: Float)

    /** The corner radius at rest, [EARLY_FRAMES] frames after [shape] turns active, and once settled. */
    private fun cornerRadii(reduceMotion: Boolean, size: Size, shape: @Composable (active: Boolean) -> Shape): Radii {
        var active by mutableStateOf(false)
        lateinit var current: Shape
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { CycleTheme(reduceMotion = reduceMotion) { current = shape(active) } }
        composeRule.mainClock.advanceTimeByFrame()
        val resting = current.cornerRadius(size)

        composeRule.runOnUiThread {
            active = true
            Snapshot.sendApplyNotifications()
        }
        repeat(EARLY_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }
        val early = current.cornerRadius(size)

        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        return Radii(resting, early, current.cornerRadius(size))
    }

    /** The switch thumb's position at rest (off), [EARLY_FRAMES] frames after turning on, and once settled. */
    private fun thumbPositions(reduceMotion: Boolean): Radii {
        var checked by mutableStateOf(false)
        lateinit var position: () -> Float
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { CycleTheme(reduceMotion = reduceMotion) { position = switchThumbProgress(checked) } }
        composeRule.mainClock.advanceTimeByFrame()
        val resting = position()

        composeRule.runOnUiThread {
            checked = true
            Snapshot.sendApplyNotifications()
        }
        repeat(EARLY_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }
        val early = position()

        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        return Radii(resting, early, position())
    }

    private fun Shape.cornerRadius(size: Size): Float {
        val outline = createOutline(size, LayoutDirection.Ltr, Density(1f)) as Outline.Rounded
        return outline.roundRect.topLeftCornerRadius.x
    }

    private fun pressedButtonFrames(reduceMotion: Boolean): Pair<ImageBitmap, ImageBitmap> {
        val source = MutableInteractionSource()
        return frames(reduceMotion, change = { check(source.tryEmit(PressInteraction.Press(Offset.Zero))) }) {
            FilledButton("Log it", {}, icon = CycleIcons.Add, interactionSource = source)
        }
    }

    private fun selectedChipFrames(reduceMotion: Boolean): Pair<ImageBitmap, ImageBitmap> {
        var selected by mutableStateOf(false)
        return frames(reduceMotion, change = { selected = true }) { FilterChip("Cramps", selected, {}) }
    }

    private fun selectedSegmentFrames(reduceMotion: Boolean): Pair<ImageBitmap, ImageBitmap> {
        var selected by mutableStateOf<Int?>(null)
        return frames(reduceMotion, change = { selected = 1 }) {
            ButtonGroup("Pain", listOf("None", "Mild", "Severe"), selected, { selected = it }, Modifier.width(280.dp))
        }
    }

    private fun checkedSwitchRowFrames(reduceMotion: Boolean): Pair<ImageBitmap, ImageBitmap> {
        var checked by mutableStateOf(false)
        return frames(reduceMotion, change = { checked = true }) {
            SwitchRow("Sleep", checked, { checked = it }, Modifier.width(280.dp), icon = CycleIcons.Bedtime)
        }
    }

    /** The component [EARLY_FRAMES] frames after [change], and once everything has settled. */
    private fun frames(
        reduceMotion: Boolean,
        change: () -> Unit,
        content: @Composable () -> Unit
    ): Pair<ImageBitmap, ImageBitmap> {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CycleTheme(reduceMotion = reduceMotion) {
                Box(
                    Modifier
                        .testTag(TAG)
                        .background(CycleTheme.colors.surface)
                        .padding(CycleTheme.spacing.large)
                ) {
                    content()
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeRule.runOnUiThread {
            change()
            Snapshot.sendApplyNotifications()
        }
        repeat(EARLY_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }
        val early = composeRule.onNodeWithTag(TAG).captureToImage()
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        return early to composeRule.onNodeWithTag(TAG).captureToImage()
    }

    private fun ImageBitmap.samePixels(other: ImageBitmap): Boolean {
        if (width != other.width || height != other.height) return false
        val mine = toPixelMap()
        val theirs = other.toPixelMap()
        return (0 until width).all { x -> (0 until height).all { y -> mine[x, y] == theirs[x, y] } }
    }

    private companion object {
        const val TAG = "control"

        /** Enough frames to recompose, start the animation and run it once. */
        const val EARLY_FRAMES = 4
        const val SETTLE_MILLIS = 2_000L

        /** In pixels at density 1, so 1px is 1dp. */
        val ButtonSize = Size(160f, 40f)
        val ChipSize = Size(120f, 36f)
        val SegmentSize = Size(80f, 48f)
    }
}
