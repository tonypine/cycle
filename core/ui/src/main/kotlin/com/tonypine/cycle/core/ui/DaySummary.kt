package com.tonypine.cycle.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.tonypine.cycle.core.designsystem.cycleLocale
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.SexualActivity
import com.tonypine.cycle.core.model.SleepQuality

/**
 * What she logged on a day in a few words, in the day log's order: "Cramps, moderate", "Bloating",
 * "Irritable", "Low energy", "Slept badly". The note is left out: show it on its own. Pass
 * [feelings] without the categories she hid ([DayFeelings.without]).
 */
@Composable
fun daySummary(flow: FlowLevel?, feelings: DayFeelings): List<String> = buildList {
    flow?.let { add(stringResource(it.summaryLabel)) }
    feelings.pain?.let { add(painSummary(it)) }
    BodySymptom.entries.filter { it in feelings.body }.forEach { add(stringResource(it.label)) }
    Mood.entries.filter { it in feelings.moods }.forEach { add(stringResource(it.label)) }
    feelings.energy?.let { add(stringResource(it.summaryLabel)) }
    feelings.sleep?.let { add(stringResource(it.label)) }
    feelings.sex?.let { add(stringResource(it.summaryLabel)) }
}

/** [pain] in a few words: "Cramps, lower back, moderate", or "Mild pain" when she said nowhere. */
@Composable
fun painSummary(pain: Pain): String {
    val kinds = PainKind.entries.filter { it in pain.kinds }
    if (kinds.isEmpty() || pain.level == PainLevel.NONE) return stringResource(pain.level.summaryAlone)
    // "Cramps, lower back": the places after the first read on in lower case.
    val locale = cycleLocale()
    val where = kinds.mapIndexed { index, kind ->
        stringResource(kind.label).let { if (index == 0) it else it.lowercase(locale) }
    }.joinToString(", ")
    return stringResource(R.string.summary_pain_where, where, stringResource(pain.level.summaryAfterWhere))
}

/** [items] joined into one line: "Bloating · Irritable". */
@Composable
fun summaryLine(items: List<String>): String = items.joinToString(stringResource(R.string.summary_separator))

val PainLevel.label: Int
    @StringRes get() = when (this) {
        PainLevel.NONE -> R.string.pain_none
        PainLevel.MILD -> R.string.pain_mild
        PainLevel.MODERATE -> R.string.pain_moderate
        PainLevel.SEVERE -> R.string.pain_severe
    }

val PainKind.label: Int
    @StringRes get() = when (this) {
        PainKind.CRAMPS -> R.string.pain_cramps
        PainKind.HEADACHE -> R.string.pain_headache
        PainKind.LOWER_BACK -> R.string.pain_lower_back
        PainKind.BREASTS -> R.string.pain_breasts
        PainKind.OVULATION -> R.string.pain_ovulation
        PainKind.DURING_SEX -> R.string.pain_during_sex
    }

val BodySymptom.label: Int
    @StringRes get() = when (this) {
        BodySymptom.BLOATING -> R.string.body_bloating
        BodySymptom.TENDER_BREASTS -> R.string.body_tender_breasts
        BodySymptom.ACNE -> R.string.body_acne
        BodySymptom.CRAVINGS -> R.string.body_cravings
        BodySymptom.NAUSEA -> R.string.body_nausea
        BodySymptom.TIRED -> R.string.body_tired
        BodySymptom.HOT_FLUSHES -> R.string.body_hot_flushes
        BodySymptom.NIGHT_SWEATS -> R.string.body_night_sweats
    }

val Mood.label: Int
    @StringRes get() = when (this) {
        Mood.CALM -> R.string.mood_calm
        Mood.HAPPY -> R.string.mood_happy
        Mood.SENSITIVE -> R.string.mood_sensitive
        Mood.SAD -> R.string.mood_sad
        Mood.ANXIOUS -> R.string.mood_anxious
        Mood.IRRITABLE -> R.string.mood_irritable
        Mood.ANGRY -> R.string.mood_angry
        Mood.LOW -> R.string.mood_low
        Mood.MOOD_SWINGS -> R.string.mood_mood_swings
    }

val EnergyLevel.label: Int
    @StringRes get() = when (this) {
        EnergyLevel.LOW -> R.string.energy_low
        EnergyLevel.NORMAL -> R.string.energy_normal
        EnergyLevel.HIGH -> R.string.energy_high
    }

val SleepQuality.label: Int
    @StringRes get() = when (this) {
        SleepQuality.WELL -> R.string.sleep_well
        SleepQuality.BADLY -> R.string.sleep_badly
    }

val SexualActivity.label: Int
    @StringRes get() = when (this) {
        SexualActivity.PROTECTED -> R.string.sex_protected
        SexualActivity.UNPROTECTED -> R.string.sex_unprotected
    }

private val FlowLevel.summaryLabel: Int
    get() = when (this) {
        FlowLevel.NONE -> R.string.summary_flow_none
        FlowLevel.SPOTTING -> R.string.summary_flow_spotting
        FlowLevel.LIGHT -> R.string.summary_flow_light
        FlowLevel.MEDIUM -> R.string.summary_flow_medium
        FlowLevel.HEAVY -> R.string.summary_flow_heavy
    }

private val PainLevel.summaryAlone: Int
    get() = when (this) {
        PainLevel.NONE -> R.string.summary_pain_none
        PainLevel.MILD -> R.string.summary_pain_mild_only
        PainLevel.MODERATE -> R.string.summary_pain_moderate_only
        PainLevel.SEVERE -> R.string.summary_pain_severe_only
    }

private val PainLevel.summaryAfterWhere: Int
    get() = when (this) {
        PainLevel.NONE -> R.string.summary_pain_none
        PainLevel.MILD -> R.string.summary_pain_mild
        PainLevel.MODERATE -> R.string.summary_pain_moderate
        PainLevel.SEVERE -> R.string.summary_pain_severe
    }

/** The energy in a summary: "Low energy". */
val EnergyLevel.summaryLabel: Int
    @StringRes get() = when (this) {
        EnergyLevel.LOW -> R.string.summary_energy_low
        EnergyLevel.NORMAL -> R.string.summary_energy_normal
        EnergyLevel.HIGH -> R.string.summary_energy_high
    }

/** Sex in a summary: "Protected sex". In the app only, never in a notification or widget. */
val SexualActivity.summaryLabel: Int
    @StringRes get() = when (this) {
        SexualActivity.PROTECTED -> R.string.summary_sex_protected
        SexualActivity.UNPROTECTED -> R.string.summary_sex_unprotected
    }
