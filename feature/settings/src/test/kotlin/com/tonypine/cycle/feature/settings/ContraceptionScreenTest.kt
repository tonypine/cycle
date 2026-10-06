package com.tonypine.cycle.feature.settings

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.ui.MethodChoice
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What TalkBack reads on Contraception and its pages, and where each button goes. Synthetic dates only. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h1400dp-mdpi")
class ContraceptionScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val today = LocalDate.of(2027, 9, 17)
    private val pill = ContraceptionStretch(
        ContraceptionMethod.COMBINED_PILL,
        LocalDate.of(2027, 5, 3),
        stopped = LocalDate.of(2027, 9, 12),
        breaks = Breaks.MONTHLY,
        id = 1
    )
    private val iud = ContraceptionStretch(ContraceptionMethod.HORMONAL_IUD, LocalDate.of(2027, 9, 13), id = 2)

    private fun showPage(current: ContraceptionStretch?, vararg stretches: ContraceptionStretch) {
        composeRule.setContent {
            Themed {
                ContraceptionScreen(
                    ContraceptionUiState(today, current, stretches.toList()),
                    onBack = { calls += "back" },
                    onAdd = { calls += "add" },
                    onStop = { calls += "stop $it" },
                    onOpen = { calls += "open $it" }
                )
            }
        }
    }

    /** "Which method?" with [choice] chosen, while her method is [current]. */
    private fun showMethodStep(current: ContraceptionStretch?, choice: MethodChoice?) {
        composeRule.setContent {
            Themed {
                AddMethodScreen(
                    AddMethodUiState(today, current, AddMethodStep.Method, choice, started = null, breaks = null),
                    onChoose = { calls += "choose $it" },
                    onMethodNext = { calls += "next" },
                    onStop = { calls += "stop $it" },
                    onDone = { calls += "done" },
                    onPickStart = {},
                    onSinceNext = {},
                    onPickBreaks = {},
                    onSave = {},
                    onBack = {},
                    onMove = {},
                    onDismissDialog = {}
                )
            }
        }
    }

    /** What TalkBack reads for each button, in order. */
    private fun spokenButtons(): List<String> = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        .filter { it.config.getOrNull(SemanticsProperties.Role) == Role.Button }
        .map { node ->
            (
                node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?: node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }
                )
                .orEmpty()
                .joinToString()
        }

    @Test
    fun `each of her methods reads its name and dates as one button that opens it`() {
        showPage(current = iud, iud, pill)

        composeRule.onNode(hasText("Now") and isHeading()).assertExists()
        composeRule.onNode(hasText("Your methods") and isHeading()).assertExists()
        assertEquals(
            listOf(
                "Back",
                "Change method",
                "Mark as stopped",
                "Hormonal IUD, Since 13 Sept 2027",
                "Combined pill, 3 May to 12 Sept 2027"
            ),
            spokenButtons()
        )

        composeRule.onNodeWithText("3 May to 12 Sept 2027").performScrollTo().performClick()
        composeRule.onNodeWithText("Mark as stopped").performClick()
        composeRule.onNodeWithText("Change method").performClick()
        assertEquals(listOf("open 1", "stop 2", "add"), calls)
    }

    @Test
    fun `on none the card says so and offers to add a method`() {
        showPage(current = null, pill)

        composeRule.onNodeWithText("None").assertExists()
        composeRule.onNodeWithText("Cycle estimates your periods from your own cycle.").assertExists()
        composeRule.onAllNodesWithText("Mark as stopped").assertCountEquals(0)
        composeRule.onNodeWithText("Add your method").performClick()
        composeRule.onNodeWithText("Cycle isn't a contraceptive", substring = true).assertExists()
        assertEquals(listOf("add"), calls)
    }

    @Test
    fun `her method now shows since when, its breaks and its calm line`() {
        val current = pill.copy(stopped = null)
        showPage(current, current)

        composeRule.onNodeWithText("Since 3 May 2027 · a break every month").assertExists()
        composeRule.onNodeWithText(
            "The pill sets your bleeds, so Cycle expects one in each break and calls it a bleed, not a period."
        ).assertExists()
    }

    @Test
    fun `the injection in its 13 weeks says until when Cycle counts it, with no mark as stopped`() {
        val injection = ContraceptionStretch(
            ContraceptionMethod.INJECTION,
            started = null,
            stopped = LocalDate.of(2027, 11, 2),
            id = 4
        )
        showPage(injection, injection)

        composeRule.onNodeWithText("Start not known").assertExists()
        composeRule.onNodeWithText("Cycle counts it until 2 November 2027, 13 weeks after your last injection.")
            .assertExists()
        composeRule.onAllNodesWithText("Mark as stopped").assertCountEquals(0)
        assertEquals("Injection, Until 2 Nov 2027", spokenButtons().last())
    }

    @Test
    fun `the method list reads each method with its line, and Next waits for a choice`() {
        showMethodStep(current = null, choice = null)

        composeRule.onNode(hasText("Which method?") and isHeading()).assertExists()
        val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        composeRule.onAllNodes(isRadio).assertCountEquals(9)
        composeRule.onNode(isRadio and hasText("Implant"))
            .assert(hasText("A rod in the arm, such as Nexplanon"))
            .performClick()
        composeRule.onNodeWithText("Next").assertIsNotEnabled()
        assertEquals(listOf("choose ${MethodChoice.Method(ContraceptionMethod.IMPLANT)}"), calls)
    }

    @Test
    fun `a method chosen goes on to since when`() {
        showMethodStep(current = iud, choice = MethodChoice.Method(ContraceptionMethod.IMPLANT))

        composeRule.onNodeWithText("Next").performScrollTo().assertIsEnabled().performClick()
        assertEquals(listOf("next"), calls)
    }

    @Test
    fun `None while on none reads Done and leaves`() {
        showMethodStep(current = null, choice = MethodChoice.None)

        composeRule.onAllNodesWithText("Next").assertCountEquals(0)
        composeRule.onNodeWithText("Done").performScrollTo().assertIsEnabled().performClick()
        assertEquals(listOf("done"), calls)
    }

    @Test
    fun `None while on a method reads Next and opens mark as stopped for it`() {
        showMethodStep(current = iud, choice = MethodChoice.None)

        composeRule.onAllNodesWithText("Done").assertCountEquals(0)
        composeRule.onNodeWithText("Next").performScrollTo().assertIsEnabled().performClick()
        assertEquals(listOf("stop 2"), calls)
    }

    @Test
    fun `None on a method that already has a stop date reads Done and leaves its date alone`() {
        val injection = ContraceptionStretch(
            ContraceptionMethod.INJECTION,
            LocalDate.of(2027, 8, 10),
            stopped = LocalDate.of(2027, 11, 9),
            id = 4
        )
        showMethodStep(current = injection, choice = MethodChoice.None)

        composeRule.onAllNodesWithText("Next").assertCountEquals(0)
        composeRule.onNodeWithText("Done").performScrollTo().assertIsEnabled().performClick()
        assertEquals(listOf("done"), calls)
    }

    @Test
    fun `a method's page reads each row with its value, and delete asks first`() {
        composeRule.setContent {
            Themed {
                StretchScreen(
                    StretchUiState.Ready(pill, today, today),
                    onEdit = {},
                    onDelete = { calls += "delete" },
                    onBack = {},
                    onMove = {},
                    onDismissDialog = {}
                )
            }
        }

        assertEquals(
            listOf(
                "Back",
                "Started, 3 May 2027",
                "Stopped, 12 September 2027",
                "Breaks, Every month",
                "Delete these dates, Cycle forgets this method for these dates. What you logged stays."
            ),
            spokenButtons()
        )
        composeRule.onNodeWithText("Delete these dates").performClick()
        composeRule.onNodeWithText(
            "Cycle forgets you used the combined pill from 3 May to 12 September 2027. " +
                "The days you logged stay, and count as your own cycle again."
        ).assertExists()
        composeRule.onNodeWithText("Delete").assertIsEnabled().performClick()
        assertEquals(listOf("delete"), calls)
    }

    @Test
    fun `a method fitted and taken out on one day lists that day once`() {
        showPage(current = null, iud.copy(stopped = iud.started))

        assertEquals("Hormonal IUD, 13 Sept 2027", spokenButtons().last())
    }

    @Test
    fun `deleting a method fitted and taken out on one day names that day once`() {
        composeRule.setContent {
            Themed {
                StretchScreen(
                    StretchUiState.Ready(iud.copy(stopped = iud.started), today, today),
                    onEdit = {},
                    onDelete = {},
                    onBack = {},
                    onMove = {},
                    onDismissDialog = {}
                )
            }
        }
        composeRule.onNodeWithText("Delete these dates").performClick()
        composeRule.onNodeWithText(
            "Cycle forgets you used the hormonal IUD on 13 September 2027. " +
                "The days you logged stay, and count as your own cycle again."
        ).assertExists()
    }
}
