package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The main action on a screen: an `accent` pill with an `onAccent` label, such as "Log it". Use one
 * per screen at most; pair it with a [TonalButton], [OutlinedButton] or [TextButton] for the others.
 *
 * Every button is a 40dp pill in a 48dp touch target, with an optional leading [icon]. Pressing it
 * squashes its corners from the pill to 14dp on the default spatial spring (they jump under reduce
 * motion), on top of the theme's state layer and press scale. TalkBack reads [text] and "button".
 */
@Composable
fun FilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: CycleIcons? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    ButtonBase(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = if (enabled) ControlColors(colors.accent, colors.onAccent) else disabledControlColors(container = true)
    )
}

/**
 * A secondary action that still needs weight, such as "Edit period": an `accentContainer` pill with
 * an `onAccentContainer` label. Otherwise the same as [FilledButton].
 */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: CycleIcons? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    ButtonBase(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = if (enabled) {
            ControlColors(colors.accentContainer, colors.onAccentContainer)
        } else {
            disabledControlColors(container = true)
        }
    )
}

/**
 * A secondary action next to a [FilledButton], such as "Cancel" beside "Save": an `accent` label in a
 * 1dp `outline` border, on whatever surface is behind it. Otherwise the same as [FilledButton].
 */
@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: CycleIcons? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    ButtonBase(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = if (enabled) {
            ControlColors(colors.accent.copy(alpha = 0f), colors.accent, border = colors.outline)
        } else {
            disabledControlColors(container = false, border = true)
        }
    )
}

/**
 * The lightest action, such as "Nah" in a dialog or "Show all" under a list: an `accent` label with
 * no container until it is pressed, focused or hovered. Otherwise the same as [FilledButton].
 */
@Composable
fun TextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: CycleIcons? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    ButtonBase(
        text = text,
        onClick = onClick,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = if (enabled) {
            ControlColors(colors.accent.copy(alpha = 0f), colors.accent)
        } else {
            disabledControlColors(container = false)
        },
        horizontalPadding = CycleTheme.spacing.medium,
        iconPadding = CycleTheme.spacing.medium
    )
}

@Composable
private fun ButtonBase(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: CycleIcons?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource,
    colors: ControlColors,
    horizontalPadding: Dp = CycleTheme.spacing.extraLarge,
    iconPadding: Dp = CycleTheme.spacing.large
) {
    val pressed by interactionSource.collectIsPressedAsState()
    ControlContainer(
        interactionSource = interactionSource,
        shape = buttonShape(pressed),
        colors = colors,
        height = ButtonHeight,
        padding = PaddingValues(
            start = if (icon != null) iconPadding else horizontalPadding,
            top = CycleTheme.spacing.small,
            end = horizontalPadding,
            bottom = CycleTheme.spacing.small
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
            CycleIcon(icon, contentDescription = null, tint = content, size = ButtonIconSize)
            Spacer(Modifier.width(CycleTheme.spacing.small))
        }
        BasicText(
            text,
            style = CycleTheme.typography.label.copy(color = content, textAlign = TextAlign.Center)
        )
    }
}

/**
 * The shape of buttons and icon buttons: a pill whose corners spring to 14dp while [pressed], on the
 * default spatial spring, and jump under reduce motion.
 */
@Composable
internal fun buttonShape(pressed: Boolean): Shape = animatedCornerShape(active = pressed)

private val ButtonHeight = 40.dp
private val ButtonIconSize = 18.dp

@Composable
private fun ButtonStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        listOf<Pair<Interaction?, Boolean>>(
            null to true,
            PressInteraction.Press(Offset.Zero) to true,
            FocusInteraction.Focus() to true,
            null to false
        ).forEach { (interaction, enabled) -> ButtonRow(rememberInteractionSourceIn(interaction), enabled) }
    }
}

@Composable
private fun ButtonRow(interactionSource: MutableInteractionSource, enabled: Boolean) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        FilledButton("Log it", {}, icon = CycleIcons.Add, enabled = enabled, interactionSource = interactionSource)
        TonalButton("Edit", {}, enabled = enabled, interactionSource = interactionSource)
        OutlinedButton("Cancel", {}, enabled = enabled, interactionSource = interactionSource)
        TextButton("Nah", {}, enabled = enabled, interactionSource = interactionSource)
    }
}

@Preview(name = "Buttons · light", widthDp = 420)
@Composable
private fun ButtonsLightPreview() = PreviewSurface(darkTheme = false) { ButtonStates() }

@Preview(name = "Buttons · dark", widthDp = 420)
@Composable
private fun ButtonsDarkPreview() = PreviewSurface(darkTheme = true) { ButtonStates() }
