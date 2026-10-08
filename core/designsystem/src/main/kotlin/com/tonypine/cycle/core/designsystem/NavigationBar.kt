package com.tonypine.cycle.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** One top-level screen in a [NavigationBar]: a one-word [label] ("Today") and its [icon]. */
@Immutable
data class NavigationDestination(val label: String, val icon: CycleIcons)

/**
 * The app's top-level navigation: three to five [destinations] in a floating, rounded
 * `surfaceContainer` bar (Zest floats it) with a 1dp shadow. Each destination shows its icon over its
 * label; the one at [selectedIndex] sits on an `accentContainer` pill with an `onAccentContainer`
 * icon and an `onSurface` label, the others are `onSurfaceVariant`.
 *
 * When the selection changes, the pill slides to the new destination and stretches on the way: its
 * leading edge moves on the fast spatial spring and its trailing edge on the default one. Under reduce
 * motion it jumps straight there. [onSelect] gets the index of the destination tapped; the caller
 * moves [selectedIndex].
 *
 * Each destination is a tab (`Role.Tab`) at least 48dp tall, with its selected state and its position,
 * so TalkBack reads "Calendar, selected, tab, 2 of 4". The icon is decoration.
 *
 * Draw the screen edge to edge and put the bar at the bottom: it pads itself by the navigation bar.
 * [interactionSources] holds one source per destination, for a preview or test to hold a state in.
 */
