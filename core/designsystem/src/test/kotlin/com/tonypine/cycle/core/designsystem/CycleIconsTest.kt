package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.ComponentState
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrix
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.MatrixCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Every icon in light, dark, 200% font scale and right-to-left, where the directional ones mirror. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CycleIconsTest(private val case: MatrixCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun icons() {
        matrix.capture("icons", case) {
            FlowRow(Modifier.width(288.dp), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                CycleIcons.entries.forEach { icon ->
                    // Each in a 48dp touch target, as an icon button would be.
                    CycleIcon(
                        icon,
                        contentDescription = icon.name,
                        modifier = Modifier.clickable(role = Role.Button) {}.padding(12.dp)
                    )
                }
            }
        }
        CycleIcons.entries.forEach {
            matrix.composeRule.onNodeWithContentDescription(it.name).assertContentDescriptionEquals(it.name)
        }
    }

    @Test
    fun theListedSymbolsAreAllThere() {
        val symbols = CycleIcons.entries.map { it.symbol }.toSet()
        val required = setOf(
            "arrow_back", "close", "add", "remove", "check", "error", "calendar_month", "today", "settings",
            "chevron_left", "chevron_right", "expand_less", "expand_more",
            "water_drop", "edit_note", "mood", "history", "healing", "bolt", "bedtime", "favorite", "edit",
            "tune", "smartphone", "download", "upload", "delete", "info", "lock", "medication"
        )
        assertEquals(required, symbols)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases() = ComponentStateMatrix.cases(listOf(ComponentState.Default))
    }
}
