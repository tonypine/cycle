package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrix
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.MatrixCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * Every button variant, with and without an icon, in every state of the matrix: pressed shows the
 * 14dp corners, the state layer and the press scale; focused the focus ring.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ButtonScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun buttons() = matrix.capture("buttons", case) {
        Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
            FilledButton("Log it", {}, icon = CycleIcons.Add, enabled = enabled, interactionSource = interactionSource)
            TonalButton("Edit period", {}, enabled = enabled, interactionSource = interactionSource)
            OutlinedButton("Cancel", {}, enabled = enabled, interactionSource = interactionSource)
            TextButton("Nah", {}, enabled = enabled, interactionSource = interactionSource)
            TextButton("Today", {}, icon = CycleIcons.Today, enabled = enabled, interactionSource = interactionSource)
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(ComponentStateMatrix.PressableStates)
    }
}
