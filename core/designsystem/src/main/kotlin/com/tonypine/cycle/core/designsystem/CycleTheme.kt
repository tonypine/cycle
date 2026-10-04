package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalCycleColors = staticCompositionLocalOf { LightCycleColors }
val LocalCycleTypography = staticCompositionLocalOf { ZestTypography }
val LocalCycleShapes = staticCompositionLocalOf { ZestShapes }
val LocalCycleSpacing = staticCompositionLocalOf { DefaultCycleSpacing }
val LocalCycleElevation = staticCompositionLocalOf { DefaultCycleElevation }

/**
 * The app's theme, built on Compose Foundation only: no Material and no dynamic colour. It provides
 * its tokens through CompositionLocals; read them with `CycleTheme.colors`, `.typography`, `.shapes`,
 * `.spacing` and `.elevation`.
 */
@Composable
fun CycleTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCycleColors provides if (darkTheme) DarkCycleColors else LightCycleColors,
        LocalCycleTypography provides ZestTypography,
        LocalCycleShapes provides ZestShapes,
        LocalCycleSpacing provides DefaultCycleSpacing,
        LocalCycleElevation provides DefaultCycleElevation,
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
}
