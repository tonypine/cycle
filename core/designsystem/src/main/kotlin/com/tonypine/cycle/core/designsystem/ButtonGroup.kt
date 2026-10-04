package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

/**
 * A connected, single-choice group: a few short options side by side, such as period flow (None,
 * Spotting, Light, Medium, Heavy) or pain. Segments are 48dp tall, 2dp apart, with a pill at the
 * group's two outer ends and 8dp corners inside. Unselected, a segment is an `onSurfaceVariant` label
 * in a 1dp `outline` border. The selected one fills with `accent`, shows a check before its
 * `onAccent` label and turns into a pill: its corners and the check's width on the default spatial
 * spring, the colours on the default effects spring, all jumping under reduce motion. Pressed,
 * focused and hovered come from [cycleIndication] in the segment's shape.
 *
 * [label] names what the group chooses ("Flow"). It is not drawn: put it on screen as a title above
 * the group. TalkBack reads it after each option ("None, Flow"), so two groups that both offer
 * "None" still sound different.
 *
 * [selectedIndex] is null while nothing is chosen. Tapping a segment calls [onSelectedChange] with
 * its index, and tapping the selected one again with null, so a choice can be cleared.
 *
 * The group fills the width it is given and shares the space left over between the segments. When
 * the labels do not fit, as at 200% font scale, the segments' padding shrinks to 4dp a side and then
 * the group scrolls sideways: labels stay on one line and never wrap. Each segment is a radio button
 * (`Role.RadioButton`) with its selected state, in a `selectableGroup`.
 * [interactionSources] takes one source per option, for previews and tests to hold a pressed or
 * focused segment.
 */
@Composable
fun ButtonGroup(
    label: String,
    options: List<String>,
    selectedIndex: Int?,
    onSelectedChange: (index: Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSources: List<MutableInteractionSource> =
        remember(options.size) { List(options.size) { MutableInteractionSource() } }
) {
    require(options.size >= 2) { "A button group holds at least two options, not ${options.size}." }
    require(selectedIndex == null || selectedIndex in options.indices) {
        "selectedIndex $selectedIndex is not an option."
    }
    require(interactionSources.size == options.size) { "Pass one interaction source per option." }
    val spacing = CycleTheme.spacing
    Layout(
        content = {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                ButtonGroupSegment(
                    label = option,
                    groupLabel = label,
                    shape = segmentShape(
                        first = index == 0,
                        last = index == options.lastIndex,
                        selected = selected
                    ),
                    selected = selected,
                    enabled = enabled,
                    interactionSource = interactionSources[index],
                    onClick = { onSelectedChange(if (selected) null else index) }
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
    ) { measurables, constraints ->
        val gap = SegmentGap.roundToPx()
        val naturals = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        val content = naturals.sum() + gap * (measurables.size - 1)
        // Through fillMaxWidth and the scroll, the group's width arrives as the minimum width. Each
        // segment gets an equal share of what is left as padding: at least 4dp a side, so a group
        // that does not fit scrolls, and 12dp a side when nothing bounds the width.
        val available = constraints.minWidth
        val leftover = available - content
        val tightest = spacing.extraSmall.roundToPx() * 2
        val fits = available > 0 && leftover >= tightest * measurables.size
        val extra = when {
            fits -> leftover / measurables.size
            available > 0 -> tightest
            else -> spacing.medium.roundToPx() * 2
        }
        // The pixels the equal shares leave over go to the first segments, so the group fills exactly.
        val remainder = if (fits) leftover % measurables.size else 0
        val minimum = MinTouchTarget.roundToPx()
        val placeables = measurables.mapIndexed { index, measurable ->
            val width = maxOf(naturals[index] + extra + if (index < remainder) 1 else 0, minimum)
            measurable.measure(Constraints(minWidth = width, maxWidth = width, maxHeight = constraints.maxHeight))
        }
        val width = maxOf(placeables.sumOf { it.width } + gap * (placeables.size - 1), constraints.minWidth)
        val height = placeables.maxOf { it.height }
        layout(width, height) {
            var x = 0
            placeables.forEach { placeable ->
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width + gap
            }
        }
    }
}

@Composable
private fun ButtonGroupSegment(
    label: String,
    groupLabel: String,
    shape: Shape,
    selected: Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit
) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    val check = animateFloatAsState(
        if (selected) 1f else 0f,
        CycleTheme.motion.defaultSpatialSpec(),
        label = "check"
    )
    ControlContainer(
        interactionSource = interactionSource,
        shape = shape,
        colors = when {
            !enabled -> disabledControlColors(container = selected, border = !selected)
            selected -> ControlColors(colors.accent, colors.onAccent)
            else -> ControlColors(colors.accent.copy(alpha = 0f), colors.onSurfaceVariant, colors.outline)
        },
        height = MinTouchTarget,
        minWidth = 0.dp,
        padding = PaddingValues(vertical = spacing.small),
        modifier = Modifier
            .semantics { contentDescription = "$label, $groupLabel" }
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) { content ->
        CycleIcon(
            CycleIcons.Check,
            contentDescription = null,
            modifier = Modifier
                .revealWidth { check.value }
                .padding(end = spacing.extraSmall),
            tint = content,
            size = SegmentIconSize
        )
        BasicText(
            label,
            style = CycleTheme.typography.label.copy(color = content),
            overflow = TextOverflow.Clip,
            softWrap = false,
            maxLines = 1
        )
    }
}

/**
 * Lays the content out at [progress] of its width (0 hides it, 1 shows all of it), clipped, so the
 * check pushes the label aside as it grows. Read in the layout phase, so the spring only re-lays out.
 */
private fun Modifier.revealWidth(progress: () -> Float): Modifier = clipToBounds().layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val width = (placeable.width * progress().coerceAtLeast(0f)).roundToInt()
    layout(width, placeable.height) { placeable.placeRelative(0, 0) }
}

/**
 * A segment's outline: [startCorner] on its start side and [endCorner] on its end side at progress 0,
 * a pill at progress 1. [progress] is read when the outline is made, so the shape object stays the
 * same while it animates. Start and end follow the layout direction.
 */
@Stable
internal class SegmentShape(
    private val startCorner: CornerSize,
    private val endCorner: CornerSize,
    private val progress: () -> Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val pill = size.minDimension / 2
        val fraction = progress()
        fun radius(corner: CornerSize) =
            CornerRadius(lerp(corner.toPx(size, density).coerceIn(0f, pill), pill, fraction).coerceIn(0f, pill))
        val start = radius(startCorner)
        val end = radius(endCorner)
        val (left, right) = if (layoutDirection == LayoutDirection.Ltr) start to end else end to start
        return Outline.Rounded(
            RoundRect(size.toRect(), topLeft = left, topRight = right, bottomRight = right, bottomLeft = left)
        )
    }
}

