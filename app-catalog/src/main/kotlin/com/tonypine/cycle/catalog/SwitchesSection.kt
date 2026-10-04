package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.Switch
import com.tonypine.cycle.core.designsystem.SwitchRow

@Composable
internal fun SwitchesSection() {
    var pain by rememberSaveable { mutableStateOf(true) }
    var energy by rememberSaveable { mutableStateOf(false) }
    var sleep by rememberSaveable { mutableStateOf(false) }
    SubsectionTitle("Try it")
    CatalogText(
        "Tap anywhere on a row: the thumb slides across and the track turns accent.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        SwitchRow("Pain", pain, { pain = it }, body = "Cramps, headaches and how bad.", icon = CycleIcons.Healing)
        SwitchRow("Energy", energy, { energy = it }, body = "Low, okay or high.", icon = CycleIcons.Bolt)
        SwitchRow("Sleep", sleep, { sleep = it }, icon = CycleIcons.Bedtime)
    }

    SubsectionTitle("States")
    VariantStates("Switch", "A 52 by 32dp track in a 48dp target. outline when off, accent when on.") {
        listOf(false, true).forEach { checked ->
            val state = if (checked) "On" else "Off"
            HeldStates.forEach { (held, interaction) ->
                Captioned("$state, ${held.lowercase()}") {
                    Switch(checked, {}, "Sample $state $held", interactionSource = rememberHeldInteraction(interaction))
                }
            }
            Captioned("$state, disabled") { Switch(checked, {}, "Sample $state disabled", enabled = false) }
        }
    }
    VariantStates("SwitchRow", "The whole row toggles. surfaceContainer, 24dp corners.") {
        SwitchRow("Default", checked = false, {}, body = "With a body and an icon.", icon = CycleIcons.Bolt)
        SwitchRow(
            "Pressed",
            checked = true,
            {},
            interactionSource = rememberHeldInteraction(PressInteraction.Press(Offset.Zero))
        )
        SwitchRow("Disabled", checked = true, {}, body = "Keeps its state, ignores taps.", enabled = false)
    }
    CatalogText(
        "The thumb slides and grows on the default spatial spring; colours change on the default effects " +
            "spring.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
}
