package com.tonypine.cycle.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.tonypine.cycle.core.model.LogCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What TalkBack reads on What to log, and what each switch does. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class WhatToLogScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private var state by mutableStateOf<WhatToLogUiState>(WhatToLogUiState.Ready(setOf(LogCategory.SEX)))

    private fun show() {
        composeRule.setContent {
            Themed {
                WhatToLogScreen(
                    state,
                    onShownChange = { category, shown -> calls += "$category $shown" },
                    onBack = { calls += "back" }
                )
            }
        }
    }

    private val isSwitch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

    private fun switchState(on: Boolean) =
        SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState(on))

    @Test
    fun `the title is a heading and back leaves`() {
        show()

        composeRule.onNode(hasText("What to log") and isHeading()).assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(listOf("back"), calls)
    }

    @Test
    fun `flow and spotting are always on`() {
        show()

        composeRule.onNode(hasText("Flow and spotting") and hasText("Always on: estimates need it"))
            .assert(isSwitch)
            .assert(switchState(true))
            .assertIsNotEnabled()
    }

    @Test
    fun `each category is a switch, on unless she hid it, and tapping it turns it the other way`() {
        show()

        listOf("Pain", "Body", "Mood", "Energy", "Sleep", "Notes").forEach { title ->
            composeRule.onNode(hasText(title) and isSwitch).assert(switchState(true))
        }
        composeRule.onNode(hasText("Sex") and isSwitch).assert(switchState(false)).performScrollTo().performClick()
        composeRule.onNode(hasText("Notes") and isSwitch).performScrollTo().performClick()

        assertEquals(listOf("SEX true", "NOTES false"), calls)
    }
}
