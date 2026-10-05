package com.tonypine.cycle

import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.designsystem.CycleTheme
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** What shows behind "Lock Cycle": nothing of her data until she unlocks. Synthetic content only. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h640dp")
class LockGateTest {
    @get:Rule(order = 0)
    val folder = TemporaryFolder()

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val storage = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val phoneLock = FakeDeviceLock()
    private var now = 0L

    @After
    fun tearDown() {
        scope.cancel()
        storage.cancel()
    }

    private fun settings(lockOn: Boolean): SettingsRepository {
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = storage) { File(folder.root, "test.preferences_pb") }
        )
        runBlocking { settings.setAppLock(lockOn) }
        return settings
    }

    private fun show(appLock: AppLock) {
        composeRule.setContent {
            CycleTheme(reduceMotion = true) {
                LockGate(appLock) { BasicText(TODAY_CONTENT) }
            }
        }
        composeRule.runOnUiThread { appLock.onForeground() }
    }

    private fun waitForText(text: String) = composeRule.waitUntil(WAIT_MILLIS) {
        composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun `with the lock on, only the lock screen shows, and the prompt opens on its own`() {
        show(AppLock(settings(lockOn = true), phoneLock, scope, clock = { now }))

        waitForText("Cycle is locked")
        composeRule.onNode(hasText("Cycle is locked") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Open it with your fingerprint, face or screen lock.").assertIsDisplayed()
        composeRule.onNodeWithText(TODAY_CONTENT).assertDoesNotExist()
        composeRule.waitUntil(WAIT_MILLIS) { phoneLock.prompting }
        assertEquals(1, phoneLock.prompts)

        composeRule.runOnUiThread { phoneLock.answer(unlocked = true) }

        waitForText(TODAY_CONTENT)
        composeRule.onNodeWithText("Cycle is locked").assertDoesNotExist()
    }

    @Test
    fun `cancelled, the lock screen stays, and Unlock opens the prompt again`() {
        show(AppLock(settings(lockOn = true), phoneLock, scope, clock = { now }))
        composeRule.waitUntil(WAIT_MILLIS) { phoneLock.prompting }

        composeRule.runOnUiThread { phoneLock.answer(unlocked = false) }
        composeRule.onNode(hasText("Unlock") and hasClickAction()).performClick()

        assertTrue(phoneLock.prompting)
        assertEquals(2, phoneLock.prompts)
        composeRule.onNodeWithText(TODAY_CONTENT).assertDoesNotExist()
    }

    @Test
    fun `locking again after a long time away takes her data off the screen`() {
        val appLock = AppLock(settings(lockOn = true), phoneLock, scope, clock = { now })
        show(appLock)
        composeRule.waitUntil(WAIT_MILLIS) { phoneLock.prompting }
        composeRule.runOnUiThread { phoneLock.answer(unlocked = true) }
        waitForText(TODAY_CONTENT)

        composeRule.runOnUiThread {
            appLock.onBackground()
            now += AppLock.GRACE_MILLIS + 1
            appLock.onForeground()
        }

        waitForText("Cycle is locked")
        composeRule.onNodeWithText(TODAY_CONTENT).assertDoesNotExist()
    }

    @Test
    fun `with the lock off, the app shows straight away`() {
        show(AppLock(settings(lockOn = false), phoneLock, scope, clock = { now }))

        waitForText(TODAY_CONTENT)
        composeRule.onNodeWithText("Cycle is locked").assertDoesNotExist()
        assertEquals(0, phoneLock.prompts)
    }

    @Test
    fun `until the setting is read, nothing shows`() {
        val unread = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { awaitCancellation() }

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences) = awaitCancellation()
        }
        show(AppLock(SettingsRepository(unread), phoneLock, scope, clock = { now }))
        composeRule.waitForIdle()

        composeRule.onNodeWithText(TODAY_CONTENT).assertDoesNotExist()
        composeRule.onNodeWithText("Cycle is locked").assertDoesNotExist()
    }

    @Test
    fun `back from the lock screen leaves the app`() {
        show(AppLock(settings(lockOn = true), phoneLock, scope, clock = { now }))
        waitForText("Cycle is locked")

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        assertTrue(composeRule.activity.isFinishing)
        assertFalse(composeRule.onAllNodes(hasText(TODAY_CONTENT)).fetchSemanticsNodes().isNotEmpty())
    }

    private companion object {
        const val TODAY_CONTENT = "Day 12 of a synthetic cycle"

        // DataStore writes on its own thread.
        const val WAIT_MILLIS = 5_000L
    }
}
