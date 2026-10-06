package com.tonypine.cycle.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tonypine.cycle.core.domain.CycleRules
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.LogCategory
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Her settings, in DataStore: the usual lengths, whether setup is done, whether she is past the
 * welcome, the prompts she has dismissed, each under the first day of its cycle, the day log
 * categories she hid, the day she last exported her data and whether the app lock is on. Until she sets them, the lengths are
 * [CycleRules.DEFAULT_CYCLE_LENGTH] and [CycleRules.DEFAULT_PERIOD_LENGTH], and every category shows.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val settings: Flow<CycleSettings> = dataStore.data.map { preferences ->
        CycleSettings(
            usualCycleLength = preferences[USUAL_CYCLE_LENGTH] ?: CycleRules.DEFAULT_CYCLE_LENGTH,
            usualPeriodLength = preferences[USUAL_PERIOD_LENGTH] ?: CycleRules.DEFAULT_PERIOD_LENGTH,
            setupDone = preferences[SETUP_DONE] ?: false,
            dismissedStillGoing = preferences[DISMISSED_STILL_GOING].toDates(),
            dismissedMissedPeriod = preferences[DISMISSED_MISSED_PERIOD].toDates(),
            dismissedCopperIudNote = preferences[DISMISSED_COPPER_IUD_NOTE].toDates(),
            hiddenCategories = preferences[HIDDEN_CATEGORIES].orEmpty()
                .mapNotNullTo(mutableSetOf()) { code -> CATEGORY_CODES.entries.firstOrNull { it.value == code }?.key }
        )
    }

    /**
     * She finished or skipped the first-run welcome, so the app opens on Today. Kept apart from
     * [CycleSettings.setupDone]: skipping the welcome leaves the estimates on the typical lengths.
     */
    val welcomeDone: Flow<Boolean> = dataStore.data.map { it[WELCOME_DONE] ?: false }.distinctUntilChanged()

    suspend fun setWelcomeDone(done: Boolean) {
        dataStore.edit { it[WELCOME_DONE] = done }
    }

    /** Setup's lengths, and setup marked done, in one write: estimates never mix the two. */
    suspend fun saveSetup(cycleLength: Int, periodLength: Int) {
        require(cycleLength > 0) { "A cycle lasts at least a day, got $cycleLength" }
        require(periodLength > 0) { "A period lasts at least a day, got $periodLength" }
        dataStore.edit {
            it[USUAL_CYCLE_LENGTH] = cycleLength
            it[USUAL_PERIOD_LENGTH] = periodLength
            it[SETUP_DONE] = true
        }
    }

    /**
     * Setup's lengths from a Cycle export, saved as [saveSetup] does, unless she has already done
     * setup on this phone: then her lengths stay.
     */
    suspend fun restoreSetup(cycleLength: Int, periodLength: Int) {
        require(cycleLength > 0) { "A cycle lasts at least a day, got $cycleLength" }
        require(periodLength > 0) { "A period lasts at least a day, got $periodLength" }
        dataStore.edit {
            if (it[SETUP_DONE] != true) {
                it[USUAL_CYCLE_LENGTH] = cycleLength
                it[USUAL_PERIOD_LENGTH] = periodLength
                it[SETUP_DONE] = true
            }
        }
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

    /** "Got it" on the copper IUD fitted on [fitted]: its "Periods can be heavier at first" card goes. */
    suspend fun dismissCopperIudNote(fitted: LocalDate) {
        dataStore.edit { it[DISMISSED_COPPER_IUD_NOTE] = it[DISMISSED_COPPER_IUD_NOTE].orEmpty() + fitted.toString() }
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

    /** The day she last exported her data, or null if she never has. */
    val lastExported: Flow<LocalDate?> =
        dataStore.data.map { preferences -> preferences[LAST_EXPORTED]?.let(LocalDate::parse) }.distinctUntilChanged()

    suspend fun setLastExported(date: LocalDate) {
        dataStore.edit { it[LAST_EXPORTED] = date.toString() }
    }

    /** "Lock Cycle": the app opens only after the phone's fingerprint, face or screen lock. Off by default. */
    val appLock: Flow<Boolean> = dataStore.data.map { it[APP_LOCK] ?: false }.distinctUntilChanged()

    suspend fun setAppLock(on: Boolean) {
        dataStore.edit {
            it[APP_LOCK] = on
            it.remove(APP_LOCK_TURNED_OFF)
        }
    }

    /**
     * Whether Cycle turned its lock off itself, because the phone no longer has a screen lock or a
     * fingerprint or face to ask for, and she has not seen the note that says so yet.
     */
    val appLockTurnedOff: Flow<Boolean> =
        dataStore.data.map { it[APP_LOCK_TURNED_OFF] ?: false }.distinctUntilChanged()

    /** Turns the lock off because the phone can't ask for it any more, and leaves her a note. */
    suspend fun turnOffAppLockWithoutScreenLock() {
        dataStore.edit {
            if (it[APP_LOCK] == true) {
                it[APP_LOCK] = false
                it[APP_LOCK_TURNED_OFF] = true
            }
        }
    }

    /** She read the note that the lock was turned off. */
    suspend fun dismissAppLockTurnedOff() {
        dataStore.edit { it.remove(APP_LOCK_TURNED_OFF) }
    }

    /** "Delete everything": every setting goes, so the app opens on the welcome with the defaults. */
    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private fun Set<String>?.toDates(): Set<LocalDate> = orEmpty().mapTo(mutableSetOf(), LocalDate::parse)

    private companion object {
        val USUAL_CYCLE_LENGTH = intPreferencesKey("usual_cycle_length")
        val USUAL_PERIOD_LENGTH = intPreferencesKey("usual_period_length")
        val SETUP_DONE = booleanPreferencesKey("setup_done")
        val WELCOME_DONE = booleanPreferencesKey("welcome_done")

        // The ISO date of her last export.
        val LAST_EXPORTED = stringPreferencesKey("last_exported")

        val APP_LOCK = booleanPreferencesKey("app_lock")
        val APP_LOCK_TURNED_OFF = booleanPreferencesKey("app_lock_turned_off")

        // ISO dates of the first day of each cycle whose prompt she dismissed.
        val DISMISSED_STILL_GOING = stringSetPreferencesKey("dismissed_still_going")
        val DISMISSED_MISSED_PERIOD = stringSetPreferencesKey("dismissed_missed_period")

        // ISO fitting dates of the copper IUDs whose heavier-periods card she dismissed.
        val DISMISSED_COPPER_IUD_NOTE = stringSetPreferencesKey("dismissed_copper_iud_note")

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
