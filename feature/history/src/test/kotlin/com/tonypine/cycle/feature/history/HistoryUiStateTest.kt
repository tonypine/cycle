package com.tonypine.cycle.feature.history

import com.tonypine.cycle.core.domain.CycleCalculator
import com.tonypine.cycle.core.model.BodySymptom
import com.tonypine.cycle.core.model.DayFeelings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import com.tonypine.cycle.core.model.LengthSummary
import com.tonypine.cycle.core.model.LogCategory
import com.tonypine.cycle.core.model.Pain
import com.tonypine.cycle.core.model.PainKind
import com.tonypine.cycle.core.model.PainLevel
import com.tonypine.cycle.core.model.Period
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** History and the cycle detail from a synthetic log, through the real calculator. */
class HistoryUiStateTest {
    private val today = HistorySamples.today

    private fun history(logs: List<DayLog>, on: LocalDate = today) =
        HistoryUiState.from(CycleCalculator.overview(logs, notSetUp, on))

    private fun detail(
        logs: List<DayLog>,
        start: String,
        on: LocalDate = today,
        feelings: List<DayFeelings> = syntheticFeelings,
        hidden: Set<LogCategory> = emptySet()
    ) = CycleDetailUiState.from(CycleCalculator.overview(logs, notSetUp, on), logs, day(start), feelings, hidden)

    private fun pastDetail(feelings: List<DayFeelings> = syntheticFeelings, hidden: Set<LogCategory> = emptySet()) =
        detail(syntheticHistory, "2027-08-05", feelings = feelings, hidden = hidden) as CycleDetailUiState.Detail

    @Test
    fun `her typical cycle is 28 days, 26 to 31, and her period 5 days, 4 to 6`() {
        val state = history(syntheticHistory) as HistoryUiState.Cycles

        assertEquals(LengthSummary(median = 28, shortest = 26, longest = 31, count = 6), state.typicalCycle)
        assertEquals(LengthSummary(median = 5, shortest = 4, longest = 6, count = 6), state.typicalPeriod)
    }

    @Test
    fun `the current cycle comes first, then the past cycles newest first`() {
        val state = history(syntheticHistory) as HistoryUiState.Cycles

        assertEquals(HistorySamples.cycles, state)
        assertEquals(
            listOf("2027-09-02", "2027-08-05", "2027-07-09", "2027-06-11", "2027-05-11", "2027-04-15", "2027-03-17"),
            state.cycles.map { it.start.toString() }
        )
        assertTrue(state.cycles.first().isCurrent)
        assertEquals(9, state.cycles.first().length)
        assertEquals(listOf(28, 27, 28, 31, 26, 29), state.cycles.drop(1).map { it.length })
    }

    @Test
    fun `with nothing logged, History is empty`() {
        assertEquals(HistoryUiState.Empty(hasPeriod = false), history(emptyList()))
    }

    @Test
    fun `with one period, her first cycle is still going`() {
        assertEquals(HistoryUiState.Empty(hasPeriod = true), history(bleed("2027-09-02", 4)))
    }

    @Test
    fun `one complete cycle gives a typical cycle of that length`() {
        val state = history(bleed("2027-08-05", 5) + bleed("2027-09-02", 4)) as HistoryUiState.Cycles

        assertEquals(LengthSummary(median = 28, shortest = 28, longest = 28, count = 1), state.typicalCycle)
        assertEquals(2, state.cycles.size)
    }

    @Test
    fun `a past cycle's detail has its dates and the flow of each period day`() {
        val state = detail(syntheticHistory, "2027-08-05") as CycleDetailUiState.Detail

        assertEquals(HistorySamples.pastCycle, state)
        assertEquals(day("2027-09-01"), state.cycle.end)
        assertEquals(Period(day("2027-08-05"), day("2027-08-10")), state.cycle.period)
        // She logged nothing on August 9, inside the period.
        assertEquals(FlowDay(day("2027-08-09"), null), state.flow[4])
        assertEquals(YearMonth.of(2027, 8), state.cycle.month)
    }

