package com.tonypine.cycle.core.data.export

import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
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
import org.junit.Assert.assertEquals
import org.junit.Test

/** The export's format, and every way a file is refused. Synthetic days only. */
class CycleCsvTest {
    private val header = "date,flow,period_started,period_ended,pain,pain_where,body,mood,energy,sleep,sex,note"

    private fun write(vararg days: LoggedDay): String = StringBuilder().also {
        CycleCsv.write(days.toList(), it)
    }.toString()

    private fun read(vararg lines: String): CsvRead = CycleCsv.read(lines.joinToString("\r\n", postfix = "\r\n"))

    private fun refused(problem: ImportProblem, vararg lines: String) =
        assertEquals(CsvRead.Refused(problem), read(*lines))

    @Test
    fun `one row per day, one column per category, empty for what she did not log`() {
        val csv = write(
            LoggedDay(
                DayLog(day("2027-03-02"), FlowLevel.MEDIUM, periodStarted = true),
                DayFeelings(
                    day("2027-03-02"),
                    pain = Pain(PainLevel.MODERATE, setOf(PainKind.LOWER_BACK, PainKind.CRAMPS)),
                    body = setOf(BodySymptom.BLOATING),
                    moods = setOf(Mood.LOW, Mood.SENSITIVE),
                    energy = EnergyLevel.LOW,
                    sleep = SleepQuality.BADLY,
                    sex = SexualActivity.PROTECTED,
                    note = "Synthetic note"
                )
            ),
            LoggedDay(DayLog(day("2027-03-06"), periodEnded = true))
        )

        assertEquals(
            "$header\r\n" +
                "2027-03-02,medium,yes,,moderate,cramps;lower_back,bloating,sensitive;low,low,badly,protected," +
                "Synthetic note\r\n" +
                "2027-03-06,,,yes,,,,,,,,\r\n",
            csv
        )
    }

    @Test
    fun `her usual lengths follow the days in a settings table and read back the same`() {
        val day = LoggedDay(DayLog(day("2027-03-02"), FlowLevel.LIGHT))

        val csv = StringBuilder().also { CycleCsv.write(listOf(day), it, UsualLengths(30, 4)) }.toString()

        assertEquals(
            "$header\r\n2027-03-02,light,,,,,,,,,,\r\n\r\nsetting,value\r\nusual_cycle_length,30\r\n" +
                "usual_period_length,4\r\n",
            csv
        )
        assertEquals(CsvRead.Days(listOf(day), UsualLengths(30, 4)), CycleCsv.read(csv))
    }

    @Test
    fun `a file without settings has no usual lengths, and a setting from a later version is skipped`() {
        assertEquals(CsvRead.Days(emptyList()), read(header))
        assertEquals(
            CsvRead.Days(emptyList(), UsualLengths(26, 6)),
            read(header, "setting,value", "usual_period_length, 6 ", "theme,dark", "usual_cycle_length,26")
        )
    }

    @Test
    fun `a usual length Cycle does not accept is refused with its line`() {
        refused(
            ImportProblem.BadLength(3, "usual_cycle_length", "12", 15..90),
            header,
            "setting,value",
            "usual_cycle_length,12",
            "usual_period_length,5"
        )
        refused(
            ImportProblem.BadLength(4, "usual_period_length", "five", 1..14),
            header,
            "setting,value",
            "usual_cycle_length,28",
            "usual_period_length,five"
        )
    }

    @Test
    fun `a settings table with a repeated, a missing or a malformed length is refused`() {
        refused(
            ImportProblem.RepeatedSetting(4, "usual_cycle_length"),
            header,
            "setting,value",
            "usual_cycle_length,28",
            "usual_cycle_length,30",
            "usual_period_length,5"
        )
        refused(ImportProblem.OneUsualLength, header, "setting,value", "usual_cycle_length,28")
        refused(
            ImportProblem.WrongValueCount(line = 3, found = 3, expected = 2),
            header,
            "setting,value",
            "usual_cycle_length,28,5"
        )
    }

    @Test
    fun `a note with commas, quotes and line breaks is quoted and reads back the same`() {
        val note = "Synthetic: \"tired\", early night\nand a second line"
        val day = LoggedDay(DayLog(day("2027-03-02")), DayFeelings(day("2027-03-02"), note = note))

        val csv = write(day)

        assertEquals(
            "$header\r\n2027-03-02,,,,,,,,,,,\"Synthetic: \"\"tired\"\", early night\nand a second line\"\r\n",
            csv
        )
        assertEquals(CsvRead.Days(listOf(day)), CycleCsv.read(csv))
    }

