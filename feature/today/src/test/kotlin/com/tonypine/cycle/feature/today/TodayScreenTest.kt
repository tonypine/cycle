package com.tonypine.cycle.feature.today

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What TalkBack reads on Today, what each button does, the sheets, and large text. Synthetic data
 * ([TodaySamples]); the sheets record `src/test/screenshots/today_sheet_<sheet>.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class TodayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions = TodayActions(
        onPeriodStarted = { calls += "started" },
        onUndoPeriodStarted = { calls += "undo started" },
        onPeriodEnded = { calls += "ended" },
        onUndoPeriodEnded = { calls += "undo ended" },
        onLogPeriod = { calls += "log $it" },
        onFlowChange = { calls += "flow $it" },
        onStillGoing = { calls += "still going" },
        onEndedOn = { calls += "ended on $it" }
    )

    private var state by mutableStateOf<TodayUiState>(TodayUiState.Loading)
    private var fontScale by mutableFloatStateOf(1f)
    private var shown = false

    /** Shows [state]; a test may show several, one after the other, in the same composition. */
    private fun show(state: TodayUiState, fontScale: Float = 1f) {
        this.state = state
        this.fontScale = fontScale
        if (!shown) {
            shown = true
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, this.fontScale)) {
                    Themed { TodayScreen(this.state, actions) }
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `TalkBack reads the cycle day as a heading and the estimate as an estimate`() {
        show(TodaySamples.midCycle)

        composeRule.onNodeWithText("Day 19").assert(isHeading()).assertIsDisplayed()
        composeRule
            .onNode(hasText("Around March 30", substring = true) and hasText("Estimated from", substring = true))
            .assert(hasText("Between March 26 and April 3", substring = true))
            .assert(hasText("Next period", substring = true))
    }

    @Test
    fun `every action is a button`() {
        TodaySamples.all.values.forEach { state ->
            show(state)
            val labels = buttonLabels(state)
            labels.forEach { label ->
                composeRule.onNodeWithText(
                    label
                ).assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            }
        }
    }

    @Test
    fun `one tap starts a period`() {
        show(TodaySamples.midCycle)
        composeRule.onNodeWithText("My period started").performClick()
        assertEquals(listOf("started"), calls)
    }

    @Test
    fun `during a period the button ends it and the day log asks for flow`() {
        show(TodaySamples.onPeriod)
        composeRule.onNodeWithText("My period ended").performClick()
        composeRule.onNodeWithText("Log flow and how you feel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Light").performClick()

        assertEquals(listOf("ended", "flow ${FlowLevel.LIGHT}"), calls)
        composeRule.onNodeWithText("Medium").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_day_log.png")
    }

    @Test
    fun `Undo on the card takes back a start or an end`() {
        show(TodaySamples.periodStarted)
        composeRule.onNodeWithText("Period started today").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()

        show(TodaySamples.periodEnded)
        composeRule.onNodeWithText("Period ended today").assertIsDisplayed()
        composeRule.onAllNodesWithText("My period", substring = true).assertCountEquals(0)
        composeRule.onNodeWithText("Undo").performClick()

        assertEquals(listOf("undo started", "undo ended"), calls)
    }

    @Test
    fun `how is this estimated explains the date and the range`() {
        show(TodaySamples.midCycle)
        composeRule.onNodeWithText("How is this estimated?").performClick()
        composeRule.waitForIdle()

        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText(
            "March 30 is the first day of your last period, March 2, plus 28 days: a typical cycle, until you " +
                "have logged one of your own."
        ).assertExists()
        composeRule.onNodeWithText(
            "It may start any day between March 26 and April 3.",
            substring = true
        ).assertExists()
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_estimate.png")
    }

    @Test
    fun `the estimate sheet says how late, calmly`() {
        show(TodaySamples.late)
        composeRule.onNodeWithText("How is this estimated?").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            "Your period was expected around March 18: the first day of your last period, February 18, plus 28 " +
                "days, the middle length of your last 6 cycles."
        ).assertExists()
        composeRule.onNodeWithText("It is 2 days later than that, so the estimate now starts today.").assertExists()
        composeRule.onNodeWithText("It may start any day from today to March 21.").assertExists()
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_estimate_late.png")
    }

    @Test
    fun `the empty state logs a period from the day she picks`() {
        show(TodaySamples.empty)
        composeRule.onNodeWithText("No periods logged yet", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Log a period").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Log it").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("March 1").performClick()
        composeRule.onNodeWithText("Log it").assertIsEnabled()
        // Days after today cannot be picked.
        composeRule.onNodeWithContentDescription("March 21")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_log_period.png")

        composeRule.onNodeWithText("Log it").performClick()
        assertEquals(listOf("log ${LocalDate.of(2027, 3, 1)}"), calls)
    }

    @Test
    fun `still going is answered once, or with the day it ended`() {
        show(TodaySamples.stillGoing)
        composeRule.onNodeWithText("Still going").performClick()
        composeRule.onNodeWithText("It ended earlier").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("When was the last day?").assertExists()
        // Only the period's days can be picked.
        composeRule.onNodeWithContentDescription(
            "March 12"
        ).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        // Today's week shows behind the sheet too, with days that cannot be tapped.
        composeRule.onAllNodesWithContentDescription(
            "March 17",
            substring = true
        ).filterToOne(hasClickAction()).performClick()
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_last_day.png")
        composeRule.onNodeWithText("Save").performClick()

        assertEquals(listOf("still going", "ended on ${LocalDate.of(2027, 3, 17)}"), calls)
    }

    @Test
    fun `at 200 percent the screen scrolls and no text clips`() {
        TodaySamples.all.values.forEach { state ->
            show(state, fontScale = 2f)
            val last = if (state is TodayUiState.Empty) "Log a period" else "How is this estimated?"
            composeRule.onNodeWithText(last).performScrollTo().assertIsDisplayed()
            composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))
                .fetchSemanticsNodes()
                .forEach { node ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                    layouts.forEach { layout ->
                        // Text wraps, so clipping means cut off at the bottom or ending in an ellipsis.
                        val text = layout.layoutInput.text
                        assertFalse("$text is cut off", layout.didOverflowHeight)
                        assertFalse(
                            "$text ends in an ellipsis",
                            (0 until layout.lineCount).any(layout::isLineEllipsized)
                        )
                    }
                }
        }
    }

    private fun buttonLabels(state: TodayUiState): List<String> = when (state) {
        TodayUiState.Loading -> emptyList()

        is TodayUiState.Empty -> listOf("Log a period")

        is TodayUiState.Tracking -> buildList {
            when (state.phase) {
                is TodayPhase.PeriodEndedToday -> add("Undo")
                TodayPhase.PeriodStartedToday -> addAll(listOf("Undo", "My period ended"))
                is TodayPhase.OnPeriod -> add("My period ended")
                else -> add("My period started")
            }
            if (state.stillGoing != null) addAll(listOf("Still going", "It ended earlier"))
            add(if (state.onPeriod) "Log flow and how you feel" else "Log how you feel")
            add("How is this estimated?")
        }
    }
}
