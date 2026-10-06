package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.Themed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The navigation bar's labels on the narrowest phone Cycle runs on, 360dp wide: English keeps
 * `labelSmall`'s 12sp, and German's "Einstellungen" shrinks the whole bar, to 11sp at the least, and
 * stays whole. The German bar is recorded at 100% and 200% in
 * `src/test/screenshots/navigation_bar_long_labels_<appearance>.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class NavigationBarLabelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun englishLabelsKeepTheirSize() {
        show(English)

        English.forEach { assertEquals(12f, label(it.label).layoutInput.style.fontSize.value, 0.01f) }
    }

    @Test
    fun germanLabelsShrinkTogetherBelow12spAndStayWhole() {
        show(German)

        val sizes = German.map { destination ->
            val label = label(destination.label)
            // Whole: no ellipsis, and as wide as its slot at most, give or take a pixel of rounding.
            val whole = !label.isLineEllipsized(0) &&
                label.multiParagraph.intrinsics.maxIntrinsicWidth <= label.layoutInput.constraints.maxWidth + 1
            assertTrue("${destination.label} is cut off", whole)
            label.layoutInput.style.fontSize.value
        }
        assertEquals(1, sizes.distinct().size)
        composeRule.onNodeWithTag(BAR).captureRoboImage("src/test/screenshots/navigation_bar_long_labels_light.png")
        assertTrue(
            "German labels at ${sizes.first()}sp",
            sizes.first() >= NavigationBarLabelFloor.value && sizes.first() < 12f
        )
    }

    @Test
    fun atALargeFontSizeLabelsShrinkNoSmallerThanTheFloorReadsAt100Percent() {
        show(German, Appearance.FontScale200)

        val size = label("Einstellungen").layoutInput.style.fontSize.value * 2f
        assertTrue("Einstellungen reads at ${size}sp", size >= NavigationBarLabelFloor.value - 0.01f && size < 12f)
        composeRule.onNodeWithTag(BAR).captureRoboImage("src/test/screenshots/navigation_bar_long_labels_font200.png")
    }

    private fun show(destinations: List<NavigationDestination>, appearance: Appearance = Appearance.Light) {
        composeRule.setContent {
            Themed(appearance) { NavigationBar(destinations, 0, onSelect = {}, modifier = Modifier.testTag(BAR)) }
        }
    }

    private fun label(text: String): TextLayoutResult =
        composeRule.onNodeWithText(text, useUnmergedTree = true).textLayout()

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private companion object {
        const val BAR = "bar"

        val English = listOf(
            NavigationDestination("Today", CycleIcons.Today),
            NavigationDestination("Calendar", CycleIcons.Calendar),
            NavigationDestination("History", CycleIcons.History),
            NavigationDestination("Settings", CycleIcons.Settings)
        )
        val German = listOf(
            NavigationDestination("Heute", CycleIcons.Today),
            NavigationDestination("Kalender", CycleIcons.Calendar),
            NavigationDestination("Verlauf", CycleIcons.History),
            NavigationDestination("Einstellungen", CycleIcons.Settings)
        )
    }
}
