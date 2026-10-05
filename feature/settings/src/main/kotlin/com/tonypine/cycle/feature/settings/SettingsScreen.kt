package com.tonypine.cycle.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.Card
import com.tonypine.cycle.core.designsystem.ClickableCard
import com.tonypine.cycle.core.designsystem.CycleDestructiveDialog
import com.tonypine.cycle.core.designsystem.CycleIcon
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.SwitchRow
import com.tonypine.cycle.core.designsystem.TextButton
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.ui.DAY_MONTH_AND_YEAR
import com.tonypine.cycle.core.ui.formatDate
import java.time.LocalDate

/**
 * The Settings tab, wired to its [viewModel]: Export my data opens Android's save screen and Import
 * from a file its file picker. [versionName] is the app's version, if known.
 *
 * The note that Cycle isn't backed up shows while the phone has no screen lock, checked each time
 * Cycle comes back to the front. "Hide for now" hides it until Cycle next opens, so it comes back
 * while the phone still has no lock.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel,
    versionName: String?,
    onUsualLengths: () -> Unit,
    onWhatToLog: () -> Unit,
    onNotices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val export = rememberExportLauncher(viewModel)
    val import = rememberImportLauncher(viewModel)
    val hasScreenLock = rememberHasScreenLock()
    var backupNoteHidden by rememberSaveable { mutableStateOf(false) }
    val shown = state
    if (shown == null) {
        Column(modifier.fillMaxSize()) {
            TopAppBar(title = stringResource(R.string.settings_title))
            LoadingState(Modifier.fillMaxSize())
        }
        return
    }
    SettingsScreen(
        state = shown,
        versionName = versionName,
        onUsualLengths = onUsualLengths,
        onWhatToLog = onWhatToLog,
        onAppLockChange = viewModel::onAppLockChange,
        onDismissLockNote = viewModel::onDismissLockNote,
        onExport = export,
        onImport = import,
        onConfirmImport = { viewModel.onConfirmImport() },
        onDismissDialog = viewModel::onDismissDialog,
        onDeleteEverything = viewModel::onDeleteEverything,
        onNotices = onNotices,
        showBackupNote = !hasScreenLock && !backupNoteHidden,
        onHideBackupNote = { backupNoteHidden = true },
        modifier = modifier
    )
}

/**
 * Settings, in four sections:
 * - Your cycle: "Usual cycle and period" and "What to log", each opening its page.
 * - Your data: a card saying everything stays on this phone, then, when [showBackupNote], a note
 *   that Cycle isn't backed up without a screen lock, with "Hide for now"; then Lock Cycle, which
 *   asks for the phone's lock before it turns on or off, with the note when Cycle turned it off
 *   itself; then Export my data, with the day of the last export, Import from a file, and Delete
 *   everything, which asks first.
 * - About: the open-source notices, then that Cycle is not a contraceptive or a diagnosis, and the
 *   version.
 *
 * Each row is one button for TalkBack, and each section title a heading. Scrolls when the text is
 * large. "Your data"'s other dialogs show from [SettingsUiState.dialog].
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    versionName: String?,
    onUsualLengths: () -> Unit,
    onWhatToLog: () -> Unit,
    onAppLockChange: (Boolean) -> Unit,
    onDismissLockNote: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onConfirmImport: () -> Unit,
    onDismissDialog: () -> Unit,
    onDeleteEverything: () -> Unit,
    onNotices: () -> Unit,
    showBackupNote: Boolean,
    onHideBackupNote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CycleTheme.colors
    val spacing = CycleTheme.spacing
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        TopAppBar(title = stringResource(R.string.settings_title))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = spacing.large)
                .padding(top = spacing.small, bottom = spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            SectionTitle(stringResource(R.string.settings_your_cycle))
            SettingsRow(
                icon = CycleIcons.WaterDrop,
                title = stringResource(R.string.usual_lengths_title),
                body = stringResource(
                    R.string.settings_usual_lengths_body,
                    state.usualCycleLength,
                    state.usualPeriodLength
                ),
                onClick = onUsualLengths,
                opensPage = true
            )
            SettingsRow(
                icon = CycleIcons.Tune,
                title = stringResource(R.string.what_to_log_title),
                body = stringResource(R.string.settings_what_to_log_body),
                onClick = onWhatToLog,
                opensPage = true
            )

            SectionTitle(stringResource(R.string.settings_your_data))
            Card(Modifier.fillMaxWidth()) {
                RowContent(
                    icon = CycleIcons.Smartphone,
                    title = stringResource(R.string.settings_on_this_phone_title),
                    body = stringResource(R.string.settings_on_this_phone_body)
                )
            }
            if (showBackupNote) BackupNote(onHide = onHideBackupNote)
            SwitchRow(
                title = stringResource(R.string.settings_lock_title),
                checked = state.appLock,
                onCheckedChange = onAppLockChange,
                body = stringResource(R.string.settings_lock_body),
                icon = CycleIcons.Lock
            )
            if (state.appLockTurnedOff) LockTurnedOffNote(onDismiss = onDismissLockNote)
            SettingsRow(
                icon = CycleIcons.Download,
                title = stringResource(R.string.settings_export_title),
                body = state.lastExported
                    ?.let { stringResource(R.string.settings_last_exported, formatDate(it, DAY_MONTH_AND_YEAR)) }
                    ?: stringResource(R.string.settings_export_body),
                onClick = onExport
            )
            SettingsRow(
                icon = CycleIcons.Upload,
                title = stringResource(R.string.settings_import_title),
                body = state.importedDays
                    ?.let { pluralStringResource(R.plurals.settings_imported, it, it) }
                    ?: stringResource(R.string.settings_import_body),
                onClick = onImport
            )
            SettingsRow(
                icon = CycleIcons.Delete,
                title = stringResource(R.string.settings_delete_title),
                body = stringResource(R.string.settings_delete_body),
                onClick = { confirmDelete = true },
                iconTint = colors.error
            )

            SectionTitle(stringResource(R.string.settings_about))
            SettingsRow(
                icon = CycleIcons.Info,
                title = stringResource(R.string.notices_title),
                body = stringResource(R.string.settings_notices_body),
                onClick = onNotices,
                opensPage = true
            )
            Column(
                modifier = Modifier.padding(horizontal = spacing.large, vertical = spacing.small),
                verticalArrangement = Arrangement.spacedBy(spacing.extraSmall)
            ) {
                BasicText(
                    stringResource(R.string.settings_disclaimer),
                    style = CycleTheme.typography.body.copy(color = colors.onSurface)
                )
                if (versionName != null) {
                    BasicText(
                        stringResource(R.string.settings_version, versionName),
                        style = CycleTheme.typography.bodySmall.copy(color = colors.onSurfaceVariant)
                    )
                }
            }
        }
    }
    CycleDestructiveDialog(
        visible = confirmDelete,
        onDismissRequest = { confirmDelete = false },
        title = stringResource(R.string.delete_everything_title),
        text = stringResource(R.string.delete_everything_text),
        confirmText = stringResource(R.string.delete_everything_confirm),
        onConfirm = {
            confirmDelete = false
            onDeleteEverything()
        },
        dismissText = stringResource(R.string.delete_everything_keep),
        icon = CycleIcons.Delete
    )
    DataDialogs(state.dialog, onConfirmImport = onConfirmImport, onDismiss = onDismissDialog)
}

/** A section's title, read as a heading. */
@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text,
        modifier = Modifier
            .padding(top = CycleTheme.spacing.large, bottom = CycleTheme.spacing.extraSmall)
            .padding(horizontal = CycleTheme.spacing.large)
            .semantics { heading() },
        style = CycleTheme.typography.label.copy(color = CycleTheme.colors.onSurfaceVariant)
    )
}

