package com.tonypine.cycle.core.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's icons: Material Symbols Rounded at weight 600, filled, grade 0 and optical size 24, as
 * vector drawables in `res/drawable/ic_symbol_*.xml` (Apache 2.0, see [OpenSourceNotices]). Draw them
 * with [CycleIcon]. Icons that point along the reading direction mirror in right-to-left layouts.
 * To add one, see "Icons" in `docs/design/design-system.md`.
 */
enum class CycleIcons(@param:DrawableRes val drawable: Int, val symbol: String) {
    /** Back navigation. Mirrors in RTL. */
    Back(R.drawable.ic_symbol_arrow_back, "arrow_back"),
    Close(R.drawable.ic_symbol_close, "close"),
    Add(R.drawable.ic_symbol_add, "add"),
    Check(R.drawable.ic_symbol_check, "check"),

    /** Always next to an error sentence, never on its own. */
    Error(R.drawable.ic_symbol_error, "error"),
    Calendar(R.drawable.ic_symbol_calendar_month, "calendar_month"),
    Today(R.drawable.ic_symbol_today, "today"),
    Settings(R.drawable.ic_symbol_settings, "settings"),

    /** Points to the start: left in LTR, right in RTL. Previous month. */
    ChevronStart(R.drawable.ic_symbol_chevron_left, "chevron_left"),

    /** Points to the end: right in LTR, left in RTL. Next month, or a row that opens a page. */
    ChevronEnd(R.drawable.ic_symbol_chevron_right, "chevron_right"),
    ChevronUp(R.drawable.ic_symbol_expand_less, "expand_less"),
    ChevronDown(R.drawable.ic_symbol_expand_more, "expand_more")
}

/**
 * Draws [icon] at [size] in [tint]. Give a [contentDescription] when the icon carries meaning on its
 * own; pass null when a label next to it already says the same, so TalkBack does not read it twice.
 * An icon alone is not a touch target: put it in a 48dp clickable.
 */
@Composable
fun CycleIcon(
    icon: CycleIcons,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = CycleTheme.colors.onSurface,
    size: Dp = 24.dp
) {
    Image(
        painter = painterResource(icon.drawable),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint)
    )
}
