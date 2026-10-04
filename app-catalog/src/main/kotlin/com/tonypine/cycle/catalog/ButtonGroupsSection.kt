package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import com.tonypine.cycle.core.designsystem.ButtonGroup
import com.tonypine.cycle.core.designsystem.CycleIcon
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme

private val Flow = listOf("None", "Spotting", "Light", "Medium", "Heavy")
private val Pain = listOf("None", "Mild", "Moderate", "Severe")

@Composable
internal fun ButtonGroupsSection() {
    var flow by rememberSaveable { mutableStateOf<Int?>(null) }
    var pain by rememberSaveable { mutableStateOf<Int?>(null) }
    SubsectionTitle("Try it")
    CatalogText(
        "Pick one: the segment fills, shows a check and grows into a pill. Tap it again to clear it.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    GroupTitle(CycleIcons.WaterDrop, "Flow")
    ButtonGroup("Flow", Flow, selectedIndex = flow, onSelectedChange = { flow = it })
    GroupTitle(CycleIcons.Healing, "Pain")
    ButtonGroup("Pain", Pain, selectedIndex = pain, onSelectedChange = { pain = it })
    CatalogText(
        "Flow: ${flow?.let { Flow[it] } ?: "nothing"}. Pain: ${pain?.let { Pain[it] } ?: "nothing"}.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )

    SubsectionTitle("States")
    listOf(
        "Default" to null,
        "Pressed" to PressInteraction.Press(Offset.Zero),
        "Focused" to FocusInteraction.Focus()
    ).forEach { (state, interaction) ->
        State("$state, Spotting held, Medium selected") {
            val sources = Flow.indices.map { index -> rememberHeldInteraction(interaction.takeIf { index == 1 }) }
            ButtonGroup("Flow", Flow, selectedIndex = 3, onSelectedChange = {}, interactionSources = sources)
        }
    }
    State("Nothing selected") { ButtonGroup("Flow", Flow, selectedIndex = null, onSelectedChange = {}) }
    State("Disabled") { ButtonGroup("Pain", Pain, selectedIndex = 1, onSelectedChange = {}, enabled = false) }
    CatalogText(
        "Segments are 48dp tall and 2dp apart: a pill at the outer ends, 8dp corners inside. Shape and " +
            "check on the default spatial spring, colour on the default effects spring. When the labels do " +
            "not fit, the group scrolls sideways.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
}

/** The visible title a screen puts above a group: an icon and the group's label. */
@Composable
private fun GroupTitle(icon: CycleIcons, title: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CycleIcon(icon, contentDescription = null, tint = CycleTheme.colors.onSurfaceVariant)
        CatalogText(title, CycleTheme.typography.titleSmall)
    }
}

@Composable
private fun State(name: String, group: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        CatalogText(name, CycleTheme.typography.labelSmall, color = CycleTheme.colors.onSurfaceVariant)
        group()
    }
}
