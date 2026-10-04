package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.OutlinedButton
import com.tonypine.cycle.core.designsystem.TextButton
import com.tonypine.cycle.core.designsystem.TonalButton

private class ButtonVariant(
    val name: String,
    val note: String,
    val button: @Composable (
        text: String,
        icon: CycleIcons?,
        enabled: Boolean,
        interactionSource: MutableInteractionSource
    ) -> Unit
)

private val ButtonVariants = listOf(
    ButtonVariant("FilledButton", "accent. The main action; one per screen at most.") { text, icon, enabled, source ->
        FilledButton(text, {}, icon = icon, enabled = enabled, interactionSource = source)
    },
    ButtonVariant("TonalButton", "accentContainer. A secondary action that still needs weight.") {
            text,
            icon,
            enabled,
            source
        ->
        TonalButton(text, {}, icon = icon, enabled = enabled, interactionSource = source)
    },
    ButtonVariant("OutlinedButton", "accent label, outline border. Next to a filled button.") {
            text,
            icon,
            enabled,
            source
        ->
        OutlinedButton(text, {}, icon = icon, enabled = enabled, interactionSource = source)
    },
    ButtonVariant("TextButton", "accent label, no container. The lightest action.") { text, icon, enabled, source ->
        TextButton(text, {}, icon = icon, enabled = enabled, interactionSource = source)
    }
)

@Composable
internal fun ButtonsSection() {
    var logged by rememberSaveable { mutableIntStateOf(0) }
    SubsectionTitle("Try it")
    CatalogText(
        "Press one: its corners squash from a pill to 14dp. Logged $logged times.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        FilledButton("Log it", { logged++ }, icon = CycleIcons.Add)
        TonalButton("Edit period", {})
        OutlinedButton("Cancel", {})
        TextButton("Nah", {})
    }

    SubsectionTitle("States")
    ButtonVariants.forEach { variant ->
        VariantStates(variant.name, variant.note) {
            HeldStates.forEach { (state, interaction) ->
                variant.button(state, null, true, rememberHeldInteraction(interaction))
            }
            variant.button("Disabled", null, false, rememberHeldInteraction(null))
            variant.button("Icon", CycleIcons.Add, true, rememberHeldInteraction(null))
        }
    }
    CatalogText(
        "Every button is 40dp tall in a 48dp touch target, with an optional leading icon.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
}
