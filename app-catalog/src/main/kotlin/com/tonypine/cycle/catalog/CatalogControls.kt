package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import com.tonypine.cycle.core.designsystem.CycleTheme

/** An interaction source held in [interaction], to show a hovered, focused or pressed state still. */
@Composable
internal fun rememberHeldInteraction(interaction: Interaction?): MutableInteractionSource {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) {
        // Wait a frame, so components that collect the source once composed (a text field) see it.
        withFrameNanos { }
        interaction?.let { source.emit(it) }
    }
    return source
}

/** The interaction states a control page shows, by name, with the interaction that holds each one. */
internal val HeldStates: List<Pair<String, Interaction?>> = listOf(
    "Default" to null,
    "Pressed" to PressInteraction.Press(Offset.Zero),
    "Focused" to FocusInteraction.Focus()
)

/** One variant of a control: its name, what it is for, and its states in a wrapping row. */
@Composable
internal fun VariantStates(name: String, note: String, states: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        CatalogText(name, CycleTheme.typography.titleSmall)
        CatalogText(note, CycleTheme.typography.bodySmall, color = CycleTheme.colors.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
            itemVerticalAlignment = Alignment.CenterVertically
        ) {
            states()
        }
    }
}

/** A control with its state named underneath, for controls with no label of their own. */
@Composable
internal fun Captioned(caption: String, control: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        control()
        CatalogText(caption, CycleTheme.typography.labelSmall, color = CycleTheme.colors.onSurfaceVariant)
    }
}
