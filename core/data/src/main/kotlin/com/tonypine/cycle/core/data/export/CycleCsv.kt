package com.tonypine.cycle.core.data.export

import com.tonypine.cycle.core.data.database.Converters
import com.tonypine.cycle.core.data.database.FeelingCodes
import com.tonypine.cycle.core.domain.CycleRules
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.Pain
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** One day of her log as an export carries it: her flow and period markers, and how she felt. */
data class LoggedDay(val log: DayLog, val feelings: DayFeelings = DayFeelings(log.date)) {
    init {
        require(log.date == feelings.date) { "One day: ${log.date} and ${feelings.date}" }
    }

    val date: LocalDate
        get() = log.date

    /** Nothing logged that day. */
    val isEmpty: Boolean
        get() = log.isEmpty && feelings.isEmpty
}

/** Her usual cycle and period lengths, in days, as she gave them at setup or in Settings. */
data class UsualLengths(val cycle: Int, val period: Int)

/** Why a file cannot be imported. Each one becomes a sentence that says what is wrong. */
sealed interface ImportProblem {
    /** The file could not be opened or read. */
    data object Unreadable : ImportProblem

    /** The file is not text, such as a photo or a PDF. */
    data object NotText : ImportProblem

    /** The file is far larger than any export. */
    data object TooLarge : ImportProblem

    /** The file has nothing in it. */
    data object Empty : ImportProblem

    /** The first line is not the column names Cycle writes. */
    data object NotAnExport : ImportProblem

    /** A quoted value that starts on [line] never closes. */
    data class UnclosedQuote(val line: Int) : ImportProblem

    /** The row on [line] has [found] values instead of [expected], one per column. */
    data class WrongValueCount(val line: Int, val found: Int, val expected: Int = CycleCsv.COLUMNS.size) :
        ImportProblem

    /** The row on [line] has [value], not an ISO date, as its date. */
    data class BadDate(val line: Int, val value: String) : ImportProblem

    /** The row on [line] is a second row for [date]. */
    data class RepeatedDate(val line: Int, val date: LocalDate) : ImportProblem

    /** The row on [line] has [value] in [column], which is not a value Cycle writes there. */
    data class UnknownValue(val line: Int, val column: String, val value: String) : ImportProblem

    /** The row on [line] says where it hurt but not how much. */
    data class PainWhereWithoutPain(val line: Int) : ImportProblem

    /** The row on [line] has a note longer than [DayFeelings.NOTE_MAX_LENGTH] characters. */
    data class NoteTooLong(val line: Int) : ImportProblem

    /** The settings line on [line] has [value], not a number of days in [range], as [setting]. */
    data class BadLength(val line: Int, val setting: String, val value: String, val range: IntRange) : ImportProblem

    /** The settings line on [line] is a second one for [setting]. */
    data class RepeatedSetting(val line: Int, val setting: String) : ImportProblem

    /** The settings give her usual cycle length without her usual period length, or the other way round. */
    data object OneUsualLength : ImportProblem
}

/** What reading an export gives: its days, or the first problem found. */
sealed interface CsvRead {
    /** The days with something logged, oldest first, and her usual lengths if the file has them. */
    data class Days(val days: List<LoggedDay>, val usualLengths: UsualLengths? = null) : CsvRead

    data class Refused(val problem: ImportProblem) : CsvRead
}

/**
 * Cycle's export: a CSV file (RFC 4180, UTF-8) with one row per day she logged, oldest first, and one
 * column per category, named in the first row. Values are the same fixed text codes the database
 * stores, a set is its codes joined by `;`, a period marker is `yes` or empty, and anything she did
 * not log is empty. A value with a comma, a quote or a line break, such as a note, is quoted.
 *
 * Once she has done setup, a blank line and a second table follow the days: her usual lengths, one
 * `setting,value` line each, so a restore brings back the estimates that use them.
 *
 * ```
 * date,flow,period_started,period_ended,pain,pain_where,body,mood,energy,sleep,sex,note
 * 2027-03-02,medium,yes,,moderate,cramps;lower_back,bloating,sensitive;low,low,badly,,"Tired, early night"
 *
 * setting,value
 * usual_cycle_length,30
 * usual_period_length,5
 * ```
 *
 * Reading checks every row and refuses the whole file on the first problem, so a file is imported in
 * full or not at all. A setting this version does not know, written by a later one, is skipped.
 */
internal object CycleCsv {
    val COLUMNS = listOf(
        "date",
        "flow",
        "period_started",
        "period_ended",
        "pain",
        "pain_where",
        "body",
        "mood",
        "energy",
        "sleep",
        "sex",
        "note"
    )

    val SETTINGS_COLUMNS = listOf("setting", "value")
    const val USUAL_CYCLE_LENGTH = "usual_cycle_length"
    const val USUAL_PERIOD_LENGTH = "usual_period_length"
    private val LENGTH_RANGES = mapOf(
        USUAL_CYCLE_LENGTH to CycleRules.USUAL_CYCLE_LENGTHS,
        USUAL_PERIOD_LENGTH to CycleRules.USUAL_PERIOD_LENGTHS
    )

