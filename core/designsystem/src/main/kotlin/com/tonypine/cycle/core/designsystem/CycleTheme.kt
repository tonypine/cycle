package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.booleanResource

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
 * `LocalIndication`, so every `clickable` gets Cycle's state layer, press scale and focus ring, and
 * colours text selection handles and highlights in `accent`.
 *
 * [reduceMotion] follows the system's animator duration scale; when it is on, every motion spec is
 * `snap()`. In a language whose long words need it (`cycle_hyphenates`, German), the type roles
 * hyphenate where a line breaks.
 */
@Composable
fun CycleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = isReduceMotionEnabled(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkCycleColors else LightCycleColors
    val motion = ZestMotion.copy(reduceMotion = reduceMotion)
    val hyphenates = booleanResource(R.bool.cycle_hyphenates)
    val typography = remember(hyphenates) { if (hyphenates) ZestTypography.hyphenated() else ZestTypography }
    CompositionLocalProvider(
        LocalCycleColors provides colors,
        LocalCycleTypography provides typography,
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
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = colors.accent,
            backgroundColor = colors.accent.copy(alpha = DefaultCycleStateAlpha.selection)
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
