package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing on a 4dp grid, for padding and gaps. Read it through [CycleTheme.spacing]. */
@Immutable
data class CycleSpacing(
    /** 4dp. */
    val extraSmall: Dp,
    /** 8dp. */
    val small: Dp,
    /** 12dp. */
    val medium: Dp,
    /** 16dp: the screen margin. */
    val large: Dp,
    /** 24dp. */
    val extraLarge: Dp,
    /** 32dp. */
    val extraExtraLarge: Dp,
    /** 48dp. */
    val huge: Dp
)

val DefaultCycleSpacing = CycleSpacing(
    extraSmall = 4.dp,
    small = 8.dp,
    medium = 12.dp,
    large = 16.dp,
    extraLarge = 24.dp,
    extraExtraLarge = 32.dp,
    huge = 48.dp
)