    @Test
    fun `reads LF line ends, a byte order mark, spaces around codes and no final line break`() {
        val text = "\uFEFF$header\n2027-03-02, light ,yes,,,,,calm ; happy,,,,\n\n2027-03-01,spotting,,,,,,,,,,"

        assertEquals(
            CsvRead.Days(
                listOf(
                    LoggedDay(DayLog(day("2027-03-01"), FlowLevel.SPOTTING)),
                    LoggedDay(
                        DayLog(day("2027-03-02"), FlowLevel.LIGHT, periodStarted = true),
                        DayFeelings(day("2027-03-02"), moods = setOf(Mood.CALM, Mood.HAPPY))
                    )
                )
            ),
            CycleCsv.read(text)
        )
    }

    @Test
    fun `a row with nothing logged is skipped`() {
        assertEquals(CsvRead.Days(emptyList()), read(header, "2027-03-02,,,,,,,,,,,"))
    }

    @Test
    fun `an empty file is refused`() = refused(ImportProblem.Empty)

    @Test
    fun `a file without Cycle's column names is refused`() =
        refused(ImportProblem.NotAnExport, "Date,Flow,Notes", "2027-03-02,light,")

    @Test
    fun `a quote that never closes is refused on the line it opens`() =
        refused(ImportProblem.UnclosedQuote(line = 3), header, "2027-03-01,,,,,,,,,,,", "2027-03-02,,,,,,,,,,,\"open")

    @Test
    fun `a row with a value missing is refused`() =
        refused(ImportProblem.WrongValueCount(line = 2, found = 11), header, "2027-03-02,,,,,,,,,,")

    @Test
    fun `a date Cycle cannot read is refused`() =
        refused(ImportProblem.BadDate(line = 2, value = "02/03/2027"), header, "02/03/2027,light,,,,,,,,,,")

    @Test
    fun `a second row for the same day is refused`() = refused(
        ImportProblem.RepeatedDate(line = 3, date = day("2027-03-02")),
        header,
        "2027-03-02,light,,,,,,,,,,",
        "2027-03-02,heavy,,,,,,,,,,"
    )

    @Test
    fun `a value Cycle does not write is refused with its column`() {
        refused(ImportProblem.UnknownValue(2, "flow", "very heavy"), header, "2027-03-02,very heavy,,,,,,,,,,")
        refused(ImportProblem.UnknownValue(2, "period_started", "no"), header, "2027-03-02,,no,,,,,,,,,")
        refused(ImportProblem.UnknownValue(2, "mood", "grumpy"), header, "2027-03-02,,,,,,,calm;grumpy,,,,")
    }

    @Test
    fun `where it hurt without how much is refused`() =
        refused(ImportProblem.PainWhereWithoutPain(line = 2), header, "2027-03-02,,,,,cramps,,,,,,")

    @Test
    fun `a note longer than the day log allows is refused`() =
        refused(ImportProblem.NoteTooLong(line = 2), header, "2027-03-02,,,,,,,,,,,${"a".repeat(501)}")

    private val stretchHeader = "method,started,stopped,breaks"
    private val implant = ContraceptionStretch(ContraceptionMethod.IMPLANT, started = null, stopped = day("2027-01-20"))
    private val pill =
        ContraceptionStretch(ContraceptionMethod.COMBINED_PILL, day("2027-02-01"), breaks = Breaks.MONTHLY)

    @Test
    fun `her contraception follows the settings in a third table and reads back the same`() {
        val day = LoggedDay(DayLog(day("2027-03-02"), FlowLevel.LIGHT))

        val csv = StringBuilder().also { CycleCsv.write(listOf(day), it, UsualLengths(30, 4), listOf(pill, implant)) }
            .toString()

        assertEquals(
            "$header\r\n2027-03-02,light,,,,,,,,,,\r\n\r\nsetting,value\r\nusual_cycle_length,30\r\n" +
                "usual_period_length,4\r\n\r\n$stretchHeader\r\nimplant,,2027-01-20,\r\n" +
                "combined_pill,2027-02-01,,monthly\r\n",
            csv
        )
        assertEquals(
            CsvRead.Days(
                listOf(day),
                UsualLengths(30, 4),
                listOf(ImportedStretch(9, implant), ImportedStretch(10, pill))
            ),
            CycleCsv.read(csv)
        )
    }

    @Test
    fun `her contraception reads without the settings table, or before it`() {
        val stretches = listOf(ImportedStretch(3, implant))

        assertEquals(
            CsvRead.Days(emptyList(), stretches = stretches),
            read(header, stretchHeader, "implant,,2027-01-20,")
        )
        assertEquals(
            CsvRead.Days(emptyList(), UsualLengths(26, 6), stretches),
            read(
                header,
                stretchHeader,
                "implant,,2027-01-20,",
                "setting,value",
                "usual_cycle_length,26",
                "usual_period_length,6"
            )
        )
    }

