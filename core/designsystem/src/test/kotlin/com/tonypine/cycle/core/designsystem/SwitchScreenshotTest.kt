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
 * The switch on its own and the switch row in every state of the matrix. Selected is checked: an
 * `accent` track with the `onAccent` thumb and its check at the end.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class SwitchScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun switch() = matrix.capture("switch", case) {
        Switch(selected, {}, "Log sleep", enabled = enabled, interactionSource = interactionSource)
    }

    @Test
    fun switchRow() = matrix.capture("switch_row", case) {
        SwitchRow(
            "Sleep",
            selected,
            {},
            body = "How long you slept and how well.",
            icon = CycleIcons.Bedtime,
            enabled = enabled,
            interactionSource = interactionSource
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(
            ComponentStateMatrix.SelectableStates,
            layoutStates = listOf(ComponentState.Default, ComponentState.Selected)
        )
    }
}
