package com.tonypine.cycle.core.designsystem

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Where a [CycleBottomSheet] rests. */
enum class CycleSheetValue {
    /** Closed: the sheet is off screen and its window is gone. */
    Hidden,

    /** Showing its top half. Only exists when the sheet is taller than half the screen. */
    PartiallyExpanded,

    /** Showing all of it, up to the status bar. */
    Expanded
}

/**
 * Holds a [CycleBottomSheet]'s position and opens and closes it. Create it with
 * [rememberCycleBottomSheetState], then call [show] and [hide] from a coroutine (a click handler's
 * `scope.launch { }`). The sheet moves between its anchors on `CycleTheme.motion`'s slow spatial
 * spring, which is `snap()` under reduce motion.
 *
 * When [skipPartiallyExpanded] is true, the sheet opens straight to [CycleSheetValue.Expanded].
 */
@Stable
class CycleBottomSheetState(
    initialValue: CycleSheetValue = CycleSheetValue.Hidden,
    val skipPartiallyExpanded: Boolean = false
) {
    init {
        require(!(skipPartiallyExpanded && initialValue == CycleSheetValue.PartiallyExpanded)) {
            "A sheet that skips the partially expanded anchor cannot start there."
        }
    }

    internal val anchoredState = AnchoredDraggableState(initialValue)

    /** The theme's motion, kept current by [CycleBottomSheet], so reduce motion applies to [show] and [hide]. */
    internal var motion: CycleMotion = ZestMotion

    private var open by mutableStateOf(initialValue != CycleSheetValue.Hidden)

    /** True from [show] until the sheet has finished closing: while its window is on screen. */
    val isVisible: Boolean get() = open

    /** The anchor the sheet is at, or was last at while it moves. */
    val currentValue: CycleSheetValue get() = anchoredState.currentValue

    /** The anchor the sheet is moving to, or [currentValue] when it rests. */
    val targetValue: CycleSheetValue get() = anchoredState.targetValue

    /** True when the sheet is tall enough to have a partially expanded anchor. */
    val hasPartiallyExpandedState: Boolean
        get() = anchoredState.anchors.hasPositionFor(CycleSheetValue.PartiallyExpanded)

    /** Opens the sheet, partially expanded when it is tall enough, and expanded otherwise. */
    suspend fun show() = open(CycleSheetValue.PartiallyExpanded)

    /** Opens the sheet, or moves it, to [CycleSheetValue.Expanded]. */
    suspend fun expand() = open(CycleSheetValue.Expanded)

    /** Moves an open sheet to [CycleSheetValue.PartiallyExpanded], or opens it there. */
    suspend fun partialExpand() {
        check(!skipPartiallyExpanded) { "This sheet skips the partially expanded anchor." }
        open(CycleSheetValue.PartiallyExpanded)
    }

    /** Slides the sheet off screen, then closes its window. */
    suspend fun hide() {
        if (!open) return
        awaitAnchors()
        anchoredState.animateTo(CycleSheetValue.Hidden, motion.slowSpatialSpec())
        open = false
    }

    private suspend fun open(value: CycleSheetValue) {
        open = true
        awaitAnchors()
        anchoredState.animateTo(reachable(value), motion.slowSpatialSpec())
    }

    /** [value], or the nearest anchor this sheet has when it has no partially expanded one. */
    private fun reachable(value: CycleSheetValue): CycleSheetValue =
        if (value == CycleSheetValue.PartiallyExpanded && !hasPartiallyExpandedState) {
            CycleSheetValue.Expanded
        } else {
            value
        }

    /** Waits until the sheet has been measured in its window. */
    private suspend fun awaitAnchors() {
        snapshotFlow { anchoredState.anchors.size }.first { it > 0 }
    }

    /** Called once the sheet has settled off screen, after a drag or a fling: the window closes. */
    internal fun onSettledHidden() {
        open = false
    }

    /**
     * Places the anchors for a sheet [sheetHeight] tall in a window [layoutHeight] tall: hidden just
     * below the window, partially expanded at half its height, and expanded where the whole sheet
     * shows.
     */
    internal fun updateAnchors(layoutHeight: Int, sheetHeight: Int) {
        val full = layoutHeight.toFloat()
        val anchors = DraggableAnchors {
            CycleSheetValue.Hidden at full
            if (!skipPartiallyExpanded && sheetHeight > full / 2) CycleSheetValue.PartiallyExpanded at full / 2
            if (sheetHeight > 0) CycleSheetValue.Expanded at max(0f, full - sheetHeight)
        }
        val target = anchoredState.targetValue.let { target ->
            when {
                anchors.hasPositionFor(target) -> target
                target == CycleSheetValue.PartiallyExpanded -> CycleSheetValue.Expanded
                else -> CycleSheetValue.Hidden
            }
        }
        anchoredState.updateAnchors(anchors, target)
    }

    /**
     * Settles a drag that ended inside the sheet's scrolling content: past [velocityThreshold], to
     * the next anchor in the direction of [velocity]; otherwise to the closest one.
     */
    internal suspend fun settle(velocity: Float, velocityThreshold: Float) {
        val anchors = anchoredState.anchors
        val offset = anchoredState.offset
        if (anchors.size == 0 || offset.isNaN()) return
        val target = if (abs(velocity) >= velocityThreshold) {
            anchors.closestAnchor(offset, searchUpwards = velocity > 0) ?: anchors.closestAnchor(offset)
        } else {
            anchors.closestAnchor(offset)
        } ?: return
        anchoredState.animateTo(target, motion.slowSpatialSpec())
    }

    /**
     * How far the bottom of a sheet [sheetHeight] tall sits above the bottom of its window: zero at
     * rest, more while the spring overshoots an anchor.
     */
    internal fun gapBelow(sheetHeight: Float): Float {
        val windowBottom = anchoredState.anchors.positionOf(CycleSheetValue.Hidden)
        val offset = anchoredState.offset
        if (windowBottom.isNaN() || offset.isNaN()) return 0f
        return (windowBottom - offset - sheetHeight).coerceAtLeast(0f)
    }

    /** How far open the sheet is, from 0 (hidden) to 1 (at its first open anchor), for the scrim. */
    internal fun openFraction(): Float {
        val anchors = anchoredState.anchors
        val offset = anchoredState.offset
        val hidden = anchors.positionOf(CycleSheetValue.Hidden)
        val firstOpen = listOf(CycleSheetValue.PartiallyExpanded, CycleSheetValue.Expanded)
            .map { anchors.positionOf(it) }
            .filterNot { it.isNaN() }
            .maxOrNull()
        if (offset.isNaN() || hidden.isNaN() || firstOpen == null || hidden == firstOpen) return 0f
        return ((hidden - offset) / (hidden - firstOpen)).coerceIn(0f, 1f)
    }

    companion object {
        /** Saves the anchor the sheet is moving to, so it reopens there after a configuration change. */
        fun Saver(skipPartiallyExpanded: Boolean): Saver<CycleBottomSheetState, CycleSheetValue> = Saver(
            save = { it.targetValue },
            restore = { CycleBottomSheetState(it, skipPartiallyExpanded) }
        )
    }
}

