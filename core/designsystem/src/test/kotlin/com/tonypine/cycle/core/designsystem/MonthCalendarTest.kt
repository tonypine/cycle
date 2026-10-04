package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.Themed
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class MonthCalendarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val days = SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionItemInfo)

    @Test
    fun theMonthNameIsAHeadingReadAgainWhenTheMonthChanges() {
        show()
        composeRule.onNodeWithText("March 2027")
            .assert(isHeading())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theGridReportsItsRowsAndColumns() {
        var month by mutableStateOf(SampleMonth)
        show(month = { month })
        assertGrid(rows = 5)
        composeRule.onAllNodes(days).assertCountEquals(31)

        // 1 August 2027 is a Sunday, so a Monday-first August spans six weeks.
        month = YearMonth.of(2027, 8)
        assertGrid(rows = 6)
        composeRule.onAllNodes(days).assertCountEquals(31)
    }

    @Test
    fun eachDayReadsAsADayCellInItsRowAndColumn() {
        show()
        composeRule.onNodeWithContentDescription("20 March, today").assert(item(row = 2, column = 5))
        composeRule.onNodeWithContentDescription("1 March").assert(item(row = 0, column = 0))
        composeRule.onNodeWithContentDescription("4 March, period").assert(item(row = 0, column = 3))
        composeRule.onNodeWithContentDescription("30 March, predicted period").assert(item(row = 4, column = 1))
        composeRule.onNodeWithContentDescription("19 March").assertIsSelected()
    }

    @Test
    fun previousAndNextAreLabelledButtonsThatMoveTheMonth() {
        var month by mutableStateOf(SampleMonth)
        show(month = { month }, onPrevious = { month = month.minusMonths(1) }, onNext = { month = month.plusMonths(1) })
        val button = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

        composeRule.onNodeWithContentDescription("Next month").assert(button).performClick()
        composeRule.onNodeWithText("April 2027").assert(isHeading())
        // The predicted period runs on into April.
        composeRule.onNodeWithContentDescription("1 April, predicted period").assert(item(row = 0, column = 3))

        composeRule.onNodeWithContentDescription("Previous month").assert(button).performClick()
        composeRule.onNodeWithContentDescription("Previous month").performClick()
        composeRule.onNodeWithText("February 2027").assert(isHeading())
    }

    @Test
    fun aDayThatIsNotEnabledKeepsItsStateButIgnoresTaps() {
        val clicked = mutableListOf<LocalDate>()
        show(onDayClick = { clicked += it })
        composeRule.onNodeWithContentDescription("18 March").assertHasClickAction().performClick()
        composeRule.onNodeWithContentDescription("30 March, predicted period")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Disabled))
            .performClick()
        assertEquals(listOf(LocalDate.of(2027, 3, 18)), clicked)
    }

    @Test
    fun aMondayFirstLocaleStartsTheWeekOnMonday() {
        show()
        assertWeekStartsOn("Monday", "1 March")
        assertTrue(left("Monday") < left("Sunday"))
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun aSundayFirstLocaleStartsTheWeekOnSunday() {
        show()
        assertWeekStartsOn("Sunday", "March 7")
        composeRule.onNodeWithContentDescription("March 1").assert(item(row = 0, column = 1))
        composeRule.onNodeWithContentDescription("March 20, today").assert(item(row = 2, column = 6))
        assertTrue(left("Sunday") < left("Monday"))
    }

    @Test
    fun rightToLeftStartsTheWeekOnTheRight() {
        show(Appearance.Rtl)
        assertTrue(left("8 March") > left("9 March"))
        assertTrue(left("Monday") > left("Sunday"))
    }

    @Test
    fun sevenCellsFitA360dpScreenInside12dpMargins() {
        show()
        assertEquals(0f, horizontalScrollRange(), 0f)
        cellWidths().forEach { assertTrue("Expected a 48dp cell, was $it", it >= 48.dp) }
        // The weekday initial sits over the centre of its column.
        assertEquals(centre("Monday"), centre("1 March"), 1f)
        assertEquals(centre("Sunday"), centre("7 March"), 1f)
    }

    @Test
    fun atDoubleFontScaleTheCellsGrowAndTheGridScrollsSideways() {
        show(Appearance.FontScale200)
        assertTrue("Expected the grid to scroll sideways", horizontalScrollRange() > 0f)
        val widths = cellWidths()
        widths.forEach { assertTrue("Expected the cell to grow past 48dp, was $it", it > 48.dp) }
        assertEquals("Every cell is the same size", 1, widths.distinct().size)
        // Today, a Saturday, starts in view: its column is the second to last.
        val today = composeRule.onNodeWithContentDescription("20 March, today").getBoundsInRoot()
        assertTrue("Today at $today is off the screen", today.right <= 348.dp && today.left >= 12.dp)
        // The header does not scroll: both arrows stay on screen.
        val next = composeRule.onNodeWithContentDescription("Next month").getBoundsInRoot()
        assertTrue("Next month at $next is off the 360dp screen", next.right <= 360.dp)
        assertEquals(centre("Saturday"), centre("20 March, today"), 1f)
    }

    @Test
    fun atDoubleFontScaleRightToLeftTodayStartsInViewToo() {
        show(Appearance.FontScale200, rtl = true)
        assertTrue("Expected the grid to scroll sideways", horizontalScrollRange() > 0f)
        val today = composeRule.onNodeWithContentDescription("20 March, today").getBoundsInRoot()
        assertTrue("Today at $today is off the screen", today.right <= 348.dp && today.left >= 12.dp)
        assertEquals(centre("Saturday"), centre("20 March, today"), 1f)
    }

    @Test
    fun theWeekRowShowsTheWeekAroundADayAcrossMonths() {
        composeRule.setContent {
            Themed(Appearance.Light) {
                WeekRow(
                    weekOf = LocalDate.of(2027, 4, 1),
                    stateOf = ::sampleDayState,
                    onDayClick = {},
                    today = SampleToday
                )
            }
        }
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo)).assert(
            SemanticsMatcher("one row of seven") {
                val info = it.config[SemanticsProperties.CollectionInfo]
                info.rowCount == 1 && info.columnCount == 7
            }
        )
        composeRule.onNodeWithContentDescription("29 March, predicted period").assert(item(row = 0, column = 0))
        composeRule.onNodeWithContentDescription("4 April").assert(item(row = 0, column = 6))
        composeRule.onAllNodesWithContentDescription("Monday").assertCountEquals(1)
    }

    private fun show(
        appearance: Appearance = Appearance.Light,
        month: () -> YearMonth = { SampleMonth },
        onPrevious: () -> Unit = {},
        onNext: () -> Unit = {},
        onDayClick: (LocalDate) -> Unit = {},
        rtl: Boolean = false
    ) {
        composeRule.setContent {
            Themed(appearance) {
                val direction = if (rtl) LayoutDirection.Rtl else LocalLayoutDirection.current
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    Box(Modifier.padding(horizontal = CycleTheme.spacing.medium)) {
                        MonthCalendar(
                            month = month(),
                            stateOf = ::sampleDayState,
                            onDayClick = onDayClick,
                            today = SampleToday,
                            onPreviousMonth = onPrevious,
                            onNextMonth = onNext,
                            selected = SampleToday.minusDays(1),
                            isEnabled = { !it.isAfter(SampleToday) }
                        )
                    }
                }
            }
        }
    }

    private fun assertGrid(rows: Int) {
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionInfo)).assert(
            SemanticsMatcher("a grid of $rows rows and 7 columns") {
                val info = it.config[SemanticsProperties.CollectionInfo]
                info.rowCount == rows && info.columnCount == 7
            }
        )
    }

    private fun assertWeekStartsOn(weekday: String, firstDayInColumn: String) {
        val weekdays = composeRule.onAllNodes(
            SemanticsMatcher("a weekday") { node ->
                node.config.getOrNull(SemanticsProperties.ContentDescription)?.singleOrNull() in WEEKDAYS
            }
        ).fetchSemanticsNodes()
        assertEquals(7, weekdays.size)
        val first = weekdays.minBy { it.boundsInRoot.left }
        assertEquals(weekday, first.config[SemanticsProperties.ContentDescription].single())
        composeRule.onNodeWithContentDescription(firstDayInColumn).assert(
            SemanticsMatcher("in the first column") {
                it.config.getOrNull(SemanticsProperties.CollectionItemInfo)?.columnIndex == 0
            }
        )
    }

    private fun item(row: Int, column: Int) = SemanticsMatcher("row $row, column $column") {
        val info = it.config.getOrNull(SemanticsProperties.CollectionItemInfo)
        info?.rowIndex == row && info.columnIndex == column
    }

    private fun cellWidths(): List<Dp> = composeRule.onAllNodes(days).fetchSemanticsNodes().map { node ->
        with(composeRule.density) { node.size.width.toDp() }
    }

    private fun horizontalScrollRange(): Float = composeRule
        .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
        .fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange].maxValue()

    private fun left(description: String) = composeRule.onNodeWithContentDescription(description).getBoundsInRoot().left

    private fun centre(description: String): Float {
        val bounds = composeRule.onNodeWithContentDescription(description).getBoundsInRoot()
        return ((bounds.left + bounds.right) / 2).value
    }

    private companion object {
        val WEEKDAYS = setOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    }
}
