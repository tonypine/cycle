package com.tonypine.cycle.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tonypine.cycle.core.designsystem.CycleTheme

/**
 * The design system catalog: a list of sections, each opening a page that shows one group of tokens
 * or one component. Add a section by adding a [CatalogSection] to [CatalogSections].
 */
@Composable
fun Catalog(modifier: Modifier = Modifier) {
    var openSection by rememberSaveable { mutableStateOf<String?>(null) }
    val section = CatalogSections.firstOrNull { it.title == openSection }
    BackHandler(enabled = section != null) { openSection = null }
    if (section == null) {
        SectionList(onOpen = { openSection = it.title }, modifier = modifier)
    } else {
        SectionPage(section, onBack = { openSection = null }, modifier = modifier)
    }
}

@Composable
fun SectionList(onOpen: (CatalogSection) -> Unit, modifier: Modifier = Modifier) {
    CatalogPage(modifier) {
        CatalogText(
            "Cycle catalog",
            CycleTheme.typography.headline,
            modifier = Modifier.semantics { heading() }
        )
        CatalogSections.forEach { section ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CycleTheme.colors.surfaceContainer, CycleTheme.shapes.large)
                    .clickable(role = Role.Button, onClickLabel = "Open") { onOpen(section) }
                    .padding(CycleTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)
            ) {
                CatalogText(section.title, CycleTheme.typography.titleSmall)
                CatalogText(
                    section.description,
                    CycleTheme.typography.bodySmall,
                    color = CycleTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionPage(section: CatalogSection, onBack: () -> Unit, modifier: Modifier = Modifier) {
    CatalogPage(modifier) {
        CatalogText(
            "Back",
            CycleTheme.typography.label,
            color = CycleTheme.colors.accent,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onBack)
                .padding(vertical = CycleTheme.spacing.medium)
        )
        CatalogText(section.title, CycleTheme.typography.headline, modifier = Modifier.semantics { heading() })
        CatalogText(section.description, CycleTheme.typography.body, color = CycleTheme.colors.onSurfaceVariant)
        section.content()
    }
}

@Composable
private fun CatalogPage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CycleTheme.colors.surface)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(CycleTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)
    ) {
        content()
    }
}
