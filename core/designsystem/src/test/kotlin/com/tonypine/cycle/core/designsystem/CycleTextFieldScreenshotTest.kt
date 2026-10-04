package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Modifier
import com.tonypine.cycle.core.designsystem.testing.ComponentState
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrix
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.MatrixCase
import com.tonypine.cycle.core.designsystem.testing.StateScope
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [CycleTextField] in every input state: empty (default), filled, focused, disabled and error, in
 * light and dark, plus 200% font scale and right-to-left. One single-line field with icons, and one
 * multi-line field with a counter. All text is synthetic.
 *
 * Rendered at xhdpi: at mdpi a 14sp counter such as "49/200" is 14px tall, and anti-aliasing leaves
 * the Accessibility Test Framework too few solid pixels to estimate its colour.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "xhdpi")
class CycleTextFieldScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun singleLine() = matrix.capture("textfield", case) {
        val state = rememberTextFieldState(if (hasValue) "Sat 27 March" else "")
        CycleTextField(
            state = state,
            label = "Period start",
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Pick a day",
            supportingText = "The first day of bleeding.",
            errorMessage = if (isError) "Pick a day up to today." else null,
            leadingIcon = CycleIcons.Calendar,
            trailingAction = if (hasValue) TextFieldAction(CycleIcons.Close, "Clear", onClick = {}) else null,
            enabled = enabled,
            interactionSource = interactionSource
        )
    }

    @Test
    fun multiLine() = matrix.capture("textfield_multiline", case) {
        val state = rememberTextFieldState(if (hasValue) "A synthetic sample note for the screenshot tests." else "")
        CycleTextField(
            state = state,
            label = "Notes",
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Anything else? Spill it here.",
            supportingText = "Only on this phone.",
            errorMessage = if (isError) "That note did not save. Try again." else null,
            maxLength = 200,
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3),
            enabled = enabled,
            interactionSource = interactionSource
        )
    }

    private val StateScope.hasValue: Boolean
        get() = state == ComponentState.Filled || state == ComponentState.Disabled || state == ComponentState.Error

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(ComponentStateMatrix.InputStates)
    }
}
