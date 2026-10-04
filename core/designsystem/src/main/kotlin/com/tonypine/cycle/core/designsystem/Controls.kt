package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/** The smallest touch target any control gets, in both directions. */
val MinTouchTarget = 48.dp

/**
 * Lays the node out at least [MinTouchTarget] in both directions and centres the content in it, so a
 * control can draw smaller than its touch target. Put it right after the control's `clickable`,
 * `selectable` or `toggleable`: those modifiers then take the whole target as their bounds, for touch
 * and for accessibility.
 */
fun Modifier.minimumTouchTarget(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val minimum = MinTouchTarget.roundToPx()
    val width = constraints.constrainWidth(maxOf(placeable.width, minimum))
    val height = constraints.constrainHeight(maxOf(placeable.height, minimum))
    layout(width, height) { placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2) }
}

/** The colours of a control in one state: its container, its border and its label and icon. */
@Immutable
internal data class ControlColors(val container: Color, val content: Color, val border: Color = container)

/**
 * The visible part of a button, icon button or chip: [colors] faded on the default effects spring,
 * a 1dp border, and the theme's indication drawn in [shape] from [interactionSource]. The caller puts
 * the interaction (`clickable`, `selectable`, `toggleable`, with no indication of its own) and
 * [minimumTouchTarget] in [modifier], in front of this.
 */
@Composable
internal fun ControlContainer(
    interactionSource: InteractionSource,
    shape: Shape,
    colors: ControlColors,
    height: Dp,
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(contentColor: Color) -> Unit
) {
    val motion = CycleTheme.motion
    val container by animateColorAsState(colors.container, motion.defaultEffectsSpec(), label = "container")
    val contentColor by animateColorAsState(colors.content, motion.defaultEffectsSpec(), label = "content")
    val border by animateColorAsState(colors.border, motion.defaultEffectsSpec(), label = "border")
    Row(
        modifier = modifier
            .indication(interactionSource, cycleIndication(shape, color = contentColor))
            .background(container, shape)
            .border(ControlBorderWidth, border, shape)
            .heightIn(min = height)
            .widthIn(min = height)
            .padding(padding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content(contentColor)
    }
}

private val ControlBorderWidth = 1.dp

/**
 * A disabled control: `onSurface` at `stateAlpha.disabledContent` for its label and icon, and at
 * `stateAlpha.disabledContainer` for its container when [container] is true (filled controls) or its
 * border when [border] is true (outlined ones). Anything else is transparent.
 */
@Composable
@ReadOnlyComposable
internal fun disabledControlColors(container: Boolean, border: Boolean = false): ControlColors {
    val onSurface = CycleTheme.colors.onSurface
    val alpha = CycleTheme.stateAlpha
    val faint = onSurface.copy(alpha = alpha.disabledContainer)
    val clear = onSurface.copy(alpha = 0f)
    return ControlColors(
        container = if (container) faint else clear,
        content = onSurface.copy(alpha = alpha.disabledContent),
        border = if (container || border) faint else clear
    )
}
