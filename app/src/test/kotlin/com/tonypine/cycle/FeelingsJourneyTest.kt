package com.tonypine.cycle

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.tonypine.cycle.core.designsystem.CycleTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The "how she feels" walkthrough on the app's own database: log a day from Today, see it summed up,
 * hide Sex in What to log and bring it back with what she logged. Today is a made-up day,
 * 20 March 2027; every date and note is synthetic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class FeelingsJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val isSelected = SemanticsMatcher.expectValue(SemanticsProperties.Selected, true)
    private val isSwitch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

    private fun switchState(state: ToggleableState) =
        SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)

    private fun waitFor(matcher: SemanticsMatcher) =
        composeRule.waitUntil(WAIT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun click(text: String) = composeRule.onNodeWithText(text).performScrollTo().performClick()

    /** A group's option, picked the way TalkBack does: the group scrolls sideways. */
    private fun pick(option: String) =
        composeRule.onNode(hasContentDescription(option)).performSemanticsAction(SemanticsActions.OnClick)

    private fun openDayLog() {
        click("Log how you feel")
        waitFor(hasText("Saturday, March 20") and isHeading())
    }

    @Test
    fun `log how she feels, hide Sex and bring it back`() {
        val data = (composeRule.activity.application as CycleApplication).data
        composeRule.setContent {
            CycleTheme(reduceMotion = true) { CycleApp(data, today = { LocalDate.of(2027, 3, 20) }) }
        }
        // Skips the first-run welcome to Today's empty state.
        waitFor(hasText("Skip for now"))
        composeRule.onNodeWithText("Skip for now").performClick()
        waitFor(hasText("Log a period"))
        composeRule.onNodeWithText("Log a period").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("March 2").performClick()
        composeRule.onNodeWithText("Log it").performClick()
        waitFor(hasText("Day 19"))

        // 1. Every section, nothing selected.
        openDayLog()
        listOf("Flow", "Pain", "Body", "Mood", "Energy", "Sleep", "Sex", "Notes").forEach { title ->
            composeRule.onNode(hasText(title) and isHeading()).assertExists()
        }
        composeRule.onAllNodes(isSelected and !isTab).assertCountEquals(0)

        // 2. Moderate pain shows where it hurts.
        pick("Moderate, Pain")
        click("Cramps")

        // 3. The rest, a note, and Log it: Today sums it up.
        click("Bloating")
        click("Irritable")
        pick("Low, Energy")
        pick("Slept badly, Sleep")
        pick("Protected, Sex")
        composeRule.onNode(hasSetTextAction()).performScrollTo().performTextInput("A synthetic note.")
        click("Log it")
        waitFor(hasText("Logged today"))
        composeRule.onNodeWithText("Cramps, moderate · Bloating · Irritable · Low energy · Slept badly · Protected sex")
            .performScrollTo()
        composeRule.onNodeWithText("Edit").assertExists()

        // 4. Sex off: gone from the sheet and the summary.
        composeRule.onNode(hasText("Settings") and isTab).performClick()
        waitFor(hasText("What to log"))
        composeRule.onNodeWithText("What to log").performClick()
        waitFor(hasText("Sex") and isSwitch)
        composeRule.onNode(hasText("Sex") and isSwitch).performScrollTo().performClick()
        waitFor(hasText("Sex") and isSwitch and switchState(ToggleableState.Off))

        composeRule.onNode(hasText("Today") and isTab).performClick()
        waitFor(hasText("Cramps, moderate · Bloating · Irritable · Low energy · Slept badly"))
        openDayLog()
        composeRule.onAllNodes(hasText("Sex") and isHeading()).assertCountEquals(0)
        // Logging again with Sex hidden keeps what she logged in it.
        pick("Normal, Energy")
        click("Log it")
        waitFor(hasText("Cramps, moderate · Bloating · Irritable · Normal energy · Slept badly"))

        // 5. Sex on again: back, with what she logged.
        composeRule.onNode(hasText("Settings") and isTab).performClick()
        waitFor(hasText("Sex") and isSwitch)
        composeRule.onNode(hasText("Sex") and isSwitch).performScrollTo().performClick()
        waitFor(hasText("Sex") and isSwitch and switchState(ToggleableState.On))

        composeRule.onNode(hasText("Today") and isTab).performClick()
        waitFor(hasText("Cramps, moderate · Bloating · Irritable · Normal energy · Slept badly · Protected sex"))
        openDayLog()
        composeRule.onNode(hasContentDescription("Protected, Sex")).assert(isSelected)
        composeRule.onNodeWithText("A synthetic note.").assertExists()
    }

    private companion object {
        // Room and DataStore write on their own threads.
        const val WAIT_MILLIS = 5_000L
    }
}
