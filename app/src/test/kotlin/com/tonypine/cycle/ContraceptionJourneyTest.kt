package com.tonypine.cycle

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateKind
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Settings › Your cycle › Contraception in the app, on her phone's storage, as the ticket's
 * walkthrough: add the pill, change to a hormonal IUD, correct the pill's start, stop the IUD and
 * delete its dates. Synthetic data only: today is Friday 17 September 2027, made up.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp")
class ContraceptionJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val today = LocalDate.of(2027, 9, 17)
    private val data get() = (composeRule.activity.application as CycleApplication).data
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    private fun tab(label: String): SemanticsNodeInteraction = composeRule.onNode(hasText(label) and isTab)

    private fun row(vararg texts: String): SemanticsNodeInteraction {
        val matcher = texts.map { hasText(it) }.reduce { all, each -> all and each } and isButton
        waitFor(matcher)
        return composeRule.onNode(matcher).performScrollTo()
    }

    private fun button(text: String) = composeRule.onNode(hasText(text) and isButton).performScrollTo()

    private fun waitFor(matcher: SemanticsMatcher) =
        composeRule.waitUntil(WAIT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForText(text: String) = waitFor(hasText(text, substring = true))

    private fun waitForStretches(count: Int) = composeRule.waitUntil(WAIT_MILLIS) {
        runBlocking { data.contraceptionRepository.observeStretches().first() }.size == count
    }

    private fun stretches() = runBlocking { data.contraceptionRepository.observeStretches().first() }

    private fun estimates() =
        runBlocking { data.cycleRepository.observeOverview(today).first() }.contraception.estimates

    @Test
    fun `she adds the pill, changes to a hormonal IUD, corrects the pill and stops and deletes the IUD`() {
        runBlocking { data.dayLogRepository.save(DayLog(LocalDate.of(2027, 4, 20), FlowLevel.MEDIUM)) }
        composeRule.setContent {
            CycleTheme(reduceMotion = true) { CycleApp(data, FakeDeviceLock(), today = { today }) }
        }
        waitFor(hasText("Settings") and isTab)

        // 1. Settings: under Your cycle, Contraception shows None.
        tab("Settings").performClick()
        row("Contraception", "None").performClick()

        // 2. Combined pill, breaks every month, since 3 May 2027.
        button("Add your method").performClick()
        waitFor(hasText("Combined pill") and isRadio)
        composeRule.onNode(hasText("Combined pill") and isRadio).performScrollTo().performClick()
        button("Next").performClick()
        waitForText("When did you start the pill?")
        repeat(4) { composeRule.onNodeWithContentDescription("Previous month").performClick() }
        composeRule.onNodeWithContentDescription("3 May").performClick()
        button("Next").performClick()
        waitForText("Do you take a break between packs?")
        composeRule.onNode(hasText("Every month") and isRadio).assertExists()
        button("Save").performClick()
        waitForStretches(1)
        assertEquals(EstimateKind.NEXT_BLEED, estimates())
        waitForText("Since 3 May 2027 · a break every month")
        composeRule.onNodeWithContentDescription("Back").performClick()
        row("Contraception", "Combined pill, since 3 May 2027").performClick()

        // 3. Change to a hormonal IUD from today: the pill ends yesterday.
        button("Change method").performClick()
        waitFor(hasText("Hormonal IUD") and isRadio)
        composeRule.onNode(hasText("Hormonal IUD") and isRadio).performScrollTo().performClick()
        button("Next").performClick()
        waitForText("When was your IUD fitted?")
        composeRule.onNodeWithContentDescription("17 September, today").performClick()
        button("Save").performClick()
        waitForStretches(2)
        row("Hormonal IUD", "Since 17 Sept 2027")
        row("Combined pill", "3 May to 16 Sept 2027")
        assertEquals(EstimateKind.NONE, estimates())

        // 4. The pill's start moves to 26 April.
        row("Combined pill", "3 May to 16 Sept 2027").performClick()
        row("Started", "3 May 2027").performClick()
        waitForText("Roughly is fine.")
        // The sheet opens on the pill's start, in May.
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        composeRule.onNodeWithContentDescription("26 April").performClick()
        composeRule.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()
        row("Started", "26 April 2027")
        composeRule.onNodeWithContentDescription("Back").performClick()
        row("Combined pill", "26 Apr to 16 Sept 2027")

        // 5. The IUD stopped today: none now, both stay listed, and the Settings row reads None.
        button("Mark as stopped").performClick()
        waitForText("When was your IUD taken out?")
        composeRule.onNodeWithContentDescription("17 September, today").performClick()
        button("Save").performClick()
        waitForText("Add your method")
        assertEquals(today, stretches().last().stopped)
        assertEquals(0, composeRule.onAllNodesWithText("Mark as stopped").fetchSemanticsNodes().size)
        composeRule.onNodeWithText("Cycle estimates your periods from your own cycle.").assertExists()
        row("Hormonal IUD", "17 Sept 2027")
        row("Combined pill", "26 Apr to 16 Sept 2027")
        composeRule.onNodeWithContentDescription("Back").performClick()
        row("Contraception", "None").performClick()

        // 6. Delete the IUD's dates: the pill stays.
        row("Hormonal IUD", "17 Sept 2027").performClick()
        row("Delete these dates").performClick()
        composeRule.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
        waitForStretches(1)
        assertEquals(ContraceptionMethod.COMBINED_PILL, stretches().single().method)
        row("Combined pill", "26 Apr to 16 Sept 2027")
        composeRule.onNodeWithText("Add your method").assertExists()
        assertEquals(EstimateKind.PERIOD, estimates())
    }

    private companion object {
        const val WAIT_MILLIS = 5_000L
    }
}
