package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.text.intl.LocaleList
import com.tonypine.cycle.core.designsystem.testing.ComponentState
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrix
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.MatrixCase
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The radio row in every state of the matrix. Selected is an `accentContainer` row with an `accent`
 * dot in the radio. A language option, its title marked as German, wraps its sentence-long body to
 * every line it needs, at 200% font size too, with the radio centred beside it.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class RadioRowScreenshotTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun radioRow() = matrix.capture("radio_row", case) {
        RadioRow(
            "Implant",
            selected,
            {},
            body = "A rod in the arm, such as Nexplanon",
            enabled = enabled,
            interactionSource = interactionSource
        )
    }

    @Test
    fun languageRow() {
        assumeTrue(case.state == ComponentState.Default || case.state == ComponentState.Selected)
        matrix.capture("radio_row_language", case) {
            RadioRow(
                "Sprache des Telefons",
                selected,
                {},
                body = "Französisch gibt es in Cycle noch nicht, deshalb nutzt diese Option Englisch.",
                titleLocale = LocaleList("de"),
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
