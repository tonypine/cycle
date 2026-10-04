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
 * The indication, animated corner shape and disabled alphas on [InteractionSample], in every state of
 * the matrix: pressed shows the state layer, the 94% scale and the 14dp corners; focused the state
 * layer and focus ring; hovered the lighter state layer.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CycleIndicationScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun indication() = matrix.capture("indication", case) {
        InteractionSample(
            label = if (isError) "Try again" else "Log it",
            interactionSource = interactionSource,
            enabled = enabled,
            isError = isError
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(ComponentStateMatrix.InteractiveStates + ComponentState.Hovered)
    }
}
