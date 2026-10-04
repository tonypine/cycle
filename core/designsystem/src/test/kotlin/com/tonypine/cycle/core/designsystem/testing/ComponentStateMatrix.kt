package com.tonypine.cycle.core.designsystem.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityCheckOptions
import com.github.takahirom.roborazzi.RoborazziATFAccessibilityChecker
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.checkRoboAccessibility
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckPreset
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityViewCheckResult
import com.google.android.apps.common.testing.accessibility.framework.checks.ImageContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.tonypine.cycle.core.designsystem.CycleTheme
import org.hamcrest.TypeSafeMatcher
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/** The states a component is captured in. Pass the ones it supports to [ComponentStateMatrix.cases]. */
enum class ComponentState(val slug: String) {
    Default("default"),

    /** Holding a value, such as a text field with text in it. Default is the empty state. */
    Filled("filled"),
    Pressed("pressed"),
    Focused("focused"),
    Hovered("hovered"),
    Selected("selected"),
    Disabled("disabled"),
    Error("error")
}

/** How the screen around the component is set up. */
enum class Appearance(val slug: String) {
    Light("light"),
    Dark("dark"),
    FontScale200("font200"),
    Rtl("rtl")
}

/** One cell of the matrix: one screenshot and one accessibility check. */
data class MatrixCase(val state: ComponentState, val appearance: Appearance) {
    override fun toString() = "${state.slug}_${appearance.slug}"
}

/**
 * What the component under test receives. Wire [interactionSource], [enabled], [selected] and
 * [isError] into its parameters; the harness drives the interaction states through the source.
 * [state] tells a component with a value, such as a text field, whether to show it
 * ([ComponentState.Filled]).
 */
class StateScope(val state: ComponentState, val interactionSource: MutableInteractionSource) {
    val enabled: Boolean get() = state != ComponentState.Disabled
    val selected: Boolean get() = state == ComponentState.Selected
    val isError: Boolean get() = state == ComponentState.Error
}

object ComponentStateMatrix {
    /** The states every interactive component covers. */
    val InteractiveStates = listOf(
        ComponentState.Default,
        ComponentState.Pressed,
        ComponentState.Focused,
        ComponentState.Disabled,
        ComponentState.Error
    )

    /** The states of a control with no error state: buttons, icon buttons. */
    val PressableStates = listOf(
        ComponentState.Default,
        ComponentState.Pressed,
        ComponentState.Focused,
        ComponentState.Disabled
    )

    /** [PressableStates] plus selected, for chips and toggles. */
    val SelectableStates = PressableStates + ComponentState.Selected

    /** The states a text input covers: empty, filled, focused, disabled and error. */
    val InputStates = listOf(
        ComponentState.Default,
        ComponentState.Filled,
        ComponentState.Focused,
        ComponentState.Disabled,
        ComponentState.Error
    )

    /**
     * Every state in light and dark, plus [layoutStates] (the default state, unless a selected one
     * changes the layout too) at 200% font scale and right-to-left. Return it from a
     * `@ParameterizedRobolectricTestRunner.Parameters` function.
     */
    fun cases(
        states: List<ComponentState> = InteractiveStates,
        layoutStates: List<ComponentState> = listOf(ComponentState.Default)
    ): List<Array<Any>> {
        val themed = states.flatMap { state ->
            listOf(Appearance.Light, Appearance.Dark).map { MatrixCase(state, it) }
        }
        val layout = layoutStates.flatMap { state ->
            listOf(Appearance.FontScale200, Appearance.Rtl).map { MatrixCase(state, it) }
        }
        return (themed + layout).map { arrayOf<Any>(it) }
    }
}

/**
 * Renders a component in one [MatrixCase], records its Roborazzi screenshot and checks its
 * accessibility ([checkAccessibility]): the test fails on a touch target under 48dp, a clickable or
 * image with nothing for TalkBack to read, and text or icons below the WCAG contrast minimum.
 *
 * ```
 * @RunWith(ParameterizedRobolectricTestRunner::class)
 * @GraphicsMode(GraphicsMode.Mode.NATIVE)
 * class ChipScreenshotTest(private val case: MatrixCase) {
 *     @get:Rule val matrix = ComponentStateMatrixRule()
 *
 *     @Test fun chip() = matrix.capture("chip", case) { Chip("Cramps", interactionSource, enabled = enabled) }
 *
 *     companion object {
 *         @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
 *         fun cases() = ComponentStateMatrix.cases()
 *     }
 * }
 * ```
 */
class ComponentStateMatrixRule : TestRule {
    val composeRule: ComposeContentTestRule = createComposeRule()

