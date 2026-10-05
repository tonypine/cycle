package com.tonypine.cycle.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Whether the app opens on the welcome, and what setup's Done and Skip write. Nothing is saved
 * before Done or Skip, so leaving setup halfway brings her back to the welcome next time.
 *
 * @param clock her day, from the phone's clock in its current zone.
 */
class OnboardingViewModel(
    private val settings: SettingsRepository,
    private val dayLogs: DayLogRepository,
    private val clock: () -> LocalDate = LocalDate::now
) : ViewModel() {
    /**
     * True while the app should show the welcome, false once she is past it, and null until the
     * settings are read. An install that already has her logs or setup, from before the welcome
     * existed, opens on Today, and is marked past the welcome: "Delete everything", which clears
     * that mark, then brings the welcome back.
     */
    val showWelcome: StateFlow<Boolean?> = settings.welcomeDone
        .map { done ->
            when {
                done -> false

                alreadyInUse() -> {
                    settings.setWelcomeDone(true)
                    false
                }

                else -> true
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Her day, for setup's calendar. */
    fun today(): LocalDate = clock()

    /** "Skip for now": Today, on the typical lengths. */
    fun onSkip() = finish { }

    /** She restored her days from a Cycle export: Today, on what she restored. */
    fun onRestored() = finish { }

    /**
     * Setup's Done: her usual lengths, and the period she picked, if any, as a period of
     * [periodLength] days. She leaves the welcome only once everything is saved, so Today's first
     * estimate already uses what she entered.
     */
    fun onDone(lastPeriodStart: LocalDate?, cycleLength: Int, periodLength: Int) = finish {
        settings.saveSetup(cycleLength, periodLength)
        lastPeriodStart?.let { dayLogs.logPeriod(it, periodLength, today()) }
    }

    private suspend fun alreadyInUse(): Boolean =
        settings.settings.first().setupDone || dayLogs.observeDayLogs().first().isNotEmpty()

    // Each write is the same the second time, so a double tap saves the same thing twice.
    private fun finish(save: suspend () -> Unit) {
        viewModelScope.launch {
            save()
            settings.setWelcomeDone(true)
        }
    }

    private companion object {
        // Keeps the answer through a configuration change without a reload.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
