package com.tonypine.cycle.catalog

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.tonypine.cycle.core.designsystem.CycleTheme

/** One entry in the catalog: a page for a group of tokens or for one component. */
class CatalogSection(val title: String, val description: String, val content: @Composable () -> Unit)

/** Every catalog section, in the order the list shows them. Later components add one entry each. */
val CatalogSections: List<CatalogSection> = listOf(
    CatalogSection("Colours", "Every colour role in light and dark, and the contrast of each pair.") {
        ColorsSection()
    },
    CatalogSection("Typography", "Every type role with its font, size, line height, weight and tracking.") {
        TypographySection()
    },
    CatalogSection("Shapes", "The corner scale, from extra small to full.") { ShapesSection() },
    CatalogSection("Spacing", "The 4dp spacing grid, and the elevation scale.") { SpacingSection() },
    CatalogSection("Motion", "The spatial and effects springs, a spring demo, a shape morph and reduce motion.") {
        MotionSection()
    },
    CatalogSection("Indication", "The state layer, press scale and focus ring in every state.") {
        IndicationSection()
    },
    CatalogSection("Icons", "Every Material Symbols Rounded icon the app ships.") { IconsSection() },
    CatalogSection("Buttons", "Filled, tonal, outlined and text buttons in every state.") { ButtonsSection() },
    CatalogSection("Icon buttons", "Standard, filled, tonal and toggle icon buttons in every state.") {
        IconButtonsSection()
    },
    CatalogSection("Chips", "Filter and assist chips in every state, and selection.") { ChipsSection() },
    CatalogSection("Text field", "The outlined text field: a form to type in, and every state.") {
        TextFieldSection()
    }
)

@Composable
internal fun CatalogText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = CycleTheme.colors.onSurface
) {
    BasicText(text, modifier = modifier, style = style.copy(color = color))
}

@Composable
internal fun SubsectionTitle(text: String) {
    CatalogText(text, CycleTheme.typography.title, modifier = Modifier.semantics { heading() })
}
