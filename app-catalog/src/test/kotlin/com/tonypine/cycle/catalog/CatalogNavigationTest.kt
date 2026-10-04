package com.tonypine.cycle.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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
    fun backLinkReturnsToTheList() {
        composeRule.onNodeWithText("Colours").performClick()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Cycle catalog").assertIsDisplayed()
    }
}
