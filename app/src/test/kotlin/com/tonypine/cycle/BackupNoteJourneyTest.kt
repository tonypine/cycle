package com.tonypine.cycle

import android.app.KeyguardManager
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The note in Settings that Cycle isn't backed up, as the ticket's walkthrough: on a phone with no
 * screen lock it shows; once she sets a lock in the phone's settings and comes back, it is gone. The
 * phone's screen lock is Robolectric's `KeyguardManager`. Synthetic data only: one made-up period in
 * 2027.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp")
class BackupNoteJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val data get() = (composeRule.activity.application as CycleApplication).data
    private val keyguard get() = shadowOf(composeRule.activity.getSystemService(KeyguardManager::class.java))
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun tab(label: String) = composeRule.onNode(hasText(label) and isTab)

    private fun waitFor(matcher: SemanticsMatcher) =
        composeRule.waitUntil(WAIT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun openSettings(screenLock: Boolean) {
        keyguard.setIsDeviceSecure(screenLock)
        // Her log skips the first-run welcome.
        runBlocking { data.dayLogRepository.save(DayLog(LocalDate.of(2027, 9, 2), FlowLevel.MEDIUM)) }
        composeRule.setContent {
            CycleTheme(reduceMotion = true) { CycleApp(data, today = { LocalDate.of(2027, 9, 10) }) }
        }
        waitFor(hasText("Settings") and isTab)
        tab("Settings").performClick()
        waitFor(hasText("Your data") and isHeading())
    }

    /** She leaves Cycle for the phone's settings, then comes back. */
    private fun leaveAndReturn() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    @Test
    fun `without a screen lock Settings says Cycle isn't backed up, and setting one hides it on return`() {
        openSettings(screenLock = false)

        // 1. A note says her data is not backed up until the phone has a screen lock.
        composeRule.onNodeWithText("Cycle isn't backed up").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("only when this phone has a screen lock", substring = true).assertIsDisplayed()

        // 2. She sets a screen lock in the phone's settings and returns to Cycle: the note is gone.
        keyguard.setIsDeviceSecure(true)
        leaveAndReturn()
        composeRule.onNodeWithText("Cycle isn't backed up").assertDoesNotExist()
    }

    @Test
    fun `with a screen lock there is no note, and removing the lock brings it back on return`() {
        openSettings(screenLock = true)
        composeRule.onNodeWithText("Everything stays on this phone").assertExists()
        composeRule.onNodeWithText("Cycle isn't backed up").assertDoesNotExist()

        keyguard.setIsDeviceSecure(false)
        leaveAndReturn()
        composeRule.onNodeWithText("Cycle isn't backed up").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `Hide for now hides the note while Cycle stays open, even across tabs`() {
        openSettings(screenLock = false)

        composeRule.onNode(hasText("Hide for now") and isButton).performScrollTo().performClick()
        composeRule.onNodeWithText("Cycle isn't backed up").assertDoesNotExist()

        tab("Today").performClick()
        tab("Settings").performClick()
        waitFor(hasText("Your data") and isHeading())
        leaveAndReturn()
        composeRule.onNodeWithText("Cycle isn't backed up").assertDoesNotExist()
    }

    private companion object {
        // Room and DataStore write on their own threads.
        const val WAIT_MILLIS = 5_000L
    }
}
