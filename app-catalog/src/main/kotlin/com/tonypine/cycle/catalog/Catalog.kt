package com.tonypine.cycle.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.ui.CycleSurface
import com.tonypine.cycle.core.ui.CycleText

/** Every design system colour, text style and component on one scrolling page. */
@Composable
fun Catalog(modifier: Modifier = Modifier) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(CycleTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.medium)
    ) {
        CycleText("Cycle catalog", style = typography.title)

        Section("Colours") {
            ColorRow("background", colors.background)
            ColorRow("surface", colors.surface)
            ColorRow("accent", colors.accent)
            ColorRow("muted", colors.muted)
            ColorRow("outline", colors.outline)
        }

        Section("Typography") {
            CycleText("Title", style = typography.title)
            CycleText("Body", style = typography.body)
            CycleText("Label", style = typography.label)
        }

        Section("Components") {
            CycleSurface(Modifier.fillMaxWidth()) {
                CycleText("CycleSurface with CycleText", color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        CycleText(title, style = CycleTheme.typography.label, color = CycleTheme.colors.muted)
        content()
    }
}

@Composable
private fun ColorRow(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(color)
                .border(1.dp, CycleTheme.colors.outline)
        )
        CycleText(name)
    }
}
