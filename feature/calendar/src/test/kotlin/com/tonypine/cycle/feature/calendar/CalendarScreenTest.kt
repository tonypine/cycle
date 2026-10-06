package com.tonypine.cycle.feature.calendar

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What TalkBack reads on the calendar, what its days and buttons do, and large text. Synthetic data. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class CalendarScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private var state by mutableStateOf<CalendarUiState>(CalendarSamples.march)
    private val actions = CalendarActions(
        onPreviousMonth = { calls += "previous" },
        onNextMonth = { calls += "next" },
        onGoToToday = { calls += "today" },
        onDayClick = { date ->
            calls += "day $date"
            // As the ViewModel does: the day is selected and its sheet has what to show.
            state = CalendarSamples.selected
        },
        onLogDay = { date, flow, _ -> calls += "log $date $flow" },
        onFillPeriod = { calls += "fill $it" },
        onClearDay = { calls += "clear $it" }
    )

    private fun show(state: CalendarUiState, fontScale: Float = 1f) {
        this.state = state
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed { CalendarScreen(this.state, actions) }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `the top bar and the month read as headings, and each day as in DayCell`() {
        show(CalendarSamples.march)

        composeRule.onNode(hasText("Calendar") and isHeading()).assertIsDisplayed()
        composeRule.onNode(hasText("March 2027") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("March 1, period").assert(hasClickAction())
        composeRule.onNodeWithContentDescription("March 20, today").assert(hasClickAction())
        composeRule.onNodeWithContentDescription("March 27, predicted period")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        composeRule.onNodeWithText("Predicted periods are estimates. Tap any day up to today to log or change it.")
            .assertIsDisplayed()
    }

    @Test
    fun `the legend has period, predicted period and today`() {
        show(CalendarSamples.march)

        listOf("Period", "Predicted period", "Today").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNode(hasText("Fertile", substring = true)).assertDoesNotExist()
    }

    @Test
    fun `on the implant the legend and TalkBack say bleeding, and nothing reads as predicted`() {
        show(CalendarSamples.implantMarch)

        listOf("Period", "Bleeding", "Today").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("Predicted period").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("March 1, period").assertExists()
        composeRule.onNodeWithContentDescription("March 10, bleeding").assertExists()
        composeRule.onNodeWithText(
            "Cycle doesn't estimate bleeding on the implant. Tap any day up to today to log or change it."
        ).assertIsDisplayed()
    }

    @Test
    fun `on the pill the legend says bleed and expected bleed`() {
        show(CalendarSamples.pillMarch)

        listOf("Bleed", "Expected bleed", "Today").forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithText("Predicted period").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("March 27, expected bleed").assertExists()
        composeRule.onNodeWithText("Expected bleeds are estimates. Tap any day up to today to log or change it.")
            .assertIsDisplayed()
    }

    @Test
    fun `the top bar goes to today and the header moves between months`() {
        show(CalendarSamples.april)

        composeRule.onNodeWithContentDescription("Go to today")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        composeRule.onNodeWithContentDescription("Next month").performClick()

        assertEquals(listOf("today", "previous", "next"), calls)
    }

    @Test
    fun `tapping a past day opens its day log sheet`() {
        show(CalendarSamples.march)
        composeRule.onNodeWithContentDescription("March 9").performClick()
        composeRule.waitForIdle()

        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNode(hasText("Tuesday, March 9") and isHeading()).assertIsDisplayed()
        composeRule.onNode(hasContentDescription("Medium, Flow"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        composeRule.onNodeWithText("Period started this day: fill in 5 days").performClick()

        assertEquals(listOf("day ${LocalDate.of(2027, 3, 9)}", "fill ${LocalDate.of(2027, 3, 9)}"), calls)
    }

    @Test
    fun `a day after today cannot be tapped`() {
        show(CalendarSamples.march)

        composeRule.onNodeWithContentDescription("March 21")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        composeRule.onAllNodes(isDialog()).assertCountEquals(0)
    }

    @Test
    fun `at 200 percent the screen scrolls and no text clips`() {
        show(CalendarSamples.march, fontScale = 2f)
        composeRule.onNodeWithText("Predicted periods are estimates", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))
            .fetchSemanticsNodes()
            .forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.forEach { layout ->
                    val text = layout.layoutInput.text
                    assertFalse("$text is cut off", layout.didOverflowHeight)
                }
            }
    }
}
