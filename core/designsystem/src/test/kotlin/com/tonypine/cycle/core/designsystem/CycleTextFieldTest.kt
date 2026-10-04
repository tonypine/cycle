package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.requestFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Behaviour and TalkBack semantics of [CycleTextField]. All text is synthetic. */
@RunWith(RobolectricTestRunner::class)
class CycleTextFieldTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val field get() = composeRule.onNode(hasSetTextAction())

    @Test
    fun labelAndValueArePartOfTheField() {
        setField(TextFieldState("Sat 27 March"), supportingText = "The first day of bleeding.")

        field.assertTextEquals("Period start", "Sat 27 March")
        field.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    @Test
    fun talkBackReadsTheFieldThenTheSupportingText() {
        setField(TextFieldState(), supportingText = "The first day of bleeding.")

        val children = composeRule.onRoot().onChildren()
        children[0].assert(hasSetTextAction()).assert(hasText("Period start"))
        children[1].assertTextEquals("The first day of bleeding.")
    }

    @Test
    fun errorIsAnnouncedAndShownAsASentenceInPlaceOfTheSupportingText() {
        setField(
            TextFieldState("Sat 27 March"),
            supportingText = "The first day of bleeding.",
            errorMessage = "Pick a day up to today."
        )

        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Pick a day up to today."))
        composeRule.onNodeWithText("Pick a day up to today.").assertIsDisplayed()
        composeRule.onNodeWithText("The first day of bleeding.").assertDoesNotExist()
    }

    @Test
    fun maxLengthRejectsLongerTextAndTheCounterSaysHowMuchIsUsed() {
        val state = TextFieldState("Note")
        setField(state, maxLength = 5)

        field.performTextInput(" and more")
        assertEquals("Note", state.text.toString())

        field.performTextInput("s")
        assertEquals("Notes", state.text.toString())
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.MaxTextLength, 5))
        composeRule.onNodeWithText("5/5").assertContentDescriptionEquals("5 of 5 characters")
    }

    @Test
    fun disabledFieldIsReadAsDisabled() {
        setField(TextFieldState("Sat 27 March"), enabled = false)

        composeRule.onNodeWithText("Sat 27 March").assertIsNotEnabled().assert(hasText("Period start"))
    }

    @Test
    fun readOnlyFieldIsReadButNotEditable() {
        val state = TextFieldState("Sat 27 March")
        setField(state, readOnly = true)

        composeRule.onNodeWithText("Sat 27 March")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsEditable, false))
            .assert(hasText("Period start"))
    }

    @Test
    fun trailingActionIsALabelledButton() {
        val state = TextFieldState("Sat 27 March")
        var cleared = false
        setField(
            state,
            trailingAction = TextFieldAction(CycleIcons.Close, "Clear") {
                state.clearText()
                cleared = true
            }
        )

        composeRule.onNodeWithContentDescription("Clear")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertTrue(cleared)
        assertEquals("", state.text.toString())
    }

    @Test
    fun interactionSourceReceivesTheFieldsFocus() {
        val source = MutableInteractionSource()
        val interactions = mutableListOf<Interaction>()
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                LaunchedEffect(source) { source.interactions.collect { interactions += it } }
                CycleTextField(TextFieldState(), label = "Period start", interactionSource = source)
            }
        }

        field.requestFocus()
        composeRule.waitForIdle()

        assertTrue(interactions.any { it is FocusInteraction.Focus })
    }

    private fun setField(
        state: TextFieldState,
        supportingText: String? = null,
        errorMessage: String? = null,
        maxLength: Int? = null,
        enabled: Boolean = true,
        readOnly: Boolean = false,
        trailingAction: TextFieldAction? = null
    ) {
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                CycleTextField(
                    state = state,
                    label = "Period start",
                    placeholder = "Pick a day",
                    supportingText = supportingText,
                    errorMessage = errorMessage,
                    maxLength = maxLength,
                    enabled = enabled,
                    readOnly = readOnly,
                    trailingAction = trailingAction
                )
            }
        }
    }
}
