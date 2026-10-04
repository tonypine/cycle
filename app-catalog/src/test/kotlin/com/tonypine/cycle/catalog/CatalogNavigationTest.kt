package com.tonypine.cycle.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    fun backLinkReturnsToTheList() {
        composeRule.onNodeWithText("Colours").performClick()
        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("Cycle catalog").assertIsDisplayed()
    }
}