@Composable
fun NavigationBar(
    destinations: List<NavigationDestination>,
    selectedIndex: Int,
    onSelect: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    interactionSources: List<MutableInteractionSource> =
        remember(destinations.size) { List(destinations.size) { MutableInteractionSource() } }
) {
    require(destinations.size in NavigationBarDestinationCount) {
        "A navigation bar holds $NavigationBarDestinationCount destinations, not ${destinations.size}."
    }
    require(selectedIndex in destinations.indices) { "selectedIndex $selectedIndex is not a destination." }
    require(interactionSources.size == destinations.size) { "Pass one interaction source per destination." }
    val spacing = CycleTheme.spacing
    val shape = CycleTheme.shapes.extraLarge
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = spacing.medium, end = spacing.medium, bottom = spacing.medium)
            .shadow(CycleTheme.elevation.level1, shape)
            .background(CycleTheme.colors.surfaceContainer, shape)
            .drawSelectionIndicator(
                indicator = rememberSelectionIndicator(selectedIndex),
                count = destinations.size,
                color = CycleTheme.colors.accentContainer,
                shape = CycleTheme.shapes.full,
                horizontalPadding = spacing.small,
                topPadding = spacing.medium
            )
            .padding(horizontal = spacing.small, vertical = spacing.medium)
    ) {
        val labelStyle = rememberLabelStyle(destinations, slotWidth = maxWidth / destinations.size)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .semantics { collectionInfo = CollectionInfo(rowCount = 1, columnCount = destinations.size) },
            verticalAlignment = Alignment.Top
        ) {
            destinations.forEachIndexed { index, destination ->
                NavigationBarItem(
                    destination = destination,
                    index = index,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    interactionSource = interactionSources[index],
                    labelStyle = labelStyle,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * `labelSmall`, shrunk when the widest label does not fit in [slotWidth] (a long word such as
 * "Einstellungen", or a large font scale), so every label stays whole and the same size. It never
 * goes below [NavigationBarLabelFloor] as it would read at 100%; a label still too wide there ends in
 * an ellipsis. TalkBack always reads the whole label.
 */
@Composable
private fun rememberLabelStyle(destinations: List<NavigationDestination>, slotWidth: Dp): TextStyle {
    val style = CycleTheme.typography.labelSmall
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    return remember(destinations, slotWidth, style, density) {
        val widest = destinations.maxOf { measurer.measure(it.label, style, maxLines = 1).size.width }
        val fit = with(density) { slotWidth.toPx() } / widest
        val floor = NavigationBarLabelFloor.value / style.fontSize.value
        val smallest = minOf(floor / maxOf(density.fontScale, 1f), 1f)
        val scale = fit.coerceIn(smallest, 1f)
        style.copy(fontSize = style.fontSize * scale, letterSpacing = style.letterSpacing * scale)
    }
}

@Composable
private fun NavigationBarItem(
    destination: NavigationDestination,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    labelStyle: TextStyle,
    modifier: Modifier
) {
    val colors = CycleTheme.colors
    val motion = CycleTheme.motion
    val iconTint by animateColorAsState(
        if (selected) colors.onAccentContainer else colors.onSurfaceVariant,
        motion.defaultEffectsSpec(),
        label = "icon"
    )
    val labelColor by animateColorAsState(
        if (selected) colors.onSurface else colors.onSurfaceVariant,
        motion.defaultEffectsSpec(),
        label = "label"
    )
    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                collectionItemInfo = CollectionItemInfo(rowIndex = 0, rowSpan = 1, columnIndex = index, columnSpan = 1)
            }
            .heightIn(min = MinTouchTarget),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The pill only takes the indication: the selection indicator is drawn by the bar, behind it.
        Box(
            modifier = Modifier
                .size(IndicatorWidth, IndicatorHeight)
                .indication(interactionSource, cycleIndication(CycleTheme.shapes.full, color = iconTint)),
            contentAlignment = Alignment.Center
        ) {
            CycleIcon(destination.icon, contentDescription = null, tint = iconTint)
        }
        BasicText(
            text = destination.label,
            style = labelStyle.copy(color = labelColor, textAlign = TextAlign.Center),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}

/**
 * The edges of the selection pill, as positions along the bar in destinations: both sit on the
 * selected index at rest. When the selection moves, the leading edge springs to the new destination
 * on the fast spatial spring and the trailing one follows on the default spatial spring, so the pill
 * stretches as it slides. Both snap under reduce motion.
 */
private class SelectionIndicator(initial: Int) {
    val start = Animatable(initial.toFloat())
    val end = Animatable(initial.toFloat())
}

@Composable
private fun rememberSelectionIndicator(selectedIndex: Int): SelectionIndicator {
    val motion = CycleTheme.motion
    val indicator = remember { SelectionIndicator(selectedIndex) }
    LaunchedEffect(selectedIndex, motion) {
        val target = selectedIndex.toFloat()
        val forward = target > (indicator.start.value + indicator.end.value) / 2
        val (leading, trailing) = if (forward) indicator.end to indicator.start else indicator.start to indicator.end
        coroutineScope {
            launch { leading.animateTo(target, motion.fastSpatialSpec()) }
            launch { trailing.animateTo(target, motion.defaultSpatialSpec()) }
        }
    }
    return indicator
}

/**
 * Draws [indicator] in [color] and [shape] behind [count] equal slots that start [horizontalPadding]
 * in from each side, [topPadding] down: where the bar lays out its items' pills. Mirrors right to left.
 */
private fun Modifier.drawSelectionIndicator(
    indicator: SelectionIndicator,
    count: Int,
    color: Color,
    shape: Shape,
    horizontalPadding: Dp,
    topPadding: Dp
): Modifier = drawBehind {
    val inset = horizontalPadding.toPx()
    val slot = (size.width - 2 * inset) / count
    val halfPill = IndicatorWidth.toPx() / 2
    val left = inset + slot * (indicator.start.value + 0.5f) - halfPill
    val right = inset + slot * (indicator.end.value + 0.5f) + halfPill
    if (right <= left) return@drawBehind
    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - right else left
    val pill = Size(right - left, IndicatorHeight.toPx())
    translate(x, topPadding.toPx()) { drawOutline(shape.createOutline(pill, layoutDirection, this), color) }
}

/** How many destinations a [NavigationBar] holds. */
val NavigationBarDestinationCount = 3..5

/**
 * The smallest a label shrinks to, as it reads at 100% font size: 11sp, a point under `labelSmall`, so
 * German's "Einstellungen" (85dp at 12sp) stays whole in a 78dp slot on a phone 360dp wide
 * (`docs/design/language.md`, Components).
 */
val NavigationBarLabelFloor = 11.sp

private val IndicatorWidth = 56.dp
private val IndicatorHeight = 32.dp

private val SampleDestinations = listOf(
    NavigationDestination("Today", CycleIcons.Today),
    NavigationDestination("Calendar", CycleIcons.Calendar),
    NavigationDestination("Log", CycleIcons.Add),
    NavigationDestination("Settings", CycleIcons.Settings)
)

@Composable
private fun NavigationBarStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        listOf(null, PressInteraction.Press(Offset.Zero), FocusInteraction.Focus()).forEach { interaction ->
            val sources = SampleDestinations.indices.map { index ->
                rememberInteractionSourceIn(interaction.takeIf { index == 1 })
            }
            NavigationBar(SampleDestinations, selectedIndex = 0, onSelect = {}, interactionSources = sources)
        }
        NavigationBar(SampleDestinations.take(3), selectedIndex = 2, onSelect = {})
    }
}

@Preview(name = "Navigation bar · light", widthDp = 360)
@Composable
private fun NavigationBarLightPreview() = PreviewSurface(darkTheme = false) { NavigationBarStates() }

@Preview(name = "Navigation bar · dark", widthDp = 360)
@Composable
private fun NavigationBarDarkPreview() = PreviewSurface(darkTheme = true) { NavigationBarStates() }
