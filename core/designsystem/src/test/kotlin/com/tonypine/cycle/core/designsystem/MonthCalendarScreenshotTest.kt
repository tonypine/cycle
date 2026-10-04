package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.ComponentState
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.MatrixCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * The sample month (a logged period, a predicted period, today and a selected day) and its week, on
 * a 360dp and a 412dp phone inside the calendar's 12dp margins, in light, dark, 200% font scale (where
 * the grid scrolls sideways) and right-to-left (where the chevrons mirror), with the accessibility
 * checks. The locale is English (UK), so the week starts on Monday. The screens are xhdpi so the
 * small weekday initials render as on a real device for the contrast check.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MonthCalendarScreenshotTest(private val width: Int, private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Before
    fun setScreen() {
        RuntimeEnvironment.setQualifiers("en-rGB-w${width}dp-h900dp-xhdpi")
    }

    @Test
    fun month() = matrix.capture("month_calendar_${width}dp", case, margin = 12.dp) { SampleMonthCalendar() }

    @Test
    fun week() = matrix.capture("week_row_${width}dp", case, margin = 12.dp) { SampleWeekCalendar() }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}dp_{1}")
        fun cases(): List<Array<Any>> = listOf(360, 412).flatMap { width ->
            Appearance.entries.map { arrayOf<Any>(width, MatrixCase(ComponentState.Default, it)) }
        }
    }
}
