package com.tonypine.cycle.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import com.tonypine.cycle.core.designsystem.BricolageGrotesque
import com.tonypine.cycle.core.designsystem.CycleTheme
import java.util.Locale

@Composable
internal fun TypographySection() {
    val type = CycleTheme.typography
    listOf(
        Triple("Display", type.display, "Day 19"),
        Triple("Headline", type.headline, "March 2027"),
        Triple("Headline emphasized", type.headlineEmphasized, "Day 19"),
        Triple("Title", type.title, "Today’s log"),
        Triple("Title emphasized", type.titleEmphasized, "Cycle day 19"),
        Triple("Title small", type.titleSmall, "Next period"),
        Triple("Body", type.body, "Next period around Mon 29 Mar."),
        Triple("Body small", type.bodySmall, "Fertile window wrapped up on 18 Mar."),
        Triple("Label", type.label, "Log it"),
        Triple("Label small", type.labelSmall, "Luteal phase"),
        Triple("Day number", type.dayNumber, "17"),
        Triple("Day number emphasized", type.dayNumberEmphasized, "20")
    ).forEach { (name, style, sample) -> TypeRole(name, style, sample) }
}

@Composable
private fun TypeRole(name: String, style: TextStyle, sample: String) {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)) {
        CatalogText(sample, style)
        CatalogText(
            "$name · ${style.describe()}",
            CycleTheme.typography.bodySmall,
            color = CycleTheme.colors.onSurfaceVariant
        )
    }
}

private fun TextStyle.describe(): String {
    val font = if (fontFamily == BricolageGrotesque) "Bricolage Grotesque" else "DM Sans"
    val size = fontSize.value.toInt()
    val line = lineHeight.value.toInt()
    val tracking = "%.1f".format(Locale.ROOT, letterSpacing.value)
    return "$font · $size / $line · ${fontWeight?.weight} · ${tracking}sp"
}
