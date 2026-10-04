package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.Themed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What TalkBack and touch get from buttons, icon buttons, chips, button groups and switches. */
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

    @Test
    fun aButtonGroupIsASelectableGroupOfRadioButtons48dpTall() {
        show { ButtonGroup("Flow", Flow, selectedIndex = 3, onSelectedChange = {}) }
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .onChildren()
            .assertCountEquals(Flow.size)
        Flow.forEachIndexed { index, option ->
            composeRule.onNodeWithText(option)
                .assert(hasRole(Role.RadioButton))
                .assertContentDescriptionEquals("$option, Flow")
                .assert(if (index == 3) isSelected() else isNotSelected())
                .assertTouchTarget()
                .assertHeightIsEqualTo(48.dp)
        }
    }

    @Test
    fun tappingASegmentSelectsItAndTappingItAgainClearsIt() {
        var selected by mutableStateOf<Int?>(null)
        show { ButtonGroup("Flow", Flow, selectedIndex = selected, onSelectedChange = { selected = it }) }

        composeRule.onNodeWithText("Medium").performClick().assertIsSelected()
        assertEquals(3, selected)
        composeRule.onNodeWithText("Light").assertIsNotSelected()

        composeRule.onNodeWithText("Light").performClick().assertIsSelected()
        composeRule.onNodeWithText("Medium").assertIsNotSelected()
        assertEquals(2, selected)

        composeRule.onNodeWithText("Light").performClick().assertIsNotSelected()
        assertEquals(null, selected)
    }

    @Test
    fun aDisabledButtonGroupKeepsItsSelectionAndIgnoresTaps() {
        var changes = 0
        show { ButtonGroup("Pain", Pain, selectedIndex = 1, onSelectedChange = { changes++ }, enabled = false) }
        composeRule.onNodeWithText("Moderate").assertIsNotEnabled().performClick().assertIsNotSelected()
        composeRule.onNodeWithText("Mild").assertIsNotEnabled().assertIsSelected()
        assertEquals(0, changes)
    }

    @Test
    fun talkBackReadsEachOptionWithItsGroupInReadingOrder() {
        show {
            Column {
                ButtonGroup("Flow", Flow, selectedIndex = null, onSelectedChange = {})
                ButtonGroup("Pain", Pain, selectedIndex = null, onSelectedChange = {})
            }
        }
        val stops = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { node ->
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
        }
        assertEquals(Flow.map { "$it, Flow" } + Pain.map { "$it, Pain" }, stops)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-mdpi")
    fun theFlowGroupFitsAPhoneWithoutScrolling() {
        show {
            ButtonGroup("Flow", Flow, selectedIndex = 1, onSelectedChange = {}, modifier = Modifier.testTag("group"))
        }
        val group = composeRule.onNodeWithTag("group").fetchSemanticsNode()
        assertEquals(0f, group.config[SemanticsProperties.HorizontalScrollAxisRange].maxValue(), 0f)
        // It fills the width: the last segment ends where the group does.
        val heavy = composeRule.onNodeWithText("Heavy").fetchSemanticsNode()
        assertEquals(group.boundsInRoot.right, heavy.boundsInRoot.right, 1f)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-mdpi")
    fun at200PercentTheLabelsStayOnOneLineAndTheGroupScrolls() {
        var selected by mutableStateOf<Int?>(null)
        composeRule.setContent {
            Themed(Appearance.FontScale200) {
                ButtonGroup(
                    "Flow",
                    Flow,
                    selectedIndex = selected,
                    onSelectedChange = { selected = it },
                    modifier = Modifier.testTag("group")
                )
            }
        }
        Flow.forEach { option ->
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule.onNodeWithText(option, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals("$option wraps", 1, layouts.single().lineCount)
        }
        val range = composeRule.onNodeWithTag("group").fetchSemanticsNode()
            .config[SemanticsProperties.HorizontalScrollAxisRange]
        assertTrue("the group should scroll at 200%", range.maxValue() > 0f)

        composeRule.onNodeWithText("Heavy").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(4, selected)
    }

    @Test
    fun aSwitchIsASwitchThatToggles() {
        var checked by mutableStateOf(false)
        show { Switch(checked, { checked = it }, "Log sleep") }
        val switch = composeRule.onNodeWithContentDescription("Log sleep")
        switch.assert(hasRole(Role.Switch)).assertIsOff().assertTouchTarget()

        switch.performClick()
        switch.assertIsOn()
        assertEquals(true, checked)

        switch.performClick()
        switch.assertIsOff()
    }

    @Test
    fun aSwitchRowIsOneSwitchThatReadsItsTitleAndBodyAndTogglesFromAnywhere() {
        var checked by mutableStateOf(false)
        show {
            SwitchRow(
                "Sleep",
                checked,
                { checked = it },
                body = "How long you slept and how well.",
                icon = CycleIcons.Bedtime
            )
        }
        val row = composeRule.onNode(hasClickAction())
        row.assert(hasRole(Role.Switch))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Text,
                    texts("Sleep", "How long you slept and how well.")
                )
            )
            // The icon is decoration: TalkBack reads the title and the body only.
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assertIsOff()
            .assertTouchTarget()
        // The switch inside is not a second stop.
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)

        row.performTouchInput { click(centerLeft + Offset(8f, 0f)) }
        row.assertIsOn()
        assertEquals(true, checked)

        row.performTouchInput { click(centerRight - Offset(8f, 0f)) }
        row.assertIsOff()
    }

    @Test
    fun aDisabledSwitchRowKeepsItsStateAndIgnoresTaps() {
        var changes = 0
        show { SwitchRow("Sleep", checked = true, { changes++ }, enabled = false) }
        composeRule.onNodeWithText("Sleep").assertIsOn().assertIsNotEnabled().performClick().assertIsOn()
        assertEquals(0, changes)
    }

    @Test
    fun talkBackStopsOnceOnEachSwitchInReadingOrder() {
        show {
            Column {
                SwitchRow("Sleep", checked = true, {}, body = "How long you slept and how well.")
                SwitchRow("Energy", checked = false, {}, icon = CycleIcons.Bolt)
                Switch(checked = false, {}, "Log sex")
            }
        }
        val stops = composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().map { node ->
            (
                node.config.getOrNull(SemanticsProperties.ContentDescription)
                    ?: node.config.getOrNull(SemanticsProperties.Text)?.map { it.text }
                )
                ?.joinToString()
        }
        assertEquals(listOf("Sleep, How long you slept and how well.", "Energy", "Log sex"), stops)
    }

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent { Themed(Appearance.Light, content) }
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun SemanticsNodeInteraction.assertTouchTarget() = assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)

    private fun texts(vararg texts: String) = texts.map { AnnotatedString(it) }

    private companion object {
        val Flow = listOf("None", "Spotting", "Light", "Medium", "Heavy")
        val Pain = listOf("None", "Mild", "Moderate", "Severe")
    }
}
