package com.tonypine.cycle.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import com.tonypine.cycle.core.designsystem.BleedingWords
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleLegend
import com.tonypine.cycle.core.designsystem.CycleLegendEntry
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.DayCell
import com.tonypine.cycle.core.designsystem.MonthCalendar
import com.tonypine.cycle.core.designsystem.WeekRow
import java.time.LocalDate
import java.time.YearMonth

// Synthetic dates from the Zest board: a made-up March 2027, period on 2 to 6, fertile window 13 to
// 18, ovulation on 17, today on 20, and the next period predicted from 29.
private val Today = LocalDate.of(2027, 3, 20)

/** The calendar's synthetic days, with fertility estimates off as in the MVP: periods only. */
private fun calendarState(date: LocalDate): CycleDayState = when (date) {
    in LocalDate.of(2027, 3, 2)..LocalDate.of(2027, 3, 6) -> CycleDayState.Period
    in LocalDate.of(2027, 3, 29)..LocalDate.of(2027, 4, 2) -> CycleDayState.PredictedPeriod
    else -> CycleDayState.Plain
}

private val StateSamples = listOf(
    Triple(CycleDayState.Plain, LocalDate.of(2027, 3, 9), "Plain · the number on its own"),
    Triple(CycleDayState.Period, LocalDate.of(2027, 3, 4), "Period · period squircle, onPeriod number"),
    Triple(
        CycleDayState.PredictedPeriod,
        LocalDate.of(2027, 3, 30),
        "Predicted period · pale squircle, dashed edge"
    ),
    Triple(CycleDayState.Fertile, LocalDate.of(2027, 3, 14), "Fertile window · tinted circle, marker dot"),
    Triple(CycleDayState.Ovulation, LocalDate.of(2027, 3, 17), "Ovulation · eight-point sun")
)

private val Week = listOf(
    CycleDayState.Fertile,
    CycleDayState.Fertile,
    CycleDayState.Ovulation,
    CycleDayState.Fertile,
    CycleDayState.Plain,
    CycleDayState.Plain,
    CycleDayState.Plain
).mapIndexed { index, state -> LocalDate.of(2027, 3, 15 + index) to state }

@Composable
internal fun DayCellSection() {
    SubsectionTitle("States")
    CatalogNote("Each state on its own, then with today: a ring and a bolder number, the shape inside it.")
    StateSamples.forEach { (state, date, label) ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
        ) {
            DayCell(date, state, onClick = {})
            DayCell(date, state, onClick = {}, isToday = true)
            CatalogText(label, CycleTheme.typography.bodySmall)
        }
    }

    SubsectionTitle("Interaction")
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.medium)
    ) {
        listOf<Pair<String, Interaction?>>(
            "Pressed" to PressInteraction.Press(Offset.Zero),
            "Focused" to FocusInteraction.Focus()
        ).forEach { (name, interaction) ->
            Labelled(name) {
                DayCell(
                    Today,
                    CycleDayState.Period,
                    onClick = {},
                    isToday = true,
                    interactionSource = rememberHeldInteraction(interaction)
                )
            }
        }
        Labelled("Selected") { DayCell(Today, CycleDayState.Period, onClick = {}, isToday = true, selected = true) }
        Labelled("Disabled") { DayCell(Today.plusDays(1), CycleDayState.Plain, onClick = {}, enabled = false) }
        Labelled("Not tappable") { DayCell(Today.plusDays(10), CycleDayState.PredictedPeriod, onClick = null) }
    }
    CatalogNote(
        "Selected adds an accent rounded-square frame. Disabled draws at " +
            "${(CycleTheme.stateAlpha.disabledContent * 100).toInt()}% and ignores taps. A future day passes no " +
            "onClick instead: it ignores taps but keeps its full colour, so a predicted period stays readable."
    )

    SubsectionTitle("Sample week")
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        Week.forEach { (date, state) ->
            DayCell(
                date,
                state,
                onClick = if (date.isAfter(Today)) null else ({}),
                isToday = date == Today,
                selected = date == Today.minusDays(1)
            )
        }
    }
    CatalogNote("15 to 21 March: the fertile window, ovulation on the 17th, the 19th selected, today on the 20th.")

    SubsectionTitle("Log a day")
    var logged by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
    ) {
        DayCell(
            LocalDate.of(2027, 3, 2),
            if (logged) CycleDayState.Period else CycleDayState.Plain,
            onClick = { logged = !logged }
        )
        CatalogText(if (logged) "Logged. Tap to undo." else "Tap to log the day.", CycleTheme.typography.bodySmall)
    }
    CatalogNote(
        "Logging morphs the circle to the period squircle on the spatial spring. Under reduce motion " +
            "it jumps to the squircle."
    )
}

