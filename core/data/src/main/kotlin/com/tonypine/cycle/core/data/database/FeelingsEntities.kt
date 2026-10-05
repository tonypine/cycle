package com.tonypine.cycle.core.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.SexualActivity
import com.tonypine.cycle.core.model.SleepQuality
import java.time.LocalDate

// How she felt, one table per category and one row per day in each, keyed by the day like
// `day_log`, so a new category is a migration that adds a table. Values are fixed text codes, and a
// set is its codes joined by commas in a fixed order: renaming a Kotlin constant cannot change what
// is on her phone. A code this version does not know is skipped on read.

@Entity(tableName = "pain")
data class PainEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "level") val level: String,
    @ColumnInfo(name = "kinds") val kinds: String
)

@Entity(tableName = "body_symptoms")
data class BodySymptomsEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "symptoms") val symptoms: String
)

@Entity(tableName = "mood")
data class MoodEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "moods") val moods: String
)

@Entity(tableName = "energy")
data class EnergyEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "level") val level: String
)

@Entity(tableName = "sleep")
data class SleepEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "quality") val quality: String
)

@Entity(tableName = "sex")
data class SexEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "protection") val protection: String
)

@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "text") val text: String
)

/** Every stored row of one day, any of them missing. */
internal data class FeelingsRows(
    val date: LocalDate,
    val pain: PainEntity? = null,
    val body: BodySymptomsEntity? = null,
    val mood: MoodEntity? = null,
    val energy: EnergyEntity? = null,
    val sleep: SleepEntity? = null,
    val sex: SexEntity? = null,
    val note: NoteEntity? = null
)

internal fun FeelingsRows.toModel(): DayFeelings = DayFeelings(
    date = date,
    pain = pain?.let { row ->
        FeelingCodes.PAIN_LEVEL.decode(row.level)?.let { Pain(it, FeelingCodes.PAIN_KIND.decodeSet(row.kinds)) }
    },
    body = body?.let { FeelingCodes.BODY.decodeSet(it.symptoms) }.orEmpty(),
    moods = mood?.let { FeelingCodes.MOOD.decodeSet(it.moods) }.orEmpty(),
    energy = energy?.let { FeelingCodes.ENERGY.decode(it.level) },
    sleep = sleep?.let { FeelingCodes.SLEEP.decode(it.quality) },
    sex = sex?.let { FeelingCodes.SEX.decode(it.protection) },
    note = note?.text.orEmpty()
)

/** The rows [this] day is stored as; a null row is a category with nothing logged. */
internal fun DayFeelings.toRows(): FeelingsRows = FeelingsRows(
    date = date,
    pain = pain?.let {
        PainEntity(date, FeelingCodes.PAIN_LEVEL.encode(it.level), FeelingCodes.PAIN_KIND.encodeSet(it.kinds))
    },
    body = body.takeIf { it.isNotEmpty() }?.let { BodySymptomsEntity(date, FeelingCodes.BODY.encodeSet(it)) },
    mood = moods.takeIf { it.isNotEmpty() }?.let { MoodEntity(date, FeelingCodes.MOOD.encodeSet(it)) },
    energy = energy?.let { EnergyEntity(date, FeelingCodes.ENERGY.encode(it)) },
    sleep = sleep?.let { SleepEntity(date, FeelingCodes.SLEEP.encode(it)) },
    sex = sex?.let { SexEntity(date, FeelingCodes.SEX.encode(it)) },
    note = note.takeIf { it.isNotEmpty() }?.let { NoteEntity(date, it) }
)

/** The text each value is stored as. Never change a code that has shipped. */
internal object FeelingCodes {
    val PAIN_LEVEL = Codes(
        PainLevel.NONE to "none",
        PainLevel.MILD to "mild",
        PainLevel.MODERATE to "moderate",
        PainLevel.SEVERE to "severe"
    )
    val PAIN_KIND = Codes(
        PainKind.CRAMPS to "cramps",
        PainKind.HEADACHE to "headache",
        PainKind.LOWER_BACK to "lower_back",
        PainKind.BREASTS to "breasts",
        PainKind.OVULATION to "ovulation",
        PainKind.DURING_SEX to "during_sex"
    )
    val BODY = Codes(
        BodySymptom.BLOATING to "bloating",
        BodySymptom.TENDER_BREASTS to "tender_breasts",
        BodySymptom.ACNE to "acne",
        BodySymptom.CRAVINGS to "cravings",
        BodySymptom.NAUSEA to "nausea",
        BodySymptom.TIRED to "tired",
        BodySymptom.HOT_FLUSHES to "hot_flushes",
        BodySymptom.NIGHT_SWEATS to "night_sweats"
    )
    val MOOD = Codes(
        Mood.CALM to "calm",
        Mood.HAPPY to "happy",
        Mood.SENSITIVE to "sensitive",
        Mood.SAD to "sad",
        Mood.ANXIOUS to "anxious",
        Mood.IRRITABLE to "irritable",
        Mood.ANGRY to "angry",
        Mood.LOW to "low",
        Mood.MOOD_SWINGS to "mood_swings"
    )
    val ENERGY = Codes(EnergyLevel.LOW to "low", EnergyLevel.NORMAL to "normal", EnergyLevel.HIGH to "high")
    val SLEEP = Codes(SleepQuality.WELL to "well", SleepQuality.BADLY to "badly")
    val SEX = Codes(SexualActivity.PROTECTED to "protected", SexualActivity.UNPROTECTED to "unprotected")

    /** One code per value of [T]; a set is written in the order the codes are listed. */
    class Codes<T : Enum<T>>(vararg pairs: Pair<T, String>) {
        private val byValue = pairs.toMap()
        private val byCode = pairs.associate { (value, code) -> code to value }

        fun encode(value: T): String = byValue.getValue(value)

        fun decode(code: String): T? = byCode[code]

        fun encodeSet(values: Set<T>): String = byValue.filterKeys { it in values }.values.joinToString(SEPARATOR)

        fun decodeSet(text: String): Set<T> =
            text.split(SEPARATOR).mapNotNullTo(mutableSetOf()) { byCode[it] }.let { decoded ->
                byValue.keys.filterTo(linkedSetOf()) { it in decoded }
            }
    }

    private const val SEPARATOR = ","
}
