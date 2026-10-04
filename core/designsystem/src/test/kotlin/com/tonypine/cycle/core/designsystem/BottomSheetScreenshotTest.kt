package com.tonypine.cycle.core.designsystem

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.Themed
import com.tonypine.cycle.core.designsystem.testing.showKeyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [CycleBottomSheet] over a screen, in each of its states in light and dark, plus the partially
 * expanded and expanded sheets at 200% font scale and right-to-left. Each case records
 * `src/test/screenshots/bottom_sheet_<state>_<appearance>.png` of the whole screen, the sheet's window
 * on top of the app's, and runs the accessibility checks on the sheet.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class BottomSheetScreenshotTest(private val case: SheetCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()
    private val composeRule get() = matrix.composeRule

    private lateinit var state: CycleBottomSheetState
    private lateinit var scope: CoroutineScope
    private lateinit var inputModeManager: InputModeManager
    private lateinit var sheetView: View

    @Test
    fun sheet() {
        composeRule.setContent {
            Themed(case.appearance) {
                state = rememberCycleBottomSheetState(
                    initialValue = when (case.state) {
                        SheetState.Expanded -> CycleSheetValue.Expanded
                        SheetState.FocusedHandle -> CycleSheetValue.Hidden
                        else -> CycleSheetValue.PartiallyExpanded
                    }
                )
                scope = rememberCoroutineScope()
                inputModeManager = LocalInputModeManager.current
                Screen(showKeyboard = case.state == SheetState.Ime)
                CycleBottomSheet(state, title = "Log today") {
                    sheetView = LocalView.current
                    SheetContent()
                }
            }
        }
        when (case.state) {
            SheetState.Ime -> {
                composeRule.onNode(hasSetTextAction()).requestFocus()
                composeRule.runOnIdle { sheetView.showKeyboard(KEYBOARD_HEIGHT) }
            }

            SheetState.FocusedHandle -> {
                // Opened from the keyboard, so focus lands on the handle and its ring shows.
                composeRule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
                composeRule.runOnIdle { scope.launch { state.show() } }
                composeRule.waitForIdle()
                composeRule.onNodeWithContentDescription("Drag handle").assertIsFocused()
            }

            else -> Unit
        }
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/bottom_sheet_$case.png")
        matrix.checkAccessibility(root = composeRule.onNode(isDialog()), suppress = CutByTheScreenEdge)
    }

    /**
     * A partially expanded sheet runs off the bottom of the screen, so a control can show only a
     * sliver there, which the framework's touch target check reports. Its whole target is still
     * 48dp, which the harness checks on the layout, and dragging the sheet up reveals it.
     */
    private object CutByTheScreenEdge : TypeSafeMatcher<AccessibilityViewCheckResult>() {
        override fun matchesSafely(result: AccessibilityViewCheckResult): Boolean {
            val element = result.element ?: return false
            return result.sourceCheckClass == TouchTargetSizeCheck::class.java &&
                element.boundsInScreen.bottom == element.window.boundsInScreen.bottom
        }

        override fun describeTo(description: Description) {
            description.appendText("a touch target cut by the bottom of the screen")
        }
    }

    /** The screen behind the sheet, and a stand-in for the keyboard, which Robolectric does not draw. */
    @Composable
    private fun Screen(showKeyboard: Boolean) {
        Box(Modifier.fillMaxSize().background(CycleTheme.colors.surface)) {
            Column(
                Modifier.padding(CycleTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
            ) {
                BasicText("Today", style = CycleTheme.typography.headline.copy(color = CycleTheme.colors.onSurface))
                BasicText(
                    "Cycle day 12. Nothing logged yet.",
                    style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurfaceVariant)
                )
            }
            if (showKeyboard) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(KEYBOARD_HEIGHT.dp)
                        .background(CycleTheme.colors.surfaceContainerHigh)
                        .semantics { invisibleToUser() },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        "Keyboard",
                        style = CycleTheme.typography.label.copy(color = CycleTheme.colors.onSurfaceVariant)
                    )
                }
            }
        }
    }

    @Composable
    private fun SheetContent() {
        BasicText(
            "How are you feeling? Add a note if you like.",
            style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurfaceVariant)
        )
        CycleTextField(
            rememberTextFieldState(),
            label = "Notes",
            modifier = Modifier.fillMaxWidth(),
            placeholder = "Anything else? Spill it here.",
            supportingText = "Only on this phone."
        )
        FilledButton("Log it", onClick = {}, modifier = Modifier.fillMaxWidth())
        TextButton("Nah", onClick = {}, modifier = Modifier.fillMaxWidth())
    }

    enum class SheetState(val slug: String) {
        PartiallyExpanded("partial"),
        Expanded("expanded"),
        Ime("ime"),
        FocusedHandle("focused_handle")
    }

    data class SheetCase(val state: SheetState, val appearance: Appearance) {
        override fun toString() = "${state.slug}_${appearance.slug}"
    }

    companion object {
        private const val KEYBOARD_HEIGHT = 280f

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val themed = SheetState.entries.flatMap { state ->
                listOf(Appearance.Light, Appearance.Dark).map { SheetCase(state, it) }
            }
            val layout = listOf(SheetState.PartiallyExpanded, SheetState.Expanded).flatMap { state ->
                listOf(Appearance.FontScale200, Appearance.Rtl).map { SheetCase(state, it) }
            }
            return (themed + layout).map { arrayOf<Any>(it) }
        }
    }
}
