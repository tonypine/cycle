package com.tonypine.cycle

import android.os.SystemClock
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.ui.DeviceLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * "Lock Cycle": whether the app shows her data or the lock screen. With the lock on, Cycle is locked
 * on a cold start, and again when she comes back after more than [GRACE_MILLIS] away, so a quick
 * switch to another app does not ask again. It unlocks through [phoneLock], the phone's fingerprint,
 * face or screen lock.
 *
 * Every prompt goes through here, Settings' too: the phone's PIN screen covers the app while she types,
 * and that is not time away. A phone that can no longer ask (no screen lock, no fingerprint or face)
 * gets the lock turned off, with a note in Settings, so she is never locked out of her data.
 *
 * One per process, in [CycleApplication]: a new process is a cold start. Call it on the main thread.
 *
 * @param clock milliseconds that keep counting while the phone sleeps, as `elapsedRealtime` does.
 */
class AppLock(
    private val settings: SettingsRepository,
    private val phoneLock: DeviceLock,
    private val scope: CoroutineScope,
    private val clock: () -> Long = SystemClock::elapsedRealtime
) : DeviceLock {
    private val unlocked = MutableStateFlow(false)

    // When the app last went to the background, until she is back.
    private var awaySince: Long? = null
    private var foreground = false
    private var prompting = false

    // The lock screen opens the prompt on its own once for each time Cycle locks.
    private var promptOnShow = true

    /** Null until the setting is read, then whether the lock screen shows instead of the app. */
    val locked: StateFlow<Boolean?> = combine(settings.appLock, unlocked) { on, unlocked ->
        on && !unlocked && phoneLock.canAuthenticate()
    }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * The app came to the front, on a cold start or a return: Cycle locks again if she was away for
     * more than the grace period, and turns the lock off if the phone can no longer ask for it.
     */
    fun onForeground() {
        foreground = true
        scope.launch {
            if (settings.appLock.first() && !phoneLock.canAuthenticate()) settings.turnOffAppLockWithoutScreenLock()
        }
        if (!prompting) lockIfAwayTooLong()
    }

    /** The app went to the background: the grace period starts. */
    fun onBackground() {
        foreground = false
        awaySince = clock()
    }

    /** The lock screen showed: the first time for each lock, it opens the prompt on its own. */
    fun onLockScreenShown() {
        if (!promptOnShow) return
        promptOnShow = false
        unlock()
    }

    /** "Unlock": opens the phone's prompt, unless it is showing already. */
    fun unlock() {
        if (prompting) return
        // Undispatched, so a second tap before the prompt shows finds it prompting.
        scope.launch(start = CoroutineStart.UNDISPATCHED) { authenticate() }
    }

    override fun canAuthenticate(): Boolean = phoneLock.canAuthenticate()

    /** The phone's prompt; once she unlocks, the app is unlocked too and the grace period restarts. */
    override suspend fun authenticate(): Boolean {
        prompting = true
        val success = try {
            phoneLock.authenticate()
        } finally {
            prompting = false
        }
        if (success) {
            awaySince = null
            unlocked.value = true
        } else if (foreground) {
            // She came back while the prompt was up, and cancelled it.
            lockIfAwayTooLong()
        }
        return success
    }

    private fun lockIfAwayTooLong() {
        val since = awaySince ?: return
        awaySince = null
        if (clock() - since > GRACE_MILLIS) {
            unlocked.value = false
            promptOnShow = true
        }
    }

    companion object {
        /** How long she can be away without Cycle asking again: a quick app switch. */
        const val GRACE_MILLIS = 30_000L
    }
}
