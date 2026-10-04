package com.tonypine.cycle.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TextContrastCheck
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityViewCheckException
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.Themed
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Proves the harness's accessibility checks catch what they should: each deliberately broken sample
 * must fail with the matching check, and a correct one must pass.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class AccessibilityHarnessTest {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @Test
    fun touchTargetUnder48dpFails() {
        show { Box(Modifier.size(24.dp).semantics { contentDescription = "Add" }.clickable {}) }
        val failure = assertThrows(AssertionError::class.java) { matrix.checkAccessibility() }
        assertTrue(failure.message!!, "Too small" in failure.message!! && "24.0.dp x 24.0.dp" in failure.message!!)
    }

    @Test
    fun touchTargetOf48dpByOnlyOneSideFails() {
        show {
            Box(Modifier.size(width = 120.dp, height = 32.dp).semantics { contentDescription = "Add" }.clickable {})
        }
        assertThrows(AssertionError::class.java) { matrix.checkAccessibility() }
    }

    @Test
    fun clickableWithNothingToReadFails() {
        show { Box(Modifier.size(48.dp).clickable {}) }
        assertFailsWith(SpeakableTextPresentCheck::class.java)
    }

    @Test
    fun lowContrastTextFails() {
        show {
            BasicText(
                "Next period in 3 days",
                modifier = Modifier.background(Color(0xFFFFFCF4)).padding(8.dp),
                style = CycleTheme.typography.body.copy(color = Color(0xFFE1D0AA))
            )
        }
        assertFailsWith(TextContrastCheck::class.java)
    }

    @Test
    fun a48dpTargetWithALabelPasses() {
        show { CycleIcon(CycleIcons.Add, contentDescription = "Add", modifier = Modifier.clickable {}.padding(12.dp)) }
        matrix.checkAccessibility()
    }

    private fun show(content: @Composable () -> Unit) {
        matrix.composeRule.setContent {
            Themed(Appearance.Light) {
                Box(Modifier.background(CycleTheme.colors.surface).padding(CycleTheme.spacing.large)) { content() }
            }
        }
    }

    private fun assertFailsWith(check: Class<*>) {
        val failure = assertThrows(AccessibilityViewCheckException::class.java) { matrix.checkAccessibility() }
        val failed = failure.results
            .filter { it.type == AccessibilityCheckResultType.ERROR || it.type == AccessibilityCheckResultType.WARNING }
            .map { it.sourceCheckClass }
        assertTrue("Expected ${check.simpleName} among $failed", check in failed)
    }
}
