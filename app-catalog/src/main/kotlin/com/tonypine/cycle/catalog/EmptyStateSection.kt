package com.tonypine.cycle.catalog

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.EmptyState
import com.tonypine.cycle.core.designsystem.EmptyStateAction
import com.tonypine.cycle.core.designsystem.EmptyStateIcon

@Composable
internal fun EmptyStateSection() {
    var logged by rememberSaveable { mutableStateOf(false) }
    SubsectionTitle("With an action")
    EmptyState(
        title = if (logged) "Nice, that's logged" else "No periods logged yet",
        body = "Log the first day of your last period and Cycle will start predicting the next one.",
        modifier = Modifier.frame(),
        illustration = { EmptyStateIcon(if (logged) CycleIcons.Check else CycleIcons.Calendar) },
        action = EmptyStateAction(if (logged) "Undo" else "Log a period", onClick = { logged = !logged })
    )

    SubsectionTitle("With a second action")
    EmptyState(
        title = "Hi! Let's get your cycle going",
        body = "Log your period and Cycle estimates the next one.",
        modifier = Modifier.frame(),
        illustration = { EmptyStateIcon(CycleIcons.WaterDrop) },
        action = EmptyStateAction("Get started", onClick = {}),
        secondaryActions = listOf(EmptyStateAction("Skip for now", onClick = {}))
    )

    SubsectionTitle("Without an action")
    EmptyState(
        title = "No notes",
        body = "Notes you add to a day show up here.",
        modifier = Modifier.frame(),
        illustration = { EmptyStateIcon(CycleIcons.Today) }
    )
    CatalogText(
        "Centred in the space it is given, and scrolls when the text is large. TalkBack reads the illustration, " +
            "title and body as one heading, then the actions.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
}

/** A box the size of a small screen, outlined so the centring shows. */
@Composable
private fun Modifier.frame(): Modifier = fillMaxWidth()
    .height(400.dp)
    .border(1.dp, CycleTheme.colors.outlineVariant, CycleTheme.shapes.large)
