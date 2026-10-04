package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph

/**
 * A pill control that shows every interaction foundation at once: [cycleIndication], an
 * [animatedCornerShape] that squashes on press, a [CycleIcon] and the disabled alphas, plus an error
 * state no button has. The indication's previews and state matrix test use it; it is not a component:
 * screens use [FilledButton] and the other controls.
 */
@Composable
internal fun InteractionSample(
    label: String,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false
) {
    val colors = CycleTheme.colors
    val alpha = CycleTheme.stateAlpha
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = animatedCornerShape(active = pressed)
    val container = when {
        !enabled -> colors.onSurface.copy(alpha = alpha.disabledContainer)
        isError -> colors.errorContainer
        else -> colors.accentContainer
    }
    val content = when {
        !enabled -> colors.onSurface.copy(alpha = alpha.disabledContent)
        isError -> colors.onErrorContainer
        else -> colors.onAccentContainer
    }
    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = cycleIndication(shape, color = content),
                enabled = enabled,
                role = Role.Button,
                onClick = {}
            )
            .background(container, shape)
            .heightIn(min = 48.dp)
            .padding(horizontal = CycleTheme.spacing.extraLarge, vertical = CycleTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CycleIcon(if (isError) CycleIcons.Error else CycleIcons.Check, contentDescription = null, tint = content)
        BasicText(label, style = CycleTheme.typography.label.copy(color = content))
    }
}

/** An interaction source that starts in [interaction], so a preview can show a pressed or focused state. */
@Composable
internal fun rememberInteractionSourceIn(interaction: Interaction?): MutableInteractionSource {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        // Wait a frame, so components that collect the source once composed (a text field) see it.
        withFrameNanos { }
        interaction?.let { source.emit(it) }
    }
    return source
}

/** [CycleTheme] on its `surface`, padded, for every component's previews. */
@Composable
internal fun PreviewSurface(darkTheme: Boolean, content: @Composable () -> Unit) {
    CycleTheme(darkTheme = darkTheme) {
        Box(Modifier.background(CycleTheme.colors.surface).padding(CycleTheme.spacing.large)) { content() }
    }
}

@Composable
private fun IndicationStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        listOf<Pair<String, Interaction?>>(
            "Default" to null,
            "Hovered" to HoverInteraction.Enter(),
            "Focused" to FocusInteraction.Focus(),
            "Pressed" to PressInteraction.Press(Offset.Zero)
        ).forEach { (label, interaction) -> InteractionSample(label, rememberInteractionSourceIn(interaction)) }
        InteractionSample("Disabled", rememberInteractionSourceIn(null), enabled = false)
        InteractionSample("Error", rememberInteractionSourceIn(null), isError = true)
    }
}

@Preview(name = "Indication · light")
@Composable
private fun IndicationLightPreview() = PreviewSurface(darkTheme = false) { IndicationStates() }

@Preview(name = "Indication · dark")
@Composable
private fun IndicationDarkPreview() = PreviewSurface(darkTheme = true) { IndicationStates() }

@Composable
private fun IconGrid() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        CycleIcons.entries.forEach { CycleIcon(it, contentDescription = it.name) }
    }
}

@Preview(name = "Icons · light", widthDp = 240)
@Composable
private fun IconsLightPreview() = PreviewSurface(darkTheme = false) { IconGrid() }

@Preview(name = "Icons · dark", widthDp = 240)
@Composable
private fun IconsDarkPreview() = PreviewSurface(darkTheme = true) { IconGrid() }

@Composable
private fun ShapeSteps() {
    val morph = remember { Morph(CyclePolygons.circle.normalized(), CyclePolygons.sun.normalized()) }
    val steps = listOf(0f, 0.5f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        Row(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
            steps.forEach { ShapeSwatch(AnimatedCornerShape(CornerSize(50), CornerSize(14.dp)) { it }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
            steps.forEach { ShapeSwatch(MorphShape(morph) { it }, square = true) }
        }
    }
}

@Composable
private fun ShapeSwatch(shape: Shape, square: Boolean = false) {
    val size = if (square) Modifier.size(48.dp) else Modifier.size(width = 88.dp, height = 48.dp)
    Box(size.background(CycleTheme.colors.accent, shape))
}

@Preview(name = "Shapes · pill to 14dp, circle to sun")
@Composable
private fun ShapesPreview() = PreviewSurface(darkTheme = false) { ShapeSteps() }
