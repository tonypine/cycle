package com.tonypine.cycle.core.data.export

import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.model.BodySymptom
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
}
