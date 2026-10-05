package com.tonypine.cycle.feature.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The welcome and both setup steps, as she goes through them. Synthetic dates in 2027 only. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2027, 3, 20)
    private val done = mutableListOf<Triple<LocalDate?, Int, Int>>()
    private var skipped = 0
    private var restores = 0

    private fun show() = composeRule.setContent {
        Themed {
            OnboardingScreen(
                today = today,
                onRestore = { restores++ },
                onSkip = { skipped++ },
                onDone = { start, cycle, period -> done += Triple(start, cycle, period) }
            )
        }
    }

    private fun slider(label: String) =
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress) and hasContentDescription(label))

    private fun button(description: String) = composeRule.onNodeWithContentDescription(description).performScrollTo()

    private fun tap(text: String) = composeRule.onNodeWithText(text).performScrollTo().performClick()

    @Test
    fun `the welcome offers Get started, Restore from a Cycle export and Skip for now`() {
        show()
        composeRule.onNodeWithText("Hi! Let's get your cycle going", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("no account, no ads, no tracking", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Get started").assertIsDisplayed()

        tap("Restore from a Cycle export")
        tap("Skip for now")

        assertEquals(1, restores)
        assertEquals(1, skipped)
        assertEquals(emptyList<Any>(), done)
    }

    @Test
    fun `step 1 waits for a day up to today`() {
        show()
        tap("Get started")

        composeRule.onNodeWithText("When did your last period start?").assertIsDisplayed()
        composeRule.onNodeWithText("Roughly is fine.").assertIsDisplayed()
        composeRule.onNodeWithText("Step 1 of 2").assertIsDisplayed()
        composeRule.onNodeWithText("March 2027").assertIsDisplayed()
        composeRule.onNodeWithText("Next").assertIsNotEnabled()
        // Days after today show but ignore taps.
        composeRule.onNodeWithContentDescription("21 March")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))

        composeRule.onNodeWithContentDescription("2 March").performClick()

        composeRule.onNodeWithContentDescription("2 March").assertIsSelected()
        composeRule.onNodeWithText("Next").assertIsEnabled()
    }

    @Test
    fun `setup hands over the day she picked and the default lengths`() {
        show()
        tap("Get started")
        composeRule.onNodeWithContentDescription("2 March").performClick()
        tap("Next")

        composeRule.onNodeWithText("How long do they usually last?").assertIsDisplayed()
        composeRule.onNodeWithText("Step 2 of 2").assertIsDisplayed()
        slider("Cycle length").assert(hasStateDescription("28 days"))
        slider("Period length").assert(hasStateDescription("5 days"))
        composeRule.onNodeWithText("28 days").assertIsDisplayed()
        composeRule.onNodeWithText("5 days").assertIsDisplayed()
        tap("Done")

        assertEquals(listOf(Triple(LocalDate.of(2027, 3, 2), 28, 5)), done)
    }

    @Test
    fun `a day in an earlier month can be picked`() {
        show()
        tap("Get started")
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        composeRule.onNodeWithContentDescription("24 February").performClick()
        tap("Next")
        tap("Done")

        assertEquals(listOf(Triple(LocalDate.of(2027, 2, 24), 28, 5)), done)
    }

    @Test
    fun `I don't remember skips the day`() {
        show()
        tap("Get started")
        composeRule.onNodeWithContentDescription("2 March").performClick()
        tap("I don't remember")
        tap("Done")

        assertEquals(listOf(Triple(null, 28, 5)), done)
    }

    @Test
    fun `back goes from step 2 to step 1 to the welcome, keeping what she picked`() {
        show()
        tap("Get started")
        composeRule.onNodeWithContentDescription("2 March").performClick()
        tap("Next")
        repeat(3) { button("Cycle one day longer").performClick() }

        pressBack()
        composeRule.onNodeWithText("When did your last period start?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2 March").assertIsSelected()

        tap("Next")
        slider("Cycle length").assert(hasStateDescription("31 days"))

        // The bar's back arrow, then the system back.
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("When did your last period start?").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("Get started").assertIsDisplayed()
        assertEquals(emptyList<Any>(), done)
    }

    @Test
    fun `minus and plus change a length a day at a time`() {
        show()
        tap("Get started")
        tap("I don't remember")
        repeat(4) { button("Period one day longer").performClick() }
        button("Cycle one day shorter").performClick()
        composeRule.onNodeWithText("27 days").assertExists()
        composeRule.onNodeWithText("9 days").assertExists()
        tap("Done")

        assertEquals(listOf(Triple(null, 27, 9)), done)
    }

    @Test
    fun `dragging a slider to its end gives the longest length she can give`() {
        show()
        tap("Get started")
        tap("I don't remember")
        slider("Cycle length").performScrollTo().performTouchInput { swipeRight(startX = centerX, endX = right + 100f) }
        slider("Period length").performScrollTo().performTouchInput { swipeLeft(startX = centerX, endX = left - 100f) }

        slider("Cycle length").assert(hasStateDescription("90 days"))
        button("Cycle one day longer").assertIsNotEnabled()
        slider("Period length").assert(hasStateDescription("1 day"))
        button("Period one day shorter").assertIsNotEnabled()
        tap("Done")

        assertEquals(listOf(Triple(null, 90, 1)), done)
    }

    @Test
    fun `TalkBack reads each length and sets it a day at a time`() {
        show()
        tap("Get started")
        tap("I don't remember")
        slider("Cycle length")
            .assert(hasStateDescription("28 days"))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(28f, 15f..90f, steps = 74)
                )
            )
            .performSemanticsAction(SemanticsActions.SetProgress) { it(19f) }
        tap("Done")

        assertEquals(listOf(Triple(null, 19, 5)), done)
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }
}
