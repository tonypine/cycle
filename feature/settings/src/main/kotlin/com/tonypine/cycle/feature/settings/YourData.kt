package com.tonypine.cycle.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.data.export.ImportProblem
import com.tonypine.cycle.core.designsystem.CycleAlertDialog
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.model.DayFeelings

/**
 * The welcome's "Restore from a Cycle export": shows the import's dialogs, and returns what opens
 * Android's file picker. Once she confirms, the days are imported and [onRestored] is called.
 */
@Composable
fun rememberRestoreFromExport(viewModel: SettingsViewModel, onRestored: () -> Unit): () -> Unit {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DataDialogs(
        dialog = state?.dialog,
        onConfirmImport = { viewModel.onConfirmImport(onImported = onRestored) },
        onDismiss = viewModel::onDismissDialog
    )
    return rememberImportLauncher(viewModel)
}

/** Opens Android's file picker; the file she picks goes to [SettingsViewModel.onImport]. */
@Composable
internal fun rememberImportLauncher(viewModel: SettingsViewModel): () -> Unit {
    val resolver = LocalContext.current.contentResolver
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.onImport { resolver.openInputStream(uri) }
    }
    return remember(launcher) { { launcher.launch(IMPORT_TYPES) } }
}

/**
 * Opens Android's save screen with today's export name; the file she saves to goes to
 * [SettingsViewModel.onExport]. Nothing is written anywhere else.
 */
@Composable
internal fun rememberExportLauncher(viewModel: SettingsViewModel): () -> Unit {
    val resolver = LocalContext.current.contentResolver
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(CSV_TYPE)) { uri ->
        if (uri != null) viewModel.onExport { resolver.openOutputStream(uri, "wt") }
    }
    return remember(launcher, viewModel) { { launcher.launch(viewModel.exportFileName()) } }
}

/**
 * The dialogs of "Your data" after she picked a file: "Import N days?", "Import N methods?" or
 * "Import your usual lengths?", nothing new to import, why a file can't be imported, an export that could not be
 * saved, and why "Lock Cycle" can't turn on. Each stays in composition, so it animates out with what it said.
 */
@Composable
internal fun DataDialogs(dialog: DataDialog?, onConfirmImport: () -> Unit, onDismiss: () -> Unit) {
    val confirm = rememberLast(dialog as? DataDialog.ConfirmImport)
    CycleAlertDialog(
        visible = dialog is DataDialog.ConfirmImport,
        onDismissRequest = onDismiss,
        title = when {
            confirm == null -> ""

            confirm.newDays > 0 ->
                pluralStringResource(R.plurals.import_confirm_title, confirm.newDays, confirm.newDays)

            confirm.newStretches > 0 ->
                pluralStringResource(R.plurals.import_methods_title, confirm.newStretches, confirm.newStretches)

            else -> stringResource(R.string.import_lengths_title)
        },
        text = stringResource(
            when {
                confirm == null || confirm.newDays > 0 -> R.string.import_confirm_text
                confirm.newStretches == 0 -> R.string.import_lengths_text
                confirm.restoresLengths -> R.string.import_methods_and_lengths_text
                else -> R.string.import_methods_text
            }
        ),
        confirmText = stringResource(R.string.import_confirm),
        onConfirm = onConfirmImport,
        dismissText = stringResource(R.string.import_cancel)
    )
    CycleAlertDialog(
        visible = dialog == DataDialog.NothingToImport,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.import_nothing_title),
        text = stringResource(R.string.import_nothing_text),
        confirmText = stringResource(R.string.import_ok),
        onConfirm = onDismiss
    )
    val refused = rememberLast(dialog as? DataDialog.ImportRefused)
    CycleAlertDialog(
        visible = dialog is DataDialog.ImportRefused,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.import_refused_title),
        text = refused?.let { problemSentence(it.problem) }.orEmpty(),
        confirmText = stringResource(R.string.import_ok),
        onConfirm = onDismiss,
        icon = CycleIcons.Error
    )
    CycleAlertDialog(
        visible = dialog == DataDialog.ExportFailed,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.export_failed_title),
        text = stringResource(R.string.export_failed_text),
        confirmText = stringResource(R.string.import_ok),
        onConfirm = onDismiss,
        icon = CycleIcons.Error
    )
    CycleAlertDialog(
        visible = dialog == DataDialog.NoScreenLock,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.no_screen_lock_title),
        text = stringResource(R.string.no_screen_lock_text),
        confirmText = stringResource(R.string.import_ok),
        onConfirm = onDismiss,
        icon = CycleIcons.Lock
    )
}

/** One sentence saying what is wrong with the file. */
@Composable
internal fun problemSentence(problem: ImportProblem): String = when (problem) {
    ImportProblem.Unreadable -> stringResource(R.string.import_problem_unreadable)

    ImportProblem.NotText -> stringResource(R.string.import_problem_not_text)

    ImportProblem.TooLarge -> stringResource(R.string.import_problem_too_large)

    ImportProblem.Empty -> stringResource(R.string.import_problem_empty)

    ImportProblem.NotAnExport -> stringResource(R.string.import_problem_not_an_export)

    is ImportProblem.UnclosedQuote -> stringResource(R.string.import_problem_unclosed_quote, problem.line)

    is ImportProblem.WrongValueCount -> pluralStringResource(
        R.plurals.import_problem_value_count,
        problem.found,
        problem.found,
        problem.line,
        problem.expected
    )

    is ImportProblem.BadDate -> stringResource(R.string.import_problem_bad_date, problem.line, problem.value)

    is ImportProblem.RepeatedDate ->
        stringResource(R.string.import_problem_repeated_date, problem.line, problem.date.toString())

    is ImportProblem.UnknownValue ->
        stringResource(R.string.import_problem_unknown_value, problem.line, problem.value, problem.column)

    is ImportProblem.PainWhereWithoutPain -> stringResource(R.string.import_problem_pain_where, problem.line)

    is ImportProblem.NoteTooLong ->
        stringResource(R.string.import_problem_note_too_long, problem.line, DayFeelings.NOTE_MAX_LENGTH)

    is ImportProblem.BadLength -> stringResource(
        R.string.import_problem_bad_length,
        problem.line,
        problem.value,
        problem.setting,
        problem.range.first,
        problem.range.last
    )

    is ImportProblem.RepeatedSetting ->
        stringResource(R.string.import_problem_repeated_setting, problem.line, problem.setting)

    ImportProblem.OneUsualLength -> stringResource(R.string.import_problem_one_usual_length)

    is ImportProblem.MissingBreaks -> stringResource(R.string.import_problem_missing_breaks, problem.line)

    is ImportProblem.StopsBeforeStarts -> stringResource(R.string.import_problem_stops_before_starts, problem.line)

    is ImportProblem.OverlappingMethod -> stringResource(R.string.import_problem_overlapping_method, problem.line)
}

/** [value], or the last non-null value it had, so a closing dialog keeps its text while it animates out. */
@Composable
internal fun <T : Any> rememberLast(value: T?): T? {
    val last = remember { LastValue<T>() }
    if (value != null) last.value = value
    return last.value
}

private class LastValue<T : Any> {
    var value: T? = null
}

// The export is a CSV file. A spreadsheet app may have saved it under any of these types.
private const val CSV_TYPE = "text/csv"
private val IMPORT_TYPES = arrayOf("text/*", "application/csv", "application/vnd.ms-excel")
