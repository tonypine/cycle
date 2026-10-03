package com.tonypine.cycle

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchShowsHomeScreen() {
        composeRule.onNodeWithText("Cycle").assertIsDisplayed()
        composeRule.onNodeWithText("Nothing logged yet.").assertIsDisplayed()
    }
}
