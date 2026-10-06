package com.tonypine.cycle

import android.app.Application
import com.tonypine.cycle.core.data.CycleData
import kotlinx.coroutines.MainScope

/**
 * Holds the one [CycleData] of the process: DataStore allows a single instance per file. Also holds
 * "Lock Cycle", whose unlocked state lasts as long as the process: a new process is a cold start, and
 * Cycle's language, read at the start of the process.
 *
 * Below Android 13 its own context stays in the phone's language: text is read in Compose or from the
 * activity, never from here.
 */
class CycleApplication : Application() {
    val data: CycleData by lazy { CycleData(this) }

    val language: AppLanguage by lazy { AppLanguage(data.languageRepository, MainScope()) }

    override fun onCreate() {
        super.onCreate()
        language.onCreate()
    }

    /** The phone's prompt, shown by whichever activity is attached. */
    val phoneLock: BiometricDeviceLock by lazy { BiometricDeviceLock(this) }

    val appLock: AppLock by lazy { AppLock(data.settingsRepository, phoneLock, MainScope()) }
}
