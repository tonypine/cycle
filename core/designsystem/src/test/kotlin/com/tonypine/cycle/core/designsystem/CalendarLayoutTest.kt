package com.tonypine.cycle.core.designsystem

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarLayoutTest {
    private val mondayFirst = WeekFields.of(Locale.UK).firstDayOfWeek
    private val sundayFirst = WeekFields.of(Locale.US).firstDayOfWeek

    @Test
    fun theLocaleGivesTheFirstDayOfTheWeek() {
        assertEquals(DayOfWeek.MONDAY, mondayFirst)
        assertEquals(DayOfWeek.SUNDAY, sundayFirst)
    }

    @Test
    fun aMondayFirstMonthStartsInItsFirstColumn() {
        // 1 March 2027 is a Monday.
        val weeks = monthWeeks(YearMonth.of(2027, 3), mondayFirst)
        assertEquals(5, weeks.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), weeks.first().map { it?.dayOfMonth })
        assertEquals(listOf(29, 30, 31, null, null, null, null), weeks.last().map { it?.dayOfMonth })
        assertEquals(LocalDate.of(2027, 3, 20), weeks[2][5])
    }

    @Test
    fun aSundayFirstMonthLeavesTheSundayBeforeItBlank() {
        val weeks = monthWeeks(YearMonth.of(2027, 3), sundayFirst)
        assertEquals(5, weeks.size)
        assertEquals(listOf(null, 1, 2, 3, 4, 5, 6), weeks.first().map { it?.dayOfMonth })
        assertEquals(listOf(28, 29, 30, 31, null, null, null), weeks.last().map { it?.dayOfMonth })
        assertEquals(LocalDate.of(2027, 3, 20), weeks[2][6])
    }

    @Test
    fun aMonthStartingOnSundaySpansSixMondayFirstWeeks() {
        // 1 August 2027 is a Sunday: the last day of a Monday-first week, the first of a Sunday-first one.
        val august = YearMonth.of(2027, 8)
        val mondayWeeks = monthWeeks(august, mondayFirst)
        assertEquals(6, mondayWeeks.size)
        assertEquals(listOf(null, null, null, null, null, null, 1), mondayWeeks.first().map { it?.dayOfMonth })
        assertEquals(listOf(30, 31, null, null, null, null, null), mondayWeeks.last().map { it?.dayOfMonth })

        val sundayWeeks = monthWeeks(august, sundayFirst)
        assertEquals(5, sundayWeeks.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), sundayWeeks.first().map { it?.dayOfMonth })
    }

    @Test
    fun everyWeekHasSevenDaysAndEveryDayOfTheMonthAppearsOnce() {
        listOf(mondayFirst, sundayFirst, DayOfWeek.SATURDAY).forEach { first ->
            (1..12).map { YearMonth.of(2028, it) }.forEach { month ->
                val weeks = monthWeeks(month, first)
                weeks.forEach { assertEquals(7, it.size) }
                assertEquals((1..month.lengthOfMonth()).map { month.atDay(it) }, weeks.flatten().filterNotNull())
                val lead = weeks.first().indexOfFirst { it != null }
                assertEquals(first, weeks.first()[lead]!!.minusDays(lead.toLong()).dayOfWeek)
            }
        }
    }

    @Test
    fun theWeekRowRunsFromTheFirstDayOfTheWeek() {
        val saturday = LocalDate.of(2027, 3, 20)
        assertEquals((15..21).map { LocalDate.of(2027, 3, it) }, weekDays(saturday, mondayFirst))
        assertEquals((14..20).map { LocalDate.of(2027, 3, it) }, weekDays(saturday, sundayFirst))
        // A week across two months shows the days of both.
        assertEquals(
            listOf(LocalDate.of(2027, 3, 29), LocalDate.of(2027, 4, 4)),
            weekDays(LocalDate.of(2027, 4, 1), mondayFirst).let { listOf(it.first(), it.last()) }
        )
    }
}
