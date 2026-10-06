package com.tonypine.cycle.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.export.YourDataRepository
import com.tonypine.cycle.core.data.repository.ContraceptionRepository
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.AndroidPhoneLanguages
import com.tonypine.cycle.core.data.settings.LanguageRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import java.io.File

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

    val languageRepository: LanguageRepository by lazy {
        LanguageRepository(
            settingsRepository,
            AndroidPhoneLanguages(appContext),
            // Outside every backup, so a restore never brings it back (0008-languages.md).
            handedOver = File(appContext.noBackupFilesDir, LANGUAGE_HANDED_OVER_FILE_NAME)
        )
    }

    val dayLogRepository: DayLogRepository by lazy { DayLogRepository(database) }

    val contraceptionRepository: ContraceptionRepository by lazy { ContraceptionRepository(database) }

    val cycleRepository: CycleRepository by lazy {
        CycleRepository(dayLogRepository, settingsRepository, contraceptionRepository)
    }

    val yourDataRepository: YourDataRepository by lazy {
        YourDataRepository(database, settingsRepository, forgetLanguage = languageRepository::forget)
    }

    private companion object {
        const val SETTINGS_FILE_NAME = "cycle_settings"
        const val LANGUAGE_HANDED_OVER_FILE_NAME = "language_handed_over"
    }
}
