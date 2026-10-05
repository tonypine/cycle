package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Picks a whole number in [valueRange] by dragging or tapping along a track, such as a length in
 * days. One step per whole number. A 16dp pill track, `accent` up to the value and `accentContainer`
 * after it, with an `accent` dot at the far end; at the value, a 4 by 44dp `accent` handle with a
 * gap on each side, which narrows to 2dp while held. The handle springs to a tapped value on the fast
 * spatial spring (it jumps under reduce motion) and follows a drag directly. Pressed and focused come
 * from [cycleIndication] on the handle. 48dp tall, it fills the width it is given, and runs from
 * right to left in right-to-left layouts.
 *
 * TalkBack reads [contentDescription] and [stateDescription] ("Cycle length, 28 days") and adjusts it
 * one step at a time; arrow keys move it one step too, Home and End to the ends. A disabled slider
 * draws at the disabled alphas and ignores input. For a value she sets with care, prefer
 * [SliderField], which adds − and + buttons and shows the value large.
 */
@Composable
fun Slider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    contentDescription: String,
    stateDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    val alpha = CycleTheme.stateAlpha
    val motion = CycleTheme.motion
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val current by rememberUpdatedState(value)
    val onChange by rememberUpdatedState(onValueChange)
    val range by rememberUpdatedState(valueRange)
    // Moves to [target], kept in range; false when that is where it already is.
    val change: (Int) -> Boolean = { target ->
        val next = target.coerceIn(range)
        if (next != current) onChange(next)
        next != current
    }
    var dragging by remember { mutableStateOf(false) }
    val position = animateFloatAsState(
        targetValue = sliderFraction(value, valueRange),
        animationSpec = if (dragging) snap() else motion.fastSpatialSpec(),
        label = "slider position"
    )
    val pressed by interactionSource.collectIsPressedAsState()
    val handleWidth = animateDpAsState(
        if (pressed) SliderHandlePressedWidth else SliderHandleWidth,
        motion.fastSpatialSpec(),
        label = "slider handle"
    )
    val active = if (enabled) colors.accent else colors.onSurface.copy(alpha = alpha.disabledContent)
    val inactive = if (enabled) colors.accentContainer else colors.onSurface.copy(alpha = alpha.disabledContainer)
    val trackPath = remember { Path() }
    val width = remember { mutableIntStateOf(0) }

    Box(
        modifier
            .fillMaxWidth()
            .height(MinTouchTarget)
            .onSizeChanged { width.intValue = it.width }
            .semantics {
                this.contentDescription = contentDescription
                this.stateDescription = stateDescription
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = value.toFloat(),
                    range = valueRange.first.toFloat()..valueRange.last.toFloat(),
                    steps = (valueRange.last - valueRange.first - 1).coerceAtLeast(0)
                )
                if (enabled) setProgress { target -> change(target.roundToInt()) } else disabled()
            }
            .onKeyEvent { event ->
                if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val forward = if (rtl) -1 else 1
                val target = when (event.key) {
                    Key.DirectionUp -> current + 1
                    Key.DirectionDown -> current - 1
                    Key.DirectionRight -> current + forward
                    Key.DirectionLeft -> current - forward
                    Key.MoveHome -> range.first
                    Key.MoveEnd -> range.last
                    else -> return@onKeyEvent false
                }
                change(target)
                true
            }
            .focusable(enabled, interactionSource)
            .pointerInput(enabled, rtl) {
                if (!enabled) return@pointerInput
                val inset = SliderTrackHeight.toPx() / 2
                fun valueAt(x: Float): Int {
                    val fromStart = if (rtl) size.width - x else x
                    val fraction = ((fromStart - inset) / (size.width - 2 * inset)).coerceIn(0f, 1f)
                    return (range.first + fraction * (range.last - range.first)).roundToInt()
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    if (drag != null) {
                        dragging = true
                        change(valueAt(drag.position.x))
                        val ended = horizontalDrag(drag.id) {
                            change(valueAt(it.position.x))
                            it.consume()
                        }
                        dragging = false
                        interactionSource.tryEmit(
                            if (ended) PressInteraction.Release(press) else PressInteraction.Cancel(press)
                        )
                    } else {
                        // No drag: a tap moves to where she tapped, unless something else, such as a
                        // scroll, took the gesture.
                        val up = currentEvent.changes.firstOrNull { it.id == down.id }
                        if (up != null && up.changedToUp() && !up.isConsumed) {
                            up.consume()
                            change(valueAt(down.position.x))
                            interactionSource.tryEmit(PressInteraction.Release(press))
                        } else {
                            interactionSource.tryEmit(PressInteraction.Cancel(press))
                        }
                    }
                }
            }
            .drawBehind {
                val inset = SliderTrackHeight.toPx() / 2
                val center = inset + position.value.coerceIn(0f, 1f) * (size.width - 2 * inset)
                val clearance = handleWidth.value.toPx() / 2 + SliderHandleGap.toPx()
                val top = (size.height - SliderTrackHeight.toPx()) / 2
                drawTrackPart(trackPath, active, 0f, center - clearance, outerAtFrom = true, rtl = rtl, top = top)
                drawTrackPart(
                    trackPath,
                    inactive,
                    center + clearance,
                    size.width,
                    outerAtFrom = false,
                    rtl = rtl,
                    top = top
                )
                val stop = size.width - inset
                if (stop - SliderStopSize.toPx() / 2 > center + clearance) {
                    drawCircle(
                        active,
                        SliderStopSize.toPx() / 2,
                        Offset(if (rtl) size.width - stop else stop, size.height / 2)
                    )
                }
            }
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset {
                    val inset = SliderTrackHeight.toPx() / 2
                    // The handle moves across the whole slider, measured once laid out.
                    val center = inset + position.value.coerceIn(0f, 1f) * (width.intValue - 2 * inset)
                    IntOffset((center - handleWidth.value.toPx() / 2).roundToInt(), 0)
                }
                .indication(interactionSource, cycleIndication(CycleTheme.shapes.full, colors.onSurface))
                .size(handleWidth.value, SliderHandleHeight)
                .background(active, CycleTheme.shapes.full)
        )
    }
}

