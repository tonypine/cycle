package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Placeholder colour roles until the visual direction lands (MOT-4). Screens read them through
 * [CycleTheme.colors].
 */
@Immutable
data class CycleColors(val background: Color, val content: Color)

val LightCycleColors = CycleColors(background = Color(0xFFFFFFFF), content = Color(0xFF1C1B1F))

val DarkCycleColors = CycleColors(background = Color(0xFF1C1B1F), content = Color(0xFFF4EFF4))
