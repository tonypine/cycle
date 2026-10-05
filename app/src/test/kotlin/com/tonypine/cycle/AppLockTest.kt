package com.tonypine.cycle

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.settings.SettingsRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/** When Cycle locks and unlocks, on a fake clock and a fake phone lock. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AppLockTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val phoneLock = FakeDeviceLock()
    private var now = 1_000_000L
    private lateinit var settings: SettingsRepository

    private suspend fun TestScope.appLock(lockOn: Boolean): AppLock {
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        settings.setAppLock(lockOn)
        val scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        return AppLock(settings, phoneLock, scope, clock = { now })
    }

    private suspend fun AppLock.awaitLocked(): Boolean = locked.first { it != null }!!

    /** A cold start: the activity starts, and the lock screen shows if the app is locked. */
    private suspend fun AppLock.start() {
        onForeground()
        if (awaitLocked()) onLockScreenShown()
    }

    /** What Settings does to turn the lock on or off: the prompt, then the setting. */
    private fun TestScope.turnLockFromSettings(appLock: AppLock, on: Boolean) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            if (appLock.authenticate()) settings.setAppLock(on)
        }
    }

    /** She leaves Cycle for [millis], then comes back. */
    private suspend fun AppLock.leaveFor(millis: Long) {
        onBackground()
        now += millis
        start()
    }

    @Test
    fun `with the lock off, Cycle opens straight away and never asks`() = runTest {
        val appLock = appLock(lockOn = false)

        appLock.start()
        appLock.leaveFor(60_000)

        assertFalse(appLock.awaitLocked())
        assertEquals(0, phoneLock.prompts)
    }

    @Test
    fun `a cold start is locked, and the prompt opens on its own, until she unlocks`() = runTest {
        val appLock = appLock(lockOn = true)

        appLock.start()

        assertTrue(appLock.awaitLocked())
        assertTrue(phoneLock.prompting)
        phoneLock.answer(unlocked = true)
        assertEquals(false, appLock.locked.value)
    }

    @Test
    fun `a cancelled prompt stays locked, and opens again only from Unlock`() = runTest {
        val appLock = appLock(lockOn = true)
        appLock.start()

        phoneLock.answer(unlocked = false)
        assertEquals(true, appLock.locked.value)
        // The lock screen showing again, as after a rotation, does not open it on its own.
        appLock.onLockScreenShown()
        assertFalse(phoneLock.prompting)

        appLock.unlock()
        phoneLock.answer(unlocked = true)
        assertEquals(false, appLock.locked.value)
        assertEquals(2, phoneLock.prompts)
    }

    @Test
    fun `Unlock while the prompt shows asks once`() = runTest {
        val appLock = appLock(lockOn = true)
        appLock.start()

        appLock.unlock()
        appLock.unlock()

        assertEquals(1, phoneLock.prompts)
    }

    @Test
    fun `back within 30 seconds, it doesn't ask again`() = runTest {
        val appLock = appLock(lockOn = true)
        appLock.start()
        phoneLock.answer(unlocked = true)

        appLock.leaveFor(AppLock.GRACE_MILLIS)

        assertEquals(false, appLock.locked.value)
        assertEquals(1, phoneLock.prompts)
    }

    @Test
    fun `back after more than 30 seconds, it locks again and asks`() = runTest {
        val appLock = appLock(lockOn = true)
        appLock.start()
        phoneLock.answer(unlocked = true)

        appLock.leaveFor(AppLock.GRACE_MILLIS + 1)

        assertEquals(true, appLock.locked.value)
        assertTrue(phoneLock.prompting)
        phoneLock.answer(unlocked = true)
        assertEquals(false, appLock.locked.value)
    }

    @Test
    fun `quick switches don't add up`() = runTest {
        val appLock = appLock(lockOn = true)
        appLock.start()
        phoneLock.answer(unlocked = true)

        repeat(3) { appLock.leaveFor(20_000) }

        assertEquals(false, appLock.locked.value)
    }

    @Test
    fun `the phone's PIN screen covering Cycle while she types is not time away`() = runTest {
        val appLock = appLock(lockOn = true)
        appLock.start()
        phoneLock.answer(unlocked = true)
        val shown = mutableListOf<Boolean?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { appLock.locked.collect { shown += it } }

        // Settings turns the lock off: the phone's PIN screen stops the activity while she types.
        turnLockFromSettings(appLock, on = false)
        appLock.onBackground()
        now += 60_000
        appLock.onForeground()
        phoneLock.answer(unlocked = true)

        assertFalse(settings.appLock.first { !it })
        // The lock screen never replaced Settings.
        assertEquals(listOf<Boolean?>(false), shown)
    }

    @Test
    fun `a prompt cancelled after a long time away locks`() = runTest {
        val appLock = appLock(lockOn = true)
        appLock.start()
        phoneLock.answer(unlocked = true)
        // Settings asks to turn the lock off, and she leaves Cycle with the prompt up.
        turnLockFromSettings(appLock, on = false)
        appLock.onBackground()
        now += 60_000
        appLock.onForeground()

        phoneLock.answer(unlocked = false)

        assertEquals(true, appLock.locked.value)
    }

    @Test
    fun `turning the lock on in Settings doesn't lock her out of the screen she is on`() = runTest {
        val appLock = appLock(lockOn = false)
        appLock.start()
        appLock.leaveFor(60_000)

        turnLockFromSettings(appLock, on = true)
        phoneLock.answer(unlocked = true)

        assertTrue(settings.appLock.first { it })
        assertEquals(false, appLock.locked.value)
    }

    @Test
    fun `with no screen lock on the phone any more, the lock turns off with a note and Cycle opens`() = runTest {
        val appLock = appLock(lockOn = true)
        phoneLock.available = false

        appLock.start()

        assertFalse(appLock.awaitLocked())
        assertEquals(0, phoneLock.prompts)
        assertFalse(settings.appLock.first { !it })
        assertTrue(settings.appLockTurnedOff.first())
    }

    @Test
    fun `the screen lock removed while Cycle was away turns the lock off on the way back`() = runTest {
        val appLock = appLock(lockOn = true)
        appLock.start()
        phoneLock.answer(unlocked = true)

        phoneLock.available = false
        appLock.leaveFor(60_000)

        assertEquals(false, appLock.locked.value)
        assertTrue(settings.appLockTurnedOff.first { it })
        assertEquals(1, phoneLock.prompts)
    }

    @Test
    fun `Android 11 and later take a strong biometric or the screen lock, Android 10 any biometric or it`() {
        assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, BiometricDeviceLock.authenticators(30))
        assertEquals(BIOMETRIC_STRONG or DEVICE_CREDENTIAL, BiometricDeviceLock.authenticators(37))
        assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, BiometricDeviceLock.authenticators(29))
    }
}
