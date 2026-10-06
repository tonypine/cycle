package com.tonypine.cycle.feature.history

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
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
import com.tonypine.cycle.core.model.BleedingSummary
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import java.time.LocalDate
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
        composeRule.setContent {
            Themed {
                HistoryScreen(
                    state,
                    onCycleClick = { calls += "cycle $it" },
                    onSeeInCalendar = { calls += "calendar $it" }
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun showEditor(state: EditPeriodUiState) {
        composeRule.setContent {
            Themed {
                EditPeriodScreen(
                    state,
                    onBack = { calls += "back" },
                    onChoose = { calls += "choose $it" },
                    onPick = { calls += "pick $it" },
                    onMonthChange = { calls += "month $it" },
                    onStillGoingChange = { calls += "still going $it" },
                    onSave = { calls += "save" }
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun showDetail(state: CycleDetailUiState) {
        composeRule.setContent {
            Themed {
                CycleDetailScreen(
                    state,
                    onBack = { calls += "back" },
                    onSeeInCalendar = { calls += "calendar $it" },
                    onEditPeriod = { calls += "edit $it" },
                    onDeletePeriod = { calls += "delete" }
                )
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
    fun `on a method, the typical cycle says time on it is left out`() {
        showHistory(HistorySamples.onImplant)

        composeRule.onNodeWithText(
            "The middle length of your last 6 cycles. Time on hormonal contraception is left out."
        ).assertIsDisplayed()
    }

    @Test
    fun `TalkBack reads the method's card as one item, the method first, then its calendar button`() {
        showHistory(HistorySamples.onImplant)

        val card = hasText("Implant") and hasText("Since Nov 9, 2026") and
            hasText("Not part of your typical cycle.") and
            hasText(
                "Last 90 days: you logged bleeding or spotting on 12 days, in 4 episodes. The longest lasted 6 days."
            )
        composeRule.onNode(card).assertIsDisplayed().assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
        composeRule.onNodeWithText("See it in the calendar").assert(isButton).performClick()

        assertEquals(listOf("calendar 2027-10"), calls)
    }

    @Test
    fun `TalkBack reads the method that cut a cycle short with the cycle, as one button`() {
        showHistory(HistorySamples.onImplant)
        val card = hasText("Oct 8, 2026 to Nov 8, 2026") and
            hasText("32 days, cut short when the implant was fitted. Not counted.")
        composeRule.onNode(hasScrollAction()).performScrollToNode(card)

        composeRule.onNode(card).assert(isButton).performClick()

        assertEquals(listOf("cycle 2026-10-08"), calls)
    }

    @Test
    fun `once the method stopped, its card reads its dates and its last 90 days`() {
        showHistory(HistorySamples.implantRemoved)

        composeRule.onNode(
            hasText("Nov 9, 2026 to Nov 3, 2027") and hasText(
                "In its last 90 days you logged bleeding or spotting on 10 days, in 3 episodes. " +
                    "The longest lasted 5 days."
            )
        ).assertIsDisplayed()
        composeRule.onNodeWithText("See it in the calendar").performClick()

        assertEquals(listOf("calendar 2027-11"), calls)
    }

    @Test
    fun `with no cycle of her own, History starts with the method's card`() {
        showHistory(HistorySamples.methodOnly)

        composeRule.onAllNodesWithText("Your typical cycle").assertCountEquals(0)
        composeRule.onNodeWithText("Cycles").assert(isHeading())
        composeRule.onNode(hasText("Implant") and hasText("Start not known")).assertIsDisplayed()
    }

    @Test
    fun `each method's card counts what fits it`() {
        val today = LocalDate.of(2027, 10, 10)
        val pill = ContraceptionStretch(
            ContraceptionMethod.COMBINED_PILL,
            started = LocalDate.of(2027, 6, 1),
            stopped = LocalDate.of(2027, 8, 31),
            breaks = Breaks.MONTHLY,
            id = 1
        )
        val ring = pill.copy(method = ContraceptionMethod.RING, id = 2)
        val injection = ContraceptionStretch(ContraceptionMethod.INJECTION, started = LocalDate.of(2027, 9, 6), id = 3)
        val iud = ContraceptionStretch(
            ContraceptionMethod.HORMONAL_IUD,
            started = LocalDate.of(2027, 1, 4),
            stopped = LocalDate.of(2027, 2, 20),
            id = 4
        )
        fun summary(from: LocalDate, to: LocalDate, days: Int) = MethodBleeding.Days(
            BleedingSummary(from, to, sinceStart = true, days = days, episodes = days.coerceAtMost(1), longest = days)
        )
        showHistory(
            HistoryUiState.Cycles(
                today = today,
                typicalCycle = null,
                typicalPeriod = null,
                entries = listOf(
                    MethodSummary(injection, isCurrent = true, today, summary(injection.started!!, today, days = 0)),
                    MethodSummary(pill, isCurrent = false, pill.stopKey, MethodBleeding.Bleeds(3)),
                    MethodSummary(ring, isCurrent = false, ring.stopKey, MethodBleeding.Bleeds(0)),
                    MethodSummary(iud, isCurrent = false, iud.stopKey, summary(iud.startKey, iud.stopKey, days = 4))
                ),
                leftOut = true
            )
        )
        val list = composeRule.onNode(hasScrollAction())

        listOf(
            hasText("Injection") and hasText("Since September 6: you logged no bleeding or spotting."),
            hasText("Pill") and hasText("You logged 3 bleeds on it."),
            hasText("Ring") and hasText("You logged no bleeds on it."),
            hasText("Hormonal IUD") and
                hasText(
                    "In that time you logged bleeding or spotting on 4 days, in 1 episode. The longest lasted 4 days."
                )
        ).forEach { card ->
            list.performScrollToNode(card)
            composeRule.onNode(card).assertIsDisplayed()
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
    fun `the detail of a cycle cut short names the method with its length and dates`() {
        showDetail(HistorySamples.cutShortCycle)

        composeRule.onNode(
            hasText("32 days") and hasText("October 8, 2026 to November 8, 2026") and
                hasText("Cut short when the implant was fitted. Not part of your typical cycle.")
        ).assertIsDisplayed()
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
    fun `edit period dates opens the editor for the cycle's period`() {
        showDetail(HistorySamples.pastCycle)
        composeRule.onNodeWithText("Edit period dates").performScrollTo().assert(isButton).performClick()

        assertEquals(listOf("edit 2027-08-05"), calls)
    }

    @Test
    fun `delete this period asks first, and keep it changes nothing`() {
        showDetail(HistorySamples.pastCycle)
        composeRule.onNodeWithText("Delete this period").performScrollTo().assert(isButton).performClick()
        composeRule.onNodeWithText("Delete this period?").assertIsDisplayed()
        composeRule.onNodeWithText("Cycle forgets your period of August 5 to August 10", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Keep it").performClick()
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), calls)

        composeRule.onNodeWithText("Delete this period").performScrollTo().performClick()
        composeRule.onNodeWithText("Delete").performClick()
        assertEquals(listOf("delete"), calls)
    }

    @Test
    fun `the editor shows the picked days, and each tap goes to its action`() {
        showEditor(HistorySamples.editPast)

        composeRule.onNode(hasText("August 5 to August 8") and hasText("Period: 4 days")).assertIsDisplayed()
        composeRule.onNodeWithText("Tap the day it ended.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Still going").assertCountEquals(0)
        composeRule.onNodeWithText("First day").performClick()
        composeRule.onNodeWithContentDescription("August 20", substring = true).performClick()
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("choose First", "pick 2027-08-20", "month 2027-07", "save", "back"), calls)
    }

    @Test
    fun `the current period can be still going, and Save waits for a change`() {
        val current = HistorySamples.editCurrent
        showEditor(current.copy(draft = PeriodDraft.of(current.period)))

        composeRule.onNodeWithText("Save").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Still going").performClick()

        assertEquals(listOf("still going true"), calls)
    }

    @Test
    fun `a refused save says why`() {
        showEditor(HistorySamples.editRefused)
        composeRule.onNodeWithText("That runs into your period of July 9 to July 13", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `a cycle edited away says so`() {
        showDetail(HistorySamples.missing)
        composeRule.onNodeWithText("This cycle has changed").assertIsDisplayed()
    }
}
