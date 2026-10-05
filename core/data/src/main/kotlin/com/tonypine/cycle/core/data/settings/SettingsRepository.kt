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
import com.tonypine.cycle.core.model.LogCategory
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Her settings, in DataStore: the usual lengths, whether setup is done, the prompts she has
 * dismissed, each under the first day of its cycle, and the day log categories she hid. Until she
 * sets them, the lengths are [CycleRules.DEFAULT_CYCLE_LENGTH] and [CycleRules.DEFAULT_PERIOD_LENGTH],
 * and every category shows.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<CycleSettings> = dataStore.data.map { preferences ->
        CycleSettings(
            usualCycleLength = preferences[USUAL_CYCLE_LENGTH] ?: CycleRules.DEFAULT_CYCLE_LENGTH,
            usualPeriodLength = preferences[USUAL_PERIOD_LENGTH] ?: CycleRules.DEFAULT_PERIOD_LENGTH,
            setupDone = preferences[SETUP_DONE] ?: false,
            dismissedStillGoing = preferences[DISMISSED_STILL_GOING].toDates(),
            dismissedMissedPeriod = preferences[DISMISSED_MISSED_PERIOD].toDates(),
            hiddenCategories = preferences[HIDDEN_CATEGORIES].orEmpty()
                .mapNotNullTo(mutableSetOf()) { code -> CATEGORY_CODES.entries.firstOrNull { it.value == code }?.key }
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

    /**
     * "What to log": shows or hides [category] in the day log and on Today. Hiding it keeps what she
     * logged in it.
     */
    suspend fun setCategoryShown(category: LogCategory, shown: Boolean) {
        val code = CATEGORY_CODES.getValue(category)
        dataStore.edit { preferences ->
            val hidden = preferences[HIDDEN_CATEGORIES].orEmpty()
            preferences[HIDDEN_CATEGORIES] = if (shown) hidden - code else hidden + code
        }
    }

    private fun Set<String>?.toDates(): Set<LocalDate> = orEmpty().mapTo(mutableSetOf(), LocalDate::parse)

    private companion object {
        val USUAL_CYCLE_LENGTH = intPreferencesKey("usual_cycle_length")
        val USUAL_PERIOD_LENGTH = intPreferencesKey("usual_period_length")
        val SETUP_DONE = booleanPreferencesKey("setup_done")

        // ISO dates of the first day of each cycle whose prompt she dismissed.
        val DISMISSED_STILL_GOING = stringSetPreferencesKey("dismissed_still_going")
        val DISMISSED_MISSED_PERIOD = stringSetPreferencesKey("dismissed_missed_period")

        // The codes of the categories she hid. A code this version does not know is ignored.
        val HIDDEN_CATEGORIES = stringSetPreferencesKey("hidden_log_categories")
        val CATEGORY_CODES = mapOf(
            LogCategory.PAIN to "pain",
            LogCategory.BODY to "body",
            LogCategory.MOOD to "mood",
            LogCategory.ENERGY to "energy",
            LogCategory.SLEEP to "sleep",
            LogCategory.SEX to "sex",
            LogCategory.NOTES to "notes"
        )
    }
}
