package com.tonypine.cycle.feature.history

import com.tonypine.cycle.core.domain.CycleCalculator
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The period editor from a synthetic log, through the real calculator, and what each tap picks. */
class EditPeriodUiStateTest {
    private val today = HistorySamples.today

    private fun editor(start: String, draft: PeriodDraft? = null) =
        EditPeriodUiState.from(CycleCalculator.overview(syntheticHistory, notSetUp, today), day(start), draft)

    private val august = editor("2027-08-05") as EditPeriodUiState.Editing

    @Test
    fun `a past period opens as stored, picking its first day, with nothing to save`() {
        assertEquals(day("2027-08-05"), august.draft.start)
        assertEquals(day("2027-08-10"), august.draft.end)
        assertEquals(PeriodDay.First, august.draft.picking)
        assertEquals(YearMonth.of(2027, 8), august.draft.month)
        assertEquals(6, august.length)
        assertFalse(august.canStillGo)
        assertFalse(august.changed)
    }

    @Test
    fun `only the current cycle's period can be still going`() {
        val current = editor("2027-09-02") as EditPeriodUiState.Editing
        assertTrue(current.canStillGo)

        val going = current.copy(draft = current.draft.stillGoing(on = true, lastDay = current.period.end))
        assertEquals(null, going.draft.end)
        assertEquals(day("2027-09-10"), going.lastDay)
        assertEquals(9, going.length)
        assertTrue(going.changed)

        val ended = going.draft.stillGoing(on = false, lastDay = current.period.end)
        assertEquals(current.draft.copy(picking = PeriodDay.Last), ended)
    }

    @Test
    fun `a cycle that no longer starts on that day is missing`() {
        assertEquals(EditPeriodUiState.Missing, editor("2027-08-06"))
    }

    @Test
    fun `picking the first day moves on to the last, which follows when it would come before`() {
        val earlier = august.draft.pick(day("2027-08-03"), today)
        assertEquals(august.draft.copy(start = day("2027-08-03"), picking = PeriodDay.Last), earlier)

        val later = august.draft.pick(day("2027-08-12"), today)
        assertEquals(day("2027-08-12"), later.start)
        assertEquals(day("2027-08-12"), later.end)
    }

    @Test
    fun `the last day can be any day from the first up to today`() {
        val last = august.draft.choose(PeriodDay.Last)
        assertEquals(day("2027-08-08"), last.pick(day("2027-08-08"), today).end)
        assertEquals(last, last.pick(day("2027-08-04"), today))
        assertEquals(last, last.pick(today.plusDays(1), today))

        val editing = august.copy(draft = last)
        assertTrue(editing.canPick(day("2027-08-05")))
        assertFalse(editing.canPick(day("2027-08-04")))
        assertFalse(editing.canPick(today.plusDays(1)))
        assertTrue(august.canPick(day("2027-08-04")))
    }

    @Test
    fun `choosing an end turns the calendar to its month`() {
        val draft = august.draft.copy(end = day("2027-09-01"))

        assertEquals(YearMonth.of(2027, 9), draft.choose(PeriodDay.Last).month)
        assertEquals(YearMonth.of(2027, 8), draft.choose(PeriodDay.Last).choose(PeriodDay.First).month)
    }

    @Test
    fun `a change clears the reason the last save was refused`() {
        val refused = HistorySamples.editRefused.draft

        assertEquals(null, refused.pick(day("2027-08-04"), today).refusal)
    }
}
