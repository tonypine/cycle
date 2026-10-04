package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.Themed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What TalkBack and touch get from cards, the empty state and the loading state. */
@RunWith(RobolectricTestRunner::class)
class ContainerSemanticsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aClickableCardIsOneButtonThatReadsItsContentAndClicks() {
        var clicks = 0
        show {
            ClickableCard(onClick = { clicks++ }, onClickLabel = "Open") {
                BasicText("Last cycle")
                BasicText("29 days")
            }
        }
        composeRule.onNode(hasClickAction())
            .assert(hasRole(Role.Button))
            .assert(hasText("Last cycle").and(hasText("29 days")))
            .assert(SemanticsMatcher("click label Open") { it.config[SemanticsActions.OnClick].label == "Open" })
            .assertIsEnabled()
            .assertTouchTarget()
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun aDisabledClickableCardSaysSoAndIgnoresClicks() {
        var clicks = 0
        show { ClickableCard(onClick = { clicks++ }, enabled = false) { BasicText("Last cycle") } }
        composeRule.onNodeWithText("Last cycle").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun anEmptyClickableCardStillHasA48dpTarget() {
        show { ClickableCard(onClick = {}) {} }
        composeRule.onNode(hasClickAction()).assertTouchTarget()
    }

    @Test
    fun aStaticCardIsATraversalGroupWithNoAction() {
        show { Card { BasicText("Cycle day 12") } }
        composeRule.onNodeWithText("Cycle day 12").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        composeRule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true)).assertExists()
    }

    @Test
    fun theEmptyStateIsOneHeadingGroupThenItsAction() {
        var clicks = 0
        show {
            EmptyState(
                title = "No periods logged yet",
                body = "Log the first day of your last period.",
                modifier = Modifier.size(320.dp, 480.dp),
                illustration = { EmptyStateIcon(CycleIcons.Calendar) },
                action = EmptyStateAction("Log a period", onClick = { clicks++ })
            )
        }
        val headings = composeRule.onAllNodes(isHeading()).fetchSemanticsNodes()
        assertEquals(1, headings.size)
        assertEquals(
            listOf("No periods logged yet", "Log the first day of your last period."),
            headings.single().config[SemanticsProperties.Text].map { it.text }
        )
        // The icon is decoration: nothing in the group describes it.
        assertEquals(null, headings.single().config.getOrNull(SemanticsProperties.ContentDescription))

        val stops = composeRule.onAllNodes(isHeading().or(hasClickAction())).fetchSemanticsNodes().map { node ->
            node.config[SemanticsProperties.Text].first().text
        }
        assertEquals(listOf("No periods logged yet", "Log a period"), stops)
        composeRule.onNodeWithText("Log a period").assert(hasRole(Role.Button)).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun theEmptyStateScrollsRatherThanClipsAt200Percent() {
        show(Appearance.FontScale200) {
            EmptyState(
                title = "No periods logged yet",
                body = "Log the first day of your last period and Cycle will start predicting the next one.",
                modifier = Modifier.size(288.dp, 400.dp),
                illustration = { EmptyStateIcon(CycleIcons.Calendar) },
                action = EmptyStateAction("Log a period", onClick = {})
            )
        }
        composeRule.onNode(hasScrollAction()).assertExists()
        composeRule.onNodeWithText("Log a period").assertIsNotDisplayed().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theEmptyStateLeavesScrollingToAColumnThatScrollsAlready() {
        show {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                EmptyState(title = "Nothing here", body = "Not yet.")
                Spacer(Modifier.size(24.dp))
            }
        }
        composeRule.onNodeWithText("Nothing here", substring = true).assertIsDisplayed()
        assertEquals(1, composeRule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size)
    }

    @Test
    fun theLoadingIndicatorIsAnIndeterminateProgressBarThatAnnouncesItself() {
        show { LoadingIndicator() }
        composeRule.onNodeWithContentDescription("Loading").assertIsLoading()
    }

    @Test
    fun theLoadingIndicatorReadsTheGivenDescription() {
        show { LoadingIndicator(contentDescription = "Loading your cycle") }
        composeRule.onNodeWithContentDescription("Loading your cycle").assertIsLoading()
    }

    @Test
    fun theLoadingStateReadsItsMessageOnceAsAProgressBar() {
        show { LoadingState(message = "Loading your cycle") }
        composeRule.onNodeWithContentDescription("Loading your cycle").assertIsLoading()
        composeRule.onNodeWithText("Loading your cycle").assertDoesNotExist()
    }

    @Test
    fun theLoadingStateWithNoMessageReadsLoading() {
        show { LoadingState() }
        composeRule.onNodeWithContentDescription("Loading").assertIsLoading()
    }

    private fun show(appearance: Appearance = Appearance.Light, content: @Composable () -> Unit) {
        composeRule.setContent { Themed(appearance, content) }
    }

    private fun hasRole(role: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

    private fun SemanticsNodeInteraction.assertTouchTarget() = assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)

    private fun SemanticsNodeInteraction.assertIsLoading() = assert(
        SemanticsMatcher.expectValue(SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate)
    )
        .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
}
