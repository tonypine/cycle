package com.tonypine.cycle.core.designsystem

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
 * The slider on its own and the slider field in every state of the matrix. The slider sits at 28 of
 * 15 to 90, the field at 5 of 1 to 14.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class SliderScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun slider() = matrix.capture("slider", case) {
        Slider(
            value = 28,
            onValueChange = {},
            valueRange = 15..90,
            contentDescription = "Cycle length",
            stateDescription = "28 days",
            enabled = enabled,
            interactionSource = interactionSource
        )
    }

    @Test
    fun sliderField() = matrix.capture("slider_field", case) {
        SliderField(
            label = "Period length",
            value = 5,
            onValueChange = {},
            valueRange = 1..14,
            valueText = "5 days",
            decreaseDescription = "Period one day shorter",
            increaseDescription = "Period one day longer",
            supportingText = "The days you bleed. Often between 2 and 7 days.",
            enabled = enabled
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(ComponentStateMatrix.PressableStates)
    }
}
