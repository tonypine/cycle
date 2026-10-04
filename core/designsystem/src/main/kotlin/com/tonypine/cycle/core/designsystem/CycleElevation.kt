package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shadow elevation. Zest is flat: cards and bars sit on tonal surfaces (`surfaceContainer`,
 * `surfaceContainerHigh`) instead of shadows, so only things that float above the page cast one.
 * Read it through [CycleTheme.elevation].
 */
@Immutable
data class CycleElevation(
    /** 0dp: everything that sits on the page, including cards. */
    val level0: Dp,
    /** 1dp: a floating navigation bar. */
    val level1: Dp,
    /** 3dp: bottom sheets and menus. */
    val level2: Dp,
    /** 6dp: dialogs. */
    val level3: Dp
)

val DefaultCycleElevation = CycleElevation(level0 = 0.dp, level1 = 1.dp, level2 = 3.dp, level3 = 6.dp)