    private const val LINE_END = "\r\n"
    private const val SET_SEPARATOR = ";"
    private const val YES = "yes"
    private const val BYTE_ORDER_MARK = '\uFEFF'

    fun write(days: List<LoggedDay>, out: Appendable, usualLengths: UsualLengths? = null) {
        out.append(COLUMNS.joinToString(",")).append(LINE_END)
        days.sortedBy { it.date }.forEach { day ->
            out.append(day.toValues().joinToString(",", transform = ::quote)).append(LINE_END)
        }
        if (usualLengths != null) {
            out.append(LINE_END).append(SETTINGS_COLUMNS.joinToString(",")).append(LINE_END)
            out.append("$USUAL_CYCLE_LENGTH,${usualLengths.cycle}").append(LINE_END)
            out.append("$USUAL_PERIOD_LENGTH,${usualLengths.period}").append(LINE_END)
        }
    }

    fun read(text: String): CsvRead {
        val records = when (val parsed = records(text.removePrefix(BYTE_ORDER_MARK.toString()))) {
            is Records.Parsed -> parsed.records
            is Records.Unclosed -> return CsvRead.Refused(ImportProblem.UnclosedQuote(parsed.line))
        }
        val header = records.firstOrNull() ?: return CsvRead.Refused(ImportProblem.Empty)
        if (header.values.map { it.trim().lowercase() } != COLUMNS) return CsvRead.Refused(ImportProblem.NotAnExport)

        val rows = records.drop(1)
        val settingsAt = rows.indexOfFirst { record -> record.values.map { it.trim().lowercase() } == SETTINGS_COLUMNS }
        val dayRows = if (settingsAt < 0) rows else rows.subList(0, settingsAt)
        val settingRows = if (settingsAt < 0) emptyList() else rows.subList(settingsAt + 1, rows.size)

        val days = mutableListOf<LoggedDay>()
        val seen = mutableSetOf<LocalDate>()
        for (record in dayRows) {
            val day = when (val row = record.toDay()) {
                is Row.Day -> row.day
                is Row.Problem -> return CsvRead.Refused(row.problem)
            }
            if (!seen.add(day.date)) return CsvRead.Refused(ImportProblem.RepeatedDate(record.line, day.date))
            if (!day.isEmpty) days += day
        }
        return when (val lengths = usualLengths(settingRows)) {
            is Settings.Lengths -> CsvRead.Days(days.sortedBy { it.date }, lengths.lengths)
            is Settings.Problem -> CsvRead.Refused(lengths.problem)
        }
    }

    private sealed interface Settings {
        class Lengths(val lengths: UsualLengths?) : Settings

        class Problem(val problem: ImportProblem) : Settings
    }

    /** The usual lengths in the settings table's [records]: both, or neither. */
    private fun usualLengths(records: List<Record>): Settings {
        val lengths = mutableMapOf<String, Int>()
        for (record in records) {
            if (record.values.size != SETTINGS_COLUMNS.size) {
                return Settings.Problem(
                    ImportProblem.WrongValueCount(record.line, record.values.size, SETTINGS_COLUMNS.size)
                )
            }
            val (name, text) = record.values.map { it.trim() }
            val range = LENGTH_RANGES[name] ?: continue
            if (name in lengths) return Settings.Problem(ImportProblem.RepeatedSetting(record.line, name))
            val days = text.toIntOrNull()?.takeIf { it in range }
                ?: return Settings.Problem(ImportProblem.BadLength(record.line, name, text, range))
            lengths[name] = days
        }
        val cycle = lengths[USUAL_CYCLE_LENGTH]
        val period = lengths[USUAL_PERIOD_LENGTH]
        return when {
            cycle != null && period != null -> Settings.Lengths(UsualLengths(cycle, period))
            cycle == null && period == null -> Settings.Lengths(null)
            else -> Settings.Problem(ImportProblem.OneUsualLength)
        }
    }

    private fun LoggedDay.toValues(): List<String> = listOf(
        date.toString(),
        log.flow?.let(Converters.FLOW::encode).orEmpty(),
        if (log.periodStarted) YES else "",
        if (log.periodEnded) YES else "",
        feelings.pain?.level?.let(FeelingCodes.PAIN_LEVEL::encode).orEmpty(),
        feelings.pain?.kinds.orEmpty().let(FeelingCodes.PAIN_KIND::encodeAll).joinToString(SET_SEPARATOR),
        FeelingCodes.BODY.encodeAll(feelings.body).joinToString(SET_SEPARATOR),
        FeelingCodes.MOOD.encodeAll(feelings.moods).joinToString(SET_SEPARATOR),
        feelings.energy?.let(FeelingCodes.ENERGY::encode).orEmpty(),
        feelings.sleep?.let(FeelingCodes.SLEEP::encode).orEmpty(),
        feelings.sex?.let(FeelingCodes.SEX::encode).orEmpty(),
        feelings.note
    )