/** Remembers a [CycleBottomSheetState], closed unless [initialValue] says otherwise, across configuration changes. */
@Composable
fun rememberCycleBottomSheetState(
    initialValue: CycleSheetValue = CycleSheetValue.Hidden,
    skipPartiallyExpanded: Boolean = false
): CycleBottomSheetState =
    rememberSaveable(skipPartiallyExpanded, saver = CycleBottomSheetState.Saver(skipPartiallyExpanded)) {
        CycleBottomSheetState(initialValue, skipPartiallyExpanded)
    }

/**
 * A modal bottom sheet (Zest): a `surfaceContainer` panel with 32dp top corners that slides up over a
 * `scrim`, in a window of its own above the screen. It shows a drag handle, the [title] as a heading,
 * then [content] in a column that scrolls when the sheet is full.
 *
 * Open and close it through [state]: `scope.launch { state.show() }`. The sheet rests partially
 * expanded (half the screen) when it is taller than that, or expanded. People close it by dragging it
 * down, tapping the scrim, going back (with the predictive back animation where the system has it) or
 * with the handle's Dismiss accessibility action; [onDismiss] runs once it has closed, however it
 * closed. The handle also expands and collapses the sheet when tapped, and through accessibility
 * actions.
 *
 * The content sits above the navigation bar and the keyboard: when the keyboard opens, a partially
 * expanded sheet expands, and the focused field scrolls into view. Focus moves into the sheet when it
 * opens and stays on the opener in the screen behind, so it is back there when the sheet closes.
 * TalkBack announces the sheet as a pane named [title].
 */
@Composable
fun CycleBottomSheet(
    state: CycleBottomSheetState,
    title: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val motion = CycleTheme.motion
    SideEffect { state.motion = motion }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(state) {
        snapshotFlow { state.isVisible }.drop(1).collect { visible -> if (!visible) currentOnDismiss() }
    }
    if (!state.isVisible) return

    val scope = rememberCoroutineScope()
    // The dialog's window is a new root: carry over the density and direction the screen uses.
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    // Someone using a keyboard or D-pad lands on the handle; touch and TalkBack land on the sheet.
    val focusHandle = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    Dialog(
        onDismissRequest = { scope.launch { state.hide() } },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalDensity provides density, LocalLayoutDirection provides layoutDirection) {
            SheetWindow()
            SheetLayer(state, title, focusHandle, modifier, content)
        }
    }
}

