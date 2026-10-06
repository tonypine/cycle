package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.RadioGroup
import com.tonypine.cycle.core.designsystem.RadioRow

@Composable
internal fun RadioRowsSection() {
    var breaks by rememberSaveable { mutableStateOf<String?>(null) }
    SubsectionTitle("Try it")
    CatalogText(
        "Tap a row: it turns accentContainer and its radio fills. Nothing is chosen at first.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    RadioGroup(
        options = Breaks.keys.toList(),
        selected = breaks,
        onSelect = { breaks = it },
        title = { it },
        body = { Breaks.getValue(it) }
    )

    SubsectionTitle("States")
    VariantStates("RadioRow", "The whole row selects. surfaceContainer, 24dp corners; accentContainer selected.") {
        RadioRow("Default", selected = false, {}, body = "With a line under it.")
        RadioRow("Selected", selected = true, {}, body = "An accent dot in an accent ring.")
        RadioRow(
            "Pressed",
            selected = false,
            {},
            interactionSource = rememberHeldInteraction(PressInteraction.Press(Offset.Zero))
        )
        RadioRow("Focused", selected = true, {}, interactionSource = rememberHeldInteraction(FocusInteraction.Focus()))
        RadioRow("Disabled", selected = false, {}, body = "Ignores taps.", enabled = false)
        RadioRow("Disabled, selected", selected = true, {}, enabled = false)
    }
    CatalogText(
        "In a RadioGroup, TalkBack reads each row's place: \"Every month, radio button, selected, 1 of 3\".",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
}

private val Breaks = linkedMapOf(
    "Every month" to "A week off, or dummy pills, in every pack",
    "Every few packs" to "Two or more packs in a row, then a break",
    "No breaks" to "One pack straight after another"
)