/**
 * The shape of one segment: a pill on the group's outer ends ([first], [last]) and 8dp corners
 * (`CycleTheme.shapes.extraSmall`) inside, springing into a pill while [selected] on the default
 * spatial spring. Under reduce motion the corners jump.
 */
@Composable
internal fun segmentShape(first: Boolean, last: Boolean, selected: Boolean): Shape {
    val progress = remember { Animatable(if (selected) 1f else 0f) }
    val spec by rememberUpdatedState(CycleTheme.motion.defaultSpatialSpec<Float>())
    LaunchedEffect(selected) { progress.animateTo(if (selected) 1f else 0f, spec) }
    val inner = CycleTheme.shapes.extraSmall.topStart
    val outer = CornerSize(50)
    val start = if (first) outer else inner
    val end = if (last) outer else inner
    return remember(start, end) { SegmentShape(start, end) { progress.value } }
}

private val SegmentGap = 2.dp
private val SegmentIconSize = 18.dp

private val FlowOptions = listOf("None", "Spotting", "Light", "Medium", "Heavy")
private val PainOptions = listOf("None", "Mild", "Moderate", "Severe")

@Composable
private fun ButtonGroupStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        ButtonGroup("Flow", FlowOptions, selectedIndex = null, onSelectedChange = {})
        ButtonGroup("Flow", FlowOptions, selectedIndex = 3, onSelectedChange = {})
        listOf(PressInteraction.Press(Offset.Zero), FocusInteraction.Focus()).forEach { interaction ->
            val sources = FlowOptions.indices.map { index ->
                rememberInteractionSourceIn(interaction.takeIf { index == 1 })
            }
            ButtonGroup("Flow", FlowOptions, selectedIndex = 3, onSelectedChange = {}, interactionSources = sources)
        }
        ButtonGroup("Pain", PainOptions, selectedIndex = 1, onSelectedChange = {}, enabled = false)
    }
}

@Preview(name = "Button group · light", widthDp = 360)
@Composable
private fun ButtonGroupLightPreview() = PreviewSurface(darkTheme = false) { ButtonGroupStates() }

@Preview(name = "Button group · dark", widthDp = 360)
@Composable
private fun ButtonGroupDarkPreview() = PreviewSurface(darkTheme = true) { ButtonGroupStates() }
