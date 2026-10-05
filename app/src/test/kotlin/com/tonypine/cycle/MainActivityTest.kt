package com.tonypine.cycle

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The app as she opens it: on Today, four tabs, back from a tab to Today. Starts with no data. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun tab(label: String): SemanticsNodeInteraction = composeRule.onNode(hasText(label) and isTab)

    private fun waitForText(text: String, substring: Boolean = false) = composeRule.waitUntil(WAIT_MILLIS) {
        composeRule.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun `the app starts on Today with four tabs`() {
        composeRule.onAllNodes(isTab).assertCountEquals(4)
        tab("Today").assertIsSelected()
        listOf("Calendar", "History", "Settings").forEach { tab(it).assertIsNotSelected() }
        waitForText("No periods logged yet")
        composeRule.onNodeWithText("Log a period").assertIsDisplayed()
    }

    @Test
    fun `each tab opens and back returns to Today`() {
        tab("Calendar").performClick()
        tab("Calendar").assertIsSelected()
        waitForText("Predicted periods are estimates", substring = true)
        tab("History").performClick()
        tab("History").assertIsSelected()
        tab("Today").assertIsNotSelected()
        composeRule.onNodeWithText("This part of Cycle is on its way.").assertIsDisplayed()
        tab("Settings").performClick()
        tab("Settings").assertIsSelected()
        tab("Today").assertIsNotSelected()
        composeRule.onNodeWithText("More settings are on their way.").assertIsDisplayed()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        tab("Today").assertIsSelected()
        waitForText("No periods logged yet")
        assertFalse(composeRule.activity.isFinishing)

        // Back from Today leaves the app.
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        assertTrue(composeRule.activity.isFinishing)
    }

    @Test
    fun `a period logged today starts, ends and undoes on her phone's database`() {
        waitForText("Log a period")
        composeRule.onNodeWithText("Log a period").performClick()
        composeRule.onNode(hasContentDescription("today", substring = true) and hasClickAction()).performClick()
        composeRule.onNodeWithText("Log it").performClick()

        waitForText("Day 1")
        waitForText("Period started today")
        composeRule.onNode(isDialog()).assertDoesNotExist()

        composeRule.onNodeWithText("My period ended").performScrollTo().performClick()
        waitForText("Period ended today")
        composeRule.onNodeWithText("Undo").performScrollTo().performClick()
        waitForText("My period ended")
        composeRule.onNodeWithText("Period started today").assertExists()
    }

    private companion object {
        // Room and DataStore write on their own threads.
        const val WAIT_MILLIS = 5_000L
    }
}
