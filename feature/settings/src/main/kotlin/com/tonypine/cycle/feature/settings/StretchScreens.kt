package com.tonypine.cycle.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.CycleAlertDialog
import com.tonypine.cycle.core.designsystem.CycleBottomSheet
import com.tonypine.cycle.core.designsystem.CycleBottomSheetState
import com.tonypine.cycle.core.designsystem.CycleDestructiveDialog
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.designsystem.rememberCycleBottomSheetState
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.StretchMove
import com.tonypine.cycle.core.model.StretchRefusal
import com.tonypine.cycle.core.ui.BreaksList
import com.tonypine.cycle.core.ui.DAY_AND_MONTH
import com.tonypine.cycle.core.ui.DAY_MONTH_AND_YEAR
import com.tonypine.cycle.core.ui.R as CoreUiR
import com.tonypine.cycle.core.ui.breaksQuestion
import com.tonypine.cycle.core.ui.breaksTitle
import com.tonypine.cycle.core.ui.formatDate
import com.tonypine.cycle.core.ui.methodInFull
import com.tonypine.cycle.core.ui.methodInSentence
import com.tonypine.cycle.core.ui.methodTitle
import com.tonypine.cycle.core.ui.sinceWhenTitle
import com.tonypine.cycle.core.ui.stopBody
import com.tonypine.cycle.core.ui.stopTitle
import java.time.LocalDate
import kotlinx.coroutines.launch

/** "Mark as stopped", wired to its [viewModel]: it leaves once saved, or with [onBack]. */
@Composable
fun StopMethodRoute(viewModel: StretchViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LeaveWhenClosed(state, onBack)
    StopMethodScreen(
        state,
        onStop = viewModel::onStop,
        onBack = onBack,
        onMove = viewModel::onMove,
        onDismissDialog = viewModel::onDismissDialog,
        modifier = modifier
    )
}

/**
 * "When was your implant taken out?" (on the injection, "When was your last injection?" and that
 * Cycle counts 13 weeks from it), with a month of days from the method's start to today, and Save
 * once a day is picked. The day picked is kept through a configuration change.
 */
@Composable
fun StopMethodScreen(
    state: StretchUiState,
    onStop: (lastDay: LocalDate) -> Unit,
    onBack: () -> Unit,
    onMove: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(R.string.contraception_stop)
    if (state !is StretchUiState.Ready) return LoadingPage(title, onBack, modifier)
    val stretch = state.stretch
    var picked by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    QuestionPage(
        barTitle = title,
        question = stopTitle(stretch.method),
        body = stopBody(stretch.method),
        onBack = onBack,
        modifier = modifier
    ) {
        DayPicker(
            today = state.today,
            picked = picked,
            onPick = { picked = it },
            isEnabled = { it >= stretch.startKey && it <= state.today }
        )
        PageButton(stringResource(R.string.stop_method_save), { picked?.let(onStop) }, enabled = picked != null)
    }
    StretchDialogs(state.dialog, onMove = onMove, onDismiss = onDismissDialog)
}

/** A stretch's page, wired to its [viewModel]: it leaves once its dates are deleted, or with [onBack]. */
@Composable
fun StretchRoute(viewModel: StretchViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LeaveWhenClosed(state, onBack)
    StretchScreen(
        state,
        onEdit = viewModel::onEdit,
        onDelete = viewModel::onDelete,
        onBack = onBack,
        onMove = viewModel::onMove,
        onDismissDialog = viewModel::onDismissDialog,
        modifier = modifier
    )
}

/**
 * One method's dates, under a top bar with its name: when it started ("Fitted · 13 September 2027",
 * or "Not known"), when it stopped ("Taken out · Still in"; on the injection "Counted until"), on a
 * combined method its breaks, and "Delete these dates". Each row opens a sheet to change it, and
 * Save there saves at once; delete asks first. New dates that overlap another method ask to move its
 * edge, and dates that can't be saved say why, from [StretchUiState.Ready.dialog].
 */
