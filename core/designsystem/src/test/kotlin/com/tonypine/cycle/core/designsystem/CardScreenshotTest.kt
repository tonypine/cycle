package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrix
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.MatrixCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/**
 * A static card above a clickable card in every state of the matrix: pressed shows the state layer
 * and the press scale in the 32dp shape, focused the focus ring, disabled the faded card. The static
 * card has no states, so it shows the same in each.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CardScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun cards() = matrix.capture("cards", case) {
        Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
            Card(Modifier.width(CardWidth)) {
                CardText("Cycle day 12", "About 16 days until your next period.")
            }
            ClickableCard(
                onClick = {},
                modifier = Modifier.width(CardWidth),
                enabled = enabled,
                interactionSource = interactionSource
            ) {
                CardText("Last cycle", "29 days, from 3 to 31 January.")
            }
        }
    }

    @Composable
    private fun CardText(title: String, body: String) {
        BasicText(title, style = CycleTheme.typography.titleSmall.copy(color = CycleTheme.colors.onSurface))
        BasicText(body, style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant))
    }

    companion object {
        private val CardWidth = 256.dp

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(ComponentStateMatrix.PressableStates)
    }
}
