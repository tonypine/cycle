package com.tonypine.cycle.feature.history

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What TalkBack reads on History and the cycle detail, and where each tap goes. Synthetic data ([HistorySamples]). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class HistoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun showHistory(state: HistoryUiState) {
        composeRule.setContent { Themed { HistoryScreen(state, onCycleClick = { calls += "cycle $it" }) } }
        composeRule.waitForIdle()
    }

    private fun showDetail(state: CycleDetailUiState) {
        composeRule.setContent {
            Themed {
                CycleDetailScreen(state, onBack = { calls += "back" }, onSeeInCalendar = { calls += "calendar $it" })
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `before her first complete cycle, History says so`() {
        showHistory(HistorySamples.firstCycle)
        composeRule.onNodeWithText("Your first cycle is still going").assertIsDisplayed()
    }

    @Test
    fun `with nothing logged, History points to Today`() {
        showHistory(HistorySamples.empty)
        composeRule.onNodeWithText("No cycles yet").assertIsDisplayed()
        composeRule.onNodeWithText("Log a period on Today", substring = true).assertIsDisplayed()
    }

    @Test
    fun `her typical cycle reads each length with its shortest and longest`() {
        showHistory(HistorySamples.cycles)

        composeRule.onNodeWithText("Your typical cycle").assert(isHeading())
        composeRule.onNode(hasText("Cycle length") and hasText("28 days"))
            .assert(hasText("Shortest to longest: 26 to 31 days"))
        composeRule.onNode(hasText("Period length") and hasText("5 days"))
            .assert(hasText("Shortest to longest: 4 to 6 days"))
        composeRule.onNodeWithText("The middle length of your last 6 cycles").assertIsDisplayed()
        composeRule.onNodeWithText("Cycles").assert(isHeading())
    }

    @Test
    fun `TalkBack reads each cycle card as one button`() {
        showHistory(HistorySamples.cycles)
        val list = composeRule.onNode(hasScrollAction())

        val cards = listOf(
            listOf("Current cycle", "Since Sep 2", "Day 9 so far", "Period: 4 days"),
            listOf("Aug 5 to Sep 1", "28 days", "Period: 6 days"),
            listOf("Jul 9 to Aug 4", "27 days", "Period: 5 days"),
            listOf("Jun 11 to Jul 8", "28 days", "Period: 5 days"),
            listOf("May 11 to Jun 10", "31 days", "Period: 6 days"),
            listOf("Apr 15 to May 10", "26 days", "Period: 4 days"),
            listOf("Mar 17 to Apr 14", "29 days", "Period: 5 days")
        )
        cards.forEach { lines ->
            val card = lines.drop(1).fold(hasText(lines.first())) { matcher, line -> matcher and hasText(line) }
            list.performScrollToNode(card)
            // One node holds every line, so TalkBack reads them together, then "button".
            composeRule.onNode(card).assert(isButton).assert(hasClickAction())
            composeRule.onAllNodesWithText(lines.first()).assertCountEquals(1)
        }
    }

    @Test
    fun `a cycle card opens that cycle`() {
        showHistory(HistorySamples.cycles)
        composeRule.onNodeWithText("Aug 5 to Sep 1", substring = true).performClick()
        composeRule.onNodeWithText("Since Sep 2", substring = true).performClick()

        assertEquals(listOf("cycle 2027-08-05", "cycle 2027-09-02"), calls)
    }

    @Test
    fun `the detail has the cycle's dates, its period and the flow of each day`() {
        showDetail(HistorySamples.pastCycle)

        composeRule.onNode(hasText("28 days") and hasText("August 5 to September 1")).assertIsDisplayed()
        composeRule.onNodeWithText("28 days", substring = true).assert(isHeading())
        composeRule.onNode(hasText("August 5 to August 10") and hasText("Period: 6 days")).assertIsDisplayed()
        // Each period day reads its date and flow as one item.
        listOf("Medium", "Heavy", "Heavy", "Medium", "Not logged", "Light").forEachIndexed { index, flow ->
            composeRule.onNode(
                hasText(flow) and hasContentDescription("August ${5 + index},", substring = true)
            ).assertExists()
        }
    }

    @Test
    fun `how she felt and her notes read as one item each, under their headings`() {
        showDetail(HistorySamples.pastCycle)

        composeRule.onNodeWithText("How you felt").performScrollTo().assert(isHeading())
        // TalkBack reads each symptom with its days, a range as "24 to 27".
        listOf(
            "Cramps, mild, Day 3",
            "Cramps, moderate, Days 1, 2",
            "Lower back, moderate, Day 1",
            "Bloating, Days 24 to 27",
            "Low energy, Day 1",
            "Protected sex, Day 14"
        ).forEach { composeRule.onNodeWithContentDescription(it).performScrollTo().assertIsDisplayed() }
        composeRule.onNodeWithText("Notes").performScrollTo().assert(isHeading())
        composeRule.onNode(hasText("Day 1") and hasText("Sample note: a heat pad helped.")).performScrollTo()
            .assertIsDisplayed()
        composeRule.onNode(hasText("Day 3") and hasText("Sample note: a short walk after lunch.")).assertExists()
    }

    @Test
    fun `a cycle with nothing logged about how she felt shows neither card`() {
        showDetail(HistorySamples.currentCycle)

        composeRule.onAllNodesWithText("How you felt").assertCountEquals(0)
        composeRule.onAllNodesWithText("Notes").assertCountEquals(0)
    }

    @Test
    fun `see it in the calendar opens the month the cycle started in, and back goes back`() {
        showDetail(HistorySamples.pastCycle)
        composeRule.onNodeWithText("See it in the calendar").performScrollTo().assert(isButton).performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("calendar ${YearMonth.of(2027, 8)}", "back"), calls)
    }

    @Test
    fun `the current cycle counts its days so far`() {
        showDetail(HistorySamples.currentCycle)
        composeRule.onNodeWithText("Current cycle").assertIsDisplayed()
        composeRule.onNode(hasText("Day 9 so far") and hasText("Since September 2")).assertIsDisplayed()
    }

    @Test
    fun `a cycle edited away says so`() {
        showDetail(HistorySamples.missing)
        composeRule.onNodeWithText("This cycle has changed").assertIsDisplayed()
    }
}
