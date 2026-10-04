package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Zest colour roles (`docs/design/visual-directions.md`). The palette belongs to the app: there is
 * no dynamic colour. Screens read the roles through [CycleTheme.colors].
 */
@Immutable
data class CycleColors(
    /** Primary actions, links, selection. */
    val accent: Color,
    /** Text and icons on [accent]. */
    val onAccent: Color,
    /** Tonal buttons, selected chips, navigation indicator. */
    val accentContainer: Color,
    /** Text on [accentContainer]. */
    val onAccentContainer: Color,
    /** Screen background. */
    val surface: Color,
    /** Cards, navigation bar. */
    val surfaceContainer: Color,
    /** Raised or pressed surfaces. */
    val surfaceContainerHigh: Color,
    /** Body text and icons. */
    val onSurface: Color,
    /** Secondary text, captions. */
    val onSurfaceVariant: Color,
    /** Control borders (chips, fields). */
    val outline: Color,
    /** Dividers, decorative borders. */
    val outlineVariant: Color,
    /** Dims the screen behind a dialog. Translucent; nothing is drawn on it directly. */
    val scrim: Color,
    /** Error text and borders. Always paired with an icon and a sentence, never colour alone. */
    val error: Color,
    /** Text on [error]. */
    val onError: Color,
    /** Error banners. */
    val errorContainer: Color,
    /** Text on [errorContainer]. */
    val onErrorContainer: Color,
    /** Logged period day (fill). */
    val period: Color,
    /** Day number on [period]. */
    val onPeriod: Color,
    /** Predicted period day (fill). Its day number uses [onSurface]. */
    val predicted: Color,
    /** Predicted period dashed edge. */
    val predictedEdge: Color,
    /** Fertile window (tint). */
    val fertile: Color,
    /** Day number in the fertile window. */
    val onFertile: Color,
    /** Ovulation day (fill) and the fertile marker dot. */
    val ovulation: Color,
    /** Day number on [ovulation]. */
    val onOvulation: Color,
    /** Today ring. */
    val today: Color
)

val LightCycleColors = CycleColors(
    accent = Color(0xFF6A2BD6),
    onAccent = Color(0xFFFFFFFF),
    accentContainer = Color(0xFFEBDDFF),
    onAccentContainer = Color(0xFF25005A),
    surface = Color(0xFFFFFCF4),
    surfaceContainer = Color(0xFFFFF2D9),
    surfaceContainerHigh = Color(0xFFFCE6BE),
    onSurface = Color(0xFF1F1A10),
    onSurfaceVariant = Color(0xFF584C33),
    outline = Color(0xFF827455),
    outlineVariant = Color(0xFFE1D0AA),
    scrim = Color(0x661F1A10),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    period = Color(0xFFD1342A),
    onPeriod = Color(0xFFFFFFFF),
    predicted = Color(0xFFFFDDD6),
    predictedEdge = Color(0xFFD1342A),
    fertile = Color(0xFFC4F0D6),
    onFertile = Color(0xFF00391F),
    ovulation = Color(0xFFB35A00),
    onOvulation = Color(0xFFFFFFFF),
    today = Color(0xFF1F1A10)
)

val DarkCycleColors = CycleColors(
    accent = Color(0xFFD3BBFF),
    onAccent = Color(0xFF3F008D),
    accentContainer = Color(0xFF5418BE),
    onAccentContainer = Color(0xFFEBDDFF),
    surface = Color(0xFF17130C),
    surfaceContainer = Color(0xFF221C12),
    surfaceContainerHigh = Color(0xFF2E271A),
    onSurface = Color(0xFFF2E8D5),
    onSurfaceVariant = Color(0xFFD2C4A5),
    outline = Color(0xFF9B8D6E),
    outlineVariant = Color(0xFF4D4430),
    scrim = Color(0x99000000),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    period = Color(0xFFFF8B7B),
    onPeriod = Color(0xFF410200),
    predicted = Color(0xFF4A1E18),
    predictedEdge = Color(0xFFFF8B7B),
    fertile = Color(0xFF1D4A32),
    onFertile = Color(0xFFB4F2CE),
    ovulation = Color(0xFFFFC24A),
    onOvulation = Color(0xFF2B1F00),
    today = Color(0xFFF2E8D5)
)
