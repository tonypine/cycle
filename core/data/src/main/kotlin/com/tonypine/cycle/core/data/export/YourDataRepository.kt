package com.tonypine.cycle.core.data.export

import androidx.room.withTransaction
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.database.toEntity
import com.tonypine.cycle.core.data.database.toModel
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** A Cycle export, read and checked, waiting for her to confirm the import. */
class ImportFile internal constructor(
    internal val days: List<LoggedDay>,
    internal val usualLengths: UsualLengths?,
    internal val stretches: List<ImportedStretch> = emptyList()
)

/** What reading a file for import gives. */
sealed interface ImportRead {
    /**
     * The file can be imported: [newDays] of its days are not on the phone yet, [restoresLengths]
     * says whether its usual lengths would be restored, on a phone where she has not done setup, and
     * [newStretches] of contraception would be added.
     */
    data class Ready(val file: ImportFile, val newDays: Int, val restoresLengths: Boolean, val newStretches: Int = 0) :
        ImportRead

    /** The file cannot be imported, because of [problem]. */
    data class Refused(val problem: ImportProblem) : ImportRead
}

/**
 * "Your data" in Settings: her whole log out to a file ([CycleCsv]) and back in, and "Delete
 * everything". Files go only where she picks, through the streams the caller opens; nothing here
 * talks to the network.
 */
class YourDataRepository(
    private val database: CycleDatabase,
    private val settings: SettingsRepository,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    private val dayLogDao = database.dayLogDao()
    private val feelingsDao = database.feelingsDao()
    private val contraceptionDao = database.contraceptionDao()

    /**
     * Writes every day she logged and her contraception to [output], read in one transaction, and her
     * usual lengths once she has done setup, then closes it.
     */
    suspend fun export(output: OutputStream) {
        val (days, stretches) = database.withTransaction { allDays() to stretches() }
        val usualLengths = settings.settings.first()
            .takeIf { it.setupDone }
            ?.let { UsualLengths(it.usualCycleLength, it.usualPeriodLength) }
        withContext(io) {
            output.bufferedWriter(Charsets.UTF_8).use { CycleCsv.write(days, it, usualLengths, stretches) }
        }
    }

    /**
     * Reads and checks the export in [input], and closes it. Nothing is written: [import] does that
     * once she confirms.
     */
    suspend fun read(input: InputStream): ImportRead {
        val text = when (val decoded = withContext(io) { decode(input) }) {
            is Decoded.Text -> decoded.text
            is Decoded.Problem -> return ImportRead.Refused(decoded.problem)
        }
        return when (val read = CycleCsv.read(text)) {
            is CsvRead.Refused -> ImportRead.Refused(read.problem)

            is CsvRead.Days -> {
                val (onPhone, stretchesOnPhone) = database.withTransaction { loggedDates() to stretches() }
                read.stretches.firstOrNull { imported -> stretchesOnPhone.any { it.overlaps(imported.stretch) } }?.let {
                    return ImportRead.Refused(ImportProblem.OverlappingMethod(it.line))
                }
                ImportRead.Ready(
                    ImportFile(read.days, read.usualLengths, read.stretches),
                    newDays = read.days.count { it.date !in onPhone },
                    restoresLengths = read.usualLengths != null && !settings.settings.first().setupDone,
                    newStretches = read.stretches.size
                )
            }
        }
    }

    /**
     * Adds the days of [file] that are not on the phone yet, and its stretches of contraception that
     * overlap none on the phone, in one transaction, and returns how many days. A day she already
     * logged on the phone stays as it is, whatever the file says about it. The file's usual lengths
     * are restored only on a phone where she has not done setup, such as after "Delete everything":
     * lengths she gave on the phone win too.
     */
    suspend fun import(file: ImportFile): Int {
        val added = database.withTransaction {
            val onPhone = loggedDates()
            val added = file.days.filter { it.date !in onPhone }
            added.forEach { day ->
                if (!day.log.isEmpty) dayLogDao.upsert(day.log.toEntity())
                if (!day.feelings.isEmpty) feelingsDao.save(day.feelings)
            }
            val stretchesOnPhone = stretches()
            file.stretches.map { it.stretch }
                .filter { imported -> stretchesOnPhone.none { it.overlaps(imported) } }
                .forEach { contraceptionDao.upsert(it.copy(id = 0).toEntity()) }
            added.size
        }
        file.usualLengths?.let { settings.restoreSetup(it.cycle, it.period) }
        return added
    }

    /**
     * "Delete everything": every logged day and her contraception, then every setting. The settings go last: clearing
     * them is what brings back the welcome, which then finds no log.
     */
    suspend fun deleteEverything() {
        withContext(io) { database.clearAllTables() }
        settings.clear()
    }

    private suspend fun allDays(): List<LoggedDay> {
        val logs = dayLogDao.getAll().associate { it.date to it.toModel() }
        val feelings = feelingsDao.getAll().associateBy { it.date }
        return (logs.keys + feelings.keys).sorted().map { date ->
            LoggedDay(logs[date] ?: DayLog(date), feelings[date] ?: DayFeelings(date))
        }
    }

    private suspend fun stretches(): List<ContraceptionStretch> = contraceptionDao.getAll().map { it.toModel() }

    private suspend fun loggedDates() = dayLogDao.getAll().mapTo(mutableSetOf()) { it.date } +
        feelingsDao.getAll().map { it.date }

    private sealed interface Decoded {
        class Text(val text: String) : Decoded

        class Problem(val problem: ImportProblem) : Decoded
    }

    /** [input] as UTF-8 text, refusing a file that is too large or not text. */
    private fun decode(input: InputStream): Decoded {
        val bytes = try {
            input.use { it.readAtMost(MAX_BYTES + 1) }
        } catch (_: IOException) {
            return Decoded.Problem(ImportProblem.Unreadable)
        }
        if (bytes.size > MAX_BYTES) return Decoded.Problem(ImportProblem.TooLarge)
        if (bytes.isEmpty()) return Decoded.Problem(ImportProblem.Empty)
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (_: CharacterCodingException) {
            return Decoded.Problem(ImportProblem.NotText)
        }
        if ('\u0000' in text) return Decoded.Problem(ImportProblem.NotText)
        return Decoded.Text(text)
    }

    // InputStream.readNBytes needs API 33.
    private fun InputStream.readAtMost(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        while (out.size() < limit) {
            val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        // Ten years of days with long notes stay under a few megabytes.
        const val MAX_BYTES = 10 * 1024 * 1024
        const val BUFFER_BYTES = 8 * 1024
    }
}
