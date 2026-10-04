package com.tonypine.cycle.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.settings.SettingsRepository
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope

// Synthetic data only: made-up dates in 2027, never anyone's real cycle.

fun day(iso: String): LocalDate = LocalDate.parse(iso)

fun inMemoryDatabase(): CycleDatabase = Room
    .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CycleDatabase::class.java)
    .allowMainThreadQueries()
    .build()

fun settingsRepository(directory: File, scope: CoroutineScope) = SettingsRepository(
    PreferenceDataStoreFactory.create(scope = scope) { File(directory, "test.preferences_pb") }
)
