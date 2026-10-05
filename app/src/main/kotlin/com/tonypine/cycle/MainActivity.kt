package com.tonypine.cycle

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.tonypine.cycle.core.designsystem.CycleTheme
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.launch

/**
 * The one activity. A [FragmentActivity], as the phone's fingerprint, face or screen-lock prompt
 * needs. With "Lock Cycle" on, the app shows the lock screen until she unlocks, and the recent-apps
 * screen shows none of it.
 */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as CycleApplication
        val data = application.data
        val appLock = application.appLock
        application.phoneLock.attach(this)
        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = appLock.onForeground()

                override fun onStop(owner: LifecycleOwner) = appLock.onBackground()
            }
        )
        // The recent-apps snapshot is on by default: only a lock that is or was on changes it.
        lifecycleScope.launch {
            data.settingsRepository.appLock.dropWhile { on -> !on }.collect { on -> hideInRecents(on) }
        }
        setContent {
            CycleTheme {
                LockGate(appLock) {
                    CycleApp(data, deviceLock = appLock)
                }
            }
        }
    }
}
