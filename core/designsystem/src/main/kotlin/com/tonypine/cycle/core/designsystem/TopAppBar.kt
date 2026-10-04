package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * An icon action in a [TopAppBar]: drawn as an [IconButton]. Name the action in
 * [contentDescription] ("Back", "Settings"), not the icon. [interactionSource] receives the
 * button's interactions; a preview or test can emit into it to hold a pressed or focused state.
 */
class AppBarAction(
    val icon: CycleIcons,
    val contentDescription: String,
    val onClick: () -> Unit,
    val interactionSource: MutableInteractionSource? = null
)

/**
 * The bar at the top of a screen: an optional [navigation] icon button (usually `CycleIcons.Back`),
 * the screen's [title] in `title` type, and up to two [actions] at the end, on `surface`.
 *
 * The title is a heading, so TalkBack users can jump to it, and reads between the navigation button
 * and the actions. It stays on one line and ends in an ellipsis when it runs out of room, at any font
 * scale, so the actions always stay on screen.
 *
 * Draw the screen edge to edge and put the bar at the top: it pads itself by the status bar and by
 * the display cutout and navigation bar on either side, and fills the space behind the status bar
 * with `surface`.
 */
@Composable
fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigation: AppBarAction? = null,
    actions: List<AppBarAction> = emptyList()
) {
    require(actions.size <= MAX_TOP_APP_BAR_ACTIONS) {
        "A top app bar holds up to $MAX_TOP_APP_BAR_ACTIONS actions, not ${actions.size}."
    }
    val spacing = CycleTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CycleTheme.colors.surface)
            .windowInsetsPadding(TopAppBarInsets)
            .heightIn(min = TopAppBarHeight)
            .padding(horizontal = spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        navigation?.let { AppBarIconButton(it) }
        BasicText(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = if (navigation == null) spacing.medium else spacing.extraSmall,
                    end = if (actions.isEmpty()) spacing.medium else spacing.extraSmall
                )
                .semantics { heading() },
            style = CycleTheme.typography.title.copy(color = CycleTheme.colors.onSurface),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
        actions.forEach { AppBarIconButton(it) }
    }
}

@Composable
private fun AppBarIconButton(action: AppBarAction) {
    val ownSource = remember { MutableInteractionSource() }
    IconButton(
        icon = action.icon,
        contentDescription = action.contentDescription,
        onClick = action.onClick,
        interactionSource = action.interactionSource ?: ownSource
    )
}

/** The most actions a [TopAppBar] holds. Anything more belongs on the screen itself. */
const val MAX_TOP_APP_BAR_ACTIONS = 2

/** The status bar above, and the display cutout and navigation bar on either side. */
private val TopAppBarInsets: WindowInsets
    @Composable get() = WindowInsets.statusBars.union(
        WindowInsets.displayCutout.union(WindowInsets.navigationBars).only(WindowInsetsSides.Horizontal)
    )

private val TopAppBarHeight = 64.dp

@Composable
private fun TopAppBarStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        listOf<Interaction?>(null, PressInteraction.Press(Offset.Zero), FocusInteraction.Focus()).forEach {
            TopAppBar(
                title = "Calendar",
                navigation = AppBarAction(CycleIcons.Back, "Back", {}, rememberInteractionSourceIn(it)),
                actions = listOf(
                    AppBarAction(CycleIcons.Today, "Go to today", {}),
                    AppBarAction(CycleIcons.Settings, "Settings", {})
                )
            )
        }
        TopAppBar(
            title = "Symptoms, moods and notes for the whole cycle",
            actions = listOf(AppBarAction(CycleIcons.Add, "Log a day", {}))
        )
        TopAppBar(title = "Today")
    }
}

@Preview(name = "Top app bar · light", widthDp = 360)
@Composable
private fun TopAppBarLightPreview() = PreviewSurface(darkTheme = false) { TopAppBarStates() }

@Preview(name = "Top app bar · dark", widthDp = 360)
@Composable
private fun TopAppBarDarkPreview() = PreviewSurface(darkTheme = true) { TopAppBarStates() }
