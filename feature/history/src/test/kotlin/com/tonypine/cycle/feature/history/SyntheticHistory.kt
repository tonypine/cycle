package com.tonypine.cycle.feature.history

import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.SexualActivity
import com.tonypine.cycle.core.model.SleepQuality
import java.time.LocalDate

// Synthetic logs only: made-up dates in 2027, never anyone's real cycle.

fun day(iso: String): LocalDate = LocalDate.parse(iso)

/** [length] days of medium flow from [start]. */
fun bleed(start: String, length: Int): List<DayLog> =
    (0 until length).map { DayLog(day(start).plusDays(it.toLong()), FlowLevel.MEDIUM) }

/**
 * The log behind [HistorySamples]: the worked example of MOT-33, six cycles of 29, 26, 31, 28, 27
 * and 28 days, with periods of 5, 4, 6, 5, 5, 6 and 4 days. August's period has a day without flow.
 */
val syntheticHistory: List<DayLog> =
    bleed("2027-03-17", 5) +
        bleed("2027-04-15", 4) +
        bleed("2027-05-11", 6) +
        bleed("2027-06-11", 5) +
        bleed("2027-07-09", 5) +
        listOf(
            DayLog(day("2027-08-05"), FlowLevel.MEDIUM),
            DayLog(day("2027-08-06"), FlowLevel.HEAVY),
            DayLog(day("2027-08-07"), FlowLevel.HEAVY),
            DayLog(day("2027-08-08"), FlowLevel.MEDIUM),
            DayLog(day("2027-08-10"), FlowLevel.LIGHT)
        ) +
        listOf(
            DayLog(day("2027-09-02"), FlowLevel.MEDIUM),
            DayLog(day("2027-09-03"), FlowLevel.HEAVY),
            DayLog(day("2027-09-04"), FlowLevel.MEDIUM),
            DayLog(day("2027-09-05"), FlowLevel.LIGHT)
        )

/** How she felt in August's cycle, behind [HistorySamples.pastCycle]: it starts on August 5, day 1. */
val syntheticFeelings: List<DayFeelings> = listOf(
    DayFeelings(
        day("2027-08-05"),
        pain = Pain(PainLevel.MODERATE, setOf(PainKind.CRAMPS, PainKind.LOWER_BACK)),
        moods = setOf(Mood.IRRITABLE),
        energy = EnergyLevel.LOW,
        note = "Sample note: a heat pad helped."
    ),
    DayFeelings(
        day("2027-08-06"),
        pain = Pain(PainLevel.MODERATE, setOf(PainKind.CRAMPS)),
        body = setOf(BodySymptom.TIRED),
        sleep = SleepQuality.BADLY
    ),
    DayFeelings(
        day("2027-08-07"),
        pain = Pain(PainLevel.MILD, setOf(PainKind.CRAMPS)),
        note = "Sample note: a short walk after lunch."
    ),
    DayFeelings(day("2027-08-18"), sex = SexualActivity.PROTECTED),
    DayFeelings(day("2027-08-28"), body = setOf(BodySymptom.BLOATING)),
    DayFeelings(day("2027-08-29"), body = setOf(BodySymptom.BLOATING)),
    DayFeelings(
        day("2027-08-30"),
        body = setOf(BodySymptom.BLOATING, BodySymptom.TENDER_BREASTS),
        moods = setOf(Mood.SENSITIVE)
    ),
    DayFeelings(day("2027-08-31"), body = setOf(BodySymptom.BLOATING))
)

val notSetUp = CycleSettings(usualCycleLength = 28, usualPeriodLength = 5, setupDone = false)
