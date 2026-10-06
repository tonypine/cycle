package com.tonypine.cycle

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.EstimateKind
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Setup from the welcome to Today on the app's own storage, with a synthetic day: on 20 March 2027,
 * a last period on 2 March, 28 and 5, then step 3 skipped or on an implant fitted at a day she
 * doesn't remember (journey A in `docs/design/contraception.md`). Records
 * `src/test/screenshots/app_today_after_setup.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class CycleAppSetupTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val data get() = (composeRule.activity.application as CycleApplication).data
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun `setup with step 3 skipped gives day 19 and an estimate from the lengths she gave`() {
        toContraception()
        tap("Skip")

        waitForText("Day 19")
        composeRule.onNodeWithText("Day 19").assertIsDisplayed()
        composeRule.onNodeWithText("Around 30 March", substring = true).performScrollTo()
        composeRule.onNodeWithText("Between 26 March and 3 April", substring = true).assertExists()
        composeRule.onNodeWithText("Estimated from the lengths you gave", substring = true).assertExists()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/app_today_after_setup.png")
        assertEquals(emptyList<ContraceptionStretch>(), stretches())

        // Settings › Your cycle › Contraception: None.
        composeRule.onNode(hasText("Settings") and isTab).performClick()
        waitForText("Contraception")
        composeRule.onNodeWithText("Contraception").performScrollTo().performClick()
        waitForText("Add your method")
        composeRule.onNodeWithText("None").assertExists()
    }

    @Test
    fun `setup on an implant with no start date opens Today on the implant, with no next period`() {
        toContraception()
        composeRule.onNode(hasText("Implant") and isRadio).performScrollTo().performClick()
        tap("Next")
        waitForText("When was your implant fitted?")
        tap("I don't remember")

        // Today's words on a method come with MOT-54; what setup decides is that nothing is estimated.
        composeRule.waitUntil(WAIT_MILLIS) { stretches().isNotEmpty() }
        assertEquals(listOf(ContraceptionStretch(ContraceptionMethod.IMPLANT, started = null, id = 1)), stretches())
        val overview = runBlocking { data.cycleRepository.observeOverview(LocalDate.of(2027, 3, 20)).first() }
        assertEquals(ContraceptionMethod.IMPLANT, overview.contraception.current?.method)
        assertEquals(EstimateKind.NONE, overview.contraception.estimates)
        assertNull(overview.estimate)

        // Settings › Your cycle › Contraception: the implant, its start not known.
        composeRule.onNode(hasText("Settings") and isTab).performClick()
        waitForText("Implant, start not known")
        composeRule.onNodeWithText("Implant, start not known").performScrollTo().performClick()
        waitForText("Start not known")
        composeRule.onAllNodesWithText("Implant").onFirst().assertExists()
    }

    // The welcome on 20 March 2027, then 2 March and the usual lengths, to step 3.
    private fun toContraception() {
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                CycleApp(data, FakeDeviceLock(), today = { LocalDate.of(2027, 3, 20) })
            }
        }
        waitForText("Get started")
        composeRule.onNodeWithText("Get started").performClick()
        composeRule.onNodeWithContentDescription("2 March").performClick()
        tap("Next")
        waitForText("How long do they usually last?")
        tap("Next")
        waitForText("Step 3 of 3")
        composeRule.onNodeWithText("Are you using contraception?").assertIsDisplayed()
    }

    private fun tap(text: String) = composeRule.onNodeWithText(text).performScrollTo().performClick()

    private fun stretches() = runBlocking { data.contraceptionRepository.observeStretches().first() }

    private fun waitForText(text: String) = composeRule.waitUntil(WAIT_MILLIS) {
        composeRule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
    }

    private companion object {
        // Room and DataStore write on their own threads.
        const val WAIT_MILLIS = 5_000L
    }
}
