package com.tonypine.cycle.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.export.ImportFile
import com.tonypine.cycle.core.data.export.ImportProblem
import com.tonypine.cycle.core.data.export.ImportRead
import com.tonypine.cycle.core.data.export.YourDataRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Settings tab: her usual lengths, and "Your data": export to a file she picks, import from
 * one, and "Delete everything". The welcome's "Restore from a Cycle export" uses its import too.
 * Files are opened by the screen, from what Android's save screen or file picker returns, and only
 * read or written here.
 *
 * @param clock her day, from the phone's clock in its current zone.
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val yourData: YourDataRepository,
    private val clock: () -> LocalDate = LocalDate::now,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val visit = MutableStateFlow(Visit())

    // The file she picked, read and checked, until she answers "Import N days?".
    private var pending: ImportFile? = null

    /** Null until her settings are read. */
    val uiState: StateFlow<SettingsUiState?> =
        combine(settings.settings, settings.lastExported, visit) { settings, lastExported, visit ->
            SettingsUiState(
                usualCycleLength = settings.usualCycleLength,
                usualPeriodLength = settings.usualPeriodLength,
                lastExported = lastExported,
                importedDays = visit.importedDays,
                dialog = visit.dialog
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** The name Android's save screen suggests: `cycle-export-2027-03-20.csv`. */
    fun exportFileName(): String = "cycle-export-${clock()}.csv"

    /**
     * Writes every day she logged to the file [open] opens, the one she picked on the save screen,
     * and remembers today as her last export. When the file cannot be written, says so.
     */
    fun onExport(open: () -> OutputStream?) {
        viewModelScope.launch {
            try {
                val output = withContext(io) { open() } ?: throw IOException("No file to write")
                yourData.export(output)
                settings.setLastExported(clock())
            } catch (_: IOException) {
                show(DataDialog.ExportFailed)
            } catch (_: SecurityException) {
                show(DataDialog.ExportFailed)
            }
        }
    }

    /**
     * Reads and checks the file [open] opens, the one she picked, then asks "Import N days?", or
     * "Import your usual lengths?" for a file that brings only those, says there is nothing new, or
     * says why it can't be imported. Nothing is written yet.
     */
    fun onImport(open: () -> InputStream?) {
        viewModelScope.launch {
            val input = try {
                withContext(io) { open() }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
            val read = input?.let { yourData.read(it) } ?: ImportRead.Refused(ImportProblem.Unreadable)
            when {
                read is ImportRead.Refused -> show(DataDialog.ImportRefused(read.problem))

                read is ImportRead.Ready && read.newDays == 0 && !read.restoresLengths ->
                    show(DataDialog.NothingToImport)

                read is ImportRead.Ready -> {
                    pending = read.file
                    show(DataDialog.ConfirmImport(read.newDays))
                }
            }
        }
    }

    /**
     * "Import": adds the new days of the file she picked, and its usual lengths if she has not done
     * setup, then calls [onImported].
     */
    fun onConfirmImport(onImported: () -> Unit = {}) {
        val file = pending ?: return
        pending = null
        visit.update { it.copy(dialog = null) }
        viewModelScope.launch {
            val added = yourData.import(file)
            // A file that brought only her usual lengths shows in "Usual cycle and period".
            if (added > 0) visit.update { it.copy(importedDays = added) }
            onImported()
        }
    }

    /** Closes the dialog showing; a file waiting for "Import" is dropped. */
    fun onDismissDialog() {
        pending = null
        visit.update { it.copy(dialog = null) }
    }

    /** "Delete everything", once she confirmed: the app then opens on the welcome. */
    fun onDeleteEverything() {
        viewModelScope.launch { yourData.deleteEverything() }
    }

    private fun show(dialog: DataDialog) = visit.update { it.copy(dialog = dialog) }

    /** What happened since she opened Settings, kept only while it is open. */
    private data class Visit(val importedDays: Int? = null, val dialog: DataDialog? = null)

    private companion object {
        // Keeps the settings flowing through a configuration change without a reload.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * The Settings tab.
 *
 * @property lastExported the day of her last export, or null if she never exported.
 * @property importedDays how many days her last import added, while Settings stays open.
 * @property dialog the "Your data" dialog showing, if any.
 */
data class SettingsUiState(
    val usualCycleLength: Int,
    val usualPeriodLength: Int,
    val lastExported: LocalDate?,
    val importedDays: Int? = null,
    val dialog: DataDialog? = null
)

/** A dialog of "Your data", after she picked a file. */
sealed interface DataDialog {
    /**
     * "Import N days?", with [newDays] the days of the file not on the phone yet, or "Import your
     * usual lengths?" when it brings none but its usual lengths.
     */
    data class ConfirmImport(val newDays: Int) : DataDialog

    /** Every day in the file is on the phone already. */
    data object NothingToImport : DataDialog

    /** The file can't be imported, because of [problem]. */
    data class ImportRefused(val problem: ImportProblem) : DataDialog

    /** The file she picked on the save screen could not be written. */
    data object ExportFailed : DataDialog
}
