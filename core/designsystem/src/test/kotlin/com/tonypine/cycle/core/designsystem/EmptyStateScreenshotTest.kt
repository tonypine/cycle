package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
 * The empty state with an icon and an action, and with a second, text action under it, in a fixed box, in light, dark, 200% font scale and
 * right-to-left. At 200% the content is taller than the box: it scrolls, so the screenshot shows its
 * top and the rest stays reachable (`ContainerSemanticsTest` scrolls to the action).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EmptyStateScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun emptyState() = matrix.capture("empty_state", case) {
        EmptyState(
            title = "No periods logged yet",
            body = "Log the first day of your last period and Cycle will start predicting the next one.",
            modifier = Modifier.size(width = 288.dp, height = 400.dp),
            illustration = { EmptyStateIcon(CycleIcons.Calendar) },
            action = EmptyStateAction("Log a period", onClick = {}, icon = CycleIcons.Add)
        )
    }

    @Test
    fun emptyStateWithTwoActions() = matrix.capture("empty_state_two_actions", case) {
        EmptyState(
            title = "Hi! Let's get your cycle going",
            body = "Log your period and Cycle estimates the next one.",
            modifier = Modifier.size(width = 288.dp, height = 400.dp),
            illustration = { EmptyStateIcon(CycleIcons.WaterDrop) },
            action = EmptyStateAction("Get started", onClick = {}),
            secondaryActions = listOf(EmptyStateAction("Skip for now", onClick = {}))
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(listOf(ComponentState.Default))
    }
}
