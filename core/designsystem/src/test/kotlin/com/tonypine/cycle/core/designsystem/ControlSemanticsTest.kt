package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.Themed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What TalkBack and touch get from buttons, icon buttons and chips. */
@RunWith(RobolectricTestRunner::class)
class ControlSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyButtonIsAButtonThatReadsItsLabelOnlyAndClicks() {
        val clicks = mutableListOf<String>()
        show {
            Column {
                FilledButton("Log it", { clicks += "filled" }, icon = CycleIcons.Add)
                TonalButton("Edit period", { clicks += "tonal" })
                OutlinedButton("Cancel", { clicks += "outlined" })
                TextButton("Nah", { clicks += "text" }, icon = CycleIcons.Close)
            }
        }
        listOf("Log it", "Edit period", "Cancel", "Nah").forEach { label ->
            composeRule.onNodeWithText(label)
                .assert(hasRole(Role.Button))
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
                .assertIsEnabled()
                .assertTouchTarget()
                .performClick()
        }
        assertEquals(listOf("filled", "tonal", "outlined", "text"), clicks)
    }

    @Test
    fun aDisabledButtonSaysSoAndIgnoresClicks() {
        var clicks = 0
        show { FilledButton("Log it", { clicks++ }, enabled = false) }
        composeRule.onNodeWithText("Log it").assertIsNotEnabled().assertTouchTarget().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun aButtonWithAShortLabelStillHasA48dpTarget() {
        show { TextButton("Ok", {}) }
        composeRule.onNodeWithText("Ok").assertTouchTarget()
    }

    @Test
    fun iconButtonsReadTheirContentDescriptionAsButtons() {
        val clicks = mutableListOf<String>()
        show {
            Row {
                IconButton(CycleIcons.ChevronEnd, "Next month", { clicks += "standard" })
                FilledIconButton(CycleIcons.Add, "Log a day", { clicks += "filled" })
                TonalIconButton(CycleIcons.Today, "Go to today", { clicks += "tonal" })
            }
        }
        listOf("Next month", "Log a day", "Go to today").forEach { description ->
            composeRule.onNodeWithContentDescription(description)
                .assert(hasRole(Role.Button))
                .assert(hasClickAction())
                .assertTouchTarget()
                .performClick()
        }
        assertEquals(listOf("standard", "filled", "tonal"), clicks)
    }

    @Test
    fun anIconToggleButtonIsACheckboxThatToggles() {
        var checked by mutableStateOf(false)
        show { IconToggleButton(CycleIcons.Calendar, "Show the calendar", checked, { checked = it }) }
        val toggle = composeRule.onNodeWithContentDescription("Show the calendar")
        toggle.assert(hasRole(Role.Checkbox)).assertIsOff().assertTouchTarget()

        toggle.performClick()
        toggle.assertIsOn()
        assertEquals(true, checked)

        toggle.performClick()
        toggle.assertIsOff()
    }

    @Test
    fun aDisabledIconToggleButtonKeepsItsState() {
        show { IconToggleButton(CycleIcons.Calendar, "Show the calendar", checked = true, {}, enabled = false) }
        composeRule.onNodeWithContentDescription("Show the calendar").assertIsOn().assertIsNotEnabled()
    }

    @Test
    fun aFilterChipIsASelectableCheckbox() {
        var selected by mutableStateOf(false)
        show { FilterChip("Cramps", selected, { selected = !selected }) }
        val chip = composeRule.onNodeWithText("Cramps")
        chip.assert(hasRole(Role.Checkbox)).assertIsNotSelected().assertTouchTarget()

        chip.performClick()
        chip.assertIsSelected()
        // The check icon is decoration: the selected state already says it.
        chip.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))

        chip.performClick()
        chip.assertIsNotSelected()
    }

    @Test
    fun anAssistChipIsAButton() {
        var clicks = 0
        show { AssistChip("Add a note", { clicks++ }, icon = CycleIcons.Add) }
        composeRule.onNodeWithText("Add a note")
            .assert(hasRole(Role.Button))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Selected))
            .assertTouchTarget()
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun talkBackStopsOnceOnEachControlInReadingOrder() {
        show {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
                FilledButton("Log it", {}, icon = CycleIcons.Add)
                IconButton(CycleIcons.ChevronEnd, "Next month", {})
                FilterChip("Cramps", selected = true, {})
                AssistChip("Add a note", {}, icon = CycleIcons.Add)
            }
        }
        val stops = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { node ->
            (
                node.config.getOrNull(SemanticsProperties.Text)
                    ?: node.config.getOrNull(SemanticsProperties.ContentDescription)
                )
                ?.joinToString()
        }
        assertEquals(listOf("Log it", "Next month", "Cramps", "Add a note"), stops)
    }

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent { Themed(Appearance.Light, content) }
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun SemanticsNodeInteraction.assertTouchTarget() = assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
}
