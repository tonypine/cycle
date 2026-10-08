package com.tonypine.cycle.core.designsystem

import android.text.format.DateFormat
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * A month of the calendar: a header with the previous and next month [IconButton]s around the month
 * name, the weekday initials, and a seven-column grid of [DayCell]s. The week starts on the locale's
 * first day (Monday in the UK, Sunday in the US), and the days outside [month] are left blank.
 *
 * [stateOf] gives each day's cycle state, [today] gets the ring and [selected] the frame. A day where
 * [isEnabled] is false shows in full colour but ignores taps, so a predicted period in the future
 * stays readable; the others call [onDayClick]. [wordsOf] gives what each day's bleeding is called,
 * for TalkBack, so a month spanning the start of a method reads "period" before it and "bleeding"
 * after.
 *
 * Each column is at least as wide as a cell (48dp, 58dp at 200% font scale) and the columns share
 * the width left over. Seven 48dp cells fit a 360dp screen inside `spacing.medium` (12dp) margins;
 * when they do not fit, at large font scales, the weekdays and the grid scroll sideways together
 * while the header stays put, and start scrolled so today is in view.
 *
 * TalkBack reads the month name as a heading, and again whenever the month changes. The grid is a
 * collection of as many rows as the month has weeks and seven columns, and each day reads as in
 * [DayCell] ("20 March, today").
 */
@Composable
fun MonthCalendar(
    month: YearMonth,
    stateOf: (LocalDate) -> CycleDayState,
    onDayClick: (LocalDate) -> Unit,
    today: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    selected: LocalDate? = null,
    isEnabled: (LocalDate) -> Boolean = { true },
    wordsOf: (LocalDate) -> BleedingWords = { BleedingWords.Period }
) {
    val firstDayOfWeek = firstDayOfWeek()
    val weeks = remember(month, firstDayOfWeek) { monthWeeks(month, firstDayOfWeek) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        MonthHeader(month, onPreviousMonth, onNextMonth)
        CalendarGrid(weeks, firstDayOfWeek, stateOf, onDayClick, today, selected, isEnabled, wordsOf)
    }
}

/**
 * The week around [weekOf], from the locale's first day of the week, with the weekday initials above
 * it: the same columns and cells as [MonthCalendar], for Today and for picking a recent day, with the
 * same [wordsOf]. Days in the next or previous month show like any other. TalkBack reads it as a collection of one row and
 * seven columns.
 */
@Composable
fun WeekRow(
    weekOf: LocalDate,
    stateOf: (LocalDate) -> CycleDayState,
    onDayClick: (LocalDate) -> Unit,
    today: LocalDate,
    modifier: Modifier = Modifier,
    selected: LocalDate? = null,
    isEnabled: (LocalDate) -> Boolean = { true },
    wordsOf: (LocalDate) -> BleedingWords = { BleedingWords.Period }
) {
    val firstDayOfWeek = firstDayOfWeek()
    val weeks = remember(weekOf, firstDayOfWeek) { listOf(weekDays(weekOf, firstDayOfWeek)) }
    CalendarGrid(weeks, firstDayOfWeek, stateOf, onDayClick, today, selected, isEnabled, wordsOf, modifier)
}

/** The weeks of [month], seven days each from [firstDayOfWeek], with null for the days outside it. */
internal fun monthWeeks(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<LocalDate?>> {
    val lead = List<LocalDate?>(daysFromWeekStart(month.atDay(1), firstDayOfWeek)) { null }
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    return (lead + days).chunked(DAYS_IN_WEEK) { week -> week + List(DAYS_IN_WEEK - week.size) { null } }
}

/** The seven days of the week around [date], from [firstDayOfWeek]. */
internal fun weekDays(date: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val start = date.minusDays(daysFromWeekStart(date, firstDayOfWeek).toLong())
    return List(DAYS_IN_WEEK) { start.plusDays(it.toLong()) }
}

private fun daysFromWeekStart(date: LocalDate, firstDayOfWeek: DayOfWeek): Int =
    Math.floorMod(date.dayOfWeek.value - firstDayOfWeek.value, DAYS_IN_WEEK)

private const val DAYS_IN_WEEK = 7

@Composable
private fun MonthHeader(month: YearMonth, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    val locale = cycleLocale()
    val name = remember(month, locale) {
        startingLine(
            DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "MMMMyyyy"), locale).format(month),
            locale
        )
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(CycleIcons.ChevronStart, stringResource(R.string.calendar_previous_month), onPreviousMonth)
        BasicText(
            name,
            modifier = Modifier
                .weight(1f)
                .semantics {
                    heading()
                    liveRegion = LiveRegionMode.Polite
                },
            style = CycleTheme.typography.title.copy(color = CycleTheme.colors.onSurface, textAlign = TextAlign.Center)
        )
        IconButton(CycleIcons.ChevronEnd, stringResource(R.string.calendar_next_month), onNextMonth)
    }
}

