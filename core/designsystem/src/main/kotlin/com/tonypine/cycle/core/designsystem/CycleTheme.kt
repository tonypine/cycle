package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape

val LocalCycleColors = staticCompositionLocalOf { LightCycleColors }
val LocalCycleTypography = staticCompositionLocalOf { ZestTypography }
val LocalCycleShapes = staticCompositionLocalOf { ZestShapes }
val LocalCycleSpacing = staticCompositionLocalOf { DefaultCycleSpacing }
val LocalCycleElevation = staticCompositionLocalOf { DefaultCycleElevation }
val LocalCycleMotion = staticCompositionLocalOf { ZestMotion }
val LocalCycleStateAlpha = staticCompositionLocalOf { DefaultCycleStateAlpha }

/**
 * The app's theme, built on Compose Foundation only: no Material and no dynamic colour. It provides
 * its tokens through CompositionLocals; read them with `CycleTheme.colors`, `.typography`, `.shapes`,
 * `.spacing`, `.elevation`, `.motion` and `.stateAlpha`. It also installs [CycleIndication] as
 * `LocalIndication`, so every `clickable` gets Cycle's state layer, press scale and focus ring.
 *
 * [reduceMotion] follows the system's animator duration scale; when it is on, every motion spec is
 * `snap()`.
 */
@Composable
fun CycleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = isReduceMotionEnabled(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkCycleColors else LightCycleColors
    val motion = ZestMotion.copy(reduceMotion = reduceMotion)
    CompositionLocalProvider(
        LocalCycleColors provides colors,
        LocalCycleTypography provides ZestTypography,
        LocalCycleShapes provides ZestShapes,
        LocalCycleSpacing provides DefaultCycleSpacing,
        LocalCycleElevation provides DefaultCycleElevation,
        LocalCycleMotion provides motion,
        LocalCycleStateAlpha provides DefaultCycleStateAlpha,
        LocalIndication provides CycleIndication(
            color = colors.onSurface,
            focusRingColor = colors.accent,
            shape = RectangleShape,
            stateAlpha = DefaultCycleStateAlpha,
            motion = motion
        ),
        content = content
    )
}

object CycleTheme {
    val colors: CycleColors
        @Composable @ReadOnlyComposable
        get() = LocalCycleColors.current

    val typography: CycleTypography
        @Composable @ReadOnlyComposable
        get() = LocalCycleTypography.current

    val shapes: CycleShapes
        @Composable @ReadOnlyComposable
        get() = LocalCycleShapes.current

    val spacing: CycleSpacing
        @Composable @ReadOnlyComposable
        get() = LocalCycleSpacing.current

    val elevation: CycleElevation
        @Composable @ReadOnlyComposable
        get() = LocalCycleElevation.current

    val motion: CycleMotion
        @Composable @ReadOnlyComposable
        get() = LocalCycleMotion.current

    val stateAlpha: CycleStateAlpha
        @Composable @ReadOnlyComposable
        get() = LocalCycleStateAlpha.current
}
