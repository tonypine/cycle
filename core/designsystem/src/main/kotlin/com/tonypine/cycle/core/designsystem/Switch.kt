package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/**
 * A setting that is on or off and takes effect at once, such as logging sleep. A 52 by 32dp pill
 * track in a 48dp touch target. On, the track is `accent` and a 24dp `onAccent` thumb with an
 * `accent` check sits at the end; off, the track is a 2dp `outline` border with a 16dp `outline` thumb
 * at the start. The thumb slides and grows on the default spatial spring and the colours change on
 * the default effects spring, all jumping under reduce motion. Pressed, focused and hovered come from
 * [cycleIndication] on the track.
 *
 * A switch alone has no label, so [contentDescription] names the setting ("Log sleep"), not its
 * state: TalkBack adds "switch" and "on" or "off". Prefer [SwitchRow], which labels it and makes the
 * whole row the target.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    SwitchTrack(
        checked = checked,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .minimumTouchTarget()
    )
}

/**
 * A full-width row that turns a setting on and off: an optional decorative [icon], the [title] and an
 * optional one-sentence [body], and a [Switch] at the end, on `surfaceContainer` with 24dp corners
 * (`CycleTheme.shapes.large`). The whole row is the target (`toggleable`, `Role.Switch`), at least
 * 56dp tall, so TalkBack reads it once: the title, the body, "switch" and "on" or "off". Pressed,
 * focused and hovered come from [cycleIndication] in the row's shape. Disabled, its text, icon and
 * switch draw at the disabled alphas and it ignores taps.
 */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    body: String? = null,
    icon: CycleIcons? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    val typography = CycleTheme.typography
    val disabled = colors.onSurface.copy(alpha = CycleTheme.stateAlpha.disabledContent)
    val shape = CycleTheme.shapes.large
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = cycleIndication(shape, colors.onSurface),
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .background(colors.surfaceContainer, shape)
            .heightIn(min = SwitchRowMinHeight)
            .padding(horizontal = spacing.large, vertical = spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(spacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            CycleIcon(icon, contentDescription = null, tint = if (enabled) colors.onSurfaceVariant else disabled)
        }
        Column(Modifier.weight(1f)) {
            BasicText(title, style = typography.titleSmall.copy(color = if (enabled) colors.onSurface else disabled))
            if (body != null) {
                BasicText(
                    body,
                    style = typography.bodySmall.copy(color = if (enabled) colors.onSurfaceVariant else disabled)
                )
            }
        }
        SwitchTrack(checked = checked, enabled = enabled, interactionSource = null)
    }
}

/**
 * The drawn switch: track, thumb and check. With an [interactionSource] it draws the theme's
 * indication on the track; in a [SwitchRow] the row draws it instead.
 */
