package com.tonypine.cycle.feature.today

import android.text.format.DateFormat
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tonypine.cycle.core.designsystem.ButtonGroup
import com.tonypine.cycle.core.designsystem.CycleBottomSheet
import com.tonypine.cycle.core.designsystem.CycleBottomSheetState
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.MonthCalendar
import com.tonypine.cycle.core.designsystem.WeekRow
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.FlowLevel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.launch

/** "How is this estimated?": the date and the range of [next], in plain words. */
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
        if (next.daysLate > 0) {
            val due = formatDate(next.expectedStart.minusDays(next.daysLate.toLong()))
            SheetText(stringResource(R.string.estimate_date_late, due, lastStart, next.cycleLength, cycle))
            SheetText(pluralStringResource(R.plurals.estimate_late, next.daysLate, next.daysLate))
            if (next.latestStart > next.expectedStart) {
                SheetText(stringResource(R.string.estimate_late_range, formatDate(next.latestStart)))
            }
        } else {
            val expected = formatDate(next.expectedStart)
            SheetText(stringResource(R.string.estimate_date, expected, lastStart, next.cycleLength, cycle))
            val spread = if (next.basis is EstimateBasis.Logged) {
                R.string.estimate_range_logged
            } else {
                R.string.estimate_range_typical
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

/** The day log for today: flow for now; MOT-36 adds how she feels. Each choice saves at once. */
@Composable
internal fun DayLogSheet(sheet: CycleBottomSheetState, flow: FlowLevel?, onFlowChange: (FlowLevel?) -> Unit) {
    val scope = rememberCoroutineScope()
    CycleBottomSheet(sheet, title = stringResource(R.string.day_log_title)) {
        val label = stringResource(R.string.day_log_flow)
        CardTitle(label)
        ButtonGroup(
            label = label,
            options = FlowLevel.entries.map { stringResource(it.label) },
            selectedIndex = flow?.ordinal,
            onSelectedChange = { index -> onFlowChange(index?.let { FlowLevel.entries[it] }) }
        )
        FilledButton(
            text = stringResource(R.string.day_log_done),
            onClick = { scope.launch { sheet.hide() } },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** "When was the last day?": the weeks of her period so far, with only its days to pick. */
@Composable
internal fun LastDaySheet(
    sheet: CycleBottomSheetState,
    today: LocalDate,
    stillGoing: StillGoing,
    onEndedOn: (lastDay: LocalDate) -> Unit
) {
    val scope = rememberCoroutineScope()
    val locale = LocalConfiguration.current.locales[0]
    CycleBottomSheet(sheet, title = stringResource(R.string.today_last_day_title)) {
        var lastDay by rememberSaveable { mutableStateOf<LocalDate?>(null) }
        val days = stillGoing.days
        val weeks = remember(days, locale) { weeksOf(days, locale) }
        SheetText(stringResource(R.string.today_last_day_body))
        weeks.forEach { week ->
            WeekRow(
                weekOf = week,
                stateOf = { if (it in days) CycleDayState.Period else CycleDayState.Plain },
                onDayClick = { lastDay = it },
                today = today,
                modifier = Modifier.calendarMargins(),
                selected = lastDay,
                isEnabled = { it in days }
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

private val FlowLevel.label: Int
    get() = when (this) {
        FlowLevel.NONE -> R.string.day_log_flow_none
        FlowLevel.SPOTTING -> R.string.day_log_flow_spotting
        FlowLevel.LIGHT -> R.string.day_log_flow_light
        FlowLevel.MEDIUM -> R.string.day_log_flow_medium
        FlowLevel.HEAVY -> R.string.day_log_flow_heavy
    }

/** The first day of each week [days] touches, in [locale]'s weeks, oldest first. */
internal fun weeksOf(days: ClosedRange<LocalDate>, locale: Locale): List<LocalDate> {
    val firstDay = WeekFields.of(locale).dayOfWeek()
    return generateSequence(days.start) { it.plusDays(1) }
        .takeWhile { it <= days.endInclusive }
        .map { it.with(firstDay, 1) }
        .distinct()
        .toList()
}

/** Skeletons for [DateFormat.getBestDateTimePattern]: the locale picks the order and punctuation. */
internal const val DAY_AND_MONTH = "dMMMM"
internal const val DAY_AND_DATE = "EEEEdMMMM"

internal fun dateFormatter(locale: Locale, skeleton: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
