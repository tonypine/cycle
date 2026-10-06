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
import com.tonypine.cycle.core.model.BleedingWord
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LogCategory
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
        onLogDay = { date, flow, _ -> calls += "log day $date $flow" },
        onFillPeriod = { calls += "fill $it" },
        onClearDay = { calls += "clear $it" },
        onStillGoing = { calls += "still going" },
        onEndedOn = { calls += "ended on $it" },
        onAddPastPeriod = { calls += "add past period $it" },
        onNoMissedPeriod = { calls += "no missed period" },
        onDismissCopperIudNote = { calls += "copper IUD got it" }
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

        // The same day log sheet as the calendar's, titled with today.
        composeRule.onNode(hasText("Saturday, March 20") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Medium").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_day_log.png")
        composeRule.onNodeWithText("Light").performClick()
        composeRule.onNodeWithText("Log it").performScrollTo().performClick()

        assertEquals(listOf("ended", "log day 2027-03-20 ${FlowLevel.LIGHT}"), calls)
    }

    @Test
    fun `logged today sums up the day, read as one item, and Edit opens the day log`() {
        show(TodaySamples.loggedToday)

        composeRule.onNode(
            hasText("Logged today") and hasText("Cramps, moderate · Bloating · Irritable · Low energy · Slept badly")
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Edit").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasText("Saturday, March 20") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Cramps").assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
        composeRule.onNodeWithText("A synthetic note.").assertExists()
    }

    @Test
    fun `logged today leaves out what she hid, and says when there is only a note`() {
        val log = TodaySamples.loggedToday.todayLog
        show(TodaySamples.loggedToday.copy(todayLog = log.copy(hiddenCategories = setOf(LogCategory.SLEEP))))
        composeRule.onNodeWithText("Cramps, moderate · Bloating · Irritable · Low energy").assertIsDisplayed()

        show(TodaySamples.loggedToday.copy(todayLog = log.copy(feelings = DayFeelings(log.date, note = "Synthetic"))))
        composeRule.onNodeWithText("A note").assertIsDisplayed()

        show(TodaySamples.loggedToday.copy(todayLog = log.copy(hiddenCategories = LogCategory.entries.toSet())))
        composeRule.onAllNodesWithText("Logged today").assertCountEquals(0)
    }

    @Test
    fun `missed a period opens the calendar on the likely month, or is dismissed`() {
        show(TodaySamples.missedPeriod)
        composeRule.onNodeWithText("Missed a period?").assertIsDisplayed()
        // It takes the late card's place.
        composeRule.onAllNodesWithText("Cycles vary").assertCountEquals(0)

        composeRule.onNodeWithText("Add a past period").performClick()
        composeRule.onNodeWithText("No, I didn't miss one").performClick()

        assertEquals(listOf("add past period 2027-02", "no missed period"), calls)
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
            "Your next period is expected around March 30, 28 days after your last one started on March 2: " +
                "a typical cycle, until you have logged one of your own."
        ).assertExists()
        composeRule.onNodeWithText(
            "It may start any day between March 26 and April 3.",
            substring = true
        ).assertExists()
        // The sheet opens all the way: its last line and button are on screen, not below it.
        composeRule.onNodeWithText("the closer the estimates follow your own rhythm", substring = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("Got it").assertIsDisplayed()
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
        composeRule.onNodeWithText("Got it").assertIsDisplayed()
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_estimate_late.png")
    }

    @Test
    fun `on the implant Today names the method, says why nothing is estimated and counts her bleeding`() {
        show(TodaySamples.onImplant)

        composeRule.onNodeWithText("Implant").assert(isHeading())
        composeRule.onNodeWithText("Bleeding on the implant can come at any time, so Cycle doesn't estimate it.")
            .assertIsDisplayed()
        composeRule
            .onNode(hasText("Last 90 days", substring = true))
            .assert(
                hasText(
                    "You logged bleeding or spotting on 5 days, in 1 episode. The longest lasted 5 days.",
                    substring = true
                )
            )
        listOf("Day ", "Next period", "Predicted", "later than expected", "Missed a period?").forEach {
            composeRule.onNodeWithText(it, substring = true).assertDoesNotExist()
        }
        composeRule.onNodeWithText("Bleeding started").performClick()
        assertEquals(listOf("started"), calls)
    }

    @Test
    fun `one tap on the implant says bleeding, and the week reads it to TalkBack`() {
        show(TodaySamples.implantBleedingStarted)

        // C2: the line under "Implant" and the Undo card.
        composeRule.onAllNodesWithText("Bleeding started today").assertCountEquals(2)
        composeRule.onNodeWithText("Bleeding stopped").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("March 20, today, bleeding").assertExists()
    }

    @Test
    fun `what changes on the implant explains its bleeding and where to get help`() {
        show(TodaySamples.onImplant)
        composeRule.onNodeWithText("What changes on the implant?").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Bleeding on the implant").assertExists()
        composeRule.onNodeWithText("The implant changes bleeding for most people.", substring = true).assertExists()
        composeRule.onNodeWithText("a GP or sexual health clinic can help", substring = true).assertExists()
        composeRule.onNodeWithText("Got it").assertIsDisplayed()
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_method.png")
    }

    @Test
    fun `on the pill the first bleed is a range in the first break`() {
        show(TodaySamples.pillFirstBreak)

        composeRule.onNodeWithText("Pill").assert(isHeading())
        composeRule.onNodeWithText("Your first bleed is expected in about 7 days").assertIsDisplayed()
        composeRule
            .onNode(hasText("Next bleed", substring = true))
            .assert(hasText("March 27 to April 2", substring = true))
            .assert(hasText("In your first pill break. Estimated from the day you started the pill.", substring = true))
            .assert(
                hasText(
                    "Bleeding between breaks is common in the first three months, and usually settles.",
                    substring = true
                )
            )
        composeRule.onNodeWithText("Bleed started").assertIsDisplayed()
    }

    @Test
    fun `on the pill with no start date and no bleed logged Today says the estimate comes with her first bleed`() {
        show(
            TodaySamples.pillFirstBreak.copy(
                phase = TodayPhase.NoEstimate,
                days = TodaySamples.pillFirstBreak.days.copy(periods = emptyList(), predicted = emptyList()),
                outlook = TodayOutlook.FirstBleedToLog,
                method = TodayMethod(ContraceptionMethod.COMBINED_PILL, Breaks.MONTHLY, firstMonths = false)
            )
        )

        composeRule.onNodeWithText("Pill").assert(isHeading())
        composeRule.onNodeWithText("Cycle will estimate your next bleed once you log one.").assertIsDisplayed()
        listOf("Next bleed", "How is this estimated?").forEach {
            composeRule.onNodeWithText(it, substring = true).assertDoesNotExist()
        }
        composeRule.onNodeWithText("Bleed started").assertIsDisplayed()
    }

    @Test
    fun `how the next bleed is estimated says it is set by the pill`() {
        show(TodaySamples.pillNextBleed)
        composeRule.onNode(
            hasText("Around March 26", substring = true)
        ).assert(hasText("Between March 24 and March 28", substring = true))
        composeRule.onNodeWithText("How is this estimated?").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            "On the combined pill, the bleed in your break comes because you stop the hormones for a few days. " +
                "It's a bleed set by the pill, not a period."
        ).assertExists()
        composeRule.onNodeWithText("These are estimates, not promises.").assertExists()
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_next_bleed.png")
    }

    @Test
    fun `after the implant Today counts the days since and says why the range is wider`() {
        show(TodaySamples.stoppedImplant)

        composeRule.onNodeWithText("12 days").assert(isHeading())
        composeRule.onNodeWithText("since your implant came out").assertIsDisplayed()
        composeRule
            .onNode(hasText("Around April 6", substring = true))
            .assert(hasText("Between March 30 and April 13", substring = true))
            .assert(
                hasText(
                    "Estimated from your usual 29-day cycle. Cycles can take a few months to settle after the " +
                        "implant, so the range is wider.",
                    substring = true
                )
            )
        composeRule.onNodeWithText("My period started").assertIsDisplayed()
        composeRule.onNodeWithText("How is this estimated?").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("29 days after you stopped the implant on March 8", substring = true).assertExists()
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_estimate_stopped.png")
    }

    @Test
    fun `late for her first period after the implant the estimate sheet still counts from the day it came out`() {
        // The implant came out on February 15; her period was due 29 days later, on March 16.
        show(
            TodaySamples.stoppedImplant.copy(
                display = TodayDisplay.DaysSince(days = 33, method = ContraceptionMethod.IMPLANT),
                phase = TodayPhase.Late(daysLate = 4),
                outlook = (TodaySamples.stoppedImplant.outlook as NextPeriod).copy(
                    expectedStart = LocalDate.of(2027, 3, 20),
                    earliestStart = LocalDate.of(2027, 3, 20),
                    latestStart = LocalDate.of(2027, 3, 23),
                    lastStart = LocalDate.of(2027, 2, 15),
                    daysLate = 4
                )
            )
        )
        composeRule.onNodeWithText("How is this estimated?").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(
            "Your period was expected around March 16, 29 days after you stopped the implant on February 15: " +
                "the middle length of your last 6 cycles."
        ).assertExists()
        composeRule.onNodeWithText("It is 4 days later than that, so the estimate now starts today.").assertExists()
        composeRule.onNodeWithText("It may start any day from today to March 23.").assertExists()
        composeRule.onAllNodesWithText("first day of your last period", substring = true).assertCountEquals(0)
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/today_sheet_estimate_stopped_late.png")
    }

    @Test
    fun `after her first period since stopping each basis ends its sentence before the settling line`() {
        val settling = (TodaySamples.midCycle.outlook as NextPeriod).copy(settlingAfter = ContraceptionMethod.IMPLANT)
        val bases = mapOf(
            EstimateBasis.Setup to "Estimated from the lengths you gave.",
            EstimateBasis.Typical to "Estimated from a typical 28-day cycle.",
            EstimateBasis.Logged(1) to "Estimated from your last cycle.",
            EstimateBasis.Logged(2) to "Estimated from your last 2 cycles."
        )
        bases.forEach { (basis, line) ->
            show(TodaySamples.midCycle.copy(outlook = settling.copy(basis = basis)))

            composeRule
                .onNode(hasText("Around March 30", substring = true))
                .assert(
                    hasText(
                        "$line Cycles can take a few months to settle after the implant, so the range is wider.",
                        substring = true
                    )
                )
        }
    }

    @Test
    fun `after the injection Today says periods can take months to come back`() {
        show(TodaySamples.stoppedInjection)

        composeRule.onNodeWithText("since you stopped the injection").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Periods can take several months to come back after the injection. Cycle will estimate again once you log one."
        ).assertExists()
        composeRule.onNodeWithText("Around", substring = true).assertDoesNotExist()
    }

    @Test
    fun `on a copper IUD the heavier-periods card goes with Got it`() {
        show(TodaySamples.copperIud)

        composeRule.onNodeWithText("Day 9").assert(isHeading())
        composeRule.onNodeWithText("Periods can be heavier at first").assertIsDisplayed()
        composeRule.onNodeWithText("Got it").performClick()
        assertEquals(listOf("copper IUD got it"), calls)
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
            composeRule.onNodeWithText(buttonLabels(state).last()).performScrollTo().assertIsDisplayed()
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
            val (started, ended) = when (state.words) {
                BleedingWord.PERIOD -> "My period started" to "My period ended"
                BleedingWord.BLEED -> "Bleed started" to "Bleed ended"
                BleedingWord.BLEEDING -> "Bleeding started" to "Bleeding stopped"
            }
            when (state.phase) {
                is TodayPhase.PeriodEndedToday -> add("Undo")
                TodayPhase.PeriodStartedToday -> addAll(listOf("Undo", ended))
                is TodayPhase.OnPeriod -> add(ended)
                else -> add(started)
            }
            if (state.stillGoing != null) addAll(listOf("Still going", "It ended earlier"))
            if (state.missedPeriod != null) addAll(listOf("Add a past period", "No, I didn't miss one"))
            if (state.copperIudNote != null) add("Got it")
            add(if (state.onPeriod) "Log flow and how you feel" else "Log how you feel")
            if (state.todayLog.isLogged) add("Edit")
            when (state.outlook) {
                is NextPeriod, is NextBleed -> add("How is this estimated?")
                is TodayOutlook.Bleeding -> add("What changes on the implant?")
                TodayOutlook.AfterInjection, TodayOutlook.FirstBleedToLog -> Unit
            }
        }
    }
}
