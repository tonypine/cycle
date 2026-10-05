package com.tonypine.cycle.core.data.export

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.inMemoryDatabase
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LogCategory
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.SexualActivity
import com.tonypine.cycle.core.model.SleepQuality
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/** Export, import and "Delete everything" on an in-memory database and a DataStore file. */
@RunWith(AndroidJUnit4::class)
class YourDataRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database = inMemoryDatabase()
    private val dayLogs = DayLogRepository(database)

    @After
    fun closeDatabase() = database.close()

    private class Phone(
        val repository: YourDataRepository,
        val settings: SettingsRepository,
        val preferences: DataStore<Preferences>
    )

    private fun TestScope.phone(): Phone {
        val preferences = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            File(folder.root, "settings.preferences_pb")
        }
        val settings = SettingsRepository(preferences)
        return Phone(YourDataRepository(database, settings), settings, preferences)
    }

    /** Two synthetic cycles: every category, period markers, spotting and a note that needs quoting. */
    private suspend fun logSyntheticHistory() {
        dayLogs.logDay(
            day("2027-01-05"),
            FlowLevel.HEAVY,
            DayFeelings(
                day("2027-01-05"),
                pain = Pain(PainLevel.SEVERE, setOf(PainKind.CRAMPS, PainKind.LOWER_BACK)),
                body = setOf(BodySymptom.BLOATING, BodySymptom.TIRED),
                moods = setOf(Mood.IRRITABLE, Mood.LOW),
                energy = EnergyLevel.LOW,
                sleep = SleepQuality.BADLY,
                note = "Synthetic: \"rough\" day, stayed in\nsecond line"
            )
        )
        dayLogs.setPeriodStarted(day("2027-01-05"), started = true)
        dayLogs.setFlow(day("2027-01-06"), FlowLevel.MEDIUM)
        dayLogs.setPeriodEnded(day("2027-01-09"), ended = true)
        dayLogs.logDay(
            day("2027-01-20"),
            FlowLevel.NONE,
            DayFeelings(day("2027-01-20"), pain = Pain(PainLevel.NONE), sex = SexualActivity.UNPROTECTED)
        )
        dayLogs.setFlow(day("2027-01-25"), FlowLevel.SPOTTING)
        dayLogs.logDay(
            day("2027-02-01"),
            null,
            DayFeelings(day("2027-02-01"), moods = setOf(Mood.CALM, Mood.HAPPY), energy = EnergyLevel.HIGH)
        )
        dayLogs.logPeriod(day("2027-02-02"), length = 5, today = day("2027-03-20"))
        dayLogs.logDay(day("2027-02-03"), FlowLevel.LIGHT, DayFeelings(day("2027-02-03"), sleep = SleepQuality.WELL))
    }

    private suspend fun CycleDatabase.everything(): Pair<List<DayLog>, List<DayFeelings>> =
        dayLogDao().getAll().map { DayLog(it.date, it.flow, it.periodStarted, it.periodEnded) } to feelingsDao().getAll()

    private suspend fun YourDataRepository.exportBytes(): ByteArray =
        ByteArrayOutputStream().also { export(it) }.toByteArray()

    @Test
    fun `exporting then importing round-trips the synthetic history exactly`() = runTest {
        val phone = phone()
        logSyntheticHistory()
        val before = database.everything()
        val exported = phone.repository.exportBytes()

        phone.repository.deleteEverything()
        val read = phone.repository.read(ByteArrayInputStream(exported)) as ImportRead.Ready
        val added = phone.repository.import(read.file)

        assertEquals(9, read.newDays)
        assertEquals(9, added)
        assertEquals(before, database.everything())
        assertEquals(exported.decodeToString(), phone.repository.exportBytes().decodeToString())
    }

    @Test
    fun `days already on the phone win, and only the new ones are counted and added`() = runTest {
        val phone = phone()
        logSyntheticHistory()
        val exported = phone.repository.exportBytes()
        // On the phone since the export: 6 January changed, 1 February cleared, 4 March new.
        dayLogs.setFlow(day("2027-01-06"), FlowLevel.HEAVY)
        dayLogs.clear(day("2027-02-01"))
        dayLogs.setFlow(day("2027-03-04"), FlowLevel.LIGHT)
        val before = database.everything()

        val read = phone.repository.read(ByteArrayInputStream(exported)) as ImportRead.Ready
        val added = phone.repository.import(read.file)

        assertEquals(1, read.newDays)
        assertEquals(1, added)
        val (logs, feelings) = database.everything()
        assertEquals(FlowLevel.HEAVY, logs.single { it.date == day("2027-01-06") }.flow)
        assertEquals(before.first, logs)
        assertEquals(
            DayFeelings(day("2027-02-01"), moods = setOf(Mood.CALM, Mood.HAPPY), energy = EnergyLevel.HIGH),
            feelings.single { it.date == day("2027-02-01") }
        )
        assertEquals(before.second.size + 1, feelings.size)
    }

    @Test
    fun `importing the same file twice adds nothing the second time`() = runTest {
        val phone = phone()
        logSyntheticHistory()
        val exported = phone.repository.exportBytes()

        val read = phone.repository.read(ByteArrayInputStream(exported)) as ImportRead.Ready

        assertEquals(0, read.newDays)
        assertEquals(0, phone.repository.import(read.file))
    }

    @Test
    fun `a malformed file is refused and nothing is written`() = runTest {
        val phone = phone()
        val text = "date,flow,period_started,period_ended,pain,pain_where,body,mood,energy,sleep,sex,note\n" +
            "2027-03-02,light,,,,,,,,,,\n2027-03-03,lots,,,,,,,,,,\n"

        val read = phone.repository.read(ByteArrayInputStream(text.encodeToByteArray()))

        assertEquals(ImportRead.Refused(ImportProblem.UnknownValue(3, "flow", "lots")), read)
        assertEquals(emptyList<DayLog>() to emptyList<DayFeelings>(), database.everything())
    }

    @Test
    fun `a file that is not text, too large or unreadable is refused`() = runTest {
        val repository = phone().repository

        assertEquals(
            ImportRead.Refused(ImportProblem.NotText),
            repository.read(ByteArrayInputStream(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A)))
        )
        assertEquals(
            ImportRead.Refused(ImportProblem.NotText),
            repository.read(ByteArrayInputStream(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00, 0x00)))
        )
        assertEquals(ImportRead.Refused(ImportProblem.Empty), repository.read(ByteArrayInputStream(ByteArray(0))))
        assertEquals(
            ImportRead.Refused(ImportProblem.TooLarge),
            repository.read(ByteArrayInputStream(ByteArray(10 * 1024 * 1024 + 1) { 'a'.code.toByte() }))
        )
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("Synthetic failure")
        }
        assertEquals(ImportRead.Refused(ImportProblem.Unreadable), repository.read(broken))
    }

    @Test
    fun `delete everything leaves no rows and no settings`() = runTest {
        val phone = phone()
        logSyntheticHistory()
        phone.settings.saveSetup(cycleLength = 30, periodLength = 4)
        phone.settings.setWelcomeDone(true)
        phone.settings.dismiss(CyclePrompt.StillGoing(day("2027-02-02"), periodDay = 8))
        phone.settings.dismiss(CyclePrompt.MissedPeriod(day("2027-02-02"), cycleDay = 60))
        phone.settings.setCategoryShown(LogCategory.SEX, shown = false)
        phone.settings.setLastExported(day("2027-03-20"))

        phone.repository.deleteEverything()

        assertEquals(emptyList<DayLog>() to emptyList<DayFeelings>(), database.everything())
        val tables = listOf("day_log", "pain", "body_symptoms", "mood", "energy", "sleep", "sex", "note")
        tables.forEach { table ->
            database.query("SELECT COUNT(*) FROM `$table`", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("rows left in $table", 0, cursor.getInt(0))
            }
        }
        assertTrue(phone.preferences.data.first().asMap().isEmpty())
        assertFalse(phone.settings.welcomeDone.first())
        assertEquals(null, phone.settings.lastExported.first())
    }
}