    @Test
    fun `the detail lists how she felt with the cycle days, in the day log's order, and her notes`() {
        val state = pastDetail()

        assertEquals(HistorySamples.pastCycle, state)
        // Pain by place then level, then body, mood, energy, sleep and sex.
        assertEquals(
            listOf(
                Symptom.Pain(PainLevel.MILD, PainKind.CRAMPS) to listOf(3),
                Symptom.Pain(PainLevel.MODERATE, PainKind.CRAMPS) to listOf(1, 2),
                Symptom.Pain(PainLevel.MODERATE, PainKind.LOWER_BACK) to listOf(1)
            ),
            state.symptoms.take(3).map { it.symptom to it.days }
        )
        assertEquals(
            listOf("Pain", "Pain", "Pain", "Body", "Body", "Body", "Feeling", "Feeling", "Energy", "Sleep", "Sex"),
            state.symptoms.map { it.symptom::class.simpleName }
        )
        assertEquals(
            listOf(24, 25, 26, 27),
            state.symptoms.single {
                it.symptom == Symptom.Body(BodySymptom.BLOATING)
            }.days
        )
        assertEquals(listOf(1, 3), state.notes.map { it.day })
    }

    @Test
    fun `the categories she hid do not show, and come back when she shows them`() {
        val hidden = pastDetail(hidden = setOf(LogCategory.BODY, LogCategory.NOTES))

        assertTrue(hidden.symptoms.none { it.symptom is Symptom.Body })
        assertEquals(emptyList<CycleNote>(), hidden.notes)
        assertEquals(8, hidden.symptoms.size)

        val allHidden = pastDetail(hidden = LogCategory.entries.toSet())
        assertEquals(emptyList<SymptomDays>(), allHidden.symptoms)

        assertEquals(HistorySamples.pastCycle, pastDetail(hidden = emptySet()))
    }

    @Test
    fun `what she logged outside the cycle stays out, and the next cycle counts from its own day 1`() {
        val around = listOf(
            DayFeelings(day("2027-08-04"), body = setOf(BodySymptom.ACNE)),
            DayFeelings(day("2027-09-02"), body = setOf(BodySymptom.NAUSEA)),
            DayFeelings(day("2027-09-03"), body = setOf(BodySymptom.NAUSEA))
        )

        assertEquals(HistorySamples.pastCycle, pastDetail(feelings = syntheticFeelings + around))
        val current = detail(syntheticHistory, "2027-09-02", feelings = syntheticFeelings + around)
        assertEquals(
            listOf(SymptomDays(Symptom.Body(BodySymptom.NAUSEA), listOf(1, 2))),
            (current as CycleDetailUiState.Detail).symptoms
        )
    }

    @Test
    fun `pain with nowhere said, and no pain, each count on their own`() {
        val state = pastDetail(
            feelings = listOf(
                DayFeelings(day("2027-08-10"), pain = Pain(PainLevel.SEVERE)),
                DayFeelings(day("2027-08-12"), pain = Pain(PainLevel.NONE)),
                DayFeelings(day("2027-08-13"), pain = Pain(PainLevel.NONE)),
                DayFeelings(day("2027-08-14"), note = "Sample note only.")
            )
        )

        assertEquals(
            listOf(
                SymptomDays(Symptom.Pain(PainLevel.NONE, place = null), listOf(8, 9)),
                SymptomDays(Symptom.Pain(PainLevel.SEVERE, place = null), listOf(6))
            ),
            state.symptoms
        )
        assertEquals(listOf(CycleNote(10, "Sample note only.")), state.notes)
    }

    @Test
    fun `three days in a row or more read as a range, one or two stay single`() {
        assertEquals(listOf(1..1, 2..2), dayRuns(listOf(1, 2)))
        assertEquals(listOf(24..27), dayRuns(listOf(24, 25, 26, 27)))
        assertEquals(listOf(1..3, 5..5, 6..6, 9..9), dayRuns(listOf(9, 1, 2, 3, 5, 6)))
        assertEquals(emptyList<IntRange>(), dayRuns(emptyList()))
    }

    @Test
    fun `the current cycle's detail counts its days so far`() {
        assertEquals(HistorySamples.currentCycle, detail(syntheticHistory, "2027-09-02"))
    }

    @Test
    fun `a period still going shows its days up to today`() {
        val logs = syntheticHistory.filter { it.date < day("2027-09-02") } +
            DayLog(day("2027-09-08"), FlowLevel.HEAVY, periodStarted = true)

        val state = detail(logs, "2027-09-08") as CycleDetailUiState.Detail

        assertTrue(state.cycle.period.isOpen)
        assertEquals(
            listOf(
                FlowDay(day("2027-09-08"), FlowLevel.HEAVY),
                FlowDay(day("2027-09-09"), null),
                FlowDay(day("2027-09-10"), null)
            ),
            state.flow
        )
    }

    @Test
    fun `a cycle whose period was edited away is missing`() {
        assertEquals(CycleDetailUiState.Missing, detail(syntheticHistory, "2027-08-06"))
        assertEquals(CycleDetailUiState.Missing, detail(emptyList(), "2027-08-05"))
    }
}
