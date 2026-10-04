package com.tonypine.cycle.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.tonypine.cycle.core.designsystem.CycleIcon
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme

@Composable
internal fun IconsSection() {
    CatalogText(
        "Material Symbols Rounded, weight 600, filled, grade 0, optical size 24. Back and the start and " +
            "end chevrons mirror in right-to-left layouts.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    CycleIcons.entries.forEach { icon ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)
        ) {
            CycleIcon(icon, contentDescription = null)
            Column {
                CatalogText("CycleIcons.${icon.name}", CycleTheme.typography.titleSmall)
                CatalogText(icon.symbol, CycleTheme.typography.bodySmall, color = CycleTheme.colors.onSurfaceVariant)
            }
        }
    }
}
