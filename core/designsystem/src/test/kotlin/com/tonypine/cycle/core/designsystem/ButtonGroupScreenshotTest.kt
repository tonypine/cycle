package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.remember
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
 * The flow and pain button groups in every state of the matrix, on a 360dp phone. Pressed and focused
 * hold "Spotting"; selected selects "Medium" and "Moderate". At 200% the groups scroll sideways.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class ButtonGroupScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun buttonGroups() = matrix.capture("button_group", case) {
        Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
            val flowSources = FlowOptions.indices.map { index ->
                if (index == 1) interactionSource else remember { MutableInteractionSource() }
            }
            ButtonGroup(
                "Flow",
                FlowOptions,
                selectedIndex = if (selected) 3 else null,
                onSelectedChange = {},
                enabled = enabled,
                interactionSources = flowSources
            )
            ButtonGroup("Pain", PainOptions, selectedIndex = if (selected) 2 else null, onSelectedChange = {
            }, enabled = enabled)
        }
    }

    companion object {
        val FlowOptions = listOf("None", "Spotting", "Light", "Medium", "Heavy")
        val PainOptions = listOf("None", "Mild", "Moderate", "Severe")

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(
            ComponentStateMatrix.SelectableStates,
            layoutStates = listOf(ComponentState.Default, ComponentState.Selected)
        )
    }
}
