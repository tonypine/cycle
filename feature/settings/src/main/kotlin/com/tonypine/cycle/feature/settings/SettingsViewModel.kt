package com.tonypine.cycle.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.export.ImportProblem
import com.tonypine.cycle.core.data.export.ImportRead
import com.tonypine.cycle.core.data.export.YourDataRepository
import com.tonypine.cycle.core.data.repository.ContraceptionRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.domain.ContraceptionEdits
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.ui.DeviceLock
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
 * The Settings tab: her usual lengths, her contraception, and "Your data": "Lock Cycle", export to
 * a file she picks, import from one, and "Delete everything". The welcome's "Restore from a Cycle export" uses its import too.
 * Files are opened by the screen, from what Android's save screen or file picker returns, and only
 * read or written here.
 *
 * @param deviceLock the phone's lock, which "Lock Cycle" asks for before it turns on or off.
 * @param contraception her methods, for the method in force today on the Contraception row.
 * @param clock her day, from the phone's clock in its current zone.
 */
class SettingsViewModel(
    private val settings: SettingsRepository,
    private val yourData: YourDataRepository,
    private val deviceLock: DeviceLock,
    contraception: ContraceptionRepository,
    private val clock: () -> LocalDate = LocalDate::now,
    private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    private val visit = MutableStateFlow(Visit())

    // The file she picked, read and checked, until she answers "Import N days?".
    private var pending: ImportRead.Ready? = null

    // The phone's prompt is showing for "Lock Cycle": another tap waits for it.
    private var authenticating = false

    /** Null until her settings are read. */
    val uiState: StateFlow<SettingsUiState?> =
        combine(
            settings.settings,
            settings.lastExported,
            settings.appLock,
            settings.appLockTurnedOff,
            combine(visit, contraception.observeStretches(), ::Pair)
        ) { settings, lastExported, appLock, appLockTurnedOff, (visit, stretches) ->
            SettingsUiState(
                usualCycleLength = settings.usualCycleLength,
                usualPeriodLength = settings.usualPeriodLength,
                lastExported = lastExported,
                contraception = ContraceptionEdits.current(stretches, clock()),
                appLock = appLock,
                appLockTurnedOff = appLockTurnedOff,
                importedDays = visit.importedDays,
                importedMethods = visit.importedMethods,
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
     * "Import N methods?" or "Import your usual lengths?" for a file that brings only those, says
     * there is nothing new, or says why it can't be imported. Nothing is written yet.
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

                read is ImportRead.Ready && read.newDays == 0 && read.newStretches == 0 && !read.restoresLengths ->
                    show(DataDialog.NothingToImport)

                read is ImportRead.Ready -> {
                    pending = read
                    show(DataDialog.ConfirmImport(read.newDays, read.newStretches, read.restoresLengths))
                }
            }
        }
    }

    /**
     * "Import": adds the new days and methods of the file she picked, and its usual lengths if she
     * has not done setup, then calls [onImported].
     */
    fun onConfirmImport(onImported: () -> Unit = {}) {
        val read = pending ?: return
        pending = null
        visit.update { it.copy(dialog = null) }
        viewModelScope.launch {
            val added = yourData.import(read.file)
            // A file that brought only her usual lengths shows in "Usual cycle and period".
            when {
                added > 0 -> visit.update { it.copy(importedDays = added, importedMethods = null) }

                read.newStretches > 0 ->
                    visit.update { it.copy(importedDays = null, importedMethods = read.newStretches) }
            }
            onImported()
        }
    }

    /** Closes the dialog showing; a file waiting for "Import" is dropped. */
    fun onDismissDialog() {
        pending = null
        visit.update { it.copy(dialog = null) }
    }

    /**
     * "Lock Cycle": turning it on or off first asks for the phone's lock, so she can't lock herself
     * out with a lock the phone doesn't have, and nobody else turns it off. Cancelled or failed, it
     * stays as it was. A phone with no screen lock, fingerprint or face can't ask: turning it on says
     * so instead, and turning it off just does, as the app would on its next start.
     */
    fun onAppLockChange(on: Boolean) {
        if (authenticating) return
        if (!deviceLock.canAuthenticate()) {
            if (on) show(DataDialog.NoScreenLock) else viewModelScope.launch { settings.setAppLock(false) }
            return
        }
        authenticating = true
        viewModelScope.launch {
            try {
                if (deviceLock.authenticate()) settings.setAppLock(on)
            } finally {
                authenticating = false
            }
        }
    }

    /** She read the note that the lock turned itself off: it does not show again. */
    fun onDismissLockNote() {
        viewModelScope.launch { settings.dismissAppLockTurnedOff() }
    }

    /** "Delete everything", once she confirmed: the app then opens on the welcome. */
    fun onDeleteEverything() {
        viewModelScope.launch { yourData.deleteEverything() }
    }

    private fun show(dialog: DataDialog) = visit.update { it.copy(dialog = dialog) }

    /** What happened since she opened Settings, kept only while it is open. */
    private data class Visit(
        val importedDays: Int? = null,
        val importedMethods: Int? = null,
        val dialog: DataDialog? = null
    )

    private companion object {
        // Keeps the settings flowing through a configuration change without a reload.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * The Settings tab.
 *
 * @property lastExported the day of her last export, or null if she never exported.
 * @property contraception the method in force today, or null on none.
 * @property appLock whether "Lock Cycle" is on.
 * @property appLockTurnedOff whether Cycle turned its lock off because the phone has no screen lock
 *   any more, until she dismisses the note that says so.
 * @property importedDays how many days her last import added, while Settings stays open.
 * @property importedMethods how many methods of contraception her last import added, when it added
 *   no days, while Settings stays open.
 * @property dialog the "Your data" dialog showing, if any.
 */
data class SettingsUiState(
    val usualCycleLength: Int,
    val usualPeriodLength: Int,
    val lastExported: LocalDate?,
    val contraception: ContraceptionStretch? = null,
    val appLock: Boolean = false,
    val appLockTurnedOff: Boolean = false,
    val importedDays: Int? = null,
    val importedMethods: Int? = null,
    val dialog: DataDialog? = null
)

/** A dialog of "Your data", after she picked a file or tried to turn on "Lock Cycle". */
sealed interface DataDialog {
    /**
     * "Import N days?", with [newDays] the days of the file not on the phone yet. With none, "Import
     * N methods?" for its [newStretches] of contraception, or "Import your usual lengths?" when it
     * brings only its usual lengths, which [restoresLengths] says it would restore.
     */
    data class ConfirmImport(val newDays: Int, val newStretches: Int = 0, val restoresLengths: Boolean = false) :
        DataDialog

    /** Every day in the file is on the phone already. */
    data object NothingToImport : DataDialog

    /** The file can't be imported, because of [problem]. */
    data class ImportRefused(val problem: ImportProblem) : DataDialog

    /** The file she picked on the save screen could not be written. */
    data object ExportFailed : DataDialog

    /** "Lock Cycle" can't turn on: the phone has no screen lock, fingerprint or face to ask for. */
    data object NoScreenLock : DataDialog
}
