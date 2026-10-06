package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * One choice of several, such as a contraception method: the [title] and an optional one-line
 * [body], with a radio at the end, on `surfaceContainer` with 24dp corners
 * (`CycleTheme.shapes.large`). Selected, the row turns `accentContainer` with `onAccentContainer`
 * text, and the radio fills with an `accent` dot inside an `accent` ring; otherwise the radio is an
 * `outline` ring. The colours change on the default effects spring, and jump under reduce motion.
 *
 * The whole row is the target (`selectable`, `Role.RadioButton`), at least 56dp tall, so TalkBack
 * reads it once: the title, the body, "radio button" and "selected" or "not selected". Pressed,
 * focused and hovered come from [cycleIndication] in the row's shape. Disabled, its text and radio
 * draw at the disabled alphas and it ignores taps.
 *
 * Put the rows of one question in a [RadioGroup], which makes them one group for TalkBack and reads
 * each row's place in it ("6 of 9"). For a setting that is on or off, use [SwitchRow]; for two to
 * five short options side by side, [ButtonGroup].
 */
@Composable
fun RadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    val typography = CycleTheme.typography
    val motion = CycleTheme.motion
    val disabled = colors.onSurface.copy(alpha = CycleTheme.stateAlpha.disabledContent)
    val shape = CycleTheme.shapes.large
    val container by animateColorAsState(
        if (selected && enabled) colors.accentContainer else colors.surfaceContainer,
        motion.defaultEffectsSpec(),
        label = "container"
    )
    val titleColor = when {
        !enabled -> disabled
        selected -> colors.onAccentContainer
        else -> colors.onSurface
    }
    val bodyColor = when {
        !enabled -> disabled
        selected -> colors.onAccentContainer
        else -> colors.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = cycleIndication(shape, colors.onSurface),
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .background(container, shape)
            .heightIn(min = RadioRowMinHeight)
            .padding(horizontal = spacing.large, vertical = spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            BasicText(title, style = typography.titleSmall.copy(color = titleColor))
            if (body != null) BasicText(body, style = typography.bodySmall.copy(color = bodyColor))
        }
        Radio(selected = selected, enabled = enabled)
    }
}

/**
 * The [RadioRow]s of one question, one under the other with `spacing.small` between them: one row
 * per option, titled by [title] with an optional [body], [selected] marked, and [onSelect] called
 * with the option tapped. Nothing is selected while [selected] is null. TalkBack reads the rows as one
 * group of as many items as [options], and each row's place in it: "Implant, A rod in the arm, radio
 * button, selected, 6 of 9".
 */
@Composable
fun <T> RadioGroup(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    title: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    body: @Composable (T) -> String? = { null },
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .selectableGroup()
            .semantics { collectionInfo = CollectionInfo(rowCount = options.size, columnCount = 1) },
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
    ) {
        options.forEachIndexed { index, option ->
            RadioRow(
                title = title(option),
                selected = option == selected,
                onClick = { onSelect(option) },
                modifier = Modifier.semantics {
                    collectionItemInfo = CollectionItemInfo(
                        rowIndex = index,
                        rowSpan = 1,
                        columnIndex = 0,
                        columnSpan = 1
                    )
                },
                body = body(option),
                enabled = enabled
            )
        }
    }
}

/** The drawn radio: a 20dp ring in a 24dp box, with a 10dp dot when [selected]. */
@Composable
private fun Radio(selected: Boolean, enabled: Boolean) {
    val colors = CycleTheme.colors
    val motion = CycleTheme.motion
    val disabled = colors.onSurface.copy(alpha = CycleTheme.stateAlpha.disabledContent)
    val clear = colors.accent.copy(alpha = 0f)
    val ring by animateColorAsState(
        when {
            !enabled -> disabled
            selected -> colors.accent
            else -> colors.outline
        },
        motion.defaultEffectsSpec(),
        label = "ring"
    )
    val dot by animateColorAsState(
        when {
            !selected -> clear
            !enabled -> disabled
            else -> colors.accent
        },
        motion.defaultEffectsSpec(),
        label = "dot"
    )
    Box(
        Modifier
            .size(RadioBoxSize)
            .drawBehind {
                val stroke = RadioBorderWidth.toPx()
                val centre = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    ring,
                    radius = RadioRingSize.toPx() / 2 - stroke / 2,
                    center = centre,
                    style = Stroke(stroke)
                )
                drawCircle(dot, radius = RadioDotSize.toPx() / 2, center = centre)
            }
    )
}

private val RadioRowMinHeight = 56.dp
private val RadioBoxSize = 24.dp
private val RadioRingSize = 20.dp
private val RadioDotSize = 10.dp
private val RadioBorderWidth = 2.dp

@Composable
private fun RadioStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        listOf<Triple<Interaction?, Boolean, Boolean>>(
            Triple(null, true, false),
            Triple(null, true, true),
            Triple(PressInteraction.Press(Offset.Zero), true, false),
            Triple(FocusInteraction.Focus(), true, true),
            Triple(null, false, false),
            Triple(null, false, true)
        ).forEach { (interaction, enabled, selected) ->
            RadioRow(
                "Implant",
                selected,
                {},
                body = "A rod in the arm",
                enabled = enabled,
                interactionSource = rememberInteractionSourceIn(interaction)
            )
        }
    }
}

@Preview(name = "Radio rows · light", widthDp = 360)
@Composable
private fun RadioRowsLightPreview() = PreviewSurface(darkTheme = false) { RadioStates() }

@Preview(name = "Radio rows · dark", widthDp = 360)
@Composable
private fun RadioRowsDarkPreview() = PreviewSurface(darkTheme = true) { RadioStates() }
