package com.tonypine.cycle.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.CycleIcon
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.animatedCornerShape
import com.tonypine.cycle.core.designsystem.cycleIndication

/**
 * A stand-in pill button for the catalog's demos, built from the interaction foundations: the
 * indication, the press squash and the disabled alphas. The real buttons come with their own ticket.
 */
@Composable
internal fun DemoButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: CycleIcons? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    val alpha = CycleTheme.stateAlpha
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = animatedCornerShape(active = pressed)
    val container = if (enabled) colors.accent else colors.onSurface.copy(alpha = alpha.disabledContainer)
    val content = if (enabled) colors.onAccent else colors.onSurface.copy(alpha = alpha.disabledContent)
    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = cycleIndication(shape, color = content),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .background(container, shape)
            .heightIn(min = 48.dp)
            .padding(horizontal = CycleTheme.spacing.extraLarge, vertical = CycleTheme.spacing.medium),
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let { CycleIcon(it, contentDescription = null, tint = content) }
        CatalogText(label, CycleTheme.typography.label, color = content)
    }
}

/** An interaction source held in [interaction], to show a hovered, focused or pressed state still. */
@Composable
internal fun rememberHeldInteraction(interaction: Interaction?): MutableInteractionSource {
    val source = remember { MutableInteractionSource() }
    LaunchedEffect(interaction) { interaction?.let { source.emit(it) } }
    return source
}
