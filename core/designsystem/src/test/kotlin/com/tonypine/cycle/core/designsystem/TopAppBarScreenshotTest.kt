package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * Three top app bars in every state of the matrix: a back button and two actions, a long title with
 * one action and no navigation, and a title alone. Pressed and focused hold the back button. The long
 * title ends in an ellipsis before its action, and at 200% the short one does too.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class TopAppBarScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun topAppBars() = matrix.capture("top_app_bar", case) {
        Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
            TopAppBar(
                title = "Calendar",
                navigation = AppBarAction(CycleIcons.Back, "Back", {}, interactionSource),
                actions = listOf(
                    AppBarAction(CycleIcons.Today, "Go to today", {}),
                    AppBarAction(CycleIcons.Settings, "Settings", {})
                )
            )
            TopAppBar(
                title = "Symptoms, moods and notes for the whole cycle",
                actions = listOf(AppBarAction(CycleIcons.Add, "Log a day", {}))
            )
            TopAppBar(title = "Today")
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(
            listOf(ComponentState.Default, ComponentState.Pressed, ComponentState.Focused)
        )
    }
}
