package com.tonypine.cycle.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository

/**
 * The data layer, wired by hand (`0001-stack.md`: no DI framework yet). Create one per process,
 * in the `Application`: DataStore allows a single instance per file.
 */
class CycleData(context: Context) {
    private val appContext = context.applicationContext

    val database: CycleDatabase by lazy { CycleDatabase.build(appContext) }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile(SETTINGS_FILE_NAME) }
        )
    }

    val dayLogRepository: DayLogRepository by lazy { DayLogRepository(database.dayLogDao()) }

    val cycleRepository: CycleRepository by lazy { CycleRepository(dayLogRepository, settingsRepository) }

    private companion object {
        const val SETTINGS_FILE_NAME = "cycle_settings"
    }
}
