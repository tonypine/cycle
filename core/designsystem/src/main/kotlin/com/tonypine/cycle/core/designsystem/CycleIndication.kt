package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Zest squashes a pressed control to 94% (`--press-scale` on the Zest board). */
const val PRESSED_SCALE = 0.94f

/**
 * Cycle's press, hover and focus feedback, in place of Material's ripple. Pressed, focused and
 * hovered components get a state layer of [color] at the [stateAlpha] opacity, drawn in [shape];
 * pressed ones also shrink to [pressedScale] on the spatial spring; keyboard and D-pad focus adds a
 * [focusRingWidth] ring in [focusRingColor], [focusRingOffset] outside the component.
 *
 * `CycleTheme` installs one as `LocalIndication`, so `Modifier.clickable` uses it. A component with
 * its own shape or content colour passes [cycleIndication] instead. The modifier order matters:
 * put `clickable` before the component's `background`, so the press scale moves the whole component.
 */
@Immutable
data class CycleIndication(
    val color: Color,
    val focusRingColor: Color,
    val shape: Shape,
    val stateAlpha: CycleStateAlpha,
    val motion: CycleMotion,
    val pressedScale: Float = PRESSED_SCALE,
    val focusRingWidth: Dp = 3.dp,
    val focusRingOffset: Dp = 2.dp
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        CycleIndicationNode(interactionSource, this)
}

/**
 * The theme's indication with its state layer in [color] and its state layer and focus ring in
 * [shape]. Pass it to `Modifier.clickable(interactionSource, indication = ...)`. For a shape that
 * animates, such as [animatedCornerShape], pass the same remembered shape the component draws with.
 */
@Composable
fun cycleIndication(shape: Shape = RectangleShape, color: Color = CycleTheme.colors.onSurface): CycleIndication =
    CycleIndication(
        color = color,
        focusRingColor = CycleTheme.colors.accent,
        shape = shape,
        stateAlpha = CycleTheme.stateAlpha,
        motion = CycleTheme.motion
    )

private class CycleIndicationNode(
    private val interactionSource: InteractionSource,
    private val indication: CycleIndication
) : Modifier.Node(),
    DrawModifierNode {
    private val layerAlpha = Animatable(0f)
    private val scale = Animatable(1f)
    private var focused by mutableStateOf(false)

    override fun onAttach() {
        coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            val hovers = mutableListOf<HoverInteraction.Enter>()
            val focuses = mutableListOf<FocusInteraction.Focus>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                    is HoverInteraction.Enter -> hovers += interaction
                    is HoverInteraction.Exit -> hovers -= interaction.enter
                    is FocusInteraction.Focus -> focuses += interaction
                    is FocusInteraction.Unfocus -> focuses -= interaction.focus
                    else -> return@collect
                }
                show(pressed = presses.isNotEmpty(), focused = focuses.isNotEmpty(), hovered = hovers.isNotEmpty())
            }
        }
    }

    private fun show(pressed: Boolean, focused: Boolean, hovered: Boolean) {
        this.focused = focused
        val alpha = indication.stateAlpha
        val layerTarget = when {
            pressed -> alpha.pressed
            focused -> alpha.focused
            hovered -> alpha.hovered
            else -> 0f
        }
        val motion = indication.motion
        coroutineScope.launch { layerAlpha.animateTo(layerTarget, motion.fastEffectsSpec()) }
        coroutineScope.launch {
            scale.animateTo(if (pressed) indication.pressedScale else 1f, motion.defaultSpatialSpec())
        }
    }

    override fun ContentDrawScope.draw() {
        val alpha = layerAlpha.value
        scale(scale.value) {
            this@draw.drawContent()
            if (alpha > 0f) {
                drawOutline(indication.shape.createOutline(size, layoutDirection, this), indication.color, alpha)
            }
        }
        if (focused) drawFocusRing()
    }

    private fun DrawScope.drawFocusRing() {
        val width = indication.focusRingWidth.toPx()
        val inset = indication.focusRingOffset.toPx() + width / 2
        val ring = Size(size.width + inset * 2, size.height + inset * 2)
        val outline = indication.shape.createOutline(ring, layoutDirection, this)
        translate(-inset, -inset) {
            drawOutline(outline, indication.focusRingColor, style = Stroke(width))
        }
    }
}
