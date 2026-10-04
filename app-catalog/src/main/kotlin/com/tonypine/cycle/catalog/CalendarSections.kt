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
import com.tonypine.cycle.core.designsystem.CycleDayState
import com.tonypine.cycle.core.designsystem.CycleLegend
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.DayCell
import java.time.LocalDate

// Synthetic dates from the Zest board: a made-up March 2027, period on 2 to 6, fertile window 13 to
// 18, ovulation on 17, today on 20, and the next period predicted from 29.
private val Today = LocalDate.of(2027, 3, 20)

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
    }
    CatalogNote(
        "Selected adds an accent rounded-square frame. A day outside the month or in the future is " +
            "disabled: it draws at ${(CycleTheme.stateAlpha.disabledContent * 100).toInt()}% and ignores taps."
    )

    SubsectionTitle("Sample week")
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        Week.forEach { (date, state) ->
            DayCell(
                date,
                state,
                onClick = {},
                isToday = date == Today,
                selected = date == Today.minusDays(1),
                enabled = !date.isAfter(Today)
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
internal fun CycleLegendSection() {
    CycleLegend()
    CatalogNote(
        "Each swatch is the day cell's shape. The legend wraps when text is large, and TalkBack reads it " +
            "as a list of five."
    )
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
