package com.tonypine.cycle.core.designsystem

import android.view.WindowManager
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * An alert dialog: a [title], a sentence or two of [text], and a confirm action with an optional
 * dismiss action, such as "Turn on reminders?" with "Remind me" and "Not now". It sits on
 * `surfaceContainer` with 32dp corners over a `scrim` that dims the screen.
 *
 * The dialog is shown while [visible] is true. Keep it in composition and flip [visible], rather than
 * removing it, so it can animate out: the scrim fades and the dialog fades and scales on
 * [CycleTheme.motion], and both snap under reduce motion.
 *
 * - [onDismissRequest] is called on back and on a tap on the scrim, unless [dismissOnBackPress] or
 *   [dismissOnScrimTap] turn them off. Set [visible] to false in it. [onDismiss], for the dismiss
 *   button, defaults to it.
 * - [icon] sits above the title, which is then centred. It is decorative: the title says what it means.
 * - The actions sit in a row at the end, and stack (confirm on top) when they do not fit, such as
 *   at 200% font scale.
 * - The title is a heading, and TalkBack reads the title, the text, then the actions. Keyboard
 *   focus moves into the dialog when it opens and goes back to whatever had it when it closes.
 *
 * For an action that deletes something, use [CycleDestructiveDialog]; for a dialog with its own
 * content, such as a text field, use [CycleDialog].
 */
@Composable
fun CycleAlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
    icon: CycleIcons? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnScrimTap: Boolean = true
) {
    DialogWindow(visible, onDismissRequest, title, dismissOnBackPress, dismissOnScrimTap) {
        DialogSurface(
            title = title,
            modifier = modifier,
            icon = icon,
            actions = {
                if (dismissText != null) TextButton(dismissText, onDismiss)
                TextButton(confirmText, onConfirm)
            }
        ) {
            DialogText(text)
        }
    }
}

/**
 * A dialog that confirms an action that loses data, such as "Delete this day?". The danger never
 * relies on colour alone: the [icon] (the error symbol unless you pass another), the [text], which
 * says what will be lost, and the `error` confirm button all carry it. A dismiss action ("Keep it")
 * is required, so there is always a safe way out, and keyboard focus starts on it.
 *
 * Otherwise the same as [CycleAlertDialog].
 */
@Composable
fun CycleDestructiveDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = onDismissRequest,
    icon: CycleIcons = CycleIcons.Error,
    dismissOnBackPress: Boolean = true,
    dismissOnScrimTap: Boolean = true
) {
    DialogWindow(visible, onDismissRequest, title, dismissOnBackPress, dismissOnScrimTap) {
        DialogSurface(
            title = title,
            modifier = modifier,
            icon = icon,
            iconTint = CycleTheme.colors.error,
            actions = {
                TextButton(dismissText, onDismiss)
                ErrorButton(confirmText, onConfirm)
            }
        ) {
            DialogText(text)
        }
    }
}

/**
 * A dialog with its own [content] below the [title] and optional [text], such as a text field to
 * name something. The content scrolls when it does not fit, and the dialog stays above the
 * keyboard: when it opens, the dialog shrinks above it, the focused field scrolls into view and the
 * actions stay visible. Keyboard focus moves to the first focusable element of [content] when the
 * dialog opens, so a text field there takes focus and opens the keyboard.
 *
 * [confirmEnabled] turns the confirm action off, for example until a field is valid. Otherwise the
 * same as [CycleAlertDialog].
 */
@Composable
fun CycleDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    dismissText: String? = null,
    onDismiss: () -> Unit = onDismissRequest,
    icon: CycleIcons? = null,
    confirmEnabled: Boolean = true,
    dismissOnBackPress: Boolean = true,
    dismissOnScrimTap: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    DialogWindow(visible, onDismissRequest, title, dismissOnBackPress, dismissOnScrimTap) {
        DialogSurface(
            title = title,
            modifier = modifier,
            icon = icon,
            actions = {
                if (dismissText != null) TextButton(dismissText, onDismiss)
                TextButton(confirmText, onConfirm, enabled = confirmEnabled)
            }
        ) {
            text?.let { DialogText(it) }
            content()
        }
    }
}

