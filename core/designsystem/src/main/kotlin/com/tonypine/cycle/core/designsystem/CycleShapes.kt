package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Zest corner scale. Components pick a step rather than a radius: cards use [extraLarge], chips
 * [small], fields [medium], and pill buttons [full]. Read them through [CycleTheme.shapes].
 */
@Immutable
data class CycleShapes(
    /** 8dp. */
    val extraSmall: RoundedCornerShape,
    /** 12dp. */
    val small: RoundedCornerShape,
    /** 16dp. */
    val medium: RoundedCornerShape,
    /** 24dp. */
    val large: RoundedCornerShape,
    /** 32dp. */
    val extraLarge: RoundedCornerShape,
    /** Pills and circles. */
    val full: Shape
)

val ZestShapes = CycleShapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
    full = CircleShape
)
