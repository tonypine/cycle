package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.Slider
import com.tonypine.cycle.core.designsystem.SliderField

@Composable
internal fun SlidersSection() {
    var cycle by rememberSaveable { mutableIntStateOf(28) }
    var period by rememberSaveable { mutableIntStateOf(5) }
    SubsectionTitle("Try it")
    CatalogText(
        "Drag or tap the track for big moves, and − or + for one day at a time.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        SliderField(
            label = "Cycle length",
            value = cycle,
            onValueChange = { cycle = it },
            valueRange = 15..90,
            valueText = "$cycle days",
            decreaseDescription = "Cycle one day shorter",
            increaseDescription = "Cycle one day longer",
            supportingText = "Often between 21 and 35 days."
        )
        SliderField(
            label = "Period length",
            value = period,
            onValueChange = { period = it },
            valueRange = 1..14,
            valueText = if (period == 1) "1 day" else "$period days",
            decreaseDescription = "Period one day shorter",
            increaseDescription = "Period one day longer"
        )
    }

    SubsectionTitle("States")
    VariantStates("Slider", "A 16dp track and a 4dp handle in a 48dp target. accent up to the value.") {
        listOf(15, 40, 90).forEach { value ->
            Captioned("At $value of 15 to 90") { SampleSlider(value) }
        }
        HeldStates.drop(1).forEach { (held, interaction) ->
            Captioned(held) { SampleSlider(40, interaction = interaction) }
        }
        Captioned("Disabled") { SampleSlider(40, enabled = false) }
    }
    VariantStates("SliderField", "Label, value, − and +, then the slider. surfaceContainer, 24dp corners.") {
        SliderField(
            label = "At the start",
            value = 1,
            onValueChange = {},
            valueRange = 1..14,
            valueText = "1 day",
            decreaseDescription = "One less",
            increaseDescription = "One more",
            supportingText = "− turns off at the start of the range, + at the end."
        )
        SliderField(
            label = "Disabled",
            value = 5,
            onValueChange = {},
            valueRange = 1..14,
            valueText = "5 days",
            decreaseDescription = "One less",
            increaseDescription = "One more",
            enabled = false
        )
    }
    CatalogText(
        "The handle springs to a tapped value on the fast spatial spring, follows a drag directly, and narrows " +
            "while held.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
}

@Composable
private fun SampleSlider(value: Int, interaction: Interaction? = null, enabled: Boolean = true) {
    Slider(
        value = value,
        onValueChange = {},
        valueRange = 15..90,
        contentDescription = "Sample",
        stateDescription = "$value days",
        enabled = enabled,
        interactionSource = rememberHeldInteraction(interaction)
    )
}
