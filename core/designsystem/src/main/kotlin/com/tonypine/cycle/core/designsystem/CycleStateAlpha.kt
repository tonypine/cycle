package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Immutable

/**
 * Opacities for interaction states. The state layer is the content colour (`onSurface`,
 * `onAccent`...) at [hovered], [focused] or [pressed] over the component. A disabled component
 * draws its container in `onSurface` at [disabledContainer] and its content in `onSurface` at
 * [disabledContent]. Read it through [CycleTheme.stateAlpha].
 */
@Immutable
data class CycleStateAlpha(
    /** 8%: a pointer over the component. */
    val hovered: Float,
    /** 10%: keyboard or D-pad focus, shown with the focus ring. */
    val focused: Float,
    /** 10%: a finger or button down. */
    val pressed: Float,
    /** 12%: the container of a disabled component. */
    val disabledContainer: Float,
    /** 45%: the label and icon of a disabled component. */
    val disabledContent: Float
)

/**
 * The state layer opacities follow Material 3; the disabled ones are the Zest board's
 * (`directions/zest.html`, `.btn:disabled`).
 */
val DefaultCycleStateAlpha = CycleStateAlpha(
    hovered = 0.08f,
    focused = 0.10f,
    pressed = 0.10f,
    disabledContainer = 0.12f,
    disabledContent = 0.45f
)