/**
 * The window behind every dialog: Compose UI's [Dialog], edge to edge, with the platform dim off and
 * the theme's `scrim` drawn in its place. The scrim covers the system bars; the [surface] stays
 * inside the safe drawing area, so it never sits under a bar, a cut-out or the keyboard. The window
 * stays open until the exit animation ends.
 */
@Composable
private fun DialogWindow(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    dismissOnBackPress: Boolean,
    dismissOnScrimTap: Boolean,
    surface: @Composable () -> Unit
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState) return

    // The dialog's window composes with its own density and layout direction; it takes the caller's,
    // so it matches the screen that opened it, including a font scale or direction set there.
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    // A dialog on its way out ignores a second back press or scrim tap.
    val dismiss = remember(state) { { if (state.targetState) currentOnDismissRequest() } }
    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            // The scrim handles taps outside the dialog: the window fills the screen.
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            windowTitle = title
        )
    ) {
        CompositionLocalProvider(LocalDensity provides density, LocalLayoutDirection provides layoutDirection) {
            DialogContent(state, dismiss, dismissOnScrimTap, surface)
        }
    }
}

/** What fills the dialog's window: the scrim, and the [surface] that fades and scales with [state]. */
@Composable
private fun DialogContent(
    state: MutableTransitionState<Boolean>,
    dismiss: () -> Unit,
    dismissOnScrimTap: Boolean,
    surface: @Composable () -> Unit
) {
    RemoveWindowDim()
    val motion = CycleTheme.motion
    val transition = rememberTransition(state, label = "dialog")
    val alpha by transition.animateFloat(
        transitionSpec = { if (targetState) motion.defaultEffectsSpec() else motion.fastEffectsSpec() },
        label = "dialog alpha"
    ) { shown -> if (shown) 1f else 0f }
    val scale by transition.animateFloat(
        transitionSpec = { if (targetState) motion.defaultSpatialSpec() else motion.fastSpatialSpec() },
        label = "dialog scale"
    ) { shown -> if (shown) 1f else HIDDEN_SCALE }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequester) {
        // Wait for the first layout: focus enters on the first child by position.
        withFrameNanos { }
        focusRequester.requestFocus()
    }

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
                .background(CycleTheme.colors.scrim)
                .then(
                    if (dismissOnScrimTap) {
                        Modifier.pointerInput(dismiss) { detectTapGestures { dismiss() } }
                    } else {
                        Modifier
                    }
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(CycleTheme.spacing.extraLarge),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    }
                    // A tap on the dialog itself, outside its controls, never reaches the scrim.
                    .pointerInput(Unit) { detectTapGestures { } }
                    // Keyboard focus enters the dialog on its first focusable element.
                    .focusRequester(focusRequester)
                    .focusGroup()
            ) {
                surface()
            }
        }
    }
}

/** Turns off the platform's dim behind the dialog window: the dialog draws the theme's `scrim`. */
@Composable
private fun RemoveWindowDim() {
    val view = LocalView.current
    SideEffect {
        (view.parent as? DialogWindowProvider)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}

/**
 * The visible dialog: `surfaceContainer`, 32dp corners and a level 3 shadow, with the optional
 * [icon], the [title] as a heading, the scrolling [content] and the [actions].
 */
@Composable
private fun DialogSurface(
    title: String,
    actions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: CycleIcons? = null,
    iconTint: Color = CycleTheme.colors.accent,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    val shape = CycleTheme.shapes.extraLarge
    Column(
        modifier
            .widthIn(min = MIN_WIDTH, max = MAX_WIDTH)
            .shadow(CycleTheme.elevation.level3, shape)
            .background(colors.surfaceContainer, shape)
            .padding(spacing.extraLarge)
    ) {
        if (icon != null) {
            CycleIcon(
                icon,
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterHorizontally),
                tint = iconTint
            )
            Spacer(Modifier.height(spacing.large))
        }
        BasicText(
            title,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { heading() },
            style = CycleTheme.typography.title.copy(
                color = colors.onSurface,
                textAlign = if (icon != null) TextAlign.Center else TextAlign.Start
            )
        )
        Spacer(Modifier.height(spacing.large))
        Column(
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            content()
        }
        Spacer(Modifier.height(spacing.extraLarge))
        DialogActions(Modifier.fillMaxWidth(), actions)
    }
}

