package com.tonypine.cycle.core.designsystem

import com.tonypine.cycle.core.designsystem.testing.ComponentState
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrix
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.MatrixCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Every cycle state, on its own and with today, in every interaction state of the matrix: default,
 * pressed, focused, selected and disabled in light and dark, plus 200% font scale and right-to-left.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DayCellScreenshotTest(private val variant: Variant, private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun dayCell() = matrix.capture("day_cell_$variant", case) {
        DayCell(
            date = SampleStateDates.getValue(variant.state),
            state = variant.state,
            onClick = {},
            isToday = variant.isToday,
            selected = selected,
            enabled = enabled,
            interactionSource = interactionSource
        )
    }

    class Variant(val state: CycleDayState, val isToday: Boolean) {
        override fun toString() = state.name.lowercase() + if (isToday) "_today" else ""
    }

    companion object {
        private val states = listOf(
            ComponentState.Default,
            ComponentState.Pressed,
            ComponentState.Focused,
            ComponentState.Selected,
            ComponentState.Disabled
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = CycleDayState.entries
            .flatMap { listOf(Variant(it, isToday = false), Variant(it, isToday = true)) }
            .flatMap { variant -> ComponentStateMatrix.cases(states).map { arrayOf(variant, it.single()) } }
    }
}