@Composable
internal fun CalendarSection() {
    var month by rememberSaveable { mutableStateOf(YearMonth.of(2027, 3)) }
    var selected by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    MonthCalendar(
        month = month,
        stateOf = ::calendarState,
        onDayClick = { selected = it },
        today = Today,
        onPreviousMonth = { month = month.minusMonths(1) },
        onNextMonth = { month = month.plusMonths(1) },
        modifier = Modifier.calendarMargins(),
        selected = selected,
        isEnabled = { !it.isAfter(Today) }
    )
    CycleLegend(entries = CycleLegendEntry.WithoutFertility)
    CatalogNote(
        "A logged period on 2 to 6 March, the next one predicted from 29 March, today on the 20th. Tap a " +
            "day up to today to select it; later days keep their colour but ignore taps. The week starts on " +
            "the phone's first day of the week."
    )
    CatalogNote(
        "Lay the calendar out with 12dp margins: seven 48dp cells fill a 360dp screen. With large text the " +
            "cells grow, and the weekdays and days scroll sideways, starting with today in view."
    )

    SubsectionTitle("Week row")
    WeekRow(
        weekOf = Today,
        stateOf = ::calendarState,
        onDayClick = { selected = it },
        today = Today,
        modifier = Modifier.calendarMargins(),
        selected = selected,
        isEnabled = { !it.isAfter(Today) }
    )
    CatalogNote("The week of today, for Today and for picking a recent day.")
}

@Composable
internal fun CycleLegendSection() {
    SubsectionTitle("Every entry")
    CycleLegend()
    SubsectionTitle("Without fertility")
    CycleLegend(entries = CycleLegendEntry.WithoutFertility)
    CatalogNote(
        "The caller chooses the entries: the MVP shows period, predicted period and today, since fertility " +
            "estimates are off by default. Each swatch is the day cell's shape. The legend wraps when text is " +
            "large, and TalkBack reads it as a list of its entries."
    )
    SubsectionTitle("In a method's words")
    CycleLegend(
        entries = CycleLegendEntry.WithoutFertility,
        words = listOf(BleedingWords.Bleed),
        predictedWords = BleedingWords.Bleed
    )
    CycleLegend(
        entries = listOf(CycleLegendEntry.Period, CycleLegendEntry.Today),
        words = listOf(BleedingWords.Period, BleedingWords.Bleeding)
    )
    CycleLegend(
        entries = CycleLegendEntry.WithoutFertility,
        words = listOf(BleedingWords.Period),
        predictedWords = BleedingWords.Bleed
    )
    CatalogNote(
        "BleedingWords name the days on a method: bleed and expected bleed on a combined pill with a break " +
            "every month, bleeding on the implant and the other methods with nothing expected. The shapes stay. " +
            "words lists the logged days on screen, each word once: a month spanning a method's start lists " +
            "both. predictedWords names the predicted days on their own, so a month with a period before the " +
            "pill and an expected bleed reads Period, Expected bleed. Day cells read the same words to " +
            "TalkBack: \"14 October, bleeding\"."
    )
}

/**
 * Widens a calendar from the catalog's 16dp page margins to the 12dp it is laid out with on a phone,
 * so seven 48dp cells fit a 360dp screen.
 */
@Composable
private fun Modifier.calendarMargins(): Modifier {
    val bleed = CycleTheme.spacing.large - CycleTheme.spacing.medium
    return layout { measurable, constraints ->
        val extra = bleed.roundToPx()
        if (!constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
        val placeable = measurable.measure(
            constraints.copy(minWidth = constraints.minWidth + 2 * extra, maxWidth = constraints.maxWidth + 2 * extra)
        )
        layout(placeable.width - 2 * extra, placeable.height) { placeable.placeRelative(-extra, 0) }
    }
}

@Composable
private fun Labelled(label: String, sample: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)
    ) {
        sample()
        CatalogText(label, CycleTheme.typography.bodySmall, color = CycleTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun CatalogNote(text: String) {
    CatalogText(text, CycleTheme.typography.bodySmall, color = CycleTheme.colors.onSurfaceVariant)
}