@Composable
private fun DialogText(text: String) {
    BasicText(text, style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurfaceVariant))
}

/**
 * Lays the actions out in a row at the end, the confirm action last. When the row does not fit, it
 * stacks them at the end instead, the confirm action on top.
 */
@Composable
private fun DialogActions(modifier: Modifier, content: @Composable () -> Unit) {
    val gap = CycleTheme.spacing.small
    Layout(content, modifier) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val gaps = gapPx * (measurables.size - 1).coerceAtLeast(0)
        val rowWidth = measurables.sumOf { it.maxIntrinsicWidth(Constraints.Infinity) } + gaps
        val stacked = constraints.hasBoundedWidth && rowWidth > constraints.maxWidth
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else rowWidth
        // Placement order is also TalkBack's order among the actions, so place them as they appear.
        if (stacked) {
            val height = placeables.sumOf { it.height } + gaps
            layout(width, height) {
                var y = 0
                placeables.asReversed().forEach {
                    it.placeRelative(width - it.width, y)
                    y += it.height + gapPx
                }
            }
        } else {
            val height = placeables.maxOfOrNull { it.height } ?: 0
            layout(width, height) {
                var x = width - placeables.sumOf { it.width } - gaps
                placeables.forEach {
                    it.placeRelative(x, (height - it.height) / 2)
                    x += it.width + gapPx
                }
            }
        }
    }
}

/** The narrowest and widest a dialog gets (Material's basic dialog). */
private val MIN_WIDTH = 280.dp
private val MAX_WIDTH = 560.dp

/** The dialog scales from this size as it appears, and back to it as it leaves. */
private const val HIDDEN_SCALE = 0.9f

@Composable
private fun DialogPreviews() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        DialogSurface(
            title = "Turn on reminders?",
            icon = CycleIcons.Today,
            actions = {
                TextButton("Not now", {})
                TextButton("Remind me", {})
            }
        ) {
            DialogText("Cycle can nudge you a day before your period is due. Reminders stay on this phone.")
        }
        DialogSurface(
            title = "Delete this day?",
            icon = CycleIcons.Error,
            iconTint = CycleTheme.colors.error,
            actions = {
                TextButton("Keep it", {})
                ErrorButton("Delete", {})
            }
        ) {
            DialogText("This removes the period and notes logged for 14 March. You can't undo it.")
        }
        DialogSurface(
            title = "Add a note",
            actions = {
                TextButton("Cancel", {})
                TextButton("Save", {})
            }
        ) {
            DialogText("A few words about the day.")
            CycleTextField(rememberTextFieldState(), label = "Note", supportingText = "Only on this phone.")
        }
    }
}

/** The dialogs on the scrim, without their window: a preview cannot open one. */
@Composable
private fun DialogPreviewFrame(darkTheme: Boolean) = CycleTheme(darkTheme = darkTheme) {
    Box(Modifier.background(CycleTheme.colors.surface)) {
        Box(Modifier.background(CycleTheme.colors.scrim).padding(CycleTheme.spacing.extraLarge)) { DialogPreviews() }
    }
}

@Preview(name = "Dialogs · light", widthDp = 360)
@Composable
private fun DialogsLightPreview() = DialogPreviewFrame(darkTheme = false)

@Preview(name = "Dialogs · dark", widthDp = 360)
@Composable
private fun DialogsDarkPreview() = DialogPreviewFrame(darkTheme = true)
