package com.tonypine.cycle.core.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.inMemoryDatabase
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.SleepQuality
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** How the feelings tables are written and read. Synthetic days only. */
@RunWith(AndroidJUnit4::class)
class FeelingsDaoTest {
    private val database = inMemoryDatabase()
    private val dao = database.feelingsDao()

    @After
    fun closeDatabase() = database.close()

    private fun rows(table: String): List<List<String?>> =
        database.openHelper.readableDatabase.query("SELECT * FROM $table ORDER BY date").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add((0 until cursor.columnCount).map { cursor.getString(it) })
            }
        }

    @Test
    fun `each category is one row per day, written as fixed codes in a fixed order`() = runTest {
        dao.save(
            DayFeelings(
                date = day("2027-03-02"),
                pain = Pain(PainLevel.SEVERE, setOf(PainKind.DURING_SEX, PainKind.CRAMPS)),
                body = setOf(BodySymptom.NIGHT_SWEATS, BodySymptom.TENDER_BREASTS),
                moods = setOf(Mood.MOOD_SWINGS),
                sleep = SleepQuality.WELL
            )
        )

        assertEquals(listOf(listOf("2027-03-02", "severe", "cramps,during_sex")), rows("pain"))
        assertEquals(listOf(listOf("2027-03-02", "tender_breasts,night_sweats")), rows("body_symptoms"))
        assertEquals(listOf(listOf("2027-03-02", "mood_swings")), rows("mood"))
        assertEquals(listOf(listOf("2027-03-02", "well")), rows("sleep"))
        listOf("energy", "sex", "note").forEach { assertEquals(it, emptyList<List<String?>>(), rows(it)) }
    }

    @Test
    fun `a code this version does not know is skipped`() = runTest {
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO pain (date, level, kinds) VALUES ('2027-03-02', 'mild', 'cramps,elbow'), " +
                "('2027-03-03', 'unbearable', 'cramps')"
        )
        database.openHelper.writableDatabase.execSQL("INSERT INTO energy (date, level) VALUES ('2027-03-03', 'zero')")

        assertEquals(
            listOf(
                DayFeelings(day("2027-03-02"), pain = Pain(PainLevel.MILD, setOf(PainKind.CRAMPS))),
                DayFeelings(day("2027-03-03"))
            ),
            dao.getAll()
        )
    }

    @Test
    fun `every day with a row in any table is read once, oldest first`() = runTest {
        dao.save(DayFeelings(day("2027-03-05"), note = "Synthetic"))
        dao.save(DayFeelings(day("2027-03-02"), moods = setOf(Mood.CALM)))
        dao.save(DayFeelings(day("2027-03-05"), moods = setOf(Mood.HAPPY), note = "Synthetic"))

        assertEquals(
            listOf(
                DayFeelings(day("2027-03-02"), moods = setOf(Mood.CALM)),
                DayFeelings(day("2027-03-05"), moods = setOf(Mood.HAPPY), note = "Synthetic")
            ),
            dao.observeAll().first()
        )
    }

    @Test
    fun `the log flows again after a write to any category`() = runTest {
        for (feelings in listOf(
            DayFeelings(day("2027-03-02"), sleep = SleepQuality.BADLY),
            DayFeelings(day("2027-03-02"), note = "Synthetic")
        )) {
            dao.delete(day("2027-03-02"))
            val subscribed = CompletableDeferred<Unit>()
            val next = async(Dispatchers.Default) {
                dao.observeAll().onEach { subscribed.complete(Unit) }.drop(1).first()
            }
            subscribed.await()

            dao.save(feelings)

            assertEquals(listOf(feelings), next.await())
        }
    }
}