    private fun quote(value: String): String = if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

    private sealed interface Row {
        data class Day(val day: LoggedDay) : Row

        data class Problem(val problem: ImportProblem) : Row
    }

    private class UnknownValue(val column: String, val value: String) : Exception()

    private fun Record.toDay(): Row {
        if (values.size != COLUMNS.size) return Row.Problem(ImportProblem.WrongValueCount(line, values.size))
        val byColumn = COLUMNS.zip(values).toMap()
        val dateText = byColumn.getValue("date").trim()
        val date = try {
            LocalDate.parse(dateText)
        } catch (_: DateTimeParseException) {
            return Row.Problem(ImportProblem.BadDate(line, dateText))
        }

        fun <T : Enum<T>> one(column: String, codes: FeelingCodes.Codes<T>): T? {
            val text = byColumn.getValue(column).trim()
            if (text.isEmpty()) return null
            return codes.decode(text) ?: throw UnknownValue(column, text)
        }

        fun <T : Enum<T>> set(column: String, codes: FeelingCodes.Codes<T>): Set<T> {
            val text = byColumn.getValue(column).trim()
            if (text.isEmpty()) return emptySet()
            return text.split(SET_SEPARATOR).mapTo(mutableSetOf()) { part ->
                codes.decode(part.trim()) ?: throw UnknownValue(column, part.trim())
            }
        }

        fun marker(column: String): Boolean = when (val text = byColumn.getValue(column).trim()) {
            "" -> false
            YES -> true
            else -> throw UnknownValue(column, text)
        }

        return try {
            val painLevel = one("pain", FeelingCodes.PAIN_LEVEL)
            val painWhere = set("pain_where", FeelingCodes.PAIN_KIND)
            if (painLevel == null && painWhere.isNotEmpty()) {
                return Row.Problem(ImportProblem.PainWhereWithoutPain(line))
            }
            val note = byColumn.getValue("note").trim()
            if (note.length > DayFeelings.NOTE_MAX_LENGTH) return Row.Problem(ImportProblem.NoteTooLong(line))
            val log = DayLog(
                date = date,
                flow = one("flow", Converters.FLOW),
                periodStarted = marker("period_started"),
                periodEnded = marker("period_ended")
            )
            val feelings = DayFeelings(
                date = date,
                pain = painLevel?.let { Pain(it, painWhere) },
                body = set("body", FeelingCodes.BODY),
                moods = set("mood", FeelingCodes.MOOD),
                energy = one("energy", FeelingCodes.ENERGY),
                sleep = one("sleep", FeelingCodes.SLEEP),
                sex = one("sex", FeelingCodes.SEX),
                note = note
            ).normalized()
            Row.Day(LoggedDay(log, feelings))
        } catch (unknown: UnknownValue) {
            Row.Problem(ImportProblem.UnknownValue(line, unknown.column, unknown.value))
        }
    }

    /** One CSV record: its values, and the line of the file it starts on, from 1. */
    private class Record(val line: Int, val values: List<String>)

    private sealed interface Records {
        class Parsed(val records: List<Record>) : Records

        class Unclosed(val line: Int) : Records
    }

    /**
     * Splits [text] into records by RFC 4180: commas between values, a line break (CRLF or LF) between
     * records, and double quotes around a value that holds either, with `""` for a quote inside it.
     * Blank lines are skipped.
     */
    private fun records(text: String): Records {
        val records = mutableListOf<Record>()
        val values = mutableListOf<String>()
        val value = StringBuilder()
        var line = 1
        var recordLine = 1
        var quoted = false
        var index = 0

        fun endRecord() {
            values += value.toString()
            value.clear()
            if (values.size > 1 || values.single().isNotEmpty()) records += Record(recordLine, values.toList())
            values.clear()
            line++
            recordLine = line
        }

        while (index < text.length) {
            val char = text[index]
            if (quoted) {
                when {
                    char == '"' && text.getOrNull(index + 1) == '"' -> {
                        value.append('"')
                        index++
                    }

                    char == '"' -> quoted = false

                    else -> {
                        if (char == '\n') line++
                        value.append(char)
                    }
                }
            } else {
                when (char) {
                    '"' -> if (value.isEmpty()) quoted = true else value.append(char)

                    ',' -> {
                        values += value.toString()
                        value.clear()
                    }

                    '\r' -> {
                        if (text.getOrNull(index + 1) == '\n') index++
                        endRecord()
                    }

                    '\n' -> endRecord()

                    else -> value.append(char)
                }
            }
            index++
        }
        if (quoted) return Records.Unclosed(recordLine)
        if (value.isNotEmpty() || values.isNotEmpty()) endRecord()
        return Records.Parsed(records)
    }
}