/**
 * The weekday initials over [weeks] of day cells, in seven columns that fill the width and scroll
 * sideways when seven cells do not fit, with today in view.
 */
@Composable
private fun CalendarGrid(
    weeks: List<List<LocalDate?>>,
    firstDayOfWeek: DayOfWeek,
    stateOf: (LocalDate) -> CycleDayState,
    onDayClick: (LocalDate) -> Unit,
    today: LocalDate,
    selected: LocalDate?,
    isEnabled: (LocalDate) -> Boolean,
    wordsOf: (LocalDate) -> BleedingWords,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
        val gap = CycleTheme.spacing.extraSmall
        val scroll = rememberScrollState()
        ScrollTodayIntoView(
            scroll,
            width,
            todayColumn = weeks.firstNotNullOfOrNull { week ->
                week.indexOf(today).takeIf { it >= 0 }
            }
        )
        Layout(
            content = {
                WeekdayInitials(firstDayOfWeek)
                DayCells(weeks, stateOf, onDayClick, today, selected, isEnabled, wordsOf)
            },
            modifier = Modifier.horizontalScroll(scroll)
        ) { measurables, _ ->
            val days = measurables.last().measure(Constraints(minWidth = width))
            val column = days.width / DAYS_IN_WEEK
            val start = days.width % DAYS_IN_WEEK / 2
            val initials = measurables.dropLast(1).map { it.measure(Constraints.fixedWidth(column)) }
            val top = initials.maxOf { it.height } + gap.roundToPx()
            layout(days.width, top + days.height) {
                initials.forEachIndexed { index, initial -> initial.placeRelative(start + index * column, 0) }
                days.placeRelative(0, top)
            }
        }
    }
}

/**
 * When the columns are wider than the [width] they are shown in, scrolls once so [todayColumn] is in
 * view: at large font scales the last columns start off screen, and today is often a weekend.
 */
@Composable
private fun ScrollTodayIntoView(scroll: ScrollState, width: Int, todayColumn: Int?) {
    // Int.MAX_VALUE until the grid is first laid out.
    val maxScroll = scroll.maxValue.takeIf { it in 1 until Int.MAX_VALUE } ?: return
    LaunchedEffect(todayColumn, maxScroll) {
        if (todayColumn == null) return@LaunchedEffect
        val column = (maxScroll + width) / DAYS_IN_WEEK
        scroll.scrollTo(((todayColumn + 1) * column - width).coerceIn(0, maxScroll))
    }
}

