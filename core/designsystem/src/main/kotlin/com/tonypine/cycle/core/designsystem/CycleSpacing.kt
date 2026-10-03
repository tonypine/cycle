package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The spacing scale. Screens read it through [CycleTheme.spacing]. */
@Immutable
data class CycleSpacing(val small: Dp = 8.dp, val medium: Dp = 16.dp, val large: Dp = 24.dp)
