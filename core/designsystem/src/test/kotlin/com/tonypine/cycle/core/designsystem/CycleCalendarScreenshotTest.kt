package com.tonypine.cycle.core.designsystem

import com.tonypine.cycle.core.designsystem.testing.ComponentState
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrix
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.MatrixCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * A sample week of day cells and the cycle phase legend, in full and without fertility, in light, dark, 200% font scale and
 * right-to-left, with the accessibility checks. The screen is 480dp wide and xhdpi: wide enough for a
 * week of cells inside the 16dp margins even at 200%, where each cell grows to 58dp, and dense enough that the legend's 14sp labels render
 * as on a real device (at mdpi their one-pixel strokes blur, and the contrast check underestimates them).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w480dp-h800dp-xhdpi")
class CycleCalendarScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun week() = matrix.capture("day_cell_week", case) { SampleWeekRow() }

    @Test
    fun legend() = matrix.capture("legend", case) { CycleLegend() }

    @Test
    fun legendWithoutFertility() = matrix.capture("legend_without_fertility", case) {
        CycleLegend(entries = CycleLegendEntry.WithoutFertility)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(listOf(ComponentState.Default))
    }
}
