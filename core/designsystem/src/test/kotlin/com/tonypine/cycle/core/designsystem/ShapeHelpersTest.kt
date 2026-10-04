package com.tonypine.cycle.core.designsystem

import android.graphics.Region
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShapeHelpersTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val density = Density(2f)
    private val button = Size(240f, 96f)

    @Test
    fun cornerShapeGoesFromPillTo14dp() {
        var progress = 0f
        val shape = AnimatedCornerShape(CornerSize(50), CornerSize(14.dp)) { progress }
        assertEquals(48f, shape.radius(button))
        progress = 1f
        assertEquals(28f, shape.radius(button))
        progress = 0.5f
        assertEquals(38f, shape.radius(button))
    }

    @Test
    fun cornerShapeOvershootStaysWithinTheBounds() {
        val shape = AnimatedCornerShape(CornerSize(50), CornerSize(14.dp)) { 1.5f }
        assertEquals(18f, shape.radius(button))
        val pastZero = AnimatedCornerShape(CornerSize(50), CornerSize(0.dp)) { 1.2f }
        assertEquals(0f, pastZero.radius(button))
    }

    @Test
    fun pressSquashesTheCornersAndKeepsTheSameShapeObject() {
        val source = MutableInteractionSource()
        val shapes = mutableListOf<Shape>()
        composeRule.setContent {
            CycleTheme(reduceMotion = false) {
                val pressed = source.collectIsPressedAsState()
                shapes += animatedCornerShape(active = pressed.value)
            }
        }
        val shape = composeRule.runOnIdle { shapes.last() }
        assertEquals(48f, shape.radius(button))

        composeRule.runOnIdle { source.tryEmit(PressInteraction.Press(Offset.Zero)) }
        composeRule.waitForIdle()
        assertEquals(28f, shape.radius(button), 0.01f)
        assertSame(shape, shapes.last())
    }

    @Test
    fun reduceMotionJumpsTheCornersStraightToTheEnd() {
        val source = MutableInteractionSource()
        var shape: Shape? = null
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                val pressed = source.collectIsPressedAsState()
                shape = animatedCornerShape(active = pressed.value)
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.runOnIdle { source.tryEmit(PressInteraction.Press(Offset.Zero)) }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(28f, shape!!.radius(button))
    }

    @Test
    fun morphShapeFillsItsBoundsAtEveryProgress() {
        val morph = Morph(CyclePolygons.circle.normalized(), CyclePolygons.sun.normalized())
        val size = Size(100f, 100f)
        listOf(0f, 0.5f, 1f).forEach { progress ->
            val outline = MorphShape(morph) { progress }.createOutline(size, LayoutDirection.Ltr, density)
            assertTrue(outline is Outline.Generic)
            // normalized() measures the curves' control points, so the drawn edge sits a hair inside.
            val bounds = outline.bounds
            assertEquals("left at $progress", 0f, bounds.left, 2f)
            assertEquals("top at $progress", 0f, bounds.top, 2f)
            assertEquals("right at $progress", 100f, bounds.right, 2f)
            assertEquals("bottom at $progress", 100f, bounds.bottom, 2f)
            assertEquals("centred at $progress", 50f, bounds.center.x, 1f)
        }
    }

    @Test
    fun morphShapeGoesFromCircleToSun() {
        val morph = Morph(CyclePolygons.circle.normalized(), CyclePolygons.sun.normalized())
        fun covers(progress: Float, degrees: Double, radius: Double): Boolean {
            val outline = MorphShape(morph) { progress }.createOutline(Size(100f, 100f), LayoutDirection.Ltr, density)
            val region = Region().apply {
                setPath((outline as Outline.Generic).path.asAndroidPath(), Region(0, 0, 100, 100))
            }
            val angle = Math.toRadians(degrees)
            return region.contains((50 + radius * cos(angle)).toInt(), (50 + radius * sin(angle)).toInt())
        }
        // The sun's eight points sit on the circle's edge; between them it dips in to about 0.78.
        assertTrue("circle at a point", covers(0f, 0.0, 45.0))
        assertTrue("sun at a point", covers(1f, 0.0, 45.0))
        assertTrue("circle between points", covers(0f, 22.5, 44.0))
        assertFalse("sun between points", covers(1f, 22.5, 44.0))
    }

    private fun Shape.radius(size: Size): Float {
        val outline = createOutline(size, LayoutDirection.Ltr, density) as Outline.Rounded
        return outline.roundRect.topLeftCornerRadius.x
    }
}
