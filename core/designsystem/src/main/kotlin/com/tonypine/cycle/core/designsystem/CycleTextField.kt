package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * An icon button at the end of a [CycleTextField], such as "Clear" or "Pick a date". It is a 48dp
 * touch target; [contentDescription] is what TalkBack reads for it.
 */
@Immutable
class TextFieldAction(val icon: CycleIcons, val contentDescription: String, val onClick: () -> Unit)

/**
 * Cycle's outlined text field (Zest `.field`), built on Foundation's [BasicTextField] with a
 * [TextFieldState]. The [label] sits inside the field above the value and is part of the field's
 * semantics, so TalkBack reads the label, then the value, then the supporting or error text.
 *
 * - The border is `outline`, 2dp `accent` while focused, and 2dp `error` while [errorMessage] is set.
 * - [errorMessage] puts the field in its error state: the error colour, an error icon and the
 *   sentence below the field, and the field announces it to TalkBack. Write it as a sentence that
 *   says how to fix the value ("Pick a day up to today."). Colour is never the only signal.
 * - [supportingText] is a hint below the field, replaced by [errorMessage] while it is set.
 * - [maxLength] caps the text and shows a character counter below the field.
 * - [readOnly] keeps the value readable, selectable and focusable, but not editable.
 * - [interactionSource] receives the field's focus and press interactions. Emitting into it, as
 *   previews and tests do, changes only how the field looks.
 * - [keyboardOptions] and [onKeyboardAction] pass through to [BasicTextField]. Without a handler,
 *   `ImeAction.Next` moves focus to the next field and `ImeAction.Done` hides the keyboard.
 *
 * Keyboard: put the field in a `verticalScroll` column with `imePadding()` (or `safeDrawingPadding()`)
 * on an edge-to-edge screen. When the keyboard opens, the column shrinks above it and Foundation
 * scrolls the focused field, with its supporting text, back into view.
 */
