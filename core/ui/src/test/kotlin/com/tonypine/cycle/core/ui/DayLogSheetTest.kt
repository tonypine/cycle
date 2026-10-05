package com.tonypine.cycle.core.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.tonypine.cycle.core.designsystem.CycleBottomSheetState
import com.tonypine.cycle.core.model.FlowLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What the day log sheet saves, when it asks first, and what TalkBack reads. Synthetic days. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class DayLogSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private lateinit var sheet: CycleBottomSheetState

    private fun show(entry: DayLogEntry) {
        var shown by mutableStateOf(entry)
        composeRule.setContent {
            Themed {
                sheet = OpenDayLogSheet(
                    shown,
                    onLog = { date, flow -> calls += "log $date $flow" },
                    onFill = { calls += "fill $it" },
                    onClear = { calls += "clear $it" }
                )
            }
        }
        composeRule.waitForIdle()
    }

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

        composeRule.onNodeWithText("Log it").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("log 2027-03-03 ${FlowLevel.HEAVY}"), calls)
        assertFalse(sheet.isVisible)
    }

    @Test
    fun `tapping the selected flow again logs no flow`() {
        show(DayLogSamples.medium)
        composeRule.onNodeWithText("Medium").performClick()
        composeRule.onNodeWithText("Log it").performClick()

        assertEquals(listOf("log 2027-03-03 null"), calls)
    }

    @Test
    fun `Nah closes the sheet without saving`() {
        show(DayLogSamples.medium)
        composeRule.onNodeWithText("Light").performClick()
        composeRule.onNodeWithText("Nah").performClick()
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
        composeRule.onNodeWithText("Clear this day").performClick()
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
        composeRule.onNodeWithText("Log it").assertIsDisplayed()
    }

    @Test
    fun `Clear clears the day and closes the sheet`() {
        show(DayLogSamples.medium)
        composeRule.onNodeWithText("Clear this day").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Clear").performClick()
        composeRule.waitForIdle()

        assertEquals(listOf("clear 2027-03-03"), calls)
        assertFalse(sheet.isVisible)
        composeRule.onAllNodes(isDialog()).assertCountEquals(0)
    }
}
