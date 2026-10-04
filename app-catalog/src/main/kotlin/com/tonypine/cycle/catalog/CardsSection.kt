package com.tonypine.cycle.catalog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tonypine.cycle.core.designsystem.Card
import com.tonypine.cycle.core.designsystem.ClickableCard
import com.tonypine.cycle.core.designsystem.CycleTheme

@Composable
internal fun CardsSection() {
    var opened by rememberSaveable { mutableIntStateOf(0) }
    SubsectionTitle("Try it")
    ClickableCard(onClick = { opened++ }, modifier = Modifier.fillMaxWidth(), onClickLabel = "Open") {
        CardLines("Last cycle", "29 days. Opened $opened times.")
    }

    SubsectionTitle("Card")
    Card(Modifier.fillMaxWidth()) {
        CardLines("Cycle day 12", "About 16 days until your next period.")
    }
    CatalogText(
        "surfaceContainer, 32dp corners, 16dp padding. Does nothing on tap; TalkBack reads its content in order.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )

    SubsectionTitle("Clickable card")
    HeldStates.forEach { (state, interaction) ->
        ClickableCard(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            interactionSource = rememberHeldInteraction(interaction)
        ) {
            CardLines(state, "Last cycle: 29 days.")
        }
    }
    ClickableCard(onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false) {
        CardLines("Disabled", "Last cycle: 29 days.")
    }
    CatalogText(
        "One Role.Button for the whole card: state layer and press scale in the 32dp shape, and the focus ring.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
}

@Composable
private fun CardLines(title: String, body: String) {
    CatalogText(title, CycleTheme.typography.titleSmall)
    CatalogText(body, CycleTheme.typography.bodySmall, color = CycleTheme.colors.onSurfaceVariant)
}
