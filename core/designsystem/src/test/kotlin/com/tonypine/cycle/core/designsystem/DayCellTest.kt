package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.Themed
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB")
class DayCellTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val today = LocalDate.of(2027, 3, 20)

    @Test
    fun everyStateReadsItsDateAndState() {
        val expected = mapOf(
            CycleDayState.Plain to "20 March",
            CycleDayState.Period to "20 March, period",
            CycleDayState.PredictedPeriod to "20 March, predicted period",
            CycleDayState.Fertile to "20 March, estimated fertile window",
            CycleDayState.Ovulation to "20 March, estimated ovulation"
        )
        composeRule.setContent {
            CycleTheme(darkTheme = false) {
                Column {
                    CycleDayState.entries.forEach { state ->
                        DayCell(today, state, onClick = {}, modifier = Modifier.testTag(state.name))
                        DayCell(today, state, onClick = {
                        }, modifier = Modifier.testTag("${state.name}-today"), isToday = true)
                    }
                }
            }
        }
        CycleDayState.entries.forEach { state ->
            val description = expected.getValue(state)
            composeRule.onNodeWithTag(state.name).assertContentDescriptionEquals(description)
            val todayDescription = description.replace("20 March", "20 March, today")
            composeRule.onNodeWithTag("${state.name}-today").assertContentDescriptionEquals(todayDescription)
        }
        composeRule.onNodeWithTag("Period-today").assertContentDescriptionEquals("20 March, today, period")
    }

    @Test
    fun aPeriodDayReadsInTheMethodsWords() {
        composeRule.setContent {
            CycleTheme(darkTheme = false) {
                Column {
                    BleedingWords.entries.forEach { words ->
                        DayCell(today, CycleDayState.Period, onClick = {}, Modifier.testTag("$words"), words = words)
                        DayCell(
                            today,
                            CycleDayState.PredictedPeriod,
                            onClick = null,
                            Modifier.testTag("$words-expected"),
                            words = words
                        )
                    }
                }
            }
        }
        composeRule.onNodeWithTag("Period").assertContentDescriptionEquals("20 March, period")
        composeRule.onNodeWithTag("Bleed").assertContentDescriptionEquals("20 March, bleed")
        composeRule.onNodeWithTag("Bleeding").assertContentDescriptionEquals("20 March, bleeding")
        composeRule.onNodeWithTag("Period-expected").assertContentDescriptionEquals("20 March, predicted period")
        composeRule.onNodeWithTag("Bleed-expected").assertContentDescriptionEquals("20 March, expected bleed")
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun theDateFollowsTheLocale() {
        composeRule.setContent {
            CycleTheme(darkTheme = false) {
                DayCell(LocalDate.of(2027, 3, 14), CycleDayState.PredictedPeriod, onClick = {}, Modifier.testTag(CELL))
            }
        }
        composeRule.onNodeWithTag(CELL).assertContentDescriptionEquals("March 14, predicted period")
    }

    @Test
    fun isAButtonOfAtLeast48dpThatExposesSelection() {
        var clicks = 0
        var selected by mutableStateOf(false)
        composeRule.setContent {
            CycleTheme(darkTheme = false) {
                DayCell(today, CycleDayState.Fertile, onClick = {
                    clicks++
                }, Modifier.testTag(CELL), selected = selected)
            }
        }
        composeRule.onNodeWithTag(CELL)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .assertIsEnabled()
            .assertHasClickAction()
            .assertIsNotSelected()
            .performClick()
        assertEquals(1, clicks)

        selected = true
        composeRule.onNodeWithTag(CELL).assertIsSelected()
    }

    @Test
    fun growsSquareWithLargeText() {
        composeRule.setContent {
            Themed(Appearance.FontScale200) {
                DayCell(today, CycleDayState.PredictedPeriod, onClick = {}, Modifier.testTag(CELL))
            }
        }
        val bounds = composeRule.onNodeWithTag(CELL).getBoundsInRoot()
        assertTrue("Expected the cell to grow past 48dp at 200%, was $bounds", bounds.width > 48.dp)
        assertEquals(bounds.width, bounds.height)
    }

    @Test
    fun aDisabledDayIgnoresTaps() {
        var clicks = 0
        composeRule.setContent {
            CycleTheme(darkTheme = false) {
                DayCell(today.plusDays(1), CycleDayState.Plain, onClick = {
                    clicks++
                }, Modifier.testTag(CELL), enabled = false)
            }
        }
        composeRule.onNodeWithTag(CELL).assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    @Config(qualifiers = "+xxxhdpi")
    fun aDayWithoutOnClickKeepsItsFullColourAndIgnoresTaps() {
        composeRule.setContent {
            CycleTheme(darkTheme = false) {
                Box(Modifier.background(CycleTheme.colors.surface)) {
                    DayCell(
                        today.plusDays(10),
                        CycleDayState.Period,
                        onClick = null,
                        Modifier.testTag(CELL),
                        selected = true
                    )
                }
            }
        }
        composeRule.onNodeWithTag(CELL)
            .assertContentDescriptionEquals("30 March, period")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Disabled))
            .assertIsSelected()
        assertColor(LightCycleColors.period, fillPixel())
    }

    @Test
    @Config(qualifiers = "+xxxhdpi")
    fun loggingADayMorphsToTheSquircleOnTheSpatialSpring() {
        var state by mutableStateOf(CycleDayState.Plain)
        show(reduceMotion = false) { state }
        assertColor(LightCycleColors.surface, cornerPixel())

        composeRule.mainClock.autoAdvance = false
        state = CycleDayState.Period
        Snapshot.sendApplyNotifications()
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        // The fill is there at once, but still close to the plain circle: its corner is not filled yet.
        assertColor(LightCycleColors.period, fillPixel())
        assertColor(LightCycleColors.surface, cornerPixel())

        composeRule.mainClock.advanceTimeBy(MORPH_MIDWAY_MS)
        composeRule.onNodeWithTag(CELL).captureRoboImage("src/test/screenshots/day_cell_log_morph_midway.png")

        composeRule.mainClock.advanceTimeBy(1_000)
        assertColor(LightCycleColors.period, cornerPixel())
    }

    @Test
    @Config(qualifiers = "+xxxhdpi")
    fun loggingADayJumpsToTheSquircleUnderReduceMotion() {
        var state by mutableStateOf(CycleDayState.Plain)
        show(reduceMotion = true) { state }

        composeRule.mainClock.autoAdvance = false
        state = CycleDayState.Period
        Snapshot.sendApplyNotifications()
        repeat(3) { composeRule.mainClock.advanceTimeByFrame() }
        assertColor(LightCycleColors.period, cornerPixel())
    }

    private fun show(reduceMotion: Boolean, state: () -> CycleDayState) {
        composeRule.setContent {
            CycleTheme(darkTheme = false, reduceMotion = reduceMotion) {
                Box(Modifier.background(CycleTheme.colors.surface)) {
                    DayCell(today.minusDays(2), state(), onClick = {}, Modifier.testTag(CELL))
                }
            }
        }
    }

    /** A point on the diagonal 14dp from the centre: inside both the circle and the squircle, clear of the number. */
    private fun fillPixel(): Color = pixelAt(distanceFromCentre = 14f)

    /**
     * A point on the diagonal 18.8dp from the centre (of a 48dp cell): outside the 36dp plain circle
     * (radius 18) but inside the period squircle's 14dp corner, which reaches 19.66dp.
     */
    private fun cornerPixel(): Color = pixelAt(distanceFromCentre = 18.8f)

    private fun pixelAt(distanceFromCentre: Float): Color {
        val image = composeRule.onNodeWithTag(CELL).captureToImage()
        val unit = minOf(image.width, image.height) / 48f
        val offset = distanceFromCentre * unit / sqrt(2f)
        return image.toPixelMap()[(image.width / 2f - offset).toInt(), (image.height / 2f - offset).toInt()]
    }

    private fun assertColor(expected: Color, actual: Color) {
        val close = abs(expected.red - actual.red) < 0.02f &&
            abs(expected.green - actual.green) < 0.02f &&
            abs(expected.blue - actual.blue) < 0.02f
        assertTrue("Expected $expected, was $actual", close)
    }

    private companion object {
        const val CELL = "cell"
        const val MORPH_MIDWAY_MS = 40L
    }
}
