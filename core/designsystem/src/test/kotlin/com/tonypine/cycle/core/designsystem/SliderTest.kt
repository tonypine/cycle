package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.Themed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What touch, keys and TalkBack do with the slider and the slider field. Lengths from 15 to 90. */
@RunWith(RobolectricTestRunner::class)
class SliderTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var value by mutableIntStateOf(28)
    private val changes = mutableListOf<Int>()

    private fun showSlider(appearance: Appearance = Appearance.Light, enabled: Boolean = true) =
        composeRule.setContent {
            Themed(appearance) {
                Slider(
                    value = value,
                    onValueChange = {
                        changes += it
                        value = it
                    },
                    valueRange = 15..90,
                    contentDescription = "Cycle length",
                    stateDescription = "$value days",
                    modifier = Modifier.width(320.dp).testTag("slider"),
                    enabled = enabled
                )
            }
        }

    private fun showField() = composeRule.setContent {
        Themed(Appearance.Light) {
            SliderField(
                label = "Cycle length",
                value = value,
                onValueChange = {
                    changes += it
                    value = it
                },
                valueRange = 15..90,
                valueText = "$value days",
                decreaseDescription = "Cycle one day shorter",
                increaseDescription = "Cycle one day longer",
                supportingText = "Often between 21 and 35 days."
            )
        }
    }

    private val slider get() = composeRule.onNodeWithTag("slider")

    @Test
    fun talkBackReadsTheLabelAndValueAndKnowsTheRange() {
        showSlider()
        slider
            .assertContentDescriptionEquals("Cycle length")
            .assert(hasStateDescription("28 days"))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(28f, 15f..90f, steps = 74)
                )
            )
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun talkBackSetsTheValueRoundedAndKeptInRange() {
        showSlider()
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(29.6f) }
        slider.assert(hasStateDescription("30 days"))
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(200f) }
        assertEquals(listOf(30, 90), changes)
    }

    @Test
    fun draggingToEitherEndGivesTheEndsOfTheRange() {
        showSlider()
        slider.performTouchInput { swipeRight(startX = centerX, endX = right + 100f) }
        assertEquals(90, value)
        slider.performTouchInput { swipeLeft(startX = centerX, endX = left - 100f) }
        assertEquals(15, value)
    }

    @Test
    fun draggingRunsTheOtherWayRightToLeft() {
        showSlider(Appearance.Rtl)
        slider.performTouchInput { swipeLeft(startX = centerX, endX = left) }
        assertEquals(90, value)
    }

    @Test
    fun aVerticalSwipeStartingOnTheTrackScrollsInsteadOfMovingIt() {
        composeRule.setContent {
            Themed(Appearance.Light) {
                Column(Modifier.height(200.dp).verticalScroll(rememberScrollState())) {
                    Slider(
                        value = value,
                        onValueChange = { changes += it },
                        valueRange = 15..90,
                        contentDescription = "Cycle length",
                        stateDescription = "$value days",
                        modifier = Modifier.width(320.dp).testTag("slider")
                    )
                    Spacer(Modifier.height(400.dp))
                }
            }
        }
        slider.performTouchInput { swipeUp(startY = centerY, endY = top - 100f) }
        assertEquals(emptyList<Int>(), changes)
    }

    @Test
    fun tappingMovesToWhereSheTapped() {
        showSlider()
        slider.performTouchInput { click(centerLeft) }
        assertEquals(15, value)
        slider.performTouchInput { click(centerRight) }
        assertEquals(90, value)
    }

    @Test
    fun aDragCutOffMidwayNoLongerHoldsTheHandle() {
        var enabled by mutableStateOf(true)
        var pressed = false
        composeRule.setContent {
            Themed(Appearance.Light) {
                val interactionSource = remember { MutableInteractionSource() }
                pressed = interactionSource.collectIsPressedAsState().value
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 15..90,
                    contentDescription = "Cycle length",
                    stateDescription = "$value days",
                    modifier = Modifier.width(320.dp).testTag("slider"),
                    enabled = enabled,
                    interactionSource = interactionSource
                )
            }
        }
        slider.performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
        }
        composeRule.waitForIdle()
        assertTrue(pressed)

        // Turning it off and on mid-drag restarts its touch handling, which cuts the drag off.
        enabled = false
        composeRule.waitForIdle()
        enabled = true
        composeRule.waitForIdle()
        assertFalse(pressed)
        slider.performTouchInput { up() }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun arrowKeysMoveOneStepAndHomeAndEndGoToTheEnds() {
        showSlider()
        slider.requestFocus()
        slider.performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(29, value)
        slider.performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(28, value)
        slider.performKeyInput { pressKey(Key.MoveEnd) }
        assertEquals(90, value)
        slider.performKeyInput { pressKey(Key.MoveHome) }
        assertEquals(15, value)
    }

    @Test
    fun aDisabledSliderIgnoresTouchAndTalkBack() {
        showSlider(enabled = false)
        slider.assertIsNotEnabled()
        slider.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.SetProgress))
        slider.performTouchInput { click(centerRight) }
        assertEquals(emptyList<Int>(), changes)
    }

    @Test
    fun minusAndPlusMoveOneStepAndStopAtTheEnds() {
        value = 16
        showField()
        composeRule.onNodeWithText("16 days").assertExists()
        composeRule.onNodeWithContentDescription("Cycle one day shorter").assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription("Cycle one day shorter").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Cycle one day longer").performClick().performClick()
        assertEquals(listOf(15, 16, 17), changes)

        value = 90
        composeRule.onNodeWithContentDescription("Cycle one day longer").assertIsNotEnabled()
    }

    @Test
    fun theFieldReadsItsLabelAndValueTogetherAndTheSliderReadsTheSame() {
        showField()
        composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion) and
                SemanticsMatcher("reads the label and value") {
                    it.config.getOrElseNullable(SemanticsProperties.Text) { null }
                        ?.map(Any::toString) == listOf("Cycle length", "28 days")
                }
        ).assertExists()
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
            .assertContentDescriptionEquals("Cycle length")
            .assert(hasStateDescription("28 days"))
    }
}
