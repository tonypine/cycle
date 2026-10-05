package com.tonypine.cycle.feature.today

import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.SleepQuality
import com.tonypine.cycle.core.ui.CalendarDays
import com.tonypine.cycle.core.ui.DayLogEntry
import java.time.LocalDate
import java.time.YearMonth

/**
 * One synthetic Today per state, for previews and screenshots. Made-up dates in 2027, never anyone's
 * real cycle. `TodayViewModelTest` checks the same states come out of a real log.
 */
internal object TodaySamples {
    val today: LocalDate = LocalDate.of(2027, 3, 20)

    val empty = TodayUiState.Empty(today, periodLength = 5)

    /** Day 19, with no cycle of her own yet: a typical 28-day cycle. */
    val midCycle = tracking(
        cycleDay = 19,
        phase = TodayPhase.BetweenPeriods(daysUntil = 10),
        periods = listOf(period("2027-03-02", "2027-03-06")),
        next = next("2027-03-30", "2027-03-26", "2027-04-03", lastStart = "2027-03-02", basis = EstimateBasis.Typical)
    )

    /** Day 25: today is the first day of the range. */
    val due = tracking(
        cycleDay = 25,
        phase = TodayPhase.Due,
        periods = listOf(period("2027-02-24", "2027-02-28")),
        next = next("2027-03-24", "2027-03-20", "2027-03-28", lastStart = "2027-02-24", basis = EstimateBasis.Typical)
    )

    /** She tapped "My period started" today. */
    val periodStarted = tracking(
        cycleDay = 1,
        phase = TodayPhase.PeriodStartedToday,
        periods = listOf(period("2027-02-20", "2027-02-24"), period("2027-03-20", "2027-03-20", open = true)),
        next = next("2027-04-17", "2027-04-14", "2027-04-20", lastStart = "2027-03-20", basis = EstimateBasis.Logged(1))
    )

    /** Day 4 of her period. */
    val onPeriod = tracking(
        cycleDay = 4,
        phase = TodayPhase.OnPeriod(periodDay = 4),
        periods = listOf(period("2027-02-17", "2027-02-21"), period("2027-03-17", "2027-03-20", open = true)),
        next = next(
            "2027-04-14",
            "2027-04-11",
            "2027-04-17",
            lastStart = "2027-03-17",
            basis = EstimateBasis.Logged(1)
        ),
        todayFlow = FlowLevel.MEDIUM
    )

    /** She tapped "My period ended" today, on day 5. */
    val periodEnded = tracking(
        cycleDay = 5,
        phase = TodayPhase.PeriodEndedToday(length = 5),
        periods = listOf(period("2027-02-16", "2027-02-20"), period("2027-03-16", "2027-03-20")),
        next = next(
            "2027-04-13",
            "2027-04-10",
            "2027-04-16",
            lastStart = "2027-03-16",
            basis = EstimateBasis.Logged(1)
        ),
        todayFlow = FlowLevel.LIGHT
    )

    /** Two days later than her usual 28 days, from six logged cycles. */
    val late = tracking(
        cycleDay = 31,
        phase = TodayPhase.Late(daysLate = 2),
        periods = listOf(period("2027-01-21", "2027-01-25"), period("2027-02-18", "2027-02-22")),
        next = next(
            "2027-03-20",
            "2027-03-20",
            "2027-03-21",
            lastStart = "2027-02-18",
            basis = EstimateBasis.Logged(6),
            daysLate = 2
        )
    )

    /** Day 8 of a period that usually lasts 5 days. */
    val stillGoing = tracking(
        cycleDay = 8,
        phase = TodayPhase.OnPeriod(periodDay = 8),
        periods = listOf(period("2027-02-13", "2027-02-17"), period("2027-03-13", "2027-03-20", open = true)),
        next = next(
            "2027-04-10",
            "2027-04-07",
            "2027-04-13",
            lastStart = "2027-03-13",
            basis = EstimateBasis.Logged(1)
        ),
        todayFlow = FlowLevel.LIGHT,
        stillGoing = StillGoing(
            CyclePrompt.StillGoing(LocalDate.parse("2027-03-13"), periodDay = 8),
            LocalDate.parse("2027-03-13")..today
        )
    )

    /** Day 51 of a cycle that usually lasts 28 days: did she miss logging a period in February? */
    val missedPeriod = tracking(
        cycleDay = 51,
        phase = TodayPhase.Late(daysLate = 22),
        periods = listOf(period("2027-01-01", "2027-01-05"), period("2027-01-29", "2027-02-02")),
        next = next(
            "2027-03-20",
            "2027-03-20",
            "2027-03-20",
            lastStart = "2027-01-29",
            basis = EstimateBasis.Logged(1),
            daysLate = 22
        ),
        missedPeriod = MissedPeriod(
            CyclePrompt.MissedPeriod(LocalDate.parse("2027-01-29"), cycleDay = 51),
            likelyMonth = YearMonth.of(2027, 2)
        )
    )

    /** Day 19, after logging how she feels: the walkthrough's day, with a synthetic note. */
    val loggedToday = midCycle.copy(
        todayLog = midCycle.todayLog.copy(
            canClear = true,
            feelings = DayFeelings(
                date = today,
                pain = Pain(PainLevel.MODERATE, setOf(PainKind.CRAMPS)),
                body = setOf(BodySymptom.BLOATING),
                moods = setOf(Mood.IRRITABLE),
                energy = EnergyLevel.LOW,
                sleep = SleepQuality.BADLY,
                note = "A synthetic note."
            )
        )
    )

    /** Every state, by name, in the order the screenshots list them. */
    val all: Map<String, TodayUiState> = linkedMapOf(
        "empty" to empty,
        "mid_cycle" to midCycle,
        "due" to due,
        "period_started" to periodStarted,
        "on_period" to onPeriod,
        "period_ended" to periodEnded,
        "late" to late,
        "still_going" to stillGoing,
        "missed_period" to missedPeriod,
        "logged_today" to loggedToday
    )

    private fun tracking(
        cycleDay: Int,
        phase: TodayPhase,
        periods: List<Period>,
        next: NextPeriod,
        todayFlow: FlowLevel? = null,
        stillGoing: StillGoing? = null,
        missedPeriod: MissedPeriod? = null
    ): TodayUiState.Tracking {
        val days = CalendarDays(
            periods = periods,
            // The next three, a usual cycle apart, five days each.
            predicted = (0L until 3L).map { ahead ->
                val start = next.expectedStart.plusDays(ahead * next.cycleLength)
                start..start.plusDays(4)
            }
        )
        return TodayUiState.Tracking(
            today = today,
            cycleDay = cycleDay,
            phase = phase,
            days = days,
            todayLog = DayLogEntry(
                date = today,
                flow = todayFlow,
                canClear = days.isPeriodDay(today) || todayFlow != null,
                isPeriodDay = days.isPeriodDay(today)
            ),
            nextPeriod = next,
            stillGoing = stillGoing,
            missedPeriod = missedPeriod
        )
    }

    private fun period(start: String, end: String, open: Boolean = false) =
        Period(LocalDate.parse(start), LocalDate.parse(end), isOpen = open)

    private fun next(
        expected: String,
        earliest: String,
        latest: String,
        lastStart: String,
        basis: EstimateBasis,
        daysLate: Int = 0
    ) = NextPeriod(
        expectedStart = LocalDate.parse(expected),
        earliestStart = LocalDate.parse(earliest),
        latestStart = LocalDate.parse(latest),
        lastStart = LocalDate.parse(lastStart),
        cycleLength = 28,
        daysLate = daysLate,
        basis = basis
    )
}
