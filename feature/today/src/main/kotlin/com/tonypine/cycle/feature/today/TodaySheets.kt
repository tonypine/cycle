package com.tonypine.cycle.feature.today

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tonypine.cycle.core.designsystem.CycleBottomSheet
import com.tonypine.cycle.core.designsystem.CycleBottomSheetState
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.MonthCalendar
import com.tonypine.cycle.core.designsystem.WeekRow
import com.tonypine.cycle.core.designsystem.firstDayOfWeek
import com.tonypine.cycle.core.model.BleedingWord
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.ui.formatDate
import com.tonypine.cycle.core.ui.methodInFull
import com.tonypine.cycle.core.ui.methodInSentence
import com.tonypine.cycle.core.ui.words
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import kotlinx.coroutines.launch

/**
 * "How is this estimated?": the date and the range of [next], in plain words. After stopping a method,
 * counted from the day she stopped it, with a range that stays wide for a few cycles.
 */
@Composable
internal fun EstimateSheet(sheet: CycleBottomSheetState, next: NextPeriod) {
    val scope = rememberCoroutineScope()
    CycleBottomSheet(sheet, title = stringResource(R.string.estimate_sheet_title)) {
        val cycle = when (val basis = next.basis) {
            EstimateBasis.Typical -> stringResource(R.string.estimate_cycle_typical)
            EstimateBasis.Setup -> stringResource(R.string.estimate_cycle_setup)
            is EstimateBasis.Logged -> pluralStringResource(R.plurals.estimate_cycle_logged, basis.count, basis.count)
        }
        val lastStart = formatDate(next.lastStart)
        val stopped = next.stoppedMethod
        if (next.daysLate > 0) {
            val due = formatDate(next.due)
            SheetText(
                if (stopped != null) {
                    stringResource(
                        R.string.estimate_date_stopped_late,
                        due,
                        lastStart,
                        next.cycleLength,
                        methodInSentence(stopped),
                        cycle
                    )
                } else {
                    stringResource(R.string.estimate_date_late, due, lastStart, next.cycleLength, cycle)
                }
            )
            SheetText(pluralStringResource(R.plurals.estimate_late, next.daysLate, next.daysLate))
            if (next.latestStart > next.expectedStart) {
                SheetText(stringResource(R.string.estimate_late_range, formatDate(next.latestStart)))
            }
        } else if (stopped != null) {
            val expected = formatDate(next.expectedStart)
            SheetText(
                stringResource(
                    R.string.estimate_date_stopped,
                    expected,
                    lastStart,
                    next.cycleLength,
                    methodInSentence(stopped),
                    cycle
                )
            )
            SheetText(
                stringResource(R.string.estimate_range, formatDate(next.earliestStart), formatDate(next.latestStart)) +
                    " " + stringResource(R.string.estimate_range_settling)
            )
        } else {
            val expected = formatDate(next.expectedStart)
            SheetText(stringResource(R.string.estimate_date, expected, lastStart, next.cycleLength, cycle))
            val spread = when {
                next.settlingAfter != null -> R.string.estimate_range_settling
                next.basis is EstimateBasis.Logged -> R.string.estimate_range_logged
                else -> R.string.estimate_range_typical
            }
            SheetText(
                stringResource(R.string.estimate_range, formatDate(next.earliestStart), formatDate(next.latestStart)) +
                    " " + stringResource(spread)
            )
        }
        SheetText(stringResource(R.string.estimate_closing))
        FilledButton(
            text = stringResource(R.string.estimate_done),
            onClick = { scope.launch { sheet.hide() } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * "How is this estimated?" for the next bleed on a combined [method] with a break every month: a bleed
 * set by the method, not a period, 28 days after the last one, give or take 2.
 */
@Composable
internal fun NextBleedSheet(sheet: CycleBottomSheetState, method: ContraceptionMethod) {
    val scope = rememberCoroutineScope()
    CycleBottomSheet(sheet, title = stringResource(R.string.estimate_sheet_title)) {
        val name = breakName(method)
        SheetText(stringResource(R.string.bleed_sheet_why, methodInFull(method), name, methodInSentence(method)))
        SheetText(stringResource(R.string.bleed_sheet_when, name))
        SheetText(stringResource(R.string.bleed_sheet_between))
        SheetText(stringResource(R.string.bleed_sheet_closing))
        FilledButton(
            text = stringResource(R.string.estimate_done),
            onClick = { scope.launch { sheet.hide() } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * "What changes on the implant?": what [method] does to bleeding, what Cycle does about it, and where
 * to go if it bothers her.
 */
@Composable
internal fun MethodSheet(sheet: CycleBottomSheetState, method: ContraceptionMethod) {
    val scope = rememberCoroutineScope()
    CycleBottomSheet(sheet, title = methodSheetTitle(method)) {
        SheetText(
            when (method) {
                ContraceptionMethod.PROGESTOGEN_PILL -> stringResource(R.string.method_sheet_mini_pill)
                ContraceptionMethod.IMPLANT -> stringResource(R.string.method_sheet_implant)
                ContraceptionMethod.HORMONAL_IUD -> stringResource(R.string.method_sheet_hormonal_iud)
                ContraceptionMethod.INJECTION -> stringResource(R.string.method_sheet_injection)
                else -> stringResource(R.string.method_sheet_combined, methodInSentence(method))
            }
        )
        SheetText(stringResource(R.string.method_sheet_what_cycle_does))
        SheetText(stringResource(R.string.method_sheet_help))
        FilledButton(
            text = stringResource(R.string.estimate_done),
            onClick = { scope.launch { sheet.hide() } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * "Log a period", from the empty state: a month to pick the first day of her last period in, up to
 * today. The days it will count, [periodLength] from the picked one, show as a period.
 */
@Composable
internal fun LogPeriodSheet(
    sheet: CycleBottomSheetState,
    today: LocalDate,
    periodLength: Int,
    onLog: (start: LocalDate) -> Unit
) {
    val scope = rememberCoroutineScope()
    CycleBottomSheet(sheet, title = stringResource(R.string.log_period_title)) {
        var month by rememberSaveable { mutableStateOf(YearMonth.from(today)) }
        var start by rememberSaveable { mutableStateOf<LocalDate?>(null) }
        SheetText(pluralStringResource(R.plurals.log_period_body, periodLength, periodLength))
        MonthCalendar(
            month = month,
            stateOf = { date ->
                val picked = start
                val counted =
                    picked != null && date >= picked && date < picked.plusDays(periodLength.toLong()) && date <= today
                if (counted) CycleDayState.Period else CycleDayState.Plain
            },
            onDayClick = { start = it },
            today = today,
            onPreviousMonth = { month = month.minusMonths(1) },
            onNextMonth = { month = month.plusMonths(1) },
            modifier = Modifier.calendarMargins(),
            selected = start,
            isEnabled = { !it.isAfter(today) }
        )
        FilledButton(
            text = stringResource(R.string.log_period_confirm),
            onClick = {
                start?.let(onLog)
                scope.launch { sheet.hide() }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = start != null
        )
    }
}

/**
 * "When was the last day?": the weeks of her period (or bleeding, in [words]) so far, with only its
 * days to pick.
 */
@Composable
internal fun LastDaySheet(
    sheet: CycleBottomSheetState,
    today: LocalDate,
    stillGoing: StillGoing,
    words: BleedingWord,
    onEndedOn: (lastDay: LocalDate) -> Unit
) {
    val scope = rememberCoroutineScope()
    val firstDayOfWeek = firstDayOfWeek()
    CycleBottomSheet(sheet, title = stringResource(R.string.today_last_day_title)) {
        var lastDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
        val days = stillGoing.days
        val weeks = remember(days, firstDayOfWeek) { weeksOf(days, firstDayOfWeek) }
        SheetText(stringResource(words.lastDayBody()))
        weeks.forEach { week ->
            WeekRow(
                weekOf = week,
                stateOf = { if (it in days) CycleDayState.Period else CycleDayState.Plain },
                onDayClick = { lastDay = it },
                today = today,
                modifier = Modifier.calendarMargins(),
                selected = lastDay,
                isEnabled = { it in days },
                wordsOf = { words.words }
            )
        }
        FilledButton(
            text = stringResource(R.string.today_save),
            onClick = {
                lastDay?.let(onEndedOn)
                scope.launch { sheet.hide() }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = lastDay != null
        )
    }
}

/**
 * Widens a calendar into the sheet's 24dp padding, to the 12dp margins calendars use, so seven 48dp
 * days fit a 360dp screen without scrolling sideways.
 */
@Composable
private fun Modifier.calendarMargins(): Modifier {
    val bleed = CycleTheme.spacing.extraLarge - CycleTheme.spacing.medium
    return layout { measurable, constraints ->
        val extra = bleed.roundToPx() * 2
        val wider = if (constraints.hasBoundedWidth) {
            constraints.copy(
                maxWidth = constraints.maxWidth + extra
            )
        } else {
            constraints
        }
        val placeable = measurable.measure(wider.copy(minWidth = minOf(wider.minWidth + extra, wider.maxWidth)))
        layout((placeable.width - extra).coerceAtLeast(0), placeable.height) { placeable.place(-extra / 2, 0) }
    }
}

@Composable
private fun SheetText(text: String) {
    BasicText(text, style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurfaceVariant))
}

/** The first day of each week [days] touches, in weeks that start on [firstDayOfWeek], oldest first. */
internal fun weeksOf(days: ClosedRange<LocalDate>, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val firstDay = WeekFields.of(firstDayOfWeek, 1).dayOfWeek()
    return generateSequence(days.start) { it.plusDays(1) }
        .takeWhile { it <= days.endInclusive }
        .map { it.with(firstDay, 1) }
        .distinct()
        .toList()
}
