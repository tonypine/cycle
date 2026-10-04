package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview

/**
 * A content container that does nothing on tap, such as a cycle summary: a `surfaceContainer` panel
 * with 32dp corners ([CycleShapes.extraLarge]) and `spacing.large` padding around [content], which
 * lays out in a column with `spacing.small` between children. TalkBack reads its children in order
 * before moving on, as one traversal group. Put text in `onSurface` and `onSurfaceVariant` on it.
 */
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .semantics { isTraversalGroup = true }
            .background(CycleTheme.colors.surfaceContainer, CycleTheme.shapes.extraLarge)
            .padding(CycleTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
        content = content
    )
}

/**
 * A [Card] that opens something, such as last cycle's details. The whole card is one `Role.Button`:
 * TalkBack reads everything in it as one item, then "button", and [onClickLabel] ("Open", "Edit")
 * when given. Pressed, focused and hovered come from the theme's indication in the card's 32dp shape:
 * the state layer, the press scale and the focus ring. A disabled card draws at
 * `stateAlpha.disabledContent` and ignores taps. The card is at least 48dp in both directions.
 *
 * Put no other clickable inside it: its content is merged into one button.
 */
@Composable
fun ClickableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = CycleTheme.shapes.extraLarge
    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = cycleIndication(shape),
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = Role.Button,
                onClick = onClick
            )
            .sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
            .alpha(if (enabled) 1f else CycleTheme.stateAlpha.disabledContent)
            .background(CycleTheme.colors.surfaceContainer, shape)
            .padding(CycleTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
        content = content
    )
}

/** A card's sample content: a title and a line under it. */
@Composable
private fun CardSampleContent(title: String, body: String) {
    BasicText(title, style = CycleTheme.typography.titleSmall.copy(color = CycleTheme.colors.onSurface))
    BasicText(body, style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant))
}

@Composable
private fun CardStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        Card(Modifier.fillMaxWidth()) { CardSampleContent("Card", "Cycle day 12 of about 28.") }
        listOf<Triple<String, Interaction?, Boolean>>(
            Triple("Default", null, true),
            Triple("Pressed", PressInteraction.Press(Offset.Zero), true),
            Triple("Focused", FocusInteraction.Focus(), true),
            Triple("Disabled", null, false)
        ).forEach { (state, interaction, enabled) ->
            ClickableCard(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled,
                interactionSource = rememberInteractionSourceIn(interaction)
            ) {
                CardSampleContent("Clickable card · $state", "Last cycle: 29 days.")
            }
        }
    }
}

@Preview(name = "Cards · light", widthDp = 360)
@Composable
private fun CardsLightPreview() = PreviewSurface(darkTheme = false) { CardStates() }

@Preview(name = "Cards · dark", widthDp = 360)
@Composable
private fun CardsDarkPreview() = PreviewSurface(darkTheme = true) { CardStates() }
