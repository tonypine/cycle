package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Placeholder text styles until the visual direction lands. Read them through [CycleTheme.typography]. */
@Immutable
data class CycleTypography(val title: TextStyle, val body: TextStyle)

val DefaultCycleTypography = CycleTypography(
    title = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
)