@Composable
fun StretchScreen(
    state: StretchUiState,
    onEdit: (ContraceptionStretch) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onMove: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state !is StretchUiState.Ready) return LoadingPage("", onBack, modifier)
    val stretch = state.stretch
    val method = stretch.method
    val spacing = CycleTheme.spacing
    val scope = rememberCoroutineScope()
    val startSheet = rememberCycleBottomSheetState(skipPartiallyExpanded = true)
    val stopSheet = rememberCycleBottomSheetState(skipPartiallyExpanded = true)
    val breaksSheet = rememberCycleBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = methodTitle(method),
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.settings_back), onBack)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = spacing.large)
                .padding(top = spacing.small, bottom = spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            SettingsRow(
                icon = CycleIcons.Calendar,
                title = stringResource(startLabel(method)),
                body = stretch.started?.let { formatDate(it, DAY_MONTH_AND_YEAR) }
                    ?: stringResource(R.string.stretch_not_known),
                onClick = { scope.launch { startSheet.show() } },
                opensPage = true
            )
            SettingsRow(
                icon = CycleIcons.Calendar,
                title = stringResource(stopLabel(method)),
                body = stretch.stopped?.let { formatDate(it, DAY_MONTH_AND_YEAR) }
                    ?: stringResource(if (method.isFitted) R.string.stretch_still_in else R.string.stretch_still_on_it),
                onClick = { scope.launch { stopSheet.show() } },
                opensPage = true
            )
            val breaks = stretch.breaks
            if (breaks != null) {
                SettingsRow(
                    icon = CycleIcons.Tune,
                    title = stringResource(R.string.stretch_breaks),
                    body = breaksTitle(breaks),
                    onClick = { scope.launch { breaksSheet.show() } },
                    opensPage = true
                )
            }
            SettingsRow(
                icon = CycleIcons.Delete,
                title = stringResource(R.string.stretch_delete),
                body = stringResource(R.string.stretch_delete_body),
                onClick = { confirmDelete = true },
                iconTint = CycleTheme.colors.error
            )
        }
    }
    DaySheet(
        sheet = startSheet,
        title = sinceWhenTitle(method),
        body = stringResource(CoreUiR.string.method_since_body),
        today = state.today,
        initial = stretch.started,
        isEnabled = { it <= state.today },
        onSave = { onEdit(stretch.copy(started = it)) }
    )
    val injection = method == ContraceptionMethod.INJECTION
    DaySheet(
        sheet = stopSheet,
        title = if (injection) stringResource(R.string.stretch_counted_until) else stopTitle(method),
        body = stringResource(if (injection) R.string.stretch_counted_until_body else CoreUiR.string.method_since_body),
        today = state.today,
        initial = stretch.stopped,
        isEnabled = { it >= stretch.startKey && it <= state.latestStop },
        onSave = { onEdit(stretch.copy(stopped = it)) }
    )
    if (stretch.breaks != null) {
        CycleBottomSheet(breaksSheet, title = breaksQuestion(method)) {
            var picked by rememberSaveable { mutableStateOf(stretch.breaks) }
            BreaksList(method, picked, { picked = it })
            FilledButton(
                text = stringResource(R.string.stretch_save),
                onClick = {
                    picked?.let { onEdit(stretch.copy(breaks = it)) }
                    scope.launch { breaksSheet.hide() }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
    CycleDestructiveDialog(
        visible = confirmDelete,
        onDismissRequest = { confirmDelete = false },
        title = stringResource(R.string.stretch_delete_title),
        text = deleteText(stretch),
        confirmText = stringResource(R.string.stretch_delete_confirm),
        onConfirm = {
            confirmDelete = false
            onDelete()
        },
        dismissText = stringResource(R.string.stretch_delete_keep),
        icon = CycleIcons.Delete
    )
    StretchDialogs(state.dialog, onMove = onMove, onDismiss = onDismissDialog)
}

/**
 * A sheet with a month of days to pick one, starting on [initial], and Save, which closes it and
 * hands the day to [onSave].
 */
@Composable
private fun DaySheet(
    sheet: CycleBottomSheetState,
    title: String,
    body: String,
    today: LocalDate,
    initial: LocalDate?,
    isEnabled: (LocalDate) -> Boolean,
    onSave: (LocalDate) -> Unit
) {
    val scope = rememberCoroutineScope()
    CycleBottomSheet(sheet, title = title) {
        var picked by rememberSaveable { mutableStateOf(initial) }
        BasicText(body, style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurfaceVariant))
        DayPicker(
            today = today,
            picked = picked,
            onPick = { picked = it },
            isEnabled = isEnabled,
            modifier = Modifier.sheetCalendarMargins()
        )
        FilledButton(
            text = stringResource(R.string.stretch_save),
            onClick = {
                picked?.let(onSave)
                scope.launch { sheet.hide() }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = picked != null
        )
    }
}

/**
 * The dialogs after saving a stretch's dates: "Move the end of your pill?" with Cancel and Move it,
 * or why the dates can't be saved. Each stays in composition, so it animates out with what it said.
 */
@Composable
internal fun StretchDialogs(dialog: StretchDialog?, onMove: () -> Unit, onDismiss: () -> Unit) {
    val moves = rememberLast(dialog as? StretchDialog.ConfirmMoves)?.moves.orEmpty()
    CycleAlertDialog(
        visible = dialog is StretchDialog.ConfirmMoves,
        onDismissRequest = onDismiss,
        title = moveTitle(moves),
        text = moveText(moves),
        confirmText = stringResource(R.string.move_confirm),
        onConfirm = onMove,
        dismissText = stringResource(R.string.move_cancel)
    )
    val refused = rememberLast(dialog as? StretchDialog.Refused)
    CycleAlertDialog(
        visible = dialog is StretchDialog.Refused,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.refused_title),
        text = refused?.let { refusalSentence(it.reason) }.orEmpty(),
        confirmText = stringResource(R.string.refused_ok),
        onConfirm = onDismiss
    )
}

/** "Move the end of your pill?", or for more than one stretch, "Move your other methods' dates?". */
@Composable
private fun moveTitle(moves: List<StretchMove>): String {
    val move = moves.singleOrNull() ?: return stringResource(R.string.move_several_title)
    val name = methodInSentence(move.from.method)
    return stringResource(if (move.movesEnd) R.string.move_end_title else R.string.move_start_title, name)
}

/** "Your combined pill would end on 5 September instead of 12 September, so the two don't overlap…". */
@Composable
private fun moveText(moves: List<StretchMove>): String {
    val move = moves.singleOrNull()
    if (move != null) {
        val (to, from) = move.edges()
        return stringResource(
            if (move.movesEnd) R.string.move_end_text else R.string.move_start_text,
            methodInFull(move.from.method),
            to,
            from
        )
    }
    val lines = moves.map { each ->
        val (to, from) = each.edges()
        stringResource(
            if (each.movesEnd) R.string.move_end_line else R.string.move_start_line,
            methodInFull(each.from.method),
            to,
            from
        )
    }
    return (lines + stringResource(R.string.move_several_end)).joinToString(" ")
}

/** The edge that moves: its new day and its day now, as "5 September". */
@Composable
private fun StretchMove.edges(): Pair<String, String> {
    val to = if (movesEnd) to.stopped else to.started
    val from = if (movesEnd) from.stopped else from.started
    return (to?.let { formatDate(it, DAY_AND_MONTH) }.orEmpty()) to
        (from?.let { formatDate(it, DAY_AND_MONTH) }.orEmpty())
}

/** The move ends the stretch earlier, rather than starting it later. */
private val StretchMove.movesEnd: Boolean
    get() = to.stopped != from.stopped

/** Why the dates can't be saved, and what she can do. */
@Composable
private fun refusalSentence(reason: StretchRefusal): String = when (reason) {
    StretchRefusal.StartAfterToday -> stringResource(R.string.refused_after_today)

    is StretchRefusal.NotAfterCurrentStart ->
        reason.current.started
            ?.let {
                stringResource(
                    R.string.refused_before_current,
                    methodInSentence(reason.current.method),
                    formatDate(it, DAY_AND_MONTH)
                )
            }
            ?: stringResource(R.string.refused_covers_whole, methodInSentence(reason.current.method))

    is StretchRefusal.CoversWhole -> stringResource(
        R.string.refused_covers_whole,
        methodInSentence(reason.stretch.method)
    )

    StretchRefusal.StopBeforeStart -> stringResource(R.string.refused_stop_before_start)

    is StretchRefusal.StopTooLate -> stringResource(
        R.string.refused_stop_too_late,
        formatDate(reason.latest, DAY_MONTH_AND_YEAR)
    )

    StretchRefusal.Gone -> stringResource(R.string.refused_gone)
}

/**
 * "Cycle forgets you used the combined pill from 3 May to 5 September 2027…", or with no stop date
 * "since 9 November 2026", with no start date "until 3 November 2027", and with neither "you use the
 * implant".
 */
@Composable
private fun deleteText(stretch: ContraceptionStretch): String {
    val name = methodInFull(stretch.method)
    val started = stretch.started
    val stopped = stretch.stopped
    return when {
        stopped == null ->
            started
                ?.let { stringResource(R.string.stretch_delete_since, name, formatDate(it, DAY_MONTH_AND_YEAR)) }
                ?: stringResource(R.string.stretch_delete_ongoing, name)

        started == null -> stringResource(R.string.stretch_delete_until, name, formatDate(stopped, DAY_MONTH_AND_YEAR))

        else -> stringResource(
            R.string.stretch_delete_from_to,
            name,
            formatDate(started, if (started.year == stopped.year) DAY_AND_MONTH else DAY_MONTH_AND_YEAR),
            formatDate(stopped, DAY_MONTH_AND_YEAR)
        )
    }
}

/** An implant or an IUD: fitted and taken out, rather than started and stopped. */
private val ContraceptionMethod.isFitted: Boolean
    get() = this == ContraceptionMethod.IMPLANT ||
        this == ContraceptionMethod.HORMONAL_IUD ||
        this == ContraceptionMethod.COPPER_IUD

private fun startLabel(method: ContraceptionMethod): Int = when {
    method == ContraceptionMethod.INJECTION -> R.string.stretch_first_injection
    method.isFitted -> R.string.stretch_fitted
    else -> R.string.stretch_started
}

private fun stopLabel(method: ContraceptionMethod): Int = when {
    method == ContraceptionMethod.INJECTION -> R.string.stretch_counted_until
    method.isFitted -> R.string.stretch_taken_out
    else -> R.string.stretch_stopped
}

/** Leaves once [state] is closed: stopped or deleted here, or gone. */
@Composable
private fun LeaveWhenClosed(state: StretchUiState, onBack: () -> Unit) {
    val leave by rememberUpdatedState(onBack)
    LaunchedEffect(state == StretchUiState.Closed) {
        if (state == StretchUiState.Closed) leave()
    }
}

@Composable
private fun LoadingPage(title: String, onBack: () -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = title,
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.settings_back), onBack)
        )
        LoadingState(Modifier.fillMaxSize())
    }
}
