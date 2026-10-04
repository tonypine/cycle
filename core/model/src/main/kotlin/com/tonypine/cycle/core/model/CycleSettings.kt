package com.tonypine.cycle.core.model

import java.time.LocalDate

/**
 * Her settings: the lengths she gives at setup, and the prompts she has already answered.
 *
 * @property usualCycleLength her usual cycle length in days, used until she has logged cycles.
 * @property usualPeriodLength her usual period length in days, used until she has logged periods.
 * @property setupDone she has finished setup, so the usual lengths are hers, not the defaults.
 * @property dismissedStillGoing first days of the periods whose "still going?" prompt she answered.
 * @property dismissedMissedPeriod first days of the cycles whose "missed a period?" prompt she
 *   answered.
 */
data class CycleSettings(
    val usualCycleLength: Int,
    val usualPeriodLength: Int,
    val setupDone: Boolean,
    val dismissedStillGoing: Set<LocalDate> = emptySet(),
    val dismissedMissedPeriod: Set<LocalDate> = emptySet()
)
