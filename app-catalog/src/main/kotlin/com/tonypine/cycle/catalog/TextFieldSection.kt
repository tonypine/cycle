package com.tonypine.cycle.catalog

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.byValue
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTextField
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.TextFieldAction

@Composable
internal fun TextFieldSection() {
    SubsectionTitle("Try it")
    CatalogText(
        "Tap a field to open the keyboard. Next moves to the following field, and the page scrolls so " +
            "the focused field stays above the keyboard.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    TextFieldDemo()

    SubsectionTitle("States")
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        val fill = Modifier.fillMaxWidth()
        CycleTextField(
            rememberTextFieldState(),
            label = "Period start",
            modifier = fill,
            placeholder = "Pick a day",
            supportingText = "Empty: the placeholder shows.",
            leadingIcon = CycleIcons.Calendar
        )
        CycleTextField(
            rememberTextFieldState("Sat 27 March"),
            label = "Period start",
            modifier = fill,
            supportingText = "Filled, with a trailing action.",
            leadingIcon = CycleIcons.Calendar,
            trailingAction = TextFieldAction(CycleIcons.Close, "Clear", onClick = {})
        )
        CycleTextField(
            rememberTextFieldState(),
            label = "Period start",
            modifier = fill,
            placeholder = "Pick a day",
            supportingText = "Focused: 2dp accent border and label.",
            interactionSource = rememberHeldInteraction(FocusInteraction.Focus())
        )
        CycleTextField(
            rememberTextFieldState("Sat 27 March"),
            label = "Period start",
            modifier = fill,
            supportingText = "Disabled.",
            enabled = false
        )
        CycleTextField(
            rememberTextFieldState("Sat 27 March"),
            label = "Period start",
            modifier = fill,
            supportingText = "Read-only: readable and selectable, not editable.",
            readOnly = true
        )
        CycleTextField(
            rememberTextFieldState("Sat 27 March"),
            label = "Period start",
            modifier = fill,
            errorMessage = "Pick a day up to today."
        )
    }
}

/** A short form with every field option. The values are synthetic and never leave the page. */
@Composable
private fun TextFieldDemo() {
    val fill = Modifier.fillMaxWidth()
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        val name = rememberTextFieldState()
        CycleTextField(
            name,
            label = "Name",
            modifier = fill,
            placeholder = "What should Cycle call you?",
            trailingAction = if (name.text.isNotEmpty()) {
                TextFieldAction(CycleIcons.Close, "Clear name", onClick = { name.clearText() })
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        val length = rememberTextFieldState()
        val days = length.text.toString().toIntOrNull()
        CycleTextField(
            length,
            label = "Usual cycle length",
            modifier = fill,
            placeholder = "28",
            supportingText = "In days.",
            errorMessage = if (days != null && days !in 15..60) "Enter a number of days from 15 to 60." else null,
            leadingIcon = CycleIcons.Calendar,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            inputTransformation = DigitsOnly
        )
        CycleTextField(
            rememberTextFieldState(),
            label = "Notes",
            modifier = fill,
            placeholder = "Anything else? Spill it here.",
            supportingText = "Only on this phone.",
            maxLength = 200,
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3)
        )
    }
}

private val DigitsOnly = InputTransformation.byValue { current, proposed ->
    if (proposed.all { it.isDigit() }) proposed else current
}