/**
 * A number she sets with care, such as her usual cycle length: the [label] and the value as large
 * text ([valueText], "28 days"), − and + [TonalIconButton]s for one step at a time, a [Slider] under
 * them for big moves, and an optional one-sentence [supportingText], on `surfaceContainer` with 24dp
 * corners (`CycleTheme.shapes.large`). The value never leaves [valueRange]: − turns off at the start
 * and + at the end.
 *
 * TalkBack reads the label and value together ("Cycle length, 28 days") and announces the new value
 * when − or + change it; [decreaseDescription] and [increaseDescription] name the buttons ("Cycle one
 * day shorter"). Then the slider, which reads the same and adjusts one step at a time. Disabled, the
 * text and controls draw at the disabled alphas and ignore input.
 */
@Composable
fun SliderField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: IntRange,
    valueText: String,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    enabled: Boolean = true
) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    val typography = CycleTheme.typography
    val disabled = colors.onSurface.copy(alpha = CycleTheme.stateAlpha.disabledContent)
    Column(
        modifier
            .fillMaxWidth()
            .semantics { isTraversalGroup = true }
            .background(colors.surfaceContainer, CycleTheme.shapes.large)
            .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            ) {
                BasicText(
                    label,
                    style = typography.titleSmall.copy(color = if (enabled) colors.onSurface else disabled)
                )
                BasicText(
                    valueText,
                    style = typography.headlineEmphasized.copy(color = if (enabled) colors.onSurface else disabled)
                )
            }
            TonalIconButton(
                CycleIcons.Remove,
                decreaseDescription,
                onClick = { onValueChange((value - 1).coerceIn(valueRange)) },
                enabled = enabled && value > valueRange.first
            )
            TonalIconButton(
                CycleIcons.Add,
                increaseDescription,
                onClick = { onValueChange((value + 1).coerceIn(valueRange)) },
                enabled = enabled && value < valueRange.last
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            contentDescription = label,
            stateDescription = valueText,
            enabled = enabled
        )
        if (supportingText != null) {
            BasicText(
                supportingText,
                style = typography.bodySmall.copy(color = if (enabled) colors.onSurfaceVariant else disabled)
            )
        }
    }
}

