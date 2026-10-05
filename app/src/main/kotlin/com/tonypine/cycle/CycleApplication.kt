package com.tonypine.cycle

import android.app.Application
import com.tonypine.cycle.core.data.CycleData
import kotlinx.coroutines.MainScope

/**
 * Holds the one [CycleData] of the process: DataStore allows a single instance per file. Also holds
 * "Lock Cycle", whose unlocked state lasts as long as the process: a new process is a cold start.
 */
class CycleApplication : Application() {
    val data: CycleData by lazy { CycleData(this) }

    /** The phone's prompt, shown by whichever activity is attached. */
    val phoneLock: BiometricDeviceLock by lazy { BiometricDeviceLock(this) }

    val appLock: AppLock by lazy { AppLock(data.settingsRepository, phoneLock, MainScope()) }
}