@Composable
private fun SwitchTrack(
    checked: Boolean,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    modifier: Modifier = Modifier
) {
    val colors = CycleTheme.colors
    val motion = CycleTheme.motion
    val alpha = CycleTheme.stateAlpha
    val faint = colors.onSurface.copy(alpha = alpha.disabledContainer)
    val dim = colors.onSurface.copy(alpha = alpha.disabledContent)
    val clear = colors.onSurface.copy(alpha = 0f)
    val target = when {
        !enabled && checked -> SwitchColors(track = faint, border = clear, thumb = colors.surface, check = dim)

        !enabled -> SwitchColors(track = clear, border = faint, thumb = dim, check = clear)

        checked -> SwitchColors(
            track = colors.accent,
            border = colors.accent,
            thumb = colors.onAccent,
            check = colors.accent
        )

        else -> SwitchColors(track = clear, border = colors.outline, thumb = colors.outline, check = clear)
    }
    val track by animateColorAsState(target.track, motion.defaultEffectsSpec(), label = "track")
    val border by animateColorAsState(target.border, motion.defaultEffectsSpec(), label = "border")
    val thumb by animateColorAsState(target.thumb, motion.defaultEffectsSpec(), label = "thumb")
    val check by animateColorAsState(target.check, motion.defaultEffectsSpec(), label = "check")
    val progress = switchThumbProgress(checked)
    val checkIcon = painterResource(CycleIcons.Check.drawable)
    val indication = interactionSource?.let {
        Modifier.indication(
            it,
            cycleIndication(CycleTheme.shapes.full, if (checked) colors.onAccent else colors.onSurface)
        )
    } ?: Modifier
    Box(
        modifier
            .then(indication)
            .size(SwitchTrackWidth, SwitchTrackHeight)
            .drawBehind {
                val pill = CornerRadius(size.height / 2)
                drawRoundRect(track, cornerRadius = pill)
                val stroke = SwitchBorderWidth.toPx()
                drawRoundRect(
                    border,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(size.height / 2 - stroke / 2),
                    style = Stroke(stroke)
                )
                val fraction = progress()
                val fromStart = lerp(SwitchThumbOffStart, SwitchThumbOnStart, fraction).toPx()
                val x = if (layoutDirection == LayoutDirection.Ltr) fromStart else size.width - fromStart
                val radius = lerp(SwitchThumbOffSize, SwitchThumbOnSize, fraction).toPx() / 2
                drawCircle(thumb, radius.coerceAtLeast(0f), Offset(x, size.height / 2))
                val iconSize = SwitchCheckSize.toPx()
                translate(x - iconSize / 2, size.height / 2 - iconSize / 2) {
                    with(checkIcon) { draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(check)) }
                }
            }
    )
}

private data class SwitchColors(val track: Color, val border: Color, val thumb: Color, val check: Color)

/**
 * Where the thumb is: 0 off, at the start, and 1 on, at the end. It moves on the default spatial
 * spring, so it overshoots a little, and jumps under reduce motion. Read the returned function when
 * drawing, so the spring only redraws.
 */
@Composable
internal fun switchThumbProgress(checked: Boolean): () -> Float {
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    val spec by rememberUpdatedState(CycleTheme.motion.defaultSpatialSpec<Float>())
    LaunchedEffect(checked) { progress.animateTo(if (checked) 1f else 0f, spec) }
    return remember(progress) { { progress.value } }
}

private val SwitchTrackWidth = 52.dp
private val SwitchTrackHeight = 32.dp
private val SwitchBorderWidth = 2.dp
private val SwitchThumbOffSize = 16.dp
private val SwitchThumbOnSize = 24.dp
private val SwitchCheckSize = 16.dp

/** The thumb's centre from the track's start edge, off and on. */
private val SwitchThumbOffStart = 16.dp
private val SwitchThumbOnStart = 36.dp
private val SwitchRowMinHeight = 56.dp

@Composable
private fun SwitchStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        listOf<Triple<Interaction?, Boolean, Boolean>>(
            Triple(null, true, false),
            Triple(null, true, true),
            Triple(PressInteraction.Press(Offset.Zero), true, false),
            Triple(FocusInteraction.Focus(), true, true),
            Triple(null, false, false),
            Triple(null, false, true)
        ).forEach { (interaction, enabled, checked) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(checked, {
                }, "Log sleep", enabled = enabled, interactionSource = rememberInteractionSourceIn(interaction))
                SwitchRow(
                    "Sleep",
                    checked,
                    {},
                    body = "How long and how well.",
                    icon = CycleIcons.Bedtime,
                    enabled = enabled,
                    interactionSource = rememberInteractionSourceIn(interaction)
                )
            }
        }
    }
}

@Preview(name = "Switches · light", widthDp = 360)
@Composable
private fun SwitchesLightPreview() = PreviewSurface(darkTheme = false) { SwitchStates() }

@Preview(name = "Switches · dark", widthDp = 360)
@Composable
private fun SwitchesDarkPreview() = PreviewSurface(darkTheme = true) { SwitchStates() }
