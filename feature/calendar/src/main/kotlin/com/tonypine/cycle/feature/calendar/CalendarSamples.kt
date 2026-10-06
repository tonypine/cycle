package com.tonypine.cycle.feature.calendar

import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.ui.CalendarDays
import com.tonypine.cycle.core.ui.DayLogEntry
import java.time.LocalDate
import java.time.YearMonth

/**
 * Synthetic calendars, for previews and screenshots: made-up dates in 2027, never anyone's real
 * cycle. `CalendarViewModelTest` checks the same days come out of a real log.
 */
internal object CalendarSamples {
    val today: LocalDate = LocalDate.of(2027, 3, 20)

    /** Two logged periods, the second from 26 February to 2 March, and three estimated ones a 28-day cycle apart. */
    private val days = CalendarDays(
        periods = listOf(
            Period(LocalDate.of(2027, 1, 29), LocalDate.of(2027, 2, 2)),
            Period(LocalDate.of(2027, 2, 26), LocalDate.of(2027, 3, 2))
        ),
        predicted = (0L until 3L).map { ahead ->
            val start = LocalDate.of(2027, 3, 26).plusDays(ahead * 28)
            start..start.plusDays(4)
        }
    )

    /** March: the end of a period that started in February, today, and the next period predicted. */
    val march = CalendarUiState.Ready(today, YearMonth.of(2027, 3), selected = null, days = days, selectedLog = null)

    /** February: the start of the same period, in the month before. */
    val february = march.copy(month = YearMonth.of(2027, 2))

    /** April: only estimates, two of them. */
    val april = march.copy(month = YearMonth.of(2027, 4))

    /** March with the 9th selected: nothing logged and no period near, so the fill chip shows. */
    val selected = march.copy(
        selected = LocalDate.of(2027, 3, 9),
        selectedLog = DayLogEntry(LocalDate.of(2027, 3, 9), fillDays = 5)
    )

    private val implant = ContraceptionStretch(ContraceptionMethod.IMPLANT, LocalDate.of(2027, 3, 4))

    /**
     * C4: the implant fitted on 4 March, after a period that keeps its word; bleeding logged on the 10th
     * and 11th; nothing predicted.
     */
    val implantMarch = march.copy(
        days = CalendarDays(
            periods = listOf(
                Period(LocalDate.of(2027, 2, 26), LocalDate.of(2027, 3, 2)),
                Period(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), stretch = implant)
            ),
            predicted = emptyList(),
            stretches = listOf(implant)
        ),
        hint = CalendarHint.NoEstimate(ContraceptionMethod.IMPLANT)
    )

    private val pill =
        ContraceptionStretch(ContraceptionMethod.COMBINED_PILL, LocalDate.of(2027, 3, 6), breaks = Breaks.MONTHLY)

    /** B7: the pill since 6 March; the first bleed expected in the first break, 27 March to 2 April. */
    val pillMarch = march.copy(
        days = CalendarDays(
            periods = days.periods,
            predicted = listOf(
                LocalDate.of(2027, 3, 27)..LocalDate.of(2027, 4, 2),
                LocalDate.of(2027, 4, 24)..LocalDate.of(2027, 4, 30),
                LocalDate.of(2027, 5, 22)..LocalDate.of(2027, 5, 28)
            ),
            stretches = listOf(pill)
        ),
        hint = CalendarHint.Bleeds
    )

    /** D: the implant came out on 8 March; her next period from her usual cycle, the range wider. */
    val stoppedImplantMarch = implantMarch.copy(
        days = implantMarch.days.copy(
            predicted = (0L until 3L).map { ahead ->
                val start = LocalDate.of(2027, 4, 5).plusDays(ahead * 28)
                start..start.plusDays(4)
            },
            stretches = listOf(implant.copy(stopped = LocalDate.of(2027, 3, 8)))
        ),
        hint = CalendarHint.Periods
    )

    /** Every month, by name, in the order the screenshots list them. */
    val all: Map<String, CalendarUiState.Ready> = linkedMapOf(
        "march" to march,
        "february" to february,
        "april" to april,
        "selected" to selected,
        "implant_march" to implantMarch,
        "pill_march" to pillMarch,
        "stopped_implant_march" to stoppedImplantMarch
    )
}
