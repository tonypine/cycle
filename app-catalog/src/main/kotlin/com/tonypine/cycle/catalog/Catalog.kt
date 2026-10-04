package com.tonypine.cycle.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.CycleTheme

/** Every design system token and component on one scrolling page. For now, the placeholder theme. */
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CatalogText("Cycle catalog", typography.title)

        Section("Colours") {
            ColorRow("background", colors.background)
            ColorRow("content", colors.content)
        }

        Section("Typography") {
            CatalogText("Title", typography.title)
            CatalogText("Body", typography.body)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CatalogText(title, CycleTheme.typography.body)
        content()
    }
}

@Composable
private fun ColorRow(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(color)
                .border(1.dp, CycleTheme.colors.content)
        )
        CatalogText(name, CycleTheme.typography.body)
    }
}

@Composable
private fun CatalogText(text: String, style: TextStyle) {
    BasicText(text, style = style.copy(color = CycleTheme.colors.content))
}
