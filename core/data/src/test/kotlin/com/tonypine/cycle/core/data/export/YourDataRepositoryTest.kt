package com.tonypine.cycle.core.data.export

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.database.toEntity
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.inMemoryDatabase
import com.tonypine.cycle.core.data.repository.ContraceptionRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
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
    private val contraception = ContraceptionRepository(database)

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
        dayLogDao().getAll().map { DayLog(it.date, it.flow, it.periodStarted, it.periodEnded) } to
            feelingsDao().getAll()

    private suspend fun YourDataRepository.exportBytes(): ByteArray =
        ByteArrayOutputStream().also { export(it) }.toByteArray()

    /** Two synthetic stretches: an implant with no start date, then the pill, still on it. */
    private suspend fun logSyntheticContraception() {
        contraception.start(ContraceptionMethod.IMPLANT, null, started = null, today = day("2026-12-01"))
        contraception.start(
            ContraceptionMethod.COMBINED_PILL,
            Breaks.MONTHLY,
            day("2027-02-15"),
            today = day("2027-03-20")
        )
    }

    private suspend fun stretches() = contraception.observeStretches().first().map { it.copy(id = 0) }

    @Test
    fun `exporting then importing round-trips the synthetic history exactly`() = runTest {
        val phone = phone()
        logSyntheticHistory()
        logSyntheticContraception()
        phone.settings.saveSetup(cycleLength = 30, periodLength = 4)
        val before = database.everything()
        val stretchesBefore = stretches()
        val settingsBefore = phone.settings.settings.first()
        val exported = phone.repository.exportBytes()

        phone.repository.deleteEverything()
        val read = phone.repository.read(ByteArrayInputStream(exported)) as ImportRead.Ready
        val added = phone.repository.import(read.file)

        assertEquals(9, read.newDays)
        assertEquals(2, read.newStretches)
        assertEquals(9, added)
        assertEquals(before, database.everything())
        assertEquals(2, stretchesBefore.size)
        assertEquals(stretchesBefore, stretches())
        assertEquals(settingsBefore, phone.settings.settings.first())
        assertEquals(exported.decodeToString(), phone.repository.exportBytes().decodeToString())
    }

    @Test
    fun `a file from before contraception was recorded still imports`() = runTest {
        val phone = phone()
        val older = "date,flow,period_started,period_ended,pain,pain_where,body,mood,energy,sleep,sex,note\r\n" +
            "2027-03-02,medium,yes,,,,,,,,,\r\n\r\nsetting,value\r\nusual_cycle_length,30\r\nusual_period_length,4\r\n"

        val read = phone.repository.read(ByteArrayInputStream(older.toByteArray())) as ImportRead.Ready
        phone.repository.import(read.file)

        assertEquals(1, read.newDays)
        assertEquals(0, read.newStretches)
        assertFalse(read.hasStretches)
        assertEquals(emptyList<Any>(), stretches())
        assertEquals(
            listOf(DayLog(day("2027-03-02"), FlowLevel.MEDIUM, periodStarted = true)),
            database.everything().first
        )
    }

    /** The implant from 9 November 2026 to 3 November 2027, then the pill from 20 November 2027. */
    private suspend fun logImplantThenPill() {
        val dao = database.contraceptionDao()
        dao.upsert(ContraceptionStretch(ContraceptionMethod.IMPLANT, day("2026-11-09"), day("2027-11-03")).toEntity())
        dao.upsert(
            ContraceptionStretch(ContraceptionMethod.COMBINED_PILL, day("2027-11-20"), breaks = Breaks.MONTHLY)
                .toEntity()
        )
    }

    /** A file with no days and these lines of contraception, from line 3. */
    private fun methodsFile(vararg rows: String): ByteArrayInputStream = ByteArrayInputStream(
        (
            "date,flow,period_started,period_ended,pain,pain_where,body,mood,energy,sleep,sex,note\r\n" +
                "method,started,stopped,breaks\r\n" + rows.joinToString("") { "$it\r\n" }
            ).toByteArray()
    )

    @Test
    fun `re-importing her export with methods on the phone adds nothing`() = runTest {
        val phone = phone()
        logSyntheticHistory()
        logSyntheticContraception()
        val exported = phone.repository.exportBytes()
        val before = database.everything()
        val stretchesBefore = stretches()

        val read = phone.repository.read(ByteArrayInputStream(exported)) as ImportRead.Ready
        val added = phone.repository.import(read.file)

        assertEquals(0, read.newDays)
        assertEquals(0, read.newStretches)
        assertTrue(read.hasStretches)
        assertEquals(0, added)
        assertEquals(before, database.everything())
        assertEquals(stretchesBefore, stretches())
    }

    @Test
    fun `a row with the method and start of a stretch on the phone is skipped, whatever its stop or breaks`() =
        runTest {
            val phone = phone()
            logImplantThenPill()
            val stretchesBefore = stretches()

            // An export from before she marked the implant stopped: skipped, not checked against the pill.
            val beforeStopping = methodsFile("implant,2026-11-09,,")
            val otherBreaks = methodsFile("combined_pill,2027-11-20,2027-12-01,none")

            listOf(beforeStopping, otherBreaks).forEach { file ->
                val read = phone.repository.read(file) as ImportRead.Ready
                phone.repository.import(read.file)

                assertEquals(0, read.newStretches)
                assertTrue(read.hasStretches)
                assertEquals(stretchesBefore, stretches())
            }
        }

    @Test
    fun `a null start matches a null start on the phone`() = runTest {
        val phone = phone()
        logSyntheticContraception()
        val stretchesBefore = stretches()

        val read = phone.repository.read(methodsFile("implant,,2026-12-31,")) as ImportRead.Ready
        phone.repository.import(read.file)

        assertEquals(0, read.newStretches)
        assertEquals(stretchesBefore, stretches())
        assertEquals(
            ImportRead.Refused(ImportProblem.OverlappingMethod(3)),
            phone.repository.read(methodsFile("copper_iud,,2026-12-31,"))
        )
    }

    @Test
    fun `only the rows new to the phone are counted and added`() = runTest {
        val phone = phone()
        logImplantThenPill()

        val read = phone.repository.read(
            methodsFile("copper_iud,2026-01-10,2026-10-31,", "implant,2026-11-09,2027-11-03,")
        ) as ImportRead.Ready
        phone.repository.import(read.file)

        assertEquals(1, read.newStretches)
        assertEquals(
            listOf(
                ContraceptionStretch(ContraceptionMethod.COPPER_IUD, day("2026-01-10"), day("2026-10-31")),
                ContraceptionStretch(ContraceptionMethod.IMPLANT, day("2026-11-09"), day("2027-11-03")),
                ContraceptionStretch(ContraceptionMethod.COMBINED_PILL, day("2027-11-20"), breaks = Breaks.MONTHLY)
            ),
            stretches()
        )
    }

    @Test
    fun `a new row that overlaps a stretch on the phone refuses the file at its line, and nothing is written`() =
        runTest {
            val phone = phone()
            logImplantThenPill()
            val stretchesBefore = stretches()

            assertEquals(
                ImportRead.Refused(ImportProblem.OverlappingMethod(4)),
                phone.repository.read(
                    methodsFile("implant,2026-11-09,2027-11-03,", "copper_iud,2027-11-04,2027-12-01,")
                )
            )
            assertEquals(stretchesBefore, stretches())
        }

    @Test
    fun `rows that overlap each other refuse the file even when one is on the phone`() = runTest {
        val phone = phone()
        logImplantThenPill()

        assertEquals(
            ImportRead.Refused(ImportProblem.OverlappingRows(3, 4)),
            phone.repository.read(
                methodsFile("implant,2026-11-09,2027-11-03,", "implant,2027-01-01,2027-06-01,")
            )
        )
        assertEquals(
            ImportRead.Refused(ImportProblem.OverlappingRows(3, 4)),
            phone.repository.read(
                methodsFile("implant,2026-11-09,2027-11-03,", "implant,2026-11-09,2027-11-03,")
            )
        )
    }

    @Test
    fun `stretches that do not overlap the phone's are added`() = runTest {
        val phone = phone()
        contraception.start(ContraceptionMethod.COPPER_IUD, null, day("2027-03-01"), today = day("2027-03-20"))
        val file = "date,flow,period_started,period_ended,pain,pain_where,body,mood,energy,sleep,sex,note\r\n" +
            "method,started,stopped,breaks\r\nimplant,2026-01-10,2027-02-28,\r\n"

        val read = phone.repository.read(ByteArrayInputStream(file.toByteArray())) as ImportRead.Ready
        phone.repository.import(read.file)

        assertEquals(1, read.newStretches)
        assertEquals(
            listOf(
                ContraceptionStretch(ContraceptionMethod.IMPLANT, day("2026-01-10"), day("2027-02-28")),
                ContraceptionStretch(ContraceptionMethod.COPPER_IUD, day("2027-03-01"))
            ),
            stretches()
        )
    }

    @Test
    fun `the usual lengths go in the export only once she has done setup`() = runTest {
        val phone = phone()
        logSyntheticHistory()

        assertFalse("usual_cycle_length" in phone.repository.exportBytes().decodeToString())

        phone.settings.saveSetup(cycleLength = 30, periodLength = 4)
        assertTrue(
            "usual_cycle_length,30\r\nusual_period_length,4\r\n" in phone.repository.exportBytes().decodeToString()
        )
    }

    @Test
    fun `a file with only the usual lengths restores them after delete everything`() = runTest {
        val phone = phone()
        phone.settings.saveSetup(cycleLength = 30, periodLength = 4)
        val exported = phone.repository.exportBytes()

        val onSetUpPhone = phone.repository.read(ByteArrayInputStream(exported)) as ImportRead.Ready
        assertEquals(0 to false, onSetUpPhone.newDays to onSetUpPhone.restoresLengths)

        phone.repository.deleteEverything()
        val read = phone.repository.read(ByteArrayInputStream(exported)) as ImportRead.Ready
        assertEquals(0 to true, read.newDays to read.restoresLengths)
        assertEquals(0, phone.repository.import(read.file))

        val settings = phone.settings.settings.first()
        assertEquals(
            Triple(30, 4, true),
            Triple(settings.usualCycleLength, settings.usualPeriodLength, settings.setupDone)
        )
    }

    @Test
    fun `usual lengths she gave on the phone win over the file's`() = runTest {
        val phone = phone()
        phone.settings.saveSetup(cycleLength = 30, periodLength = 4)
        val exported = phone.repository.exportBytes()
        phone.settings.saveSetup(cycleLength = 26, periodLength = 6)

        phone.repository.import((phone.repository.read(ByteArrayInputStream(exported)) as ImportRead.Ready).file)

        val settings = phone.settings.settings.first()
        assertEquals(26 to 6, settings.usualCycleLength to settings.usualPeriodLength)
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
        assertFalse(read.restoresLengths)
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
        logSyntheticContraception()
        phone.settings.saveSetup(cycleLength = 30, periodLength = 4)
        phone.settings.setWelcomeDone(true)
        phone.settings.dismiss(CyclePrompt.StillGoing(day("2027-02-02"), periodDay = 8))
        phone.settings.dismiss(CyclePrompt.MissedPeriod(day("2027-02-02"), cycleDay = 60))
        phone.settings.setCategoryShown(LogCategory.SEX, shown = false)
        phone.settings.setLastExported(day("2027-03-20"))

        phone.repository.deleteEverything()

        assertEquals(emptyList<DayLog>() to emptyList<DayFeelings>(), database.everything())
        val tables =
            listOf("day_log", "pain", "body_symptoms", "mood", "energy", "sleep", "sex", "note", "contraception")
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
