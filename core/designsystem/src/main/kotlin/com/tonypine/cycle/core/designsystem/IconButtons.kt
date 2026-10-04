package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * An action shown as an icon alone, such as the previous and next month arrows: an `onSurface` icon
 * with no container until it is pressed, focused or hovered.
 *
 * Every icon button is a 40dp circle in a 48dp touch target, and squashes its corners to 14dp while
 * pressed, like a button. An icon alone says nothing to TalkBack, so [contentDescription] is
 * required: name the action ("Next month"), not the icon. Prefer a button with a label whenever
 * there is room.
 */
@Composable
fun IconButton(
    icon: CycleIcons,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    IconButtonBase(
        icon = icon,
        contentDescription = contentDescription,
        interactionSource = interactionSource,
        colors = if (enabled) {
            ControlColors(colors.onSurface.copy(alpha = 0f), colors.onSurface)
        } else {
            disabledControlColors(container = false)
        },
        modifier = modifier.clickable(interactionSource, null, enabled, role = Role.Button, onClick = onClick)
    )
}

/** The main action as an icon alone, such as "Add": an `accent` circle. Otherwise the same as [IconButton]. */
@Composable
fun FilledIconButton(
    icon: CycleIcons,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    IconButtonBase(
        icon = icon,
        contentDescription = contentDescription,
        interactionSource = interactionSource,
        colors = if (enabled) {
            ControlColors(
                colors.accent,
                colors.onAccent
            )
        } else {
            disabledControlColors(container = true)
        },
        modifier = modifier.clickable(interactionSource, null, enabled, role = Role.Button, onClick = onClick)
    )
}

/** A secondary icon action that needs weight: an `accentContainer` circle. Otherwise the same as [IconButton]. */
@Composable
fun TonalIconButton(
    icon: CycleIcons,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    IconButtonBase(
        icon = icon,
        contentDescription = contentDescription,
        interactionSource = interactionSource,
        colors = if (enabled) {
            ControlColors(colors.accentContainer, colors.onAccentContainer)
        } else {
            disabledControlColors(container = true)
        },
        modifier = modifier.clickable(interactionSource, null, enabled, role = Role.Button, onClick = onClick)
    )
}

/**
 * An icon that turns a setting on and off, such as showing the fertile window: an `onSurfaceVariant`
 * icon when off, and an `accentContainer` circle with an `onAccentContainer` icon when [checked]. The
 * fill fades on the default effects spring. TalkBack reads [contentDescription], "checkbox" and
 * "checked" or "not checked", so describe the setting ("Show fertile window"), not the state.
 * Otherwise the same as [IconButton].
 */
@Composable
fun IconToggleButton(
    icon: CycleIcons,
    contentDescription: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    IconButtonBase(
        icon = icon,
        contentDescription = contentDescription,
        interactionSource = interactionSource,
        colors = when {
            !enabled -> disabledControlColors(container = checked)
            checked -> ControlColors(colors.accentContainer, colors.onAccentContainer)
            else -> ControlColors(colors.accentContainer.copy(alpha = 0f), colors.onSurfaceVariant)
        },
        modifier = modifier.toggleable(
            value = checked,
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange
        )
    )
}

/** [modifier] holds the interaction: `clickable` or `toggleable`, with no indication of its own. */
@Composable
private fun IconButtonBase(
    icon: CycleIcons,
    contentDescription: String,
    interactionSource: MutableInteractionSource,
    colors: ControlColors,
    modifier: Modifier
) {
    val pressed by interactionSource.collectIsPressedAsState()
    ControlContainer(
        interactionSource = interactionSource,
        shape = buttonShape(pressed),
        colors = colors,
        height = IconButtonSize,
        padding = PaddingValues(0.dp),
        modifier = modifier.minimumTouchTarget()
    ) { content ->
        CycleIcon(icon, contentDescription = contentDescription, tint = content)
    }
}

private val IconButtonSize = 40.dp

@Composable
private fun IconButtonStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        listOf<Triple<Interaction?, Boolean, Boolean>>(
            Triple(null, true, false),
            Triple(PressInteraction.Press(Offset.Zero), true, false),
            Triple(FocusInteraction.Focus(), true, false),
            Triple(null, false, false),
            Triple(null, true, true),
            Triple(null, false, true)
        ).forEach { (interaction, enabled, checked) ->
            IconButtonRow(rememberInteractionSourceIn(interaction), enabled, checked)
        }
    }
}

@Composable
private fun IconButtonRow(interactionSource: MutableInteractionSource, enabled: Boolean, checked: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        IconButton(CycleIcons.ChevronEnd, "Next month", {}, enabled = enabled, interactionSource = interactionSource)
        FilledIconButton(CycleIcons.Add, "Log a day", {}, enabled = enabled, interactionSource = interactionSource)
        TonalIconButton(CycleIcons.Today, "Go to today", {}, enabled = enabled, interactionSource = interactionSource)
        IconToggleButton(
            CycleIcons.Calendar,
            "Show the calendar",
            checked = checked,
            onCheckedChange = {},
            enabled = enabled,
            interactionSource = interactionSource
        )
    }
}

@Preview(name = "Icon buttons · light")
@Composable
private fun IconButtonsLightPreview() = PreviewSurface(darkTheme = false) { IconButtonStates() }

@Preview(name = "Icon buttons · dark")
@Composable
private fun IconButtonsDarkPreview() = PreviewSurface(darkTheme = true) { IconButtonStates() }
