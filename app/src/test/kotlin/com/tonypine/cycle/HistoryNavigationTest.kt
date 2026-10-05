package com.tonypine.cycle

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * History in the app, on her phone's database: the list, a cycle's details, back, and "See it in
 * the calendar". Synthetic data only: made-up periods in 2027, never anyone's real cycle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class HistoryNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun tab(label: String): SemanticsNodeInteraction = composeRule.onNode(hasText(label) and isTab)

    private fun waitFor(matcher: SemanticsMatcher) =
        composeRule.waitUntil(WAIT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForText(text: String) = waitFor(hasText(text))

    private fun showApp(periods: List<Pair<String, Int>>) {
        val data = (composeRule.activity.application as CycleApplication).data
        runBlocking {
            periods.forEach { (start, length) ->
                (0 until length).forEach {
                    data.dayLogRepository.save(DayLog(LocalDate.parse(start).plusDays(it.toLong()), FlowLevel.MEDIUM))
                }
            }
        }
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                CycleApp(data, today = { LocalDate.of(2027, 9, 10) })
            }
        }
        // Her logs skip the first-run welcome; the tabs show once the settings are read.
        waitFor(hasText("History") and isTab)
        tab("History").performClick()
    }

    @Test
    fun `with one period, History says her first cycle is still going`() {
        showApp(listOf("2027-09-02" to 4))
        waitForText("Your first cycle is still going")
    }

    @Test
    fun `a cycle opens its details, back returns, and the calendar opens on its month`() {
        showApp(listOf("2027-07-09" to 5, "2027-08-05" to 6, "2027-09-02" to 4))
        waitForText("Your typical cycle")

        composeRule.onNodeWithText("Aug 5 to Sep 1", substring = true).performScrollTo().performClick()
        waitForText("August 5 to August 10")
        tab("History").assertIsSelected()

        composeRule.onNodeWithContentDescription("Back").performClick()
        waitForText("Your typical cycle")

        composeRule.onNodeWithText("Jul 9 to Aug 4", substring = true).performScrollTo().performClick()
        waitForText("July 9 to July 13")
        composeRule.onNodeWithText("See it in the calendar").performScrollTo().performClick()

        // The calendar opens on July, not on this month, September.
        waitFor(hasText("July 2027") and isHeading())
        tab("Calendar").assertIsSelected()
        tab("History").assertIsNotSelected()

        // Back from the calendar returns to Today, like from any tab.
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        tab("Today").assertIsSelected()
    }

    private companion object {
        // Room and DataStore write on their own threads.
        const val WAIT_MILLIS = 5_000L
    }
}
