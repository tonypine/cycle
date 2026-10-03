package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The colour roles the app draws with. Screens read them through [CycleTheme.colors]. */
@Immutable
data class CycleColors(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val accent: Color,
    val onAccent: Color,
    val muted: Color,
    val outline: Color,
    val isDark: Boolean
)

val LightCycleColors = CycleColors(
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF231917),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF231917),
    accent = Color(0xFFB4385A),
    onAccent = Color(0xFFFFFFFF),
    muted = Color(0xFF6F5B5E),
    outline = Color(0xFFD8C2C5),
    isDark = false
)

val DarkCycleColors = CycleColors(
    background = Color(0xFF1A1113),
    onBackground = Color(0xFFF1DEE0),
    surface = Color(0xFF261A1D),
    onSurface = Color(0xFFF1DEE0),
    accent = Color(0xFFFFB1C3),
    onAccent = Color(0xFF65002B),
    muted = Color(0xFFD5C2C5),
    outline = Color(0xFF524346),
    isDark = true
)
