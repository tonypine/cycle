package com.tonypine.cycle.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.NavigationBar
import com.tonypine.cycle.core.designsystem.NavigationDestination
import com.tonypine.cycle.core.designsystem.TopAppBar

private val Destinations = listOf(
    NavigationDestination("Today", CycleIcons.Today),
    NavigationDestination("Calendar", CycleIcons.Calendar),
    NavigationDestination("Log", CycleIcons.Add),
    NavigationDestination("Settings", CycleIcons.Settings)
)

@Composable
internal fun AppBarsSection() {
    var selected by rememberSaveable { mutableIntStateOf(1) }
    SubsectionTitle("Try it")
    CatalogText(
        "Pick a destination: the pill slides over and stretches on the way. The full-screen demo puts " +
            "both bars on an edge-to-edge screen.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    NavigationBar(Destinations, selected, onSelect = { selected = it })

    SubsectionTitle("Top app bar")
    VariantStates("With navigation and two actions", "Title in title type. Back held in each state.") {
        HeldStates.forEach { (state, interaction) -> CalendarBar(state, interaction) }
    }
    VariantStates("A long title", "One line, ending in an ellipsis before the actions.") {
        TopAppBar(
            title = "Symptoms, moods and notes for the whole cycle",
            actions = listOf(AppBarAction(CycleIcons.Add, "Log a day", {}))
        )
    }
    VariantStates("Title alone", "No navigation and no actions.") { TopAppBar(title = "Today") }

    SubsectionTitle("Navigation bar")
    VariantStates("Calendar held", "Today selected, Calendar in each state.") {
        HeldStates.forEach { (state, interaction) ->
            Column {
                CatalogText(state, CycleTheme.typography.labelSmall, color = CycleTheme.colors.onSurfaceVariant)
                NavigationBar(
                    Destinations,
                    selectedIndex = 0,
                    onSelect = {},
                    interactionSources = Destinations.indices.map { index ->
                        rememberHeldInteraction(interaction.takeIf { index == 1 })
                    }
                )
            }
        }
    }
    VariantStates("Three destinations", "The fewest a bar holds; five is the most.") {
        NavigationBar(Destinations.take(3), selectedIndex = 2, onSelect = {})
    }
}

@Composable
private fun CalendarBar(state: String, interaction: Interaction?) {
    Column {
        CatalogText(state, CycleTheme.typography.labelSmall, color = CycleTheme.colors.onSurfaceVariant)
        TopAppBar(
            title = "Calendar",
            navigation = AppBarAction(CycleIcons.Back, "Back, $state", {}, rememberHeldInteraction(interaction)),
            actions = listOf(
                AppBarAction(CycleIcons.Today, "Go to today", {}),
                AppBarAction(CycleIcons.Settings, "Settings", {})
            )
        )
    }
}

/**
 * Both bars on a full-screen, edge-to-edge screen, the way an app screen uses them: the top app bar
 * under the status bar, the content scrolling between them, and the navigation bar floating above the
 * system navigation bar. The shaded strips mark the system bars, so the space each bar keeps clear
 * shows even where the system draws no bars (Robolectric). Back returns to the catalog.
 */
@Composable
internal fun AppBarsDemo(onClose: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(1) }
    val spacing = CycleTheme.spacing
    Box(
        Modifier
            .fillMaxSize()
            .background(CycleTheme.colors.surface)
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = Destinations[selected].label,
                navigation = AppBarAction(CycleIcons.Back, "Back to the catalog", onClose),
                actions = listOf(
                    AppBarAction(CycleIcons.Today, "Go to today", {}),
                    AppBarAction(CycleIcons.Settings, "Settings", {})
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .windowInsetsPadding(
                        WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal)
                    )
                    .padding(spacing.large),
                verticalArrangement = Arrangement.spacedBy(spacing.large)
            ) {
                CatalogText(
                    "This screen draws edge to edge. The shaded strips mark the status bar and the navigation " +
                        "bar; each bar keeps its controls clear of them.",
                    CycleTheme.typography.body
                )
                repeat(DEMO_CARDS) { card -> DemoCard(card + 1) }
            }
            NavigationBar(Destinations, selected, onSelect = { selected = it })
        }
        SystemBarShade(Modifier.align(Alignment.TopCenter).windowInsetsTopHeight(WindowInsets.statusBars))
        SystemBarShade(Modifier.align(Alignment.BottomCenter).windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun DemoCard(number: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CycleTheme.colors.surfaceContainer, CycleTheme.shapes.large)
            .padding(CycleTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)
    ) {
        CatalogText("Sample card $number", CycleTheme.typography.titleSmall)
        CatalogText(
            "Content scrolls between the bars.",
            CycleTheme.typography.bodySmall,
            color = CycleTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun SystemBarShade(modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(CycleTheme.colors.onSurface.copy(alpha = CycleTheme.stateAlpha.pressed))
    )
}

private const val DEMO_CARDS = 6
