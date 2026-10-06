package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.Themed
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h640dp-mdpi")
class CycleLegendTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val labels =
        listOf("Period", "Predicted period", "Estimated fertile window", "Estimated ovulation", "Today")

    @Test
    fun readsAsAListOfEveryState() {
        show(Appearance.Light)
        composeRule.onNodeWithTag(LEGEND).assert(
            SemanticsMatcher("a list of ${labels.size}") {
                val info = it.config.getOrNull(SemanticsProperties.CollectionInfo)
                info?.rowCount == labels.size && info.columnCount == 1
            }
        )
        labels.forEachIndexed { index, label ->
            composeRule.onNodeWithText(label).assert(
                SemanticsMatcher("item $index of the list") {
                    it.config.getOrNull(SemanticsProperties.CollectionItemInfo)?.rowIndex == index
                }
            )
        }
    }

    @Test
    fun showsOnlyTheChosenEntriesAndReadsAsAListOfThem() {
        show(Appearance.Light, CycleLegendEntry.WithoutFertility)
        val chosen = listOf("Period", "Predicted period", "Today")
        composeRule.onNodeWithTag(LEGEND).assert(
            SemanticsMatcher("a list of ${chosen.size}") {
                val info = it.config.getOrNull(SemanticsProperties.CollectionInfo)
                info?.rowCount == chosen.size && info.columnCount == 1
            }
        )
        chosen.forEachIndexed { index, label ->
            composeRule.onNodeWithText(label).assert(
                SemanticsMatcher("item $index of the list") {
                    it.config.getOrNull(SemanticsProperties.CollectionItemInfo)?.rowIndex == index
                }
            )
        }
        composeRule.onNodeWithText("Estimated fertile window").assertDoesNotExist()
        composeRule.onNodeWithText("Estimated ovulation").assertDoesNotExist()
    }

    @Test
    fun namesTheDaysInTheMethodsWords() {
        show(Appearance.Light, CycleLegendEntry.WithoutFertility, listOf(BleedingWords.Bleed))
        listOf("Bleed", "Expected bleed", "Today").forEach { composeRule.onNodeWithText(it).assertExists() }
        composeRule.onNodeWithText("Period").assertDoesNotExist()
        composeRule.onNodeWithText("Predicted period").assertDoesNotExist()
    }

    @Test
    fun showsOneLoggedEntryPerWordOnScreen() {
        show(
            Appearance.Light,
            listOf(CycleLegendEntry.Period, CycleLegendEntry.Today),
            listOf(BleedingWords.Period, BleedingWords.Bleeding)
        )
        val chosen = listOf("Period", "Bleeding", "Today")
        composeRule.onNodeWithTag(LEGEND).assert(
            SemanticsMatcher("a list of ${chosen.size}") {
                it.config.getOrNull(SemanticsProperties.CollectionInfo)?.rowCount == chosen.size
            }
        )
        chosen.forEachIndexed { index, label ->
            composeRule.onNodeWithText(label).assert(
                SemanticsMatcher("item $index of the list") {
                    it.config.getOrNull(SemanticsProperties.CollectionItemInfo)?.rowIndex == index
                }
            )
        }
    }

    @Test
    fun wrapsAtDoubleFontScaleWithoutClipping() {
        show(Appearance.FontScale200)
        val legend = composeRule.onNodeWithTag(LEGEND).getBoundsInRoot()
        val items = labels.map { composeRule.onNodeWithText(it).getBoundsInRoot() }
        val lines = items.map { it.top }.distinct()
        assertTrue("Expected the legend to wrap, but its items sit on ${lines.size} line(s)", lines.size > 1)
        items.forEachIndexed { index, item ->
            assertTrue(
                "${labels[index]} at $item is outside the legend at $legend",
                item.left >= legend.left && item.right <= legend.right
            )
        }
    }

    private fun show(
        appearance: Appearance,
        entries: List<CycleLegendEntry> = CycleLegendEntry.entries,
        words: List<BleedingWords> = listOf(BleedingWords.Period)
    ) {
        composeRule.setContent {
            Themed(appearance) {
                Box(Modifier.padding(CycleTheme.spacing.large)) {
                    CycleLegend(Modifier.testTag(LEGEND), entries, words)
                }
            }
        }
    }

    private companion object {
        const val LEGEND = "legend"
    }
}
