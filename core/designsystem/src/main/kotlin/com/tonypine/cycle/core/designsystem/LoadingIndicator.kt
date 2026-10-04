package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.InfiniteAnimationPolicy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Shows that something is loading, in line with other content, such as inside a card: an `accent`
 * shape in a 48dp box that morphs from the sun to a clover, a pentagon and a cookie and back, turning
 * a quarter at each step, on the slow spatial spring. Under reduce motion it holds still on the sun.
 *
 * TalkBack reads it as an indeterminate progress bar with [contentDescription] ("Loading" by
 * default; say what is loading when you can, "Loading your cycle"), and announces it politely when
 * it appears. Use [LoadingState] when the whole screen is waiting.
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier, contentDescription: String = stringResource(R.string.loading)) {
    LoadingShape(modifier.semantics { loading(contentDescription) })
}

/**
 * A screen, or the part of one, with nothing to show until something loads: a [LoadingIndicator]
 * centred in the space it fills, with an optional [message] under it in `body` `onSurfaceVariant`.
 * TalkBack reads it as one indeterminate progress bar described by [message], or "Loading".
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String? = null) {
    val description = message ?: stringResource(R.string.loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { loading(description) }
            .padding(CycleTheme.spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingShape()
        if (message != null) {
            BasicText(
                message,
                style = CycleTheme.typography.body.copy(
                    color = CycleTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

private fun SemanticsPropertyReceiver.loading(description: String) {
    contentDescription = description
    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
    liveRegion = LiveRegionMode.Polite
}

/**
 * The indicator's drawing, with no semantics. The morph loop runs through the coroutine's
 * [InfiniteAnimationPolicy], so a test with the clock advancing on its own holds the first frame
 * rather than waiting forever for the loop to end.
 */
@Composable
private fun LoadingShape(modifier: Modifier = Modifier) {
    val color = CycleTheme.colors.accent
    val motion = CycleTheme.motion
    val spec by rememberUpdatedState(motion.slowSpatialSpec<Float>())
    var step by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(motion.reduceMotion) {
        if (motion.reduceMotion) return@LaunchedEffect
        val loop: suspend () -> Unit = {
            while (true) {
                coroutineScope {
                    launch { rotation.animateTo(rotation.value + STEP_DEGREES, spec) }
                    progress.animateTo(1f, spec)
                }
                rotation.snapTo(rotation.value % FULL_TURN)
                progress.snapTo(0f)
                step = (step + 1) % LoadingMorphs.size
            }
        }
        val policy = coroutineContext[InfiniteAnimationPolicy]
        if (policy == null) loop() else policy.onInfiniteOperation(loop)
    }
    Box(
        modifier
            .size(LoadingIndicatorSize)
            .drawBehind {
                val still = motion.reduceMotion
                val morph = LoadingMorphs[if (still) 0 else step]
                val path = morph.toPath(if (still) 0f else progress.value.coerceIn(0f, 1f)).asComposePath()
                val shapeSize = size.minDimension * SHAPE_FRACTION
                path.transform(Matrix().apply { scale(shapeSize, shapeSize) })
                rotate(if (still) 0f else rotation.value) {
                    translate((size.width - shapeSize) / 2, (size.height - shapeSize) / 2) {
                        drawPath(path, color)
                    }
                }
            }
    )
}

private val LoadingIndicatorSize = 48.dp

/** The shape fills 38dp of the 48dp box, leaving room for its points as it turns. */
private const val SHAPE_FRACTION = 38f / 48f
private const val STEP_DEGREES = 90f
private const val FULL_TURN = 360f

/** The polygons the indicator morphs through, in order, back to the first. */
private val LoadingPolygons: List<RoundedPolygon> = listOf(
    CyclePolygons.sun,
    RoundedPolygon.star(
        numVerticesPerRadius = 4,
        innerRadius = 0.5f,
        rounding = CornerRounding(radius = 0.4f),
        innerRounding = CornerRounding(radius = 0.2f)
    ),
    RoundedPolygon(numVertices = 5, rounding = CornerRounding(radius = 0.3f)),
    RoundedPolygon.star(
        numVerticesPerRadius = 9,
        innerRadius = 0.85f,
        rounding = CornerRounding(radius = 0.2f),
        innerRounding = CornerRounding(radius = 0.2f)
    )
).map { it.normalized() }

private val LoadingMorphs: List<Morph> = LoadingPolygons.indices.map { i ->
    Morph(LoadingPolygons[i], LoadingPolygons[(i + 1) % LoadingPolygons.size])
}

@Composable
private fun LoadingSamples() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        LoadingIndicator()
        LoadingState(Modifier.size(width = 280.dp, height = 200.dp), message = "Loading your cycle")
    }
}

@Preview(name = "Loading · light")
@Composable
private fun LoadingLightPreview() = PreviewSurface(darkTheme = false) { LoadingSamples() }

@Preview(name = "Loading · dark")
@Composable
private fun LoadingDarkPreview() = PreviewSurface(darkTheme = true) { LoadingSamples() }