    override fun apply(base: Statement, description: Description): Statement = composeRule.apply(base, description)

    /**
     * Captures `src/test/screenshots/<name>_<state>_<appearance>.png` and checks accessibility.
     * The component is padded so the focus ring and press scale show in full.
     */
    fun capture(name: String, case: MatrixCase, content: @Composable StateScope.() -> Unit) {
        lateinit var scope: StateScope
        composeRule.setContent {
            val source = remember { MutableInteractionSource() }
            scope = remember { StateScope(case.state, source) }
            Themed(case.appearance) {
                Box(
                    Modifier
                        .testTag(CAPTURE_TAG)
                        .background(CycleTheme.colors.surface)
                        .padding(CycleTheme.spacing.large)
                ) {
                    scope.content()
                }
            }
        }
        case.state.interaction()?.let { interaction ->
            composeRule.waitForIdle()
            composeRule.runOnIdle { check(scope.interactionSource.tryEmit(interaction)) }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CAPTURE_TAG).captureRoboImage("src/test/screenshots/${name}_$case.png")
        checkAccessibility(disabled = case.state == ComponentState.Disabled)
    }

    /**
     * Checks whatever is on screen. First every clickable must be laid out at least 48dp in both
     * directions ([AssertionError] otherwise). ATF cannot catch this in Compose: Compose stretches a
     * small clickable's touch bounds to 48dp and reports those to accessibility, but the stretch
     * fails as soon as a neighbour overlaps it. Then the Accessibility Test Framework's latest checks
     * run on the view and its screenshot, failing on errors and warnings
     * (`AccessibilityViewCheckException`). When [disabled] is true, contrast checks are skipped:
     * WCAG 1.4.3 exempts inactive controls, which draw at the disabled alphas on purpose.
     *
     * [root] is the window to check; pass `composeRule.onNode(isDialog())` for a dialog, which opens a
     * window of its own. The touch target check covers every window.
     */
    @OptIn(ExperimentalRoborazziApi::class)
    fun checkAccessibility(disabled: Boolean = false, root: SemanticsNodeInteraction? = null) {
        checkTouchTargets()
        (root ?: composeRule.onRoot()).checkRoboAccessibility(
            roborazziATFAccessibilityCheckOptions = RoborazziATFAccessibilityCheckOptions(
                checker = RoborazziATFAccessibilityChecker(
                    preset = AccessibilityCheckPreset.LATEST,
                    suppressions = contrastResults(suppress = disabled)
                ),
                failureLevel = RoborazziATFAccessibilityChecker.CheckLevel.Warning
            )
        )
    }

    private fun checkTouchTargets() {
        val minimum = with(composeRule.density) { MIN_TOUCH_TARGET.toPx() }
        val clickables = composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick), useUnmergedTree = true)
            .fetchSemanticsNodes()
        val small = clickables.filter { it.size.width < minimum || it.size.height < minimum }
        if (small.isNotEmpty()) {
            throw AssertionError(
                "Touch targets must be at least $MIN_TOUCH_TARGET in both directions. Too small:\n" +
                    small.joinToString("\n") { node ->
                        val size = with(composeRule.density) { DpSize(node.size.width.toDp(), node.size.height.toDp()) }
                        "  - $size: ${node.config}"
                    }
            )
        }
    }

    private fun ComponentState.interaction(): Interaction? = when (this) {
        ComponentState.Pressed -> PressInteraction.Press(Offset.Zero)
        ComponentState.Focused -> FocusInteraction.Focus()
        ComponentState.Hovered -> HoverInteraction.Enter()
        else -> null
    }

    private companion object {
        const val CAPTURE_TAG = "component-state-matrix"
        val MIN_TOUCH_TARGET = 48.dp

        /** Matches contrast results when [suppress] is true, and nothing otherwise. */
        fun contrastResults(suppress: Boolean) = object : TypeSafeMatcher<AccessibilityViewCheckResult>() {
            override fun matchesSafely(result: AccessibilityViewCheckResult) = suppress &&
                (
                    result.sourceCheckClass == TextContrastCheck::class.java ||
                        result.sourceCheckClass == ImageContrastCheck::class.java
                    )

            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("a contrast check result")
            }
        }
    }
}

/** [CycleTheme] set up for [appearance]: dark palette, doubled font scale or right-to-left. */
@Composable
fun Themed(appearance: Appearance, content: @Composable () -> Unit) {
    CycleTheme(darkTheme = appearance == Appearance.Dark, reduceMotion = false) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, if (appearance == Appearance.FontScale200) 2f else 1f),
            LocalLayoutDirection provides if (appearance ==
                Appearance.Rtl
            ) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            },
            content = content
        )
    }
}