    @Test
    fun `every method and kind of breaks is written with its code`() {
        val stretches = ContraceptionMethod.entries.mapIndexed { index, method ->
            val start = day("2027-01-01").plusMonths(index.toLong())
            ContraceptionStretch(
                method,
                start,
                start.plusDays(20),
                Breaks.entries[index % 3].takeIf {
                    method.isCombined
                }
            )
        }

        val csv = StringBuilder().also { CycleCsv.write(emptyList(), it, stretches = stretches) }.toString()

        assertEquals(
            listOf(
                "combined_pill,2027-01-01,2027-01-21,monthly",
                "progestogen_pill,2027-02-01,2027-02-21,",
                "patch,2027-03-01,2027-03-21,none",
                "ring,2027-04-01,2027-04-21,monthly",
                "implant,2027-05-01,2027-05-21,",
                "hormonal_iud,2027-06-01,2027-06-21,",
                "copper_iud,2027-07-01,2027-07-21,",
                "injection,2027-08-01,2027-08-21,"
            ),
            csv.lines().drop(3).filter { it.isNotEmpty() }
        )
        assertEquals(stretches, (CycleCsv.read(csv) as CsvRead.Days).stretches.map { it.stretch })
    }

    @Test
    fun `a stretch that cannot be read refuses the file with its line`() {
        refused(ImportProblem.WrongValueCount(3, 3, 4), header, stretchHeader, "implant,2027-01-01,")
        refused(ImportProblem.UnknownValue(3, "method", "diaphragm"), header, stretchHeader, "diaphragm,2027-01-01,,")
        refused(ImportProblem.BadDate(3, "01/01/2027"), header, stretchHeader, "implant,01/01/2027,,")
        refused(ImportProblem.BadDate(3, "soon"), header, stretchHeader, "implant,2027-01-01,soon,")
        refused(ImportProblem.MissingBreaks(3), header, stretchHeader, "ring,2027-01-01,,")
        refused(ImportProblem.UnknownValue(3, "breaks", "weekly"), header, stretchHeader, "ring,2027-01-01,,weekly")
        refused(
            ImportProblem.UnknownValue(3, "breaks", "monthly"),
            header,
            stretchHeader,
            "implant,2027-01-01,,monthly"
        )
        refused(ImportProblem.StopsBeforeStarts(3), header, stretchHeader, "implant,2027-01-02,2027-01-01,")
    }

    @Test
    fun `rows that overlap each other refuse the file naming both lines`() {
        refused(
            ImportProblem.OverlappingRows(3, 4),
            header,
            stretchHeader,
            "implant,2027-01-01,2027-03-01,",
            "patch,2027-03-01,,monthly"
        )
        refused(
            ImportProblem.OverlappingRows(3, 4),
            header,
            stretchHeader,
            "implant,,2027-03-01,",
            "injection,,2026-01-01,"
        )
    }

    @Test
    fun `identical rows refuse the file naming both lines`() {
        refused(
            ImportProblem.OverlappingRows(3, 4),
            header,
            stretchHeader,
            "implant,2026-11-09,2027-11-03,",
            "implant,2026-11-09,2027-11-03,"
        )
        refused(ImportProblem.OverlappingRows(3, 4), header, stretchHeader, "copper_iud,,,", "copper_iud,,,")
    }

    @Test
    fun `an overlap names the first line reading down that overlaps, and the earliest row it overlaps`() {
        refused(
            ImportProblem.OverlappingRows(3, 5),
            header,
            stretchHeader,
            "implant,2027-01-01,2027-02-01,",
            "copper_iud,2027-03-01,2027-04-01,",
            "injection,2027-01-15,2027-03-15,",
            "hormonal_iud,2027-03-20,,"
        )
        refused(
            ImportProblem.OverlappingRows(4, 5),
            header,
            stretchHeader,
            "copper_iud,2027-03-01,2027-04-01,",
            "implant,2027-01-01,2027-02-01,",
            "injection,2027-01-15,2027-01-20,"
        )
    }

    @Test
    fun `an overlap is found top to bottom with the file's other problems`() {
        refused(
            ImportProblem.OverlappingRows(3, 4),
            header,
            stretchHeader,
            "implant,2027-01-01,,",
            "copper_iud,2027-02-01,,",
            "implant,soon,,"
        )
        refused(
            ImportProblem.BadDate(4, "soon"),
            header,
            stretchHeader,
            "implant,2027-01-01,,",
            "implant,soon,,",
            "copper_iud,2027-02-01,,"
        )
    }
}
