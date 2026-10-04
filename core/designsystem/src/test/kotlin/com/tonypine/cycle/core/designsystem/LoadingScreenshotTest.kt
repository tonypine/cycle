package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * The inline loading indicator above a loading state with a message, in light, dark, 200% font scale
 * and right-to-left. The test clock holds infinite animations, so both show their first frame, the
 * sun.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoadingScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun loading() = matrix.capture("loading", case) {
        Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
            LoadingIndicator()
            LoadingState(Modifier.size(width = 288.dp, height = 240.dp), message = "Loading your cycle")
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(listOf(ComponentState.Default))
    }
}
