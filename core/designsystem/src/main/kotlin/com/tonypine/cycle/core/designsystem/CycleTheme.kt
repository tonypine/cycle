package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalCycleColors = staticCompositionLocalOf { LightCycleColors }
val LocalCycleTypography = staticCompositionLocalOf { DefaultCycleTypography }

/**
 * The app's theme, built on Compose Foundation only: no Material and no dynamic colour. It provides
 * its tokens through CompositionLocals; read them with `CycleTheme.colors` and `CycleTheme.typography`.
 */
@Composable
fun CycleTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalCycleColors provides if (darkTheme) DarkCycleColors else LightCycleColors,
        LocalCycleTypography provides DefaultCycleTypography,
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
}
