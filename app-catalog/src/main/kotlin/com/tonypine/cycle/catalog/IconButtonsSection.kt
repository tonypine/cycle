package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledIconButton
import com.tonypine.cycle.core.designsystem.IconButton
import com.tonypine.cycle.core.designsystem.IconToggleButton
import com.tonypine.cycle.core.designsystem.TonalIconButton

@Composable
internal fun IconButtonsSection() {
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    SubsectionTitle("Try it")
    CatalogText(
        "Press one, or toggle the last. Calendar: ${if (showCalendar) "shown" else "hidden"}.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    Row(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        IconButton(CycleIcons.ChevronStart, "Previous month", {})
        FilledIconButton(CycleIcons.Add, "Log a day", {})
        TonalIconButton(CycleIcons.Today, "Go to today", {})
        IconToggleButton(CycleIcons.Calendar, "Show the calendar", showCalendar, { showCalendar = it })
    }

    SubsectionTitle("States")
    VariantStates("IconButton", "onSurface icon, no container.") {
        PressableStates { state, enabled, source ->
            IconButton(CycleIcons.ChevronEnd, "Next month, $state", {}, enabled = enabled, interactionSource = source)
        }
    }
    VariantStates("FilledIconButton", "accent circle. The main action as an icon.") {
        PressableStates { state, enabled, source ->
            FilledIconButton(CycleIcons.Add, "Log a day, $state", {}, enabled = enabled, interactionSource = source)
        }
    }
    VariantStates("TonalIconButton", "accentContainer circle.") {
        PressableStates { state, enabled, source ->
            TonalIconButton(CycleIcons.Today, "Go to today, $state", {}, enabled = enabled, interactionSource = source)
        }
    }
    VariantStates("IconToggleButton", "onSurfaceVariant when off; accentContainer circle when checked.") {
        listOf(false, true).forEach { checked ->
            PressableStates(prefix = if (checked) "checked " else "") { state, enabled, source ->
                IconToggleButton(
                    CycleIcons.Calendar,
                    "Show the calendar, $state",
                    checked = checked,
                    onCheckedChange = {},
                    enabled = enabled,
                    interactionSource = source
                )
            }
        }
    }
    CatalogText(
        "Every icon button is a 40dp circle in a 48dp touch target, and needs a content description.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
}

/** Shows an unlabelled control in each held state and disabled, each captioned with its state. */
@Composable
private fun PressableStates(
    prefix: String = "",
    control: @Composable (state: String, enabled: Boolean, source: MutableInteractionSource) -> Unit
) {
    HeldStates.forEach { (state, interaction) ->
        Captioned(prefix + state.lowercase()) { control(state, true, rememberHeldInteraction(interaction)) }
    }
    Captioned(prefix + "disabled") { control("Disabled", false, rememberHeldInteraction(null)) }
}
