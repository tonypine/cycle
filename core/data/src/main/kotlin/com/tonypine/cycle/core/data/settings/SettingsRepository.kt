package com.tonypine.cycle.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tonypine.cycle.core.domain.CycleRules
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.CycleSettings
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Her settings, in DataStore: the usual lengths, whether setup is done, and the prompts she has
 * dismissed, each under the first day of its cycle. Until she sets them, the lengths are
 * [CycleRules.DEFAULT_CYCLE_LENGTH] and [CycleRules.DEFAULT_PERIOD_LENGTH].
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<CycleSettings> = dataStore.data.map { preferences ->
        CycleSettings(
            usualCycleLength = preferences[USUAL_CYCLE_LENGTH] ?: CycleRules.DEFAULT_CYCLE_LENGTH,
            usualPeriodLength = preferences[USUAL_PERIOD_LENGTH] ?: CycleRules.DEFAULT_PERIOD_LENGTH,
            setupDone = preferences[SETUP_DONE] ?: false,
            dismissedStillGoing = preferences[DISMISSED_STILL_GOING].toDates(),
            dismissedMissedPeriod = preferences[DISMISSED_MISSED_PERIOD].toDates()
        )
    }

    suspend fun setUsualCycleLength(days: Int) {
        require(days > 0) { "A cycle lasts at least a day, got $days" }
        dataStore.edit { it[USUAL_CYCLE_LENGTH] = days }
    }

    suspend fun setUsualPeriodLength(days: Int) {
        require(days > 0) { "A period lasts at least a day, got $days" }
        dataStore.edit { it[USUAL_PERIOD_LENGTH] = days }
    }

    suspend fun setSetupDone(done: Boolean) {
        dataStore.edit { it[SETUP_DONE] = done }
    }

    /** She answered [prompt]: it is not asked again in the same cycle. */
    suspend fun dismiss(prompt: CyclePrompt) {
        val key = when (prompt) {
            is CyclePrompt.StillGoing -> DISMISSED_STILL_GOING
            is CyclePrompt.MissedPeriod -> DISMISSED_MISSED_PERIOD
        }
        dataStore.edit { it[key] = it[key].orEmpty() + prompt.cycleStart.toString() }
    }

    private fun Set<String>?.toDates(): Set<LocalDate> = orEmpty().mapTo(mutableSetOf(), LocalDate::parse)

    private companion object {
        val USUAL_CYCLE_LENGTH = intPreferencesKey("usual_cycle_length")
        val USUAL_PERIOD_LENGTH = intPreferencesKey("usual_period_length")
        val SETUP_DONE = booleanPreferencesKey("setup_done")

        // ISO dates of the first day of each cycle whose prompt she dismissed.
        val DISMISSED_STILL_GOING = stringSetPreferencesKey("dismissed_still_going")
        val DISMISSED_MISSED_PERIOD = stringSetPreferencesKey("dismissed_missed_period")
    }
}
