package com.tonypine.cycle.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.tonypine.cycle.core.designsystem.CycleBottomSheetState
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.FlowLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What the day log sheet saves, when it asks first, and what TalkBack reads. Synthetic days and notes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class DayLogSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val logged = mutableListOf<DayFeelings>()
    private lateinit var sheet: CycleBottomSheetState

    private fun show(entry: DayLogEntry) {
        var shown by mutableStateOf(entry)
        composeRule.setContent {
            Themed {
                sheet = OpenDayLogSheet(
                    shown,
                    onLog = { date, flow, feelings ->
                        calls += "log $date $flow"
                        logged += feelings
                    },
                    onFill = { calls += "fill $it" },
                    onClear = { calls += "clear $it" }
                )
            }
        }
        composeRule.waitForIdle()
    }

    /** Scrolls the sheet to the only node showing [text] and taps it. */
    private fun click(text: String) = composeRule.onNodeWithText(text).performScrollTo().performClick()

    /**
     * Picks the option read as [description] ("Low, Energy"), the way TalkBack does: a group scrolls
     * sideways, so scrolling to a segment would not bring it into the sheet's view.
     */
    private fun clickOption(description: String) =
        composeRule.onNode(hasContentDescription(description)).performSemanticsAction(SemanticsActions.OnClick)

    private val isCheckbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    private val isRadioButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
    private val isSelected = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
    private val isNotSelected = SemanticsMatcher.expectValue(SemanticsProperties.Selected, false)

    @Test
    fun `the title is the day, read as a heading`() {
        show(DayLogSamples.empty)

        composeRule.onNodeWithText("Tuesday, March 9").assert(isHeading()).assertIsDisplayed()
    }

    @Test
    fun `each flow reads as a radio button with its group, and the logged one is selected`() {
        show(DayLogSamples.medium)

        composeRule.onNode(hasContentDescription("Medium, Flow"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        composeRule.onNode(hasContentDescription("Light, Flow"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, false))
    }

    @Test
    fun `the flow is a draft until Log it`() {
        show(DayLogSamples.medium)
        composeRule.onNodeWithText("Heavy").performClick()
        assertEquals(emptyList<String>(), calls)

        click("Log it")
        composeRule.waitForIdle()

        assertEquals(listOf("log 2027-03-03 ${FlowLevel.HEAVY}"), calls)
        assertFalse(sheet.isVisible)
    }

    @Test
    fun `tapping the selected flow again logs no flow`() {
        show(DayLogSamples.medium)
        composeRule.onNodeWithText("Medium").performClick()
        click("Log it")

        assertEquals(listOf("log 2027-03-03 null"), calls)
    }

    @Test
    fun `Nah closes the sheet without saving`() {
        show(DayLogSamples.medium)
        composeRule.onNodeWithText("Light").performClick()
        click("Nah")
        composeRule.waitForIdle()

        assertEquals(emptyList<String>(), calls)
        assertFalse(sheet.isVisible)
    }

    @Test
    fun `the fill chip logs the period at once and closes the sheet`() {
        show(DayLogSamples.empty)
        composeRule.onNodeWithText("Period started this day: fill in 5 days")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("fill 2027-03-09"), calls)
        assertFalse(sheet.isVisible)
    }

    @Test
    fun `no fill chip near a period, and nothing to clear on an empty day`() {
        show(DayLogSamples.nearPeriod)

        composeRule.onAllNodesWithText("Period started this day", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("Clear this day").assertCountEquals(0)
    }

    @Test
    fun `Clear this day asks first, and Keep it keeps the day`() {
        show(DayLogSamples.medium)
        click("Clear this day")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Clear March 3?").assert(isHeading())
        composeRule.onNodeWithText(
            "March 3 will no longer count as a period day, and what you logged for it goes. " +
                "The other days of the period stay."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Keep it").performClick()
        composeRule.waitForIdle()

        assertEquals(emptyList<String>(), calls)
        composeRule.onNodeWithText("Clear March 3?").assertDoesNotExist()
        composeRule.onNodeWithText("Log it").assertExists()
    }

    @Test
    fun `Clear clears the day and closes the sheet`() {
        show(DayLogSamples.medium)
        click("Clear this day")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Clear").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("clear 2027-03-03"), calls)
        assertFalse(sheet.isVisible)
        composeRule.onAllNodes(isDialog()).assertCountEquals(0)
    }

    @Test
    fun `every section title reads as a heading, and an empty day has nothing selected`() {
        show(DayLogSamples.empty)

        listOf("Flow", "Pain", "Body", "Mood", "Energy", "Sleep", "Sex", "Notes").forEach { title ->
            composeRule.onNode(hasText(title) and isHeading()).assertExists()
        }
        composeRule.onAllNodes(isSelected).assertCountEquals(0)
    }

    @Test
    fun `where it hurts shows once the pain is above none, as checkboxes`() {
        show(DayLogSamples.empty)
        composeRule.onAllNodesWithText("Cramps").assertCountEquals(0)

        clickOption("None, Pain")
        composeRule.onAllNodesWithText("Cramps").assertCountEquals(0)

        clickOption("Moderate, Pain")
        composeRule.onNode(hasContentDescription("Moderate, Pain")).assert(isRadioButton).assert(isSelected)
        composeRule.onNodeWithText("Where?").assertExists()
        composeRule.onNodeWithText("Cramps").assert(isCheckbox).assert(isNotSelected)

        click("Cramps")
        composeRule.onNodeWithText("Cramps").assert(isCheckbox).assert(isSelected)
    }

    @Test
    fun `Log it saves every category she picked`() {
        show(DayLogSamples.empty)
        clickOption("Moderate, Pain")
        click("Cramps")
        click("Bloating")
        click("Irritable")
        clickOption("Low, Energy")
        clickOption("Slept badly, Sleep")
        clickOption("Protected, Sex")
        composeRule.onNode(hasSetTextAction()).performScrollTo().performTextInput("A synthetic note.")
        click("Log it")
        composeRule.waitForIdle()

        assertEquals(listOf("log 2027-03-09 null"), calls)
        assertEquals(listOf(DayLogSamples.everyFeeling.copy(date = DayLogSamples.empty.date)), logged)
        assertFalse(sheet.isVisible)
    }

    @Test
    fun `a saved day reopens with the same selections`() {
        show(DayLogSamples.feelings)

        composeRule.onNode(hasContentDescription("Moderate, Pain")).assert(isSelected)
        listOf("Cramps", "Bloating", "Irritable").forEach { composeRule.onNodeWithText(it).assert(isSelected) }
        listOf("Low, Energy", "Slept badly, Sleep", "Protected, Sex").forEach {
            composeRule.onNode(hasContentDescription(it)).assert(isSelected)
        }
        composeRule.onNodeWithText("A synthetic note.").assertExists()

        click("Log it")
        assertEquals(listOf(DayLogSamples.everyFeeling), logged)
    }

    @Test
    fun `taking a choice back logs it as not logged`() {
        show(DayLogSamples.feelings)
        clickOption("Moderate, Pain")
        click("Bloating")
        clickOption("Low, Energy")
        click("Log it")

        assertEquals(listOf(DayLogSamples.everyFeeling.copy(pain = null, body = emptySet(), energy = null)), logged)
    }

    @Test
    fun `a hidden category is not shown, and Log it keeps what she logged in it`() {
        show(DayLogSamples.hidden)

        composeRule.onAllNodes(hasText("Sex") and isHeading()).assertCountEquals(0)
        composeRule.onAllNodes(hasText("Notes") and isHeading()).assertCountEquals(0)
        composeRule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        composeRule.onNode(hasText("Sleep") and isHeading()).assertExists()

        clickOption("High, Energy")
        click("Log it")

        assertEquals(listOf(DayLogSamples.everyFeeling.copy(energy = EnergyLevel.HIGH)), logged)
    }

    @Test
    fun `a day with only how she felt can be cleared`() {
        show(DayLogEntry(DayLogSamples.empty.date, canClear = true, feelings = DayLogSamples.everyFeeling))
        click("Clear this day")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("What you logged for March 9 goes.").assertIsDisplayed()
    }
}