@Composable
fun CycleTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    supportingText: String? = null,
    errorMessage: String? = null,
    leadingIcon: CycleIcons? = null,
    trailingAction: TextFieldAction? = null,
    maxLength: Int? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    inputTransformation: InputTransformation? = null,
    interactionSource: MutableInteractionSource? = null
) {
    // Foundation gets a source of its own, forwarded to [interactionSource]. An interaction emitted
    // into [interactionSource] from outside (a held focus in a preview) then only restyles the field:
    // Foundation does not draw a cursor for it or scroll the field into view.
    val fieldSource = remember { MutableInteractionSource() }
    LaunchedEffect(fieldSource, interactionSource) {
        interactionSource?.let { target -> fieldSource.interactions.collect { target.emit(it) } }
    }
    val focused by (interactionSource ?: fieldSource).collectIsFocusedAsState()
    val isError = errorMessage != null
    val colors = textFieldColors(enabled = enabled, readOnly = readOnly, focused = focused, isError = isError)
    val motion = CycleTheme.motion
    val borderColor by animateColorAsState(colors.border, motion.defaultEffectsSpec(), label = "border colour")
    val borderWidth by animateDpAsState(
        if (enabled && (focused || isError)) ACTIVE_BORDER else RESTING_BORDER,
        motion.defaultEffectsSpec(),
        label = "border width"
    )
    val labelColor by animateColorAsState(colors.label, motion.defaultEffectsSpec(), label = "label colour")

    val typography = CycleTheme.typography
    val spacing = CycleTheme.spacing
    val shape = CycleTheme.shapes.medium
    val transformation = when {
        maxLength == null -> inputTransformation
        inputTransformation == null -> InputTransformation.maxLength(maxLength)
        else -> inputTransformation.then(InputTransformation.maxLength(maxLength))
    }

    Column(modifier) {
        BasicTextField(
            state = state,
            // The border sits outside the field's semantics bounds, in a band as wide as the active
            // border, so accessibility checks measure the text, not the border, as its foreground.
            modifier = Modifier
                .fillMaxWidth()
                .border(borderWidth, borderColor, shape)
                .padding(ACTIVE_BORDER)
                .semantics { if (errorMessage != null) error(errorMessage) },
            enabled = enabled,
            readOnly = readOnly,
            inputTransformation = transformation,
            textStyle = typography.body.copy(color = colors.text),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            lineLimits = lineLimits,
            interactionSource = fieldSource,
            cursorBrush = SolidColor(colors.cursor),
            decorator = { innerTextField ->
                Row(
                    modifier = Modifier
                        .heightIn(min = MIN_HEIGHT - ACTIVE_BORDER * 2)
                        .padding(
                            start = spacing.large - ACTIVE_BORDER,
                            end = (if (trailingAction != null) spacing.extraSmall else spacing.large) - ACTIVE_BORDER
                        ),
                    horizontalArrangement = Arrangement.spacedBy(spacing.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingIcon?.let { CycleIcon(it, contentDescription = null, tint = colors.icon) }
                    Column(Modifier.weight(1f).padding(vertical = spacing.small)) {
                        BasicText(label, style = typography.labelSmall.copy(color = labelColor))
                        Box {
                            if (placeholder != null && state.text.isEmpty()) {
                                BasicText(placeholder, style = typography.body.copy(color = colors.placeholder))
                            }
                            innerTextField()
                        }
                    }
                    trailingAction?.let { TrailingActionButton(it, enabled = enabled, tint = colors.icon) }
                }
            }
        )
        BelowField(
            errorMessage = errorMessage,
            supportingText = supportingText,
            length = state.text.length,
            maxLength = maxLength,
            enabled = enabled
        )
    }
}

@Composable
private fun TrailingActionButton(action: TextFieldAction, enabled: Boolean, tint: Color) {
    val shape = CycleTheme.shapes.full
    Box(
        modifier = Modifier
            .size(MIN_TOUCH_TARGET)
            .clickable(
                interactionSource = null,
                indication = cycleIndication(shape, color = tint),
                enabled = enabled,
                role = Role.Button,
                onClick = action.onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CycleIcon(action.icon, contentDescription = action.contentDescription, tint = tint)
    }
}

/** The line below the field: the error (icon and sentence) or the supporting text, and the counter. */
@Composable
private fun BelowField(errorMessage: String?, supportingText: String?, length: Int, maxLength: Int?, enabled: Boolean) {
    val message = errorMessage ?: supportingText
    if (message == null && maxLength == null) return
    val colors = CycleTheme.colors
    val alpha = CycleTheme.stateAlpha
    val spacing = CycleTheme.spacing
    val style = CycleTheme.typography.bodySmall
    val secondary = if (enabled) colors.onSurfaceVariant else colors.onSurface.copy(alpha = alpha.disabledContent)
    Row(
        modifier = Modifier.padding(start = spacing.large, end = spacing.large, top = spacing.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(spacing.extraSmall)) {
            if (errorMessage != null) {
                CycleIcon(CycleIcons.Error, contentDescription = null, tint = colors.error, size = ERROR_ICON)
                BasicText(errorMessage, style = style.copy(color = colors.error))
            } else if (supportingText != null) {
                BasicText(supportingText, style = style.copy(color = secondary))
            }
        }
        if (maxLength != null) {
            val counterDescription = stringResource(R.string.cycle_text_field_counter, length, maxLength)
            BasicText(
                "$length/$maxLength",
                modifier = Modifier.semantics { contentDescription = counterDescription },
                style = style.copy(color = secondary)
            )
        }
    }
}

/** The colours of one text field state, all from [CycleTheme.colors]. */
private class TextFieldColors(
    val border: Color,
    val label: Color,
    val text: Color,
    val placeholder: Color,
    val icon: Color,
    val cursor: Color
)

@Composable
private fun textFieldColors(enabled: Boolean, readOnly: Boolean, focused: Boolean, isError: Boolean): TextFieldColors {
    val colors = CycleTheme.colors
    val alpha = CycleTheme.stateAlpha
    if (!enabled) {
        val content = colors.onSurface.copy(alpha = alpha.disabledContent)
        return TextFieldColors(
            border = colors.onSurface.copy(alpha = alpha.disabledContainer),
            label = content,
            text = content,
            placeholder = content,
            icon = content,
            cursor = Color.Transparent
        )
    }
    val accent = if (isError) colors.error else colors.accent
    return TextFieldColors(
        border = when {
            isError -> colors.error
            focused -> colors.accent
            readOnly -> colors.outlineVariant
            else -> colors.outline
        },
        label = if (isError || focused) accent else colors.onSurfaceVariant,
        text = colors.onSurface,
        placeholder = colors.onSurfaceVariant,
        icon = colors.onSurfaceVariant,
        cursor = accent
    )
}

/** The Zest field's minimum height (`.field`, `min-height: 56px`). */
private val MIN_HEIGHT = 56.dp
private val MIN_TOUCH_TARGET = 48.dp
private val RESTING_BORDER = 1.dp
private val ACTIVE_BORDER = 2.dp
private val ERROR_ICON = 16.dp

@Composable
private fun TextFieldStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        CycleTextField(
            rememberTextFieldState(),
            label = "Period start",
            placeholder = "Pick a day",
            supportingText = "Empty",
            leadingIcon = CycleIcons.Calendar
        )
        CycleTextField(
            rememberTextFieldState("Sat 27 March"),
            label = "Period start",
            supportingText = "Filled",
            leadingIcon = CycleIcons.Calendar,
            trailingAction = TextFieldAction(CycleIcons.Close, "Clear", onClick = {})
        )
        CycleTextField(
            rememberTextFieldState(),
            label = "Period start",
            placeholder = "Pick a day",
            supportingText = "Focused",
            interactionSource = rememberInteractionSourceIn(FocusInteraction.Focus())
        )
        CycleTextField(
            rememberTextFieldState("Sat 27 March"),
            label = "Period start",
            supportingText = "Disabled",
            enabled = false
        )
        CycleTextField(
            rememberTextFieldState("Sat 27 March"),
            label = "Period start",
            supportingText = "Read-only",
            readOnly = true
        )
        CycleTextField(
            rememberTextFieldState("Sat 27 March"),
            label = "Period start",
            errorMessage = "Pick a day up to today."
        )
        CycleTextField(
            rememberTextFieldState("A synthetic sample note."),
            label = "Notes",
            supportingText = "Multi-line, with a counter",
            maxLength = 200,
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3)
        )
    }
}

@Preview(name = "Text field · light", widthDp = 360)
@Composable
private fun TextFieldLightPreview() = CycleTheme(darkTheme = false) {
    Box(Modifier.background(CycleTheme.colors.surface).padding(CycleTheme.spacing.large)) { TextFieldStates() }
}

@Preview(name = "Text field · dark", widthDp = 360)
@Composable
private fun TextFieldDarkPreview() = CycleTheme(darkTheme = true) {
    Box(Modifier.background(CycleTheme.colors.surface).padding(CycleTheme.spacing.large)) { TextFieldStates() }
}
