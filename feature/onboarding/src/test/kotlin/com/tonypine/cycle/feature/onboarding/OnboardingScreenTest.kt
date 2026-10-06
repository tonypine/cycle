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
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionMethod.COMBINED_PILL
import com.tonypine.cycle.core.model.ContraceptionMethod.IMPLANT
import com.tonypine.cycle.core.model.ContraceptionStretch
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The welcome and the three setup steps, as she goes through them. Synthetic dates only. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2027, 3, 20)
    private val done = mutableListOf<Setup>()
    private var skipped = 0
    private var restores = 0

    private fun show() = composeRule.setContent {
        Themed {
            OnboardingScreen(
                today = today,
                onRestore = { restores++ },
                onSkip = { skipped++ },
                onDone = { start, cycle, period, contraception -> done += Setup(start, cycle, period, contraception) }
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
        composeRule.onNodeWithText("Step 1 of 3").assertIsDisplayed()
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
        composeRule.onNodeWithText("Step 2 of 3").assertIsDisplayed()
        slider("Cycle length").assert(hasStateDescription("28 days"))
        slider("Period length").assert(hasStateDescription("5 days"))
        composeRule.onNodeWithText("28 days").assertIsDisplayed()
        composeRule.onNodeWithText("5 days").assertIsDisplayed()
        skipContraception()

        assertEquals(listOf(Setup(LocalDate.of(2027, 3, 2), 28, 5)), done)
    }

    @Test
    fun `a day in an earlier month can be picked`() {
        show()
        tap("Get started")
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        composeRule.onNodeWithContentDescription("24 February").performClick()
        tap("Next")
        skipContraception()

        assertEquals(listOf(Setup(LocalDate.of(2027, 2, 24), 28, 5)), done)
    }

    @Test
    fun `I don't remember skips the day`() {
        show()
        tap("Get started")
        composeRule.onNodeWithContentDescription("2 March").performClick()
        tap("I don't remember")
        skipContraception()

        assertEquals(listOf(Setup(null, 28, 5)), done)
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
        skipContraception()

        assertEquals(listOf(Setup(null, 27, 9)), done)
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
        skipContraception()

        assertEquals(listOf(Setup(null, 90, 1)), done)
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
        skipContraception()

        assertEquals(listOf(Setup(null, 19, 5)), done)
    }

    @Test
    fun `step 3 asks about contraception, with nothing chosen and Next off`() {
        show()
        toContraception()

        composeRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
        composeRule.onNodeWithText("Are you using contraception?").assertIsDisplayed()
        composeRule.onNodeWithText("You can change this later in Settings", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Next").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("Skip").performScrollTo().assertIsEnabled()
        composeRule.onNodeWithText("Implant").performScrollTo().assertIsNotSelected()

        tap("Implant")

        composeRule.onNodeWithText("Implant").assertIsSelected()
        composeRule.onNodeWithText("Next").performScrollTo().assertIsEnabled()
    }

    @Test
    fun `Skip on step 3 hands over no method`() {
        show()
        toContraception()
        tap("Implant")
        tap("Skip")

        assertEquals(listOf(Setup(LocalDate.of(2027, 3, 2), 28, 5)), done)
    }

    @Test
    fun `None reads Done and hands over no method`() {
        show()
        toContraception()
        tap("None")
        composeRule.onNodeWithText("Next").assertDoesNotExist()
        tap("Done")

        assertEquals(listOf(Setup(LocalDate.of(2027, 3, 2), 28, 5)), done)
    }

    @Test
    fun `an implant with I don't remember hands over the implant with no start`() {
        show()
        toContraception()
        tap("Implant")
        tap("Next")

        composeRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
        composeRule.onNodeWithText("When was your implant fitted?").assertIsDisplayed()
        composeRule.onNodeWithText("Roughly is fine.").assertIsDisplayed()
        composeRule.onNodeWithText("Done").performScrollTo().assertIsNotEnabled()
        tap("I don't remember")

        assertEquals(listOf(Setup(LocalDate.of(2027, 3, 2), 28, 5, ContraceptionStretch(IMPLANT, null))), done)
    }

    @Test
    fun `an implant fitted in an earlier month hands over its day`() {
        show()
        toContraception()
        tap("Implant")
        tap("Next")
        composeRule.onNodeWithText("March 2027").assertIsDisplayed()
        repeat(4) { composeRule.onNodeWithContentDescription("Previous month").performClick() }
        composeRule.onNodeWithText("November 2026").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("9 November").performClick()
        composeRule.onNodeWithContentDescription("9 November").assertIsSelected()
        tap("Done")

        val fitted = LocalDate.of(2026, 11, 9)
        assertEquals(listOf(Setup(LocalDate.of(2027, 3, 2), 28, 5, ContraceptionStretch(IMPLANT, fitted))), done)
    }

    @Test
    fun `days after today can't be a start`() {
        show()
        toContraception()
        tap("Implant")
        tap("Next")

        composeRule.onNodeWithContentDescription("21 March")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun `the combined pill asks about breaks, every month to start with, still step 3`() {
        show()
        toContraception()
        tap("Combined pill")
        tap("Next")
        composeRule.onNodeWithText("When did you start the pill?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("1 March").performClick()
        composeRule.onNodeWithText("Done").assertDoesNotExist()
        tap("Next")

        composeRule.onNodeWithText("Step 3 of 3").assertIsDisplayed()
        composeRule.onNodeWithText("Do you take a break between packs?").assertIsDisplayed()
        composeRule.onNodeWithText("Every month").assertIsSelected()
        tap("Every few packs")
        tap("Done")

        val pill = ContraceptionStretch(COMBINED_PILL, LocalDate.of(2027, 3, 1), breaks = Breaks.EVERY_FEW_PACKS)
        assertEquals(listOf(Setup(LocalDate.of(2027, 3, 2), 28, 5, pill)), done)
    }

    @Test
    fun `the ring with I don't remember still asks about its breaks`() {
        show()
        toContraception()
        tap("Vaginal ring")
        tap("Next")
        tap("I don't remember")

        composeRule.onNodeWithText("Do you have a ring-free week?").assertIsDisplayed()
        tap("Done")

        val ring = ContraceptionStretch(ContraceptionMethod.RING, null, breaks = Breaks.MONTHLY)
        assertEquals(listOf(Setup(LocalDate.of(2027, 3, 2), 28, 5, ring)), done)
    }

    @Test
    fun `back goes from breaks to since when to the method to step 2, keeping her answers`() {
        show()
        toContraception()
        tap("Patch")
        tap("Next")
        composeRule.onNodeWithContentDescription("1 March").performClick()
        tap("Next")
        tap("No breaks")

        pressBack()
        composeRule.onNodeWithText("When did you start the patch?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("1 March").assertIsSelected()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithText("Are you using contraception?").assertIsDisplayed()
        composeRule.onNodeWithText("Patch").performScrollTo().assertIsSelected()
        pressBack()
        composeRule.onNodeWithText("How long do they usually last?").assertIsDisplayed()

        tap("Next")
        tap("Next")
        tap("Next")
        composeRule.onNodeWithText("No breaks").assertIsSelected()
        tap("Done")

        val patch = ContraceptionStretch(ContraceptionMethod.PATCH, LocalDate.of(2027, 3, 1), breaks = Breaks.NONE)
        assertEquals(listOf(Setup(LocalDate.of(2027, 3, 2), 28, 5, patch)), done)
    }

    @Test
    fun `her answers on step 3 survive a configuration change`() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            Themed {
                OnboardingScreen(
                    today = today,
                    onRestore = {},
                    onSkip = {},
                    onDone = { start, cycle, period, contraception ->
                        done += Setup(start, cycle, period, contraception)
                    }
                )
            }
        }
        toContraception()
        tap("Implant")
        tap("Next")
        composeRule.onNodeWithContentDescription("1 March").performClick()

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("When was your implant fitted?").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("1 March").assertIsSelected()
        pressBack()
        composeRule.onNodeWithText("Implant").performScrollTo().assertIsSelected()
    }

    // Get started, 2 March, the default lengths, to step 3.
    private fun toContraception() {
        tap("Get started")
        composeRule.onNodeWithContentDescription("2 March").performClick()
        tap("Next")
        tap("Next")
    }

    // From step 2: on to step 3, and Skip it.
    private fun skipContraception() {
        tap("Next")
        tap("Skip")
    }

    private data class Setup(
        val start: LocalDate?,
        val cycle: Int,
        val period: Int,
        val contraception: ContraceptionStretch? = null
    )

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }
}
