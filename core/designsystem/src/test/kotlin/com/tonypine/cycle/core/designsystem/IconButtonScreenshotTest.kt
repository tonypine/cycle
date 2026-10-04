package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
 * The standard, filled, tonal and toggle icon buttons in every state of the matrix. Selected checks
 * the toggle; the others stay as they are. The chevron mirrors in right-to-left.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IconButtonScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun iconButtons() = matrix.capture("icon_buttons", case) {
        Row(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
            IconButton(CycleIcons.ChevronEnd, "Next month", {
            }, enabled = enabled, interactionSource = interactionSource)
            FilledIconButton(CycleIcons.Add, "Log a day", {}, enabled = enabled, interactionSource = interactionSource)
            TonalIconButton(CycleIcons.Today, "Go to today", {
            }, enabled = enabled, interactionSource = interactionSource)
            IconToggleButton(
                CycleIcons.Calendar,
                "Show the calendar",
                checked = selected,
                onCheckedChange = {},
                enabled = enabled,
                interactionSource = interactionSource
            )
        }
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
