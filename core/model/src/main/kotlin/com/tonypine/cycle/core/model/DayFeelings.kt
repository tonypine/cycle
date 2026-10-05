package com.tonypine.cycle.core.model

import java.io.Serializable
import java.time.LocalDate

/**
 * How she felt on one calendar day: the "common" tier of `docs/research/tracking-data.md`. Every
 * category is optional, and each is stored on its own, one row per day per category.
 *
 * Serializable so the day log sheet can keep a draft across a configuration change.
 *
 * @property pain how much it hurt that day and where, or null when she logged nothing about it.
 * @property body the physical symptoms she picked.
 * @property moods the moods she picked: any number, no score.
 * @property sex sensitive: never in a notification, never in a widget.
 * @property note free text, at most [NOTE_MAX_LENGTH] characters. Empty when she wrote nothing.
 */
data class DayFeelings(
    val date: LocalDate,
    val pain: Pain? = null,
    val body: Set<BodySymptom> = emptySet(),
    val moods: Set<Mood> = emptySet(),
    val energy: EnergyLevel? = null,
    val sleep: SleepQuality? = null,
    val sex: SexualActivity? = null,
    val note: String = ""
) : Serializable {
    /** Nothing logged: storage keeps no row for such a day. */
    val isEmpty: Boolean
        get() = this == DayFeelings(date)

    /** What she logged, without the categories she has [hidden], for the screens that show it. */
    fun without(hidden: Set<LogCategory>): DayFeelings = copy(
        pain = pain.takeUnless { LogCategory.PAIN in hidden },
        body = body.takeUnless { LogCategory.BODY in hidden }.orEmpty(),
        moods = moods.takeUnless { LogCategory.MOOD in hidden }.orEmpty(),
        energy = energy.takeUnless { LogCategory.ENERGY in hidden },
        sleep = sleep.takeUnless { LogCategory.SLEEP in hidden },
        sex = sex.takeUnless { LogCategory.SEX in hidden },
        note = note.takeUnless { LogCategory.NOTES in hidden }.orEmpty()
    )

    /**
     * The day as it is stored: where it hurt only with some pain, and the note trimmed and cut to
     * [NOTE_MAX_LENGTH].
     */
    fun normalized(): DayFeelings = copy(
        pain = pain?.let { if (it.level == PainLevel.NONE) it.copy(kinds = emptySet()) else it },
        note = note.trim().take(NOTE_MAX_LENGTH)
    )

    companion object {
        const val NOTE_MAX_LENGTH = 500
    }
}

/**
 * Pain on a day: one [level] for the day, and where it hurt. Severity per kind waits for the symptom
 * diary (`tracking-data.md`, "For the premenstrual diary").
 *
 * @property kinds where it hurt; always empty when [level] is [PainLevel.NONE].
 */
data class Pain(val level: PainLevel, val kinds: Set<PainKind> = emptySet()) : Serializable

enum class PainLevel {
    /** She logged that nothing hurt. */
    NONE,
    MILD,
    MODERATE,
    SEVERE
}

enum class PainKind {
    CRAMPS,
    HEADACHE,
    LOWER_BACK,
    BREASTS,
    OVULATION,
    DURING_SEX
}

enum class BodySymptom {
    BLOATING,
    TENDER_BREASTS,
    ACNE,
    CRAVINGS,
    NAUSEA,
    TIRED,
    HOT_FLUSHES,
    NIGHT_SWEATS
}

enum class Mood {
    CALM,
    HAPPY,
    SENSITIVE,
    SAD,
    ANXIOUS,
    IRRITABLE,
    ANGRY,
    LOW,
    MOOD_SWINGS
}

enum class EnergyLevel {
    LOW,
    NORMAL,
    HIGH
}

enum class SleepQuality {
    WELL,
    BADLY
}

/** Matches Health Connect's `SexualActivityRecord` protection. Sensitive: see [DayFeelings.sex]. */
enum class SexualActivity {
    PROTECTED,
    UNPROTECTED
}

/**
 * A category of the day log she can hide in "What to log". Flow and spotting are not here: they are
 * always on, because the estimates need them. Hiding one keeps everything logged in it.
 */
enum class LogCategory {
    PAIN,
    BODY,
    MOOD,
    ENERGY,
    SLEEP,
    SEX,
    NOTES
}
