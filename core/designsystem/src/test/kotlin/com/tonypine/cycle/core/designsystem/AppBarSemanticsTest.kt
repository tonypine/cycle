package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.Themed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What TalkBack and touch get from the top app bar and the navigation bar. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class AppBarSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndTheIconsAreButtons() {
        val clicks = mutableListOf<String>()
        show { CalendarBar(onClick = { clicks += it }) }
        composeRule.onNodeWithText("Calendar").assert(isHeading())
        listOf("Back", "Go to today", "Settings").forEach { description ->
            composeRule.onNodeWithContentDescription(description)
                .assert(hasRole(Role.Button))
                .assertTouchTarget()
                .performClick()
        }
        assertEquals(listOf("Back", "Go to today", "Settings"), clicks)
    }

    @Test
    fun talkBackReadsNavigationThenTitleThenActions() {
        show { CalendarBar() }
        assertEquals(listOf("Back", "Calendar", "Go to today", "Settings"), stops(hasClickAction() or isHeading()))
    }

    @Test
    fun aLongTitleEllipsizesAt200PercentAndKeepsBothActions() {
        show(Appearance.FontScale200) {
            TopAppBar(
                title = "Symptoms, moods and notes for the whole cycle",
                navigation = AppBarAction(CycleIcons.Back, "Back", {}),
                actions = listOf(
                    AppBarAction(CycleIcons.Add, "Log a day", {}),
                    AppBarAction(CycleIcons.Settings, "Settings", {})
                )
            )
        }
        val title = composeRule.onNode(isHeading())
        assertTrue("the title should end in an ellipsis", title.textLayout().isLineEllipsized(0))
        assertEquals(1, title.textLayout().lineCount)
        val screen = composeRule.onRoot().getBoundsInRoot()
        listOf("Log a day", "Settings").forEach { description ->
            val action = composeRule.onNodeWithContentDescription(description).assertIsDisplayed().assertTouchTarget()
            assertTrue("$description should stay on screen", action.getBoundsInRoot().right <= screen.right)
        }
    }

    @Test
    fun aTopAppBarHoldsAtMostTwoActions() {
        val action = AppBarAction(CycleIcons.Add, "Log a day", {})
        assertThrows(IllegalArgumentException::class.java) {
            show { TopAppBar("Calendar", actions = listOf(action, action, action)) }
        }
    }

    @Test
    fun destinationsAreTabsWithTheirSelectedStateAndPosition() {
        var selected by mutableIntStateOf(1)
        show { NavigationBar(Destinations, selected, onSelect = { selected = it }) }

        val bar = composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo))
        assertEquals(4, bar.fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].columnCount)
        Destinations.forEachIndexed { index, destination ->
            val tab = composeRule.onNodeWithText(destination.label)
                .assert(hasRole(Role.Tab))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                .assertTouchTarget()
            if (index == 1) tab.assertIsSelected() else tab.assertIsNotSelected()
            // TalkBack reads "Calendar, selected, tab, 2 of 4" from the role, state and position.
            val position = tab.fetchSemanticsNode().config[SemanticsProperties.CollectionItemInfo]
            assertEquals(
                listOf(0, 1, index, 1),
                position.let {
                    listOf(it.rowIndex, it.rowSpan, it.columnIndex, it.columnSpan)
                }
            )
        }

        composeRule.onNodeWithText("Settings").performClick().assertIsSelected()
        composeRule.onNodeWithText("Calendar").assertIsNotSelected()
        assertEquals(3, selected)
    }

    @Test
    fun talkBackReadsTheDestinationsInOrder() {
        show { NavigationBar(Destinations, 0, onSelect = {}) }
        assertEquals(Destinations.map { it.label }, stops(hasClickAction()))
    }

    @Test
    fun aNavigationBarHoldsAtLeastThreeDestinations() {
        assertThrows(IllegalArgumentException::class.java) {
            show { NavigationBar(Destinations.take(2), 0, onSelect = {}) }
        }
    }

    @Test
    fun aNavigationBarHoldsAtMostFiveDestinations() {
        assertThrows(IllegalArgumentException::class.java) {
            show { NavigationBar(Destinations + Destinations.take(2), 0, onSelect = {}) }
        }
    }

    @Composable
    private fun CalendarBar(onClick: (String) -> Unit = {}) {
        TopAppBar(
            title = "Calendar",
            navigation = AppBarAction(CycleIcons.Back, "Back", { onClick("Back") }),
            actions = listOf(
                AppBarAction(CycleIcons.Today, "Go to today", { onClick("Go to today") }),
                AppBarAction(CycleIcons.Settings, "Settings", { onClick("Settings") })
            )
        )
    }

    /** What TalkBack reads at each node [matcher] finds, in traversal order. */
    private fun stops(matcher: SemanticsMatcher): List<String?> =
        composeRule.onAllNodes(matcher).fetchSemanticsNodes().map { node ->
            (
                node.config.getOrNull(SemanticsProperties.Text)
                    ?: node.config.getOrNull(SemanticsProperties.ContentDescription)
                )
                ?.joinToString()
        }

    private fun show(appearance: Appearance = Appearance.Light, content: @Composable () -> Unit) {
        composeRule.setContent { Themed(appearance, content) }
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun SemanticsNodeInteraction.assertTouchTarget() = assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)

    private fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.single()
    }

    private companion object {
        val Destinations = listOf(
            NavigationDestination("Today", CycleIcons.Today),
            NavigationDestination("Calendar", CycleIcons.Calendar),
            NavigationDestination("Log", CycleIcons.Add),
            NavigationDestination("Settings", CycleIcons.Settings)
        )
    }
}
