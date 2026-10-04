package com.tonypine.cycle.core.domain

import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate

// Synthetic logs only: made-up dates in 2027, never anyone's real cycle.

fun day(iso: String): LocalDate = LocalDate.parse(iso)

/** [length] days of medium flow from [start]. */
fun bleed(start: LocalDate, length: Int = 5, flow: FlowLevel = FlowLevel.MEDIUM): List<DayLog> =
    (0 until length).map { DayLog(start.plusDays(it.toLong()), flow) }

/**
 * Periods of [periodLength] days whose cycles have [cycleLengths] days, oldest first, so that the
 * last period starts on [lastStart].
 */
fun cycles(vararg cycleLengths: Int, lastStart: LocalDate, periodLength: Int = 5): List<DayLog> {
    val starts = cycleLengths.reversed().runningFold(lastStart) { start, length -> start.minusDays(length.toLong()) }
    return starts.reversed().flatMap { bleed(it, periodLength) }
}

val notSetUp = CycleSettings(usualCycleLength = 28, usualPeriodLength = 5, setupDone = false)

fun setUp(cycleLength: Int = 28, periodLength: Int = 5) =
    CycleSettings(usualCycleLength = cycleLength, usualPeriodLength = periodLength, setupDone = true)
