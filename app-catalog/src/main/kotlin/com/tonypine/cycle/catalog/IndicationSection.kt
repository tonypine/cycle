package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.PRESSED_SCALE

@Composable
internal fun IndicationSection() {
    SubsectionTitle("Try it")
    CatalogText(
        "Press, hover with a mouse, or move focus with a keyboard or D-pad.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    FilledButton("Log it", onClick = {}, icon = CycleIcons.Check)

    SubsectionTitle("States")
    listOf<Triple<String, Interaction?, String>>(
        Triple("Default", null, "No layer."),
        Triple("Hovered", HoverInteraction.Enter(), "${CycleTheme.stateAlpha.hovered.percent()} layer."),
        Triple(
            "Focused",
            FocusInteraction.Focus(),
            "${CycleTheme.stateAlpha.focused.percent()} layer and a 3dp accent ring."
        ),
        Triple(
            "Pressed",
            PressInteraction.Press(Offset.Zero),
            "${CycleTheme.stateAlpha.pressed.percent()} layer, ${PRESSED_SCALE.percent()} scale, pill to 14dp."
        )
    ).forEach { (name, interaction, note) ->
        StateRow(note) { FilledButton(name, onClick = {}, interactionSource = rememberHeldInteraction(interaction)) }
    }
    StateRow(
        "Container ${CycleTheme.stateAlpha.disabledContainer.percent()}, content " +
            "${CycleTheme.stateAlpha.disabledContent.percent()} onSurface."
    ) {
        FilledButton("Disabled", onClick = {}, enabled = false)
    }
}

@Composable
private fun StateRow(note: String, sample: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        sample()
        CatalogText(note, CycleTheme.typography.bodySmall, color = CycleTheme.colors.onSurfaceVariant)
    }
}

private fun Float.percent(): String = "${(this * 100).toInt()}%"
