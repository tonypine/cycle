package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * A [Morph] between two polygons as a Compose [Shape]: progress 0 is the start polygon and 1 the
 * end one. The polygons should be normalized (bounds 0 to 1, see [RoundedPolygon.normalized]); the
 * outline stretches them to the component's size. Like [AnimatedCornerShape], [progress] is read
 * when the outline is made, so the shape object stays the same while it animates.
 */
@Stable
class MorphShape(private val morph: Morph, private val progress: () -> Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress()).asComposePath()
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

/**
 * A shape that morphs from [start] to [end] on the spatial spring when [atEnd] changes, for the
 * moments where a shape change means something (a day being logged, the ovulation day). With
 * reduce motion on, the spec is `snap()` and the shape jumps to its end state.
 */
@Composable
fun animatedMorphShape(
    start: RoundedPolygon,
    end: RoundedPolygon,
    atEnd: Boolean,
    animationSpec: FiniteAnimationSpec<Float> = CycleTheme.motion.defaultSpatialSpec()
): Shape {
    val progress = remember { Animatable(if (atEnd) 1f else 0f) }
    val spec by rememberUpdatedState(animationSpec)
    LaunchedEffect(atEnd) { progress.animateTo(if (atEnd) 1f else 0f, spec) }
    return remember(start, end) { MorphShape(Morph(start.normalized(), end.normalized())) { progress.value } }
}

/** Zest's expressive polygons, built with graphics-shapes. Morph between them with [animatedMorphShape]. */
object CyclePolygons {
    /** A circle: the today, fertile and default day cell. */
    val circle: RoundedPolygon = RoundedPolygon.circle(numVertices = 8)

    /** A soft eight-point sun: the ovulation day. */
    val sun: RoundedPolygon = RoundedPolygon.star(
        numVerticesPerRadius = 8,
        innerRadius = 0.78f,
        rounding = CornerRounding(radius = 0.18f),
        innerRounding = CornerRounding(radius = 0.12f)
    )
}