/** The sheet draws its own scrim and motion, so the dialog window neither dims nor animates. */
@Composable
private fun SheetWindow() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window
    DisposableEffect(window) {
        window?.setDimAmount(0f)
        window?.setWindowAnimations(0)
        onDispose {}
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SheetLayer(
    state: CycleBottomSheetState,
    title: String,
    focusHandle: Boolean,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val motion = CycleTheme.motion
    val density = LocalDensity.current

    // Back: the sheet shrinks a little with the gesture's progress, closes when it completes, and
    // springs back when it is cancelled.
    val backProgress = remember { Animatable(0f) }
    PredictiveBackHandler { events ->
        try {
            events.collect { backProgress.snapTo(it.progress) }
            state.hide()
        } catch (cancelled: CancellationException) {
            scope.launch { backProgress.animateTo(0f, motion.fastSpatialSpec()) }
            throw cancelled
        }
    }

    // Close the window once a drag or fling has settled the sheet off screen.
    LaunchedEffect(state) {
        var opened = false
        snapshotFlow { state.anchoredState.settledValue }.collect { settled ->
            if (settled != CycleSheetValue.Hidden) {
                opened = true
            } else if (opened) {
                state.onSettledHidden()
            }
        }
    }

    // Typing needs room: a partially expanded sheet expands when the keyboard opens.
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible) {
        if (imeVisible && state.targetValue == CycleSheetValue.PartiallyExpanded) state.expand()
    }

    val sheetFocus = remember { FocusRequester() }
    val handleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { (if (focusHandle) handleFocus else sheetFocus).requestFocus() }

    val scrim = CycleTheme.colors.scrim
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(state) { detectTapGestures { scope.launch { state.hide() } } }
    ) {
        drawRect(scrim, alpha = state.openFraction())
    }

    Layout(
        content = {
            Sheet(state, title, backProgress::value, sheetFocus, handleFocus, modifier, content)
        },
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
            .imePadding()
            .nestedScroll(remember(state, density) { SheetNestedScrollConnection(state, density) })
    ) { measurables, constraints ->
        val sheet = measurables.single().measure(constraints.copy(minHeight = 0))
        state.updateAnchors(constraints.maxHeight, sheet.height)
        layout(constraints.maxWidth, constraints.maxHeight) {
            val offset = state.anchoredState.offset.takeUnless { it.isNaN() } ?: constraints.maxHeight.toFloat()
            sheet.place((constraints.maxWidth - sheet.width) / 2, offset.roundToInt())
        }
    }
}

@Composable
private fun Sheet(
    state: CycleBottomSheetState,
    title: String,
    backProgress: () -> Float,
    sheetFocus: FocusRequester,
    handleFocus: FocusRequester,
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    val shape = CycleTheme.shapes.extraLarge.copy(bottomStart = ZeroCorner, bottomEnd = ZeroCorner)
    val motion = CycleTheme.motion
    val backShift = spacing.extraLarge
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val progress = backProgress()
                scaleX = lerp(1f, BACK_SCALE, progress)
                translationY = backShift.toPx() * progress
            }
            .shadow(CycleTheme.elevation.level2, shape, clip = false)
            // The spring overshoots its anchor: fill the gap it opens below the sheet.
            .drawBehind {
                val gap = state.gapBelow(size.height)
                if (gap > 0f) {
                    val top = size.height - 1f
                    drawRect(colors.surfaceContainer, Offset(0f, top), Size(size.width, gap + 1f))
                }
            }
            .background(colors.surfaceContainer, shape)
            .anchoredDraggable(
                state = state.anchoredState,
                orientation = Orientation.Vertical,
                flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                    state.anchoredState,
                    positionalThreshold = { distance -> distance / 2 },
                    animationSpec = motion.slowSpatialSpec()
                )
            )
            .semantics { paneTitle = title }
            .focusRequester(sheetFocus)
            .focusTarget()
            .windowInsetsPadding(
                WindowInsets.navigationBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
            )
    ) {
        DragHandle(state, handleFocus, Modifier.align(Alignment.CenterHorizontally))
        BasicText(
            title,
            modifier = Modifier
                .padding(horizontal = spacing.extraLarge)
                .semantics { heading() },
            style = CycleTheme.typography.title.copy(color = colors.onSurface)
        )
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = spacing.extraLarge,
                    end = spacing.extraLarge,
                    top = spacing.large,
                    bottom = spacing.extraLarge
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.large),
            content = content
        )
    }
}

/**
 * The 32 by 4dp handle at the top of the sheet, in a 48dp touch target. Tapping it expands a
 * partially expanded sheet and collapses an expanded one (or closes it, without a partial anchor).
 * TalkBack reads "Drag handle", the sheet's state, and offers Expand or Collapse, and Dismiss.
 */
