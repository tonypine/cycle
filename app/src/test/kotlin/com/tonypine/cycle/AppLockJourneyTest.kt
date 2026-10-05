package com.tonypine.cycle

import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import android.hardware.biometrics.BiometricPrompt
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowBiometricManager
import org.robolectric.shadows.ShadowBiometricPrompt

/**
 * "Lock Cycle" through the real activity and the phone's prompt, which Robolectric stands in for.
 * Starts past the welcome, with no data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp")
class AppLockJourneyTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val application = ApplicationProvider.getApplicationContext<CycleApplication>()
    private val settings = application.data.settingsRepository
    private var scenario: ActivityScenario<MainActivity>? = null
    private val isSwitch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

    @After
    fun tearDown() {
        scenario?.close()
    }

    /** The phone has a screen lock, or none at all. */
    private fun phoneHasScreenLock(has: Boolean) {
        val biometrics = Shadow.extract<ShadowBiometricManager>(
            application.getSystemService(BiometricManager::class.java)
        )
        biometrics.setCanAuthenticate(has)
        if (has) biometrics.setAuthenticatorType(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
    }

    private fun launch(lockOn: Boolean) {
        runBlocking {
            settings.setWelcomeDone(true)
            settings.setAppLock(lockOn)
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun waitForText(text: String) = composeRule.waitUntil(WAIT_MILLIS) {
        composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun shows(text: String) = composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()

    private fun awaitPrompt() = composeRule.waitUntil(WAIT_MILLIS) { ShadowBiometricPrompt.getCurrentPrompt() != null }

    @Test
    fun `with the lock on, a cold start shows only the lock and the prompt, then Today once she unlocks`() {
        phoneHasScreenLock(true)
        launch(lockOn = true)

        waitForText("Cycle is locked")
        awaitPrompt()
        assertFalse(shows(TODAY_EMPTY))
        assertFalse(shows("Today"))

        ShadowBiometricPrompt.authenticateCurrentSessionSuccessfully()

        waitForText(TODAY_EMPTY)
        assertFalse(shows("Cycle is locked"))
    }

    @Test
    fun `a cancelled prompt keeps the lock screen`() {
        phoneHasScreenLock(true)
        launch(lockOn = true)
        awaitPrompt()

        ShadowBiometricPrompt.authenticateCurrentSessionWithError(BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED, "")
        composeRule.waitForIdle()

        assertTrue(shows("Cycle is locked"))
        assertFalse(shows(TODAY_EMPTY))
    }

    @Test
    fun `turning the lock on in Settings asks for the phone's lock first`() {
        phoneHasScreenLock(true)
        launch(lockOn = false)
        waitForText(TODAY_EMPTY)
        composeRule.onNode(hasText("Settings") and hasRoleTab()).performClick()
        val lock = composeRule.onNode(hasText("Lock Cycle") and isSwitch)
        waitForText("Lock Cycle")

        lock.performScrollTo().performClick()
        awaitPrompt()
        lock.assertIsOff()
        ShadowBiometricPrompt.authenticateCurrentSessionSuccessfully()

        composeRule.waitUntil(WAIT_MILLIS) {
            composeRule.onAllNodes(hasText("Lock Cycle") and isSwitch and isOn()).fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(runBlocking { settings.appLock.first() })
        // She stays on Settings: turning the lock on doesn't lock her out of it.
        assertFalse(shows("Cycle is locked"))
    }

    @Test
    fun `a phone with no screen lock any more opens Cycle and turns the lock off with a note`() {
        phoneHasScreenLock(false)
        launch(lockOn = true)

        waitForText(TODAY_EMPTY)
        assertFalse(shows("Cycle is locked"))
        composeRule.onNode(hasText("Settings") and hasRoleTab()).performClick()
        waitForText("Lock Cycle is off")
        composeRule.onNode(hasText("Lock Cycle") and isSwitch).performScrollTo().assertIsOff()
        assertFalse(runBlocking { settings.appLock.first() })
    }

    private fun hasRoleTab() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private companion object {
        const val TODAY_EMPTY = "No periods logged yet"

        // Room and DataStore write on their own threads.
        const val WAIT_MILLIS = 5_000L
    }
}
