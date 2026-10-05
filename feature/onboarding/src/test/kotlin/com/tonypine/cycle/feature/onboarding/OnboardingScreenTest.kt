package com.tonypine.cycle.feature.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
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

    private fun field(label: String) = composeRule.onNode(hasSetTextAction() and hasText(label, substring = true))

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
        field("Cycle length, in days").assert(hasText("28"))
        field("Period length, in days").assert(hasText("5"))
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
        field("Cycle length, in days").performTextClearance()
        field("Cycle length, in days").performTextInput("31")

        pressBack()
        composeRule.onNodeWithText("When did your last period start?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2 March").assertIsSelected()

        tap("Next")
        field("Cycle length, in days").assert(hasText("31"))

        // The bar's back arrow, then the system back.
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("When did your last period start?").assertIsDisplayed()
        pressBack()
        composeRule.onNodeWithText("Get started").assertIsDisplayed()
        assertEquals(emptyList<Any>(), done)
    }

    @Test
    fun `an impossible length shows how to fix it, and Done waits until it is fixed`() {
        show()
        tap("Get started")
        tap("I don't remember")
        field("Cycle length, in days").performTextClearance()
        field("Cycle length, in days").performTextInput("12")
        // No error while she types.
        composeRule.onNodeWithText("Enter a number of days from 15 to 90.").assertDoesNotExist()

        tap("Done")

        composeRule.onNodeWithText("Enter a number of days from 15 to 90.").assertIsDisplayed()
        assertEquals(emptyList<Any>(), done)

        // The error follows what she types from then on.
        field("Cycle length, in days").performTextClearance()
        field("Cycle length, in days").performTextInput("45")
        composeRule.onNodeWithText("Enter a number of days from 15 to 90.").assertDoesNotExist()
        tap("Done")

        assertEquals(listOf(Triple(null, 45, 5)), done)
    }

    @Test
    fun `an unusual length is accepted`() {
        show()
        tap("Get started")
        tap("I don't remember")
        field("Cycle length, in days").performTextClearance()
        field("Cycle length, in days").performTextInput("19")
        field("Period length, in days").performTextClearance()
        field("Period length, in days").performTextInput("9")
        tap("Done")

        assertEquals(listOf(Triple(null, 19, 9)), done)
    }

    @Test
    fun `empty and too long fields each say what to enter`() {
        show()
        tap("Get started")
        tap("I don't remember")
        field("Cycle length, in days").performTextClearance()
        field("Period length, in days").performTextClearance()
        field("Period length, in days").performTextInput("15")
        tap("Done")

        composeRule.onNodeWithText("Enter the number of days in your cycle, such as 28.").assertIsDisplayed()
        composeRule.onNodeWithText("Enter a number of days from 1 to 14.").assertIsDisplayed()

        field("Period length, in days").performTextClearance()
        composeRule.onNodeWithText("Enter the number of days your period lasts, such as 5.").assertIsDisplayed()
        assertEquals(emptyList<Any>(), done)
    }

    @Test
    fun `the length fields take two digits and nothing else`() {
        show()
        tap("Get started")
        tap("I don't remember")
        field("Cycle length, in days").performTextClearance()
        field("Cycle length, in days").performTextInput("3a")
        field("Cycle length, in days").performTextInput("3")
        field("Cycle length, in days").performTextInput("1")
        field("Cycle length, in days").performTextInput("7")

        field("Cycle length, in days").assert(hasText("31"))
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }
}