/** The narrow weekday names, in `labelSmall`. TalkBack reads each day's full name. */
@Composable
private fun WeekdayInitials(firstDayOfWeek: DayOfWeek) {
    val locale = cycleLocale()
    val style = CycleTheme.typography.labelSmall.copy(
        color = CycleTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    repeat(DAYS_IN_WEEK) { index ->
        val day = firstDayOfWeek.plus(index.toLong())
        val fullName = day.getDisplayName(TextStyle.FULL, locale)
        BasicText(
            day.getDisplayName(TextStyle.NARROW, locale),
            modifier = Modifier.semantics { contentDescription = fullName },
            style = style
        )
    }
}

/**
 * The day cells of [weeks], all the size of the largest, centred in seven equal columns at least as
 * wide as the width it is given. Each cell is keyed by its date, so moving to another month never
 * replays the logging morph.
 */
@Composable
private fun DayCells(
    weeks: List<List<LocalDate?>>,
    stateOf: (LocalDate) -> CycleDayState,
    onDayClick: (LocalDate) -> Unit,
    today: LocalDate,
    selected: LocalDate?,
    isEnabled: (LocalDate) -> Boolean,
    wordsOf: (LocalDate) -> BleedingWords
) {
    val slots = remember(weeks) {
        weeks.flatMapIndexed { row, week ->
            week.mapIndexedNotNull { column, date -> date?.let { Slot(it, row, column) } }
        }
    }
    Layout(
        content = {
            slots.forEach { slot ->
                key(slot.date) {
                    DayCell(
                        date = slot.date,
                        state = stateOf(slot.date),
                        onClick = if (isEnabled(slot.date)) ({ onDayClick(slot.date) }) else null,
                        modifier = Modifier.semantics {
                            collectionItemInfo = CollectionItemInfo(slot.row, 1, slot.column, 1)
                        },
                        isToday = slot.date == today,
                        selected = slot.date == selected,
                        words = wordsOf(slot.date)
                    )
                }
            }
        },
        modifier = Modifier.semantics { collectionInfo = CollectionInfo(weeks.size, DAYS_IN_WEEK) }
    ) { measurables, constraints ->
        val cells = measurables.map { it.measure(Constraints()) }
        val side = cells.maxOfOrNull { maxOf(it.width, it.height) } ?: 0
        val column = maxOf(side, constraints.minWidth / DAYS_IN_WEEK)
        val width = maxOf(column * DAYS_IN_WEEK, constraints.minWidth)
        val start = (width - column * DAYS_IN_WEEK) / 2
        layout(width, weeks.size * side) {
            cells.forEachIndexed { index, cell ->
                val slot = slots[index]
                cell.placeRelative(
                    x = start + slot.column * column + (column - cell.width) / 2,
                    y = slot.row * side + (side - cell.height) / 2
                )
            }
        }
    }
}

private class Slot(val date: LocalDate, val row: Int, val column: Int)

/** March 2027 from the Zest board: a logged period on 2 to 6 March and the next one predicted from 29 March. */
internal val SampleMonth: YearMonth = YearMonth.of(2027, 3)

/** The cycle state of each day around [SampleMonth]. Synthetic. */
internal fun sampleDayState(date: LocalDate): CycleDayState = when (date) {
    in LocalDate.of(2027, 3, 2)..LocalDate.of(2027, 3, 6) -> CycleDayState.Period
    in LocalDate.of(2027, 3, 29)..LocalDate.of(2027, 4, 2) -> CycleDayState.PredictedPeriod
    else -> CycleDayState.Plain
}

/** [SampleMonth] with today on the 20th, the 19th selected, and the days after today not tappable. */
@Composable
internal fun SampleMonthCalendar(modifier: Modifier = Modifier) {
    MonthCalendar(
        month = SampleMonth,
        stateOf = ::sampleDayState,
        onDayClick = {},
        today = SampleToday,
        onPreviousMonth = {},
        onNextMonth = {},
        modifier = modifier,
        selected = SampleToday.minusDays(1),
        isEnabled = { !it.isAfter(SampleToday) }
    )
}

/** The week of [SampleToday] in [SampleMonth]'s states, the 19th selected and the days after today not tappable. */
@Composable
internal fun SampleWeekCalendar(modifier: Modifier = Modifier) {
    WeekRow(
        weekOf = SampleToday,
        stateOf = ::sampleDayState,
        onDayClick = {},
        today = SampleToday,
        modifier = modifier,
        selected = SampleToday.minusDays(1),
        isEnabled = { !it.isAfter(SampleToday) }
    )
}

/** A calendar inside the 12dp margins it is laid out with on a phone. */
@Composable
private fun CalendarPreviewSurface(darkTheme: Boolean, content: @Composable () -> Unit) {
    CycleTheme(darkTheme = darkTheme) {
        Box(Modifier.background(CycleTheme.colors.surface).padding(CycleTheme.spacing.medium)) { content() }
    }
}

@Composable
private fun CalendarSamples() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        SampleMonthCalendar()
        SampleWeekCalendar()
        CycleLegend(entries = CycleLegendEntry.WithoutFertility)
    }
}

@Preview(name = "Calendar · light", widthDp = 360)
@Composable
private fun CalendarLightPreview() = CalendarPreviewSurface(darkTheme = false) { CalendarSamples() }

@Preview(name = "Calendar · dark", widthDp = 360)
@Composable
private fun CalendarDarkPreview() = CalendarPreviewSurface(darkTheme = true) { CalendarSamples() }

@Preview(name = "Calendar · 200%", widthDp = 360, fontScale = 2f)
@Composable
private fun CalendarLargeTextPreview() = CalendarPreviewSurface(darkTheme = false) { CalendarSamples() }

@Preview(name = "Calendar · right-to-left", widthDp = 360, locale = "ar")
@Composable
private fun CalendarRtlPreview() = CalendarPreviewSurface(darkTheme = false) { CalendarSamples() }
