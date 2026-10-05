package com.tonypine.cycle

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tonypine.cycle.core.designsystem.CycleTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The calendar journeys on the app's own database, from an empty log: catch up on a forgotten period,
 * clear a day, and get from Today's "missed a period?" to the calendar. Today is a made-up day,
 * 20 March 2027; every date is synthetic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CalendarJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun start() {
        val data = (composeRule.activity.application as CycleApplication).data
        composeRule.setContent {
            CycleTheme(reduceMotion = true) { CycleApp(data, today = { LocalDate.of(2027, 3, 20) }) }
        }
        waitFor(hasText("Log a period"))
    }

    private fun waitFor(matcher: SemanticsMatcher) =
        composeRule.waitUntil(WAIT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    /** Logs a past period from Today's empty state: [day], [monthsBack] months before this one. */
    private fun logPeriodFromToday(day: String, monthsBack: Int = 0) {
        composeRule.onNodeWithText("Log a period").performClick()
        composeRule.waitForIdle()
        repeat(monthsBack) { composeRule.onNodeWithContentDescription("Previous month").performClick() }
        composeRule.onNodeWithContentDescription(day).performClick()
        composeRule.onNodeWithText("Log it").performClick()
    }

    @Test
    fun `fill in a forgotten period, clear one of its days, and Today follows`() {
        start()
        logPeriodFromToday("March 2")
        waitFor(hasText("Day 19"))
        waitFor(hasText("Estimated from a typical 28-day cycle"))

        composeRule.onNode(hasText("Calendar") and isTab).performClick()
        waitFor(hasText("March 2027") and isHeading())
        composeRule.onNodeWithContentDescription("March 2, period").assertExists()
        composeRule.onNodeWithContentDescription("March 30, predicted period")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))

        // February: nothing logged, so the sheet offers to fill in her usual 5 days.
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        composeRule.onNodeWithContentDescription("February 2").performClick()
        waitFor(hasText("Tuesday, February 2") and isHeading())
        composeRule.onNodeWithText("Period started this day: fill in 5 days").performClick()

        waitFor(hasContentDescription("February 6, period"))
        composeRule.onNode(isDialog()).assertDoesNotExist()
        (2..6).forEach { composeRule.onNodeWithContentDescription("February $it, period").assertExists() }

        composeRule.onNodeWithContentDescription("February 4, period").performClick()
        waitFor(hasText("Clear this day"))
        composeRule.onNodeWithText("Clear this day").performScrollTo().performClick()
        waitFor(hasText("Clear February 4?"))
        composeRule.onNodeWithText("Clear").performClick()

        waitFor(hasContentDescription("February 4") and hasClickAction())
        listOf(2, 3, 5, 6).forEach { composeRule.onNodeWithContentDescription("February $it, period").assertExists() }

        composeRule.onNode(hasText("Today") and isTab).performClick()
        waitFor(hasText("Day 19"))
        // Her first cycle, 2 February to 1 March, now makes the estimate.
        composeRule.onNodeWithText("Estimated from your last cycle").performScrollTo()
    }

    @Test
    fun `missed a period opens the calendar on the month it was due`() {
        start()
        logPeriodFromToday("January 29", monthsBack = 2)
        waitFor(hasText("Day 51"))

        composeRule.onNodeWithText("Missed a period?").assertExists()
        composeRule.onNodeWithText("Add a past period").performScrollTo().performClick()

        waitFor(hasText("February 2027") and isHeading())
        composeRule.onNode(hasText("Calendar") and isTab).assertIsSelected()
    }

    private companion object {
        // Room and DataStore write on their own threads.
        const val WAIT_MILLIS = 5_000L
    }
}
