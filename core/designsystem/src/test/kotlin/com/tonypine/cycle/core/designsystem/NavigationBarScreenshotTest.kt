package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
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
 * The navigation bar with four destinations in every state of the matrix. Each state is about
 * "Calendar", the second: pressed and focused hold it while "Today" is selected, and selected moves
 * the selection to it.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class NavigationBarScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun navigationBar() = matrix.capture("navigation_bar", case) {
        val others = remember { List(Destinations.size) { MutableInteractionSource() } }
        NavigationBar(
            destinations = Destinations,
            selectedIndex = if (selected) 1 else 0,
            onSelect = {},
            interactionSources = others.mapIndexed { index, source -> if (index == 1) interactionSource else source }
        )
    }

    companion object {
        val Destinations = listOf(
            NavigationDestination("Today", CycleIcons.Today),
            NavigationDestination("Calendar", CycleIcons.Calendar),
            NavigationDestination("Log", CycleIcons.Add),
            NavigationDestination("Settings", CycleIcons.Settings)
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(
            listOf(ComponentState.Default, ComponentState.Pressed, ComponentState.Focused, ComponentState.Selected),
            layoutStates = listOf(ComponentState.Default, ComponentState.Selected)
        )
    }
}
