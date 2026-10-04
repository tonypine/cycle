package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.tonypine.cycle.core.designsystem.AssistChip
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilterChip

private val Symptoms = listOf("Cramps", "Headache", "Bloating", "Tired")

@Composable
internal fun ChipsSection() {
    var selected by rememberSaveable { mutableStateOf(setOf("Cramps")) }
    SubsectionTitle("Try it")
    CatalogText(
        "Select a symptom: the chip fills, shows a check and grows from 12dp corners into a pill.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        Symptoms.forEach { symptom ->
            FilterChip(
                symptom,
                selected = symptom in selected,
                onClick = { selected = if (symptom in selected) selected - symptom else selected + symptom }
            )
        }
        AssistChip("Add a note", {}, icon = CycleIcons.Add)
    }

    SubsectionTitle("States")
    VariantStates("FilterChip", "An option that stays on. outline border; accentContainer pill when selected.") {
        HeldStates.forEach { (state, interaction) ->
            FilterChip(state, selected = false, {}, interactionSource = rememberHeldInteraction(interaction))
        }
        FilterChip("Disabled", selected = false, {}, enabled = false)
        FilterChip("Selected", selected = true, {})
        FilterChip(
            "Selected, pressed",
            selected = true,
            {},
            interactionSource = rememberHeldInteraction(PressInteraction.Press(Offset.Zero))
        )
        FilterChip("Selected, disabled", selected = true, {}, enabled = false)
    }
    VariantStates("AssistChip", "A suggested next step that acts once. outline border, optional icon.") {
        HeldStates.forEach { (state, interaction) ->
            AssistChip(state, {}, icon = CycleIcons.Add, interactionSource = rememberHeldInteraction(interaction))
        }
        AssistChip("Disabled", {}, icon = CycleIcons.Add, enabled = false)
        AssistChip("No icon", {})
    }
    CatalogText(
        "Every chip is 36dp tall in a 48dp touch target. Shape on the fast spatial spring, colour on the " +
            "default effects spring.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
}
