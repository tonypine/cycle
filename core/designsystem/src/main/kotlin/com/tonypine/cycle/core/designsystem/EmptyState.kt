package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** A button under an [EmptyState]: its [label], an optional leading [icon], and what it does. */
@Immutable
class EmptyStateAction(val label: String, val onClick: () -> Unit, val icon: CycleIcons? = null)

/**
 * What a screen or a list shows when it has nothing yet, such as before the first period is logged:
 * an [illustration] (an [EmptyStateIcon] or a drawing), a [title] in `title`, one [body] sentence
 * in `onSurfaceVariant`, an optional [action] as a [FilledButton] and an optional [secondaryAction]
 * as a [TextButton] under it, such as "Skip for now", centred one above the other.
 *
 * Given a bounded height (a screen, or a box with a size), it fills it, centres its content, and
 * scrolls when the content is taller, as at 200% font scale; it never clips. In a column that
 * scrolls already, it takes its content's height and leaves scrolling to the column.
 *
 * TalkBack reads the illustration, title and body as one item marked as a heading, then the actions
 * as buttons.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    illustration: (@Composable () -> Unit)? = null,
    action: EmptyStateAction? = null,
    secondaryAction: EmptyStateAction? = null
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val scroll = if (constraints.hasBoundedHeight) {
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
        } else {
            Modifier
        }
        Column(
            modifier = scroll
                .fillMaxWidth()
                .padding(CycleTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraLarge, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) { heading() },
                verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (illustration != null) {
                    Box(Modifier.padding(bottom = CycleTheme.spacing.large)) { illustration() }
                }
                val typography = CycleTheme.typography
                val colors = CycleTheme.colors
                BasicText(
                    title,
                    style = typography.title.copy(color = colors.onSurface, textAlign = TextAlign.Center)
                )
                BasicText(
                    body,
                    style = typography.body.copy(color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                )
            }
            if (action != null || secondaryAction != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (action != null) FilledButton(action.label, action.onClick, icon = action.icon)
                    if (secondaryAction != null) {
                        TextButton(secondaryAction.label, secondaryAction.onClick, icon = secondaryAction.icon)
                    }
                }
            }
        }
    }
}

/**
 * The default [EmptyState] illustration: [icon] at 48dp in `onAccentContainer`, in a 96dp
 * `accentContainer` circle. It is decoration; the title says what is empty.
 */
@Composable
fun EmptyStateIcon(icon: CycleIcons, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(EmptyStateIconContainerSize)
            .background(CycleTheme.colors.accentContainer, CycleTheme.shapes.full),
        contentAlignment = Alignment.Center
    ) {
        CycleIcon(
            icon,
            contentDescription = null,
            tint = CycleTheme.colors.onAccentContainer,
            size = EmptyStateIconSize
        )
    }
}

private val EmptyStateIconContainerSize = 96.dp
private val EmptyStateIconSize = 48.dp

@Composable
private fun EmptyStateSample() {
    EmptyState(
        title = "No periods logged yet",
        body = "Log the first day of your last period and Cycle will start predicting the next one.",
        modifier = Modifier.size(width = 328.dp, height = 480.dp),
        illustration = { EmptyStateIcon(CycleIcons.Calendar) },
        action = EmptyStateAction("Log a period", onClick = {}, icon = CycleIcons.Add)
    )
}

@Composable
private fun EmptyStateTwoActionsSample() {
    EmptyState(
        title = "Hi! Let's get your cycle going",
        body = "Log your period and Cycle estimates the next one.",
        modifier = Modifier.size(width = 328.dp, height = 480.dp),
        illustration = { EmptyStateIcon(CycleIcons.WaterDrop) },
        action = EmptyStateAction("Get started", onClick = {}),
        secondaryAction = EmptyStateAction("Skip for now", onClick = {})
    )
}

@Preview(name = "Empty state · light", widthDp = 360)
@Composable
private fun EmptyStateLightPreview() = PreviewSurface(darkTheme = false) { EmptyStateSample() }

@Preview(name = "Empty state · dark", widthDp = 360)
@Composable
private fun EmptyStateDarkPreview() = PreviewSurface(darkTheme = true) { EmptyStateSample() }

@Preview(name = "Empty state · 200% font", widthDp = 360, fontScale = 2f)
@Composable
private fun EmptyStateLargeFontPreview() = PreviewSurface(darkTheme = false) { EmptyStateSample() }

@Preview(name = "Empty state · two actions · light", widthDp = 360)
@Composable
private fun EmptyStateTwoActionsLightPreview() = PreviewSurface(darkTheme = false) { EmptyStateTwoActionsSample() }

@Preview(name = "Empty state · two actions · dark", widthDp = 360)
@Composable
private fun EmptyStateTwoActionsDarkPreview() = PreviewSurface(darkTheme = true) { EmptyStateTwoActionsSample() }