/** Where [value] sits along [range], from 0 at the start to 1 at the end. */
internal fun sliderFraction(value: Int, range: IntRange): Float =
    if (range.last == range.first) 0f else (value.coerceIn(range) - range.first).toFloat() / (range.last - range.first)

/**
 * One part of the track, from [from] to [to] measured from the start edge: the pill's round end on
 * the outer side ([outerAtFrom]) and small corners on the side next to the handle.
 */
private fun DrawScope.drawTrackPart(
    path: Path,
    color: Color,
    from: Float,
    to: Float,
    outerAtFrom: Boolean,
    rtl: Boolean,
    top: Float
) {
    if (to <= from) return
    val height = SliderTrackHeight.toPx()
    val outer = CornerRadius(height / 2)
    val inner = CornerRadius(SliderTrackInnerCorner.toPx())
    val outerOnLeft = outerAtFrom != rtl
    val left = if (rtl) size.width - to else from
    val right = if (rtl) size.width - from else to
    val leftCorner = if (outerOnLeft) outer else inner
    val rightCorner = if (outerOnLeft) inner else outer
    path.reset()
    path.addRoundRect(RoundRect(left, top, right, top + height, leftCorner, rightCorner, rightCorner, leftCorner))
    drawPath(path, color)
}

private val SliderTrackHeight = 16.dp
private val SliderTrackInnerCorner = 2.dp
private val SliderHandleWidth = 4.dp
private val SliderHandlePressedWidth = 2.dp
private val SliderHandleHeight = 44.dp

/** The space between the handle and the track on each side. */
private val SliderHandleGap = 6.dp
private val SliderStopSize = 4.dp

@Composable
private fun SliderStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        listOf<Triple<Interaction?, Boolean, Int>>(
            Triple(null, true, 28),
            Triple(null, true, 15),
            Triple(null, true, 90),
            Triple(PressInteraction.Press(Offset.Zero), true, 40),
            Triple(FocusInteraction.Focus(), true, 40),
            Triple(null, false, 28)
        ).forEach { (interaction, enabled, value) ->
            Slider(
                value = value,
                onValueChange = {},
                valueRange = 15..90,
                contentDescription = "Cycle length",
                stateDescription = "$value days",
                enabled = enabled,
                interactionSource = rememberInteractionSourceIn(interaction)
            )
        }
        SliderField(
            label = "Cycle length",
            value = 28,
            onValueChange = {},
            valueRange = 15..90,
            valueText = "28 days",
            decreaseDescription = "Cycle one day shorter",
            increaseDescription = "Cycle one day longer",
            supportingText = "Often between 21 and 35 days."
        )
        SliderField(
            label = "Period length",
            value = 1,
            onValueChange = {},
            valueRange = 1..14,
            valueText = "1 day",
            decreaseDescription = "Period one day shorter",
            increaseDescription = "Period one day longer",
            enabled = false
        )
    }
}

@Preview(name = "Sliders · light", widthDp = 360)
@Composable
private fun SlidersLightPreview() = PreviewSurface(darkTheme = false) { SliderStates() }

@Preview(name = "Sliders · dark", widthDp = 360)
@Composable
private fun SlidersDarkPreview() = PreviewSurface(darkTheme = true) { SliderStates() }