/** Cycle turned its lock off because the phone has no screen lock now, until she dismisses it. */
@Composable
private fun LockTurnedOffNote(onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        RowContent(
            icon = CycleIcons.Info,
            title = stringResource(R.string.settings_lock_off_title),
            body = stringResource(R.string.settings_lock_off_body)
        )
        TextButton(
            stringResource(R.string.settings_lock_off_ok),
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

/**
 * That Cycle isn't in the phone's backup because the phone has no screen lock, why, and that an
 * export keeps a copy, with "Hide for now" under it.
 */
@Composable
private fun BackupNote(onHide: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        RowContent(
            icon = CycleIcons.Info,
            title = stringResource(R.string.settings_not_backed_up_title),
            body = stringResource(R.string.settings_not_backed_up_body)
        )
        TextButton(
            text = stringResource(R.string.settings_not_backed_up_hide),
            onClick = onHide,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

/** A row that does something, as one button; [opensPage] adds the chevron of a row that opens a page. */
@Composable
private fun SettingsRow(
    icon: CycleIcons,
    title: String,
    body: String,
    onClick: () -> Unit,
    opensPage: Boolean = false,
    iconTint: Color = CycleTheme.colors.onSurfaceVariant
) {
    ClickableCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        RowContent(icon, title, body, iconTint) {
            if (opensPage) {
                CycleIcon(CycleIcons.ChevronEnd, contentDescription = null, tint = CycleTheme.colors.onSurfaceVariant)
            }
        }
    }
}

/** An icon, a title with a line under it, then [trailing]. */
@Composable
private fun RowContent(
    icon: CycleIcons,
    title: String,
    body: String,
    iconTint: Color = CycleTheme.colors.onSurfaceVariant,
    trailing: @Composable () -> Unit = {}
) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    Row(
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CycleIcon(icon, contentDescription = null, tint = iconTint)
        Column(Modifier.weight(1f)) {
            BasicText(title, style = typography.titleSmall.copy(color = colors.onSurface))
            BasicText(body, style = typography.bodySmall.copy(color = colors.onSurfaceVariant))
        }
        trailing()
    }
}

@Preview
@Composable
private fun SettingsPreview() {
    CycleTheme {
        SettingsScreen(
            state = SettingsUiState(28, 5, lastExported = LocalDate.of(2027, 3, 20)),
            versionName = "1.0.0",
            onUsualLengths = {},
            onWhatToLog = {},
            onAppLockChange = {},
            onDismissLockNote = {},
            onExport = {},
            onImport = {},
            onConfirmImport = {},
            onDismissDialog = {},
            onDeleteEverything = {},
            onNotices = {},
            showBackupNote = true,
            onHideBackupNote = {}
        )
    }
}
