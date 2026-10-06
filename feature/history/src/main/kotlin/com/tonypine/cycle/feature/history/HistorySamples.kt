package com.tonypine.cycle.feature.history

import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.EnergyLevel
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.Mood
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.Period
import com.tonypine.cycle.core.model.PeriodRefusal
import com.tonypine.cycle.core.model.SexualActivity
import com.tonypine.cycle.core.model.SleepQuality
import java.time.LocalDate
import java.time.YearMonth

/**
 * One synthetic History per state, for previews and screenshots. Made-up dates in 2027, never anyone's
 * real cycle: six cycles of 29, 26, 31, 28, 27 and 28 days, with periods of 5, 4, 6, 5, 5, 6 and 4
 * days. `HistoryUiStateTest` checks the same states come out of a real log.
 */
internal object HistorySamples {
    val today: LocalDate = LocalDate.of(2027, 9, 10)

    private val current = CycleSummary(
        start = day("2027-09-02"),
        end = null,
        length = 9,
        period = Period(day("2027-09-02"), day("2027-09-05"))
    )

    private val past = listOf(
        cycle("2027-08-05", length = 28, periodLength = 6),
        cycle("2027-07-09", length = 27, periodLength = 5),
        cycle("2027-06-11", length = 28, periodLength = 5),
        cycle("2027-05-11", length = 31, periodLength = 6),
        cycle("2027-04-15", length = 26, periodLength = 4),
        cycle("2027-03-17", length = 29, periodLength = 5)
    )

    val loading = HistoryUiState.Loading

    val empty = HistoryUiState.Empty(hasPeriod = false)

    val firstCycle = HistoryUiState.Empty(hasPeriod = true)

    /** Her typical cycle, then the current cycle and six past ones. */
    val cycles = HistoryUiState.Cycles(
        today = today,
        typicalCycle = LengthSummary(median = 28, shortest = 26, longest = 31, count = 6),
        typicalPeriod = LengthSummary(median = 5, shortest = 4, longest = 6, count = 6),
        cycles = listOf(current) + past
    )

    /**
     * August's cycle, with a day she logged no flow on, and how she felt: cramps over the first
     * three days, bloating on days 24 to 27, and two notes.
     */
    val pastCycle = CycleDetailUiState.Detail(
        today = today,
        cycle = past.first(),
        flow = flow(
            "2027-08-05",
            FlowLevel.MEDIUM,
            FlowLevel.HEAVY,
            FlowLevel.HEAVY,
            FlowLevel.MEDIUM,
            null,
            FlowLevel.LIGHT
        ),
        symptoms = listOf(
            SymptomDays(Symptom.Pain(PainLevel.MILD, PainKind.CRAMPS), listOf(3)),
            SymptomDays(Symptom.Pain(PainLevel.MODERATE, PainKind.CRAMPS), listOf(1, 2)),
            SymptomDays(Symptom.Pain(PainLevel.MODERATE, PainKind.LOWER_BACK), listOf(1)),
            SymptomDays(Symptom.Body(BodySymptom.BLOATING), listOf(24, 25, 26, 27)),
            SymptomDays(Symptom.Body(BodySymptom.TENDER_BREASTS), listOf(26)),
            SymptomDays(Symptom.Body(BodySymptom.TIRED), listOf(2)),
            SymptomDays(Symptom.Feeling(Mood.SENSITIVE), listOf(26)),
            SymptomDays(Symptom.Feeling(Mood.IRRITABLE), listOf(1)),
            SymptomDays(Symptom.Energy(EnergyLevel.LOW), listOf(1)),
            SymptomDays(Symptom.Sleep(SleepQuality.BADLY), listOf(2)),
            SymptomDays(Symptom.Sex(SexualActivity.PROTECTED), listOf(14))
        ),
        notes = listOf(
            CycleNote(1, "Sample note: a heat pad helped."),
            CycleNote(3, "Sample note: a short walk after lunch.")
        )
    )

    /** The cycle she is in: day 9, after a 4-day period. */
    val currentCycle = CycleDetailUiState.Detail(
        today = today,
        cycle = current,
        flow = flow("2027-09-02", FlowLevel.MEDIUM, FlowLevel.HEAVY, FlowLevel.MEDIUM, FlowLevel.LIGHT)
    )

    val missing = CycleDetailUiState.Missing

    /** August's period in the editor, its last day moved from August 10 to August 8 and picking it. */
    val editPast = EditPeriodUiState.Editing(
        today = today,
        period = past.first().period,
        canStillGo = false,
        draft = PeriodDraft(
            start = day("2027-08-05"),
            end = day("2027-08-08"),
            picking = PeriodDay.Last,
            month = YearMonth.of(2027, 8)
        )
    )

    /** The current cycle's period in the editor, set still going. */
    val editCurrent = EditPeriodUiState.Editing(
        today = today,
        period = current.period,
        canStillGo = true,
        draft = PeriodDraft(
            start = day("2027-09-02"),
            end = null,
            picking = PeriodDay.First,
            month = YearMonth.of(2027, 9)
        )
    )

    /** August's period moved to start on July 12, refused: it runs into July's. */
    val editRefused = editPast.copy(
        draft = editPast.draft.copy(
            start = day("2027-07-12"),
            picking = PeriodDay.First,
            month = YearMonth.of(2027, 7),
            refusal = PeriodRefusal.TooClose(past[1].period)
        )
    )

    /** Every list state by name, for the screenshot tests. */
    val all: Map<String, HistoryUiState> = mapOf(
        "loading" to loading,
        "empty" to empty,
        "first_cycle" to firstCycle,
        "cycles" to cycles
    )

    /** Every detail state by name, for the screenshot tests. */
    val allDetails: Map<String, CycleDetailUiState> = mapOf(
        "past" to pastCycle,
        "current" to currentCycle,
        "missing" to missing
    )

    /** Every editor state by name, for the screenshot tests. */
    val allEdits: Map<String, EditPeriodUiState> = mapOf(
        "past" to editPast,
        "current" to editCurrent,
        "refused" to editRefused
    )

    private fun cycle(start: String, length: Int, periodLength: Int): CycleSummary {
        val first = day(start)
        return CycleSummary(
            start = first,
            end = first.plusDays(length - 1L),
            length = length,
            period = Period(first, first.plusDays(periodLength - 1L))
        )
    }

    private fun flow(start: String, vararg levels: FlowLevel?): List<FlowDay> =
        levels.mapIndexed { index, level -> FlowDay(day(start).plusDays(index.toLong()), level) }

    private fun day(iso: String): LocalDate = LocalDate.parse(iso)
}
