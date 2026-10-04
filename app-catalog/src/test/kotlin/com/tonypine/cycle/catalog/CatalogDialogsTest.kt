package com.tonypine.cycle.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowDialog

/** The Dialogs page opens each dialog, and its switch turns back and scrim dismissal off. */
@RunWith(RobolectricTestRunner::class)
class CatalogDialogsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<CatalogActivity>()

    @Before
    fun openDialogsPage() {
        composeRule.onNodeWithText("Dialogs").performScrollTo().performClick()
    }

    @Test
    fun eachButtonOpensItsDialog() {
        listOf("Alert" to "Turn on reminders?", "Destructive" to "Delete this day?", "Text field" to "Add a note")
            .forEach { (button, title) ->
                composeRule.onNodeWithText(button).performScrollTo().performClick()
                composeRule.onNodeWithText(title).assertIsDisplayed()
                pressBackInDialog()
                composeRule.onNode(isDialog()).assertDoesNotExist()
            }
        composeRule.onNodeWithText("Dismissed.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theTextFieldDialogSavesWhatWasTyped() {
        composeRule.onNodeWithText("Text field").performScrollTo().performClick()
        composeRule.onNodeWithText("Note").performTextInput("Synthetic note")
        composeRule.onNodeWithText("Save").performClick()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("Saved: Synthetic note").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun backDoesNotCloseADialogWhenDismissalIsOff() {
        composeRule.onNodeWithText("Close on back and scrim tap").performScrollTo().performClick()
        composeRule.onNodeWithText("Alert").performScrollTo().performClick()
        pressBackInDialog()
        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText("Not now").performClick()
        composeRule.onNode(isDialog()).assertDoesNotExist()
        composeRule.onNodeWithText("Not now.").performScrollTo().assertIsDisplayed()
    }

    private fun pressBackInDialog() {
        // Let the click open the dialog's window first.
        composeRule.waitForIdle()
        composeRule.runOnUiThread {
            // Dialog.onBackPressed hands the press to the dialog's OnBackPressedDispatcher.
            @Suppress("DEPRECATION")
            ShadowDialog.getLatestDialog().onBackPressed()
        }
        composeRule.waitForIdle()
    }
}
