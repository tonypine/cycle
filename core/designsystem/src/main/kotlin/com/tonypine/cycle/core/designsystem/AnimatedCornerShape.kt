package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

/**
 * A rounded rectangle whose corners move from [restingCorner] (progress 0) to [activeCorner]
 * (progress 1). [progress] is read when the outline is made, so the shape object stays the same
 * while it animates and `Modifier.background`, `border` and [CycleIndication] redraw on their own.
 * Progress past 1 (a spring overshooting) pushes the corners a little further, within the bounds.
 */
@Stable
class AnimatedCornerShape(
    private val restingCorner: CornerSize,
    private val activeCorner: CornerSize,
    private val progress: () -> Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val max = size.minDimension / 2
        val from = restingCorner.toPx(size, density).coerceIn(0f, max)
        val to = activeCorner.toPx(size, density).coerceIn(0f, max)
        val radius = lerp(from, to, progress()).coerceIn(0f, max)
        return Outline.Rounded(RoundRect(size.toRect(), CornerRadius(radius)))
    }
}

/**
 * A shape whose corners spring between [restingCorner] and [activeCorner] when [active] changes, for
 * shape change on press or selection. The defaults are Zest's button: a pill that squashes to 14dp
 * while pressed. Drive [active] from `interactionSource.collectIsPressedAsState()` or the selected
 * state, and use the returned shape for the background and for [cycleIndication]. With reduce motion
 * on, the spec is `snap()` and the corners jump.
 */
@Composable
fun animatedCornerShape(
    active: Boolean,
    restingCorner: CornerSize = CornerSize(50),
    activeCorner: CornerSize = CornerSize(14.dp),
    animationSpec: FiniteAnimationSpec<Float> = CycleTheme.motion.defaultSpatialSpec()
): Shape {
    val progress = remember { Animatable(if (active) 1f else 0f) }
    val spec by rememberUpdatedState(animationSpec)
    LaunchedEffect(active) { progress.animateTo(if (active) 1f else 0f, spec) }
    return remember(restingCorner, activeCorner) { AnimatedCornerShape(restingCorner, activeCorner) { progress.value } }
}
