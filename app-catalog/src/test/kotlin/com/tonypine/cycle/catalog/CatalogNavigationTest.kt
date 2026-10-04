package com.tonypine.cycle.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class CatalogNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<CatalogActivity>()

    @Test
    fun everySectionOpensFromTheListAndBackReturnsToIt() {
        CatalogSections.forEach { section ->
            composeRule.onNodeWithText(section.title).performScrollTo().performClick()
            composeRule.onNodeWithText(section.description).assertIsDisplayed()
            composeRule.onNodeWithText("Cycle catalog").assertDoesNotExist()

            composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            composeRule.onNodeWithText("Cycle catalog").assertIsDisplayed()
        }
    }

    @Test
    fun theBottomSheetPageOpensADemoSheetWithATextField() {
        composeRule.onNodeWithText("Bottom sheet").performScrollTo().performClick()
        composeRule.onNodeWithText("Open the sheet").performClick()

        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNode(hasSetTextAction() and hasText("Notes")).assertExists()
        composeRule.onNodeWithText("Nah").performScrollTo().performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("Closed 1 time.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theAppBarsDemoOpensFullScreenAndBackReturnsToItsPage() {
        composeRule.onNodeWithText("App bars").performScrollTo().performClick()
        composeRule.onNodeWithText("Open the full-screen demo").performClick()
        composeRule.onNodeWithText("Sample card 1").assertIsDisplayed()
        composeRule.onNodeWithText("Open the full-screen demo").assertDoesNotExist()

        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("Open the full-screen demo").assertIsDisplayed()

        composeRule.onNodeWithText("Open the full-screen demo").performClick()
        composeRule.onNodeWithContentDescription("Back to the catalog").performClick()
        composeRule.onNodeWithText("Open the full-screen demo").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-rGB")
    fun theCalendarPageShowsAMondayFirstMonthThatMovesAndTheMvpLegend() {
        composeRule.onNodeWithText("Calendar").performScrollTo().performClick()
        composeRule.onNodeWithText("March 2027").assertIsDisplayed()
        val weekdays = listOf("Monday", "Sunday").map {
            composeRule.onAllNodesWithContentDescription(it).onFirst().getBoundsInRoot().left
        }
        assertTrue("Expected Monday before Sunday, at $weekdays", weekdays[0] < weekdays[1])
        composeRule.onNodeWithContentDescription("4 March, period").assertExists()
        composeRule.onNodeWithContentDescription("29 March, predicted period").assertExists()
        composeRule.onAllNodesWithContentDescription("20 March, today").onFirst().assertExists()

        composeRule.onNodeWithContentDescription("Next month").performClick()
        composeRule.onNodeWithText("April 2027").assertIsDisplayed()

        listOf("Period", "Predicted period", "Today").forEach { composeRule.onNodeWithText(it).assertExists() }
        composeRule.onNodeWithText("Estimated fertile window").assertDoesNotExist()
        composeRule.onNodeWithText("Estimated ovulation").assertDoesNotExist()
    }

    @Test
    fun backLinkReturnsToTheList() {
        composeRule.onNodeWithText("Colours").performClick()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Cycle catalog").assertIsDisplayed()
    }
}
