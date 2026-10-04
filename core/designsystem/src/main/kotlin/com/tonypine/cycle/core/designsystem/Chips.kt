package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * One option the person can turn on and off, such as a symptom ("Cramps") or a mood. Unselected, it
 * is an `onSurfaceVariant` label in a 1dp `outline` border with 12dp corners. When [selected], it
 * fills with `accentContainer`, shows a check before the label, and its corners grow into a pill: the
 * shape on the fast spatial spring, the colours on the default effects spring, both jumping under
 * reduce motion. The selection is never colour alone: the check and the shape change carry it too.
 *
 * A chip is 36dp tall in a 48dp touch target. TalkBack reads [label], "checkbox" and "selected" or
 * "not selected". Put chips in a `FlowRow` with `CycleTheme.spacing.small` between them.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    val motion = CycleTheme.motion
    val spacing = CycleTheme.spacing
    val start by animateDpAsState(
        if (selected) spacing.small else spacing.large,
        motion.fastSpatialSpec(),
        label = "start padding"
    )
    ControlContainer(
        interactionSource = interactionSource,
        shape = chipShape(selected),
        colors = when {
            !enabled -> disabledControlColors(container = selected, border = !selected)
            selected -> ControlColors(colors.accentContainer, colors.onAccentContainer)
            else -> ControlColors(colors.accentContainer.copy(alpha = 0f), colors.onSurfaceVariant, colors.outline)
        },
        height = ChipHeight,
        padding = PaddingValues(start = start, top = spacing.small, end = spacing.large, bottom = spacing.small),
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onClick
            )
            .minimumTouchTarget()
    ) { content ->
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(motion.fastSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
            exit = shrinkHorizontally(motion.fastSpatialSpec()) + fadeOut(motion.defaultEffectsSpec())
        ) {
            CycleIcon(
                CycleIcons.Check,
                contentDescription = null,
                modifier = Modifier.padding(end = spacing.small),
                tint = content,
                size = ChipIconSize
            )
        }
        BasicText(label, style = CycleTheme.typography.label.copy(color = content))
    }
}

/**
 * A suggested next step that sits with content, such as "Add a note" under a logged day: an
 * `onSurfaceVariant` label and optional leading [icon] in a 1dp `outline` border with 12dp corners.
 * It acts once and has no selected state; use a [FilterChip] for an option that stays on, and a button
 * for the screen's main action. TalkBack reads [label] and "button". Otherwise the same as
 * [FilterChip].
 */
@Composable
fun AssistChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: CycleIcons? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    ControlContainer(
        interactionSource = interactionSource,
        shape = CycleTheme.shapes.small,
        colors = if (enabled) {
            ControlColors(colors.surface.copy(alpha = 0f), colors.onSurfaceVariant, colors.outline)
        } else {
            disabledControlColors(container = false, border = true)
        },
        height = ChipHeight,
        padding = PaddingValues(
            start = if (icon != null) spacing.small else spacing.large,
            top = spacing.small,
            end = spacing.large,
            bottom = spacing.small
        ),
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .minimumTouchTarget()
    ) { content ->
        if (icon != null) {
            CycleIcon(icon, contentDescription = null, tint = content, size = ChipIconSize)
            Spacer(Modifier.width(spacing.small))
        }
        BasicText(label, style = CycleTheme.typography.label.copy(color = content))
    }
}

/**
 * The shape of a filter chip: 12dp corners (`CycleTheme.shapes.small`) that spring into a pill while
 * [selected], on the fast spatial spring, and jump under reduce motion.
 */
@Composable
internal fun chipShape(selected: Boolean): Shape = animatedCornerShape(
    active = selected,
    restingCorner = CycleTheme.shapes.small.topStart,
    activeCorner = CornerSize(50),
    animationSpec = CycleTheme.motion.fastSpatialSpec()
)

private val ChipHeight = 36.dp
private val ChipIconSize = 18.dp

@Composable
private fun ChipStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        listOf<Triple<Interaction?, Boolean, Boolean>>(
            Triple(null, true, false),
            Triple(PressInteraction.Press(Offset.Zero), true, false),
            Triple(FocusInteraction.Focus(), true, false),
            Triple(null, false, false),
            Triple(null, true, true),
            Triple(null, false, true)
        ).forEach { (interaction, enabled, selected) ->
            ChipRow(rememberInteractionSourceIn(interaction), enabled, selected)
        }
    }
}

@Composable
private fun ChipRow(interactionSource: MutableInteractionSource, enabled: Boolean, selected: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        FilterChip("Cramps", selected, {}, enabled = enabled, interactionSource = interactionSource)
        AssistChip("Add a note", {}, icon = CycleIcons.Add, enabled = enabled, interactionSource = interactionSource)
    }
}

@Preview(name = "Chips · light")
@Composable
private fun ChipsLightPreview() = PreviewSurface(darkTheme = false) { ChipStates() }

@Preview(name = "Chips · dark")
@Composable
private fun ChipsDarkPreview() = PreviewSurface(darkTheme = true) { ChipStates() }