@Composable
private fun DragHandle(state: CycleBottomSheetState, focusRequester: FocusRequester, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val colors = CycleTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val expanded = state.targetValue == CycleSheetValue.Expanded
    val canCollapse = state.hasPartiallyExpandedState
    val label = stringResource(R.string.bottom_sheet_handle)
    val stateLabel = stringResource(
        if (expanded) R.string.bottom_sheet_expanded else R.string.bottom_sheet_partially_expanded
    )
    val expandLabel = stringResource(R.string.bottom_sheet_expand)
    val collapseLabel = stringResource(R.string.bottom_sheet_collapse)
    val dismissLabel = stringResource(R.string.bottom_sheet_dismiss)
    val toggle: () -> Unit = {
        scope.launch {
            when {
                !expanded -> state.expand()
                canCollapse -> state.partialExpand()
                else -> state.hide()
            }
        }
    }
    Box(
        modifier = modifier
            .focusRequester(focusRequester)
            // Focusable even when the sheet's window starts in touch mode, so the sheet can hand
            // focus to it on open for someone using a keyboard.
            .focusProperties { canFocus = true }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClickLabel = when {
                    !expanded -> expandLabel
                    canCollapse -> collapseLabel
                    else -> dismissLabel
                },
                onClick = toggle
            )
            .minimumTouchTarget()
            .semantics {
                contentDescription = label
                stateDescription = stateLabel
                customActions = listOfNotNull(
                    CustomAccessibilityAction(expandLabel) {
                        toggle()
                        true
                    }.takeIf { !expanded },
                    CustomAccessibilityAction(collapseLabel) {
                        toggle()
                        true
                    }.takeIf { expanded && canCollapse },
                    CustomAccessibilityAction(dismissLabel) {
                        scope.launch { state.hide() }
                        true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(width = HandleTargetWidth, height = HandleTargetHeight)
                .indication(interactionSource, cycleIndication(CycleTheme.shapes.full, colors.onSurfaceVariant)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(width = HandleWidth, height = HandleHeight)
                    .background(colors.onSurfaceVariant, CycleTheme.shapes.full)
                    .clearAndSetSemantics {}
            )
        }
    }
}

/**
 * Lets a drag inside the sheet's scrolling content move the sheet: dragging up expands the sheet
 * before the content scrolls, and dragging down past the content's top pulls the sheet down.
 */
private class SheetNestedScrollConnection(private val state: CycleBottomSheetState, density: Density) :
    NestedScrollConnection {
    private val velocityThreshold = with(density) { FlingVelocityThreshold.toPx() }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.y
        return if (delta < 0 && source == NestedScrollSource.UserInput) {
            Offset(0f, state.anchoredState.dispatchRawDelta(delta))
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
        if (source == NestedScrollSource.UserInput) {
            Offset(0f, state.anchoredState.dispatchRawDelta(available.y))
        } else {
            Offset.Zero
        }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val anchors = state.anchoredState.anchors
        val offset = state.anchoredState.offset
        return if (available.y < 0 && anchors.size > 0 && !offset.isNaN() && offset > anchors.minPosition()) {
            state.settle(available.y, velocityThreshold)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.settle(available.y, velocityThreshold)
        return available
    }
}

private val ZeroCorner = CornerSize(0.dp)
private val HandleWidth = 32.dp
private val HandleHeight = 4.dp
private val HandleTargetWidth = 48.dp
private val HandleTargetHeight = 24.dp
private val FlingVelocityThreshold = 125.dp
private const val BACK_SCALE = 0.9f

@Composable
private fun BottomSheetPreview(darkTheme: Boolean, value: CycleSheetValue) {
    CycleTheme(darkTheme = darkTheme) {
        Box(Modifier.fillMaxSize().background(CycleTheme.colors.surface)) {
            CycleBottomSheet(rememberCycleBottomSheetState(initialValue = value), title = "Log today") {
                BasicText(
                    "How are you feeling? Pick anything that fits.",
                    style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurfaceVariant)
                )
                FilledButton("Log it", onClick = {}, modifier = Modifier.fillMaxWidth())
                TextButton("Nah", onClick = {}, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Preview(name = "Bottom sheet · light", widthDp = 360, heightDp = 640)
@Composable
private fun BottomSheetLightPreview() = BottomSheetPreview(darkTheme = false, value = CycleSheetValue.Expanded)

@Preview(name = "Bottom sheet · dark", widthDp = 360, heightDp = 640)
@Composable
private fun BottomSheetDarkPreview() = BottomSheetPreview(darkTheme = true, value = CycleSheetValue.Expanded)
