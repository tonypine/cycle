package com.tonypine.cycle.core.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.tonypine.cycle.core.model.DayFeelings
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How she felt, across the seven category tables. Each method reads or writes them together. */
@Dao
abstract class FeelingsDao {
    /** Every day she logged how she felt, oldest first, again after any change to a category. */
    fun observeAll(): Flow<List<DayFeelings>> = observeDates().map { getAll() }

    /** The days with a row in any category. Room re-runs it whenever one of the tables changes. */
    @Query(
        "SELECT date FROM pain UNION SELECT date FROM body_symptoms UNION SELECT date FROM mood " +
            "UNION SELECT date FROM energy UNION SELECT date FROM sleep UNION SELECT date FROM sex " +
            "UNION SELECT date FROM note ORDER BY date"
    )
    internal abstract fun observeDates(): Flow<List<LocalDate>>

    /** Every day she logged how she felt, oldest first, from one read of all the tables. */
    @Transaction
    open suspend fun getAll(): List<DayFeelings> {
        val pain = allPain().associateBy { it.date }
        val body = allBody().associateBy { it.date }
        val mood = allMood().associateBy { it.date }
        val energy = allEnergy().associateBy { it.date }
        val sleep = allSleep().associateBy { it.date }
        val sex = allSex().associateBy { it.date }
        val note = allNotes().associateBy { it.date }
        val dates = (pain.keys + body.keys + mood.keys + energy.keys + sleep.keys + sex.keys + note.keys).sorted()
        return dates.map { date ->
            FeelingsRows(date, pain[date], body[date], mood[date], energy[date], sleep[date], sex[date], note[date])
                .toModel()
        }
    }

    /** How she felt on [date]; empty when she logged nothing. */
    @Transaction
    open suspend fun get(date: LocalDate): DayFeelings =
        FeelingsRows(date, pain(date), body(date), mood(date), energy(date), sleep(date), sex(date), note(date))
            .toModel()

    /** Replaces [feelings.date][DayFeelings.date] with [feelings], category by category. */
    @Transaction
    open suspend fun save(feelings: DayFeelings) {
        val date = feelings.date
        val rows = feelings.toRows()
        rows.pain?.let { upsertPain(it) } ?: deletePain(date)
        rows.body?.let { upsertBody(it) } ?: deleteBody(date)
        rows.mood?.let { upsertMood(it) } ?: deleteMood(date)
        rows.energy?.let { upsertEnergy(it) } ?: deleteEnergy(date)
        rows.sleep?.let { upsertSleep(it) } ?: deleteSleep(date)
        rows.sex?.let { upsertSex(it) } ?: deleteSex(date)
        rows.note?.let { upsertNote(it) } ?: deleteNote(date)
    }

    /** Removes everything she logged about how she felt on [date]. */
    @Transaction
    open suspend fun delete(date: LocalDate) = save(DayFeelings(date))

    @Query("SELECT * FROM pain ORDER BY date")
    internal abstract suspend fun allPain(): List<PainEntity>

    @Query("SELECT * FROM body_symptoms ORDER BY date")
    internal abstract suspend fun allBody(): List<BodySymptomsEntity>

    @Query("SELECT * FROM mood ORDER BY date")
    internal abstract suspend fun allMood(): List<MoodEntity>

    @Query("SELECT * FROM energy ORDER BY date")
    internal abstract suspend fun allEnergy(): List<EnergyEntity>

    @Query("SELECT * FROM sleep ORDER BY date")
    internal abstract suspend fun allSleep(): List<SleepEntity>

    @Query("SELECT * FROM sex ORDER BY date")
    internal abstract suspend fun allSex(): List<SexEntity>

    @Query("SELECT * FROM note ORDER BY date")
    internal abstract suspend fun allNotes(): List<NoteEntity>

    @Query("SELECT * FROM pain WHERE date = :date")
    internal abstract suspend fun pain(date: LocalDate): PainEntity?

    @Query("SELECT * FROM body_symptoms WHERE date = :date")
    internal abstract suspend fun body(date: LocalDate): BodySymptomsEntity?

    @Query("SELECT * FROM mood WHERE date = :date")
    internal abstract suspend fun mood(date: LocalDate): MoodEntity?

    @Query("SELECT * FROM energy WHERE date = :date")
    internal abstract suspend fun energy(date: LocalDate): EnergyEntity?

    @Query("SELECT * FROM sleep WHERE date = :date")
    internal abstract suspend fun sleep(date: LocalDate): SleepEntity?

    @Query("SELECT * FROM sex WHERE date = :date")
    internal abstract suspend fun sex(date: LocalDate): SexEntity?

    @Query("SELECT * FROM note WHERE date = :date")
    internal abstract suspend fun note(date: LocalDate): NoteEntity?

    @Upsert
    internal abstract suspend fun upsertPain(row: PainEntity)

    @Upsert
    internal abstract suspend fun upsertBody(row: BodySymptomsEntity)

    @Upsert
    internal abstract suspend fun upsertMood(row: MoodEntity)

    @Upsert
    internal abstract suspend fun upsertEnergy(row: EnergyEntity)

    @Upsert
    internal abstract suspend fun upsertSleep(row: SleepEntity)

    @Upsert
    internal abstract suspend fun upsertSex(row: SexEntity)

    @Upsert
    internal abstract suspend fun upsertNote(row: NoteEntity)

    @Query("DELETE FROM pain WHERE date = :date")
    internal abstract suspend fun deletePain(date: LocalDate)

    @Query("DELETE FROM body_symptoms WHERE date = :date")
    internal abstract suspend fun deleteBody(date: LocalDate)

    @Query("DELETE FROM mood WHERE date = :date")
    internal abstract suspend fun deleteMood(date: LocalDate)

    @Query("DELETE FROM energy WHERE date = :date")
    internal abstract suspend fun deleteEnergy(date: LocalDate)

    @Query("DELETE FROM sleep WHERE date = :date")
    internal abstract suspend fun deleteSleep(date: LocalDate)

    @Query("DELETE FROM sex WHERE date = :date")
    internal abstract suspend fun deleteSex(date: LocalDate)

    @Query("DELETE FROM note WHERE date = :date")
    internal abstract suspend fun deleteNote(date: LocalDate)
}
