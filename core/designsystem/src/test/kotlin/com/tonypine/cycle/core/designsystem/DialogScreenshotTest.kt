package com.tonypine.cycle.core.designsystem

import android.graphics.Insets
import android.view.WindowInsets
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.requestFocus
import androidx.test.platform.app.InstrumentationRegistry
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.tonypine.cycle.core.designsystem.testing.Appearance
import com.tonypine.cycle.core.designsystem.testing.ComponentStateMatrixRule
import com.tonypine.cycle.core.designsystem.testing.Themed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/**
 * Every dialog over a sample screen, captured as the whole screen: the alert, the alert with its
 * confirm action focused, the destructive dialog and a text field dialog with the keyboard open, in
 * light and dark, and the alert, destructive and keyboard ones at 200% font scale (actions stacked)
 * and right-to-left. Each case also runs the accessibility checks on the dialog's window.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-mdpi")
class DialogScreenshotTest(private val case: DialogCase) {
    @get:Rule
    val matrix = ComponentStateMatrixRule()

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun dialog() {
        val composeRule = matrix.composeRule
        // Touch mode is global: a keyboard user's dialog opens in keyboard mode, where buttons take focus.
        if (case.variant == DialogVariant.Focused) InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        composeRule.setContent {
            Themed(case.appearance) {
                SampleScreen()
                when (case.variant) {
                    DialogVariant.Alert, DialogVariant.Focused -> AlertSample()
                    DialogVariant.Destructive -> DestructiveSample()
                    DialogVariant.Ime -> NoteSample()
                }
            }
        }
        when (case.variant) {
            DialogVariant.Focused -> composeRule.onNodeWithText("Remind me").requestFocus()
            DialogVariant.Ime -> showKeyboard()
            else -> Unit
        }
        composeRule.waitForIdle()
        captureScreenRoboImage("src/test/screenshots/dialog_$case.png")
        matrix.checkAccessibility(root = composeRule.onNode(isDialog()))
    }

    /** Applies IME insets to the dialog's window, as the system does when the keyboard opens. */
    private fun showKeyboard() {
        matrix.composeRule.runOnIdle {
            val window = ShadowDialog.getLatestDialog().window!!
            val height = (KEYBOARD_HEIGHT_DP * window.context.resources.displayMetrics.density).toInt()
            val insets = WindowInsets.Builder()
                .setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, height))
                .setVisible(WindowInsets.Type.ime(), true)
                .build()
            window.decorView.dispatchApplyWindowInsets(insets)
        }
    }

    enum class DialogVariant(val slug: String) {
        Alert("alert"),
        Focused("alert-focused"),
        Destructive("destructive"),
        Ime("ime")
    }

    data class DialogCase(val variant: DialogVariant, val appearance: Appearance) {
        override fun toString() = "${variant.slug}_${appearance.slug}"
    }

    companion object {
        private const val KEYBOARD_HEIGHT_DP = 280

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val themed = DialogVariant.entries.flatMap { variant ->
                listOf(Appearance.Light, Appearance.Dark).map { DialogCase(variant, it) }
            }
            val layout = listOf(DialogVariant.Alert, DialogVariant.Destructive, DialogVariant.Ime).flatMap { variant ->
                listOf(Appearance.FontScale200, Appearance.Rtl).map { DialogCase(variant, it) }
            }
            return (themed + layout).map { arrayOf<Any>(it) }
        }
    }
}

/** A screen behind the dialog, so the scrim shows. */
@Composable
private fun SampleScreen() {
    Column(
        Modifier
            .fillMaxSize()
            .background(CycleTheme.colors.surface)
            .padding(CycleTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)
    ) {
        BasicText("Cycle day 19", style = CycleTheme.typography.headline.copy(color = CycleTheme.colors.onSurface))
        CycleLegend()
    }
}

@Composable
private fun AlertSample() {
    CycleAlertDialog(
        visible = true,
        onDismissRequest = {},
        title = "Turn on reminders?",
        text = "Cycle can nudge you a day before your period is due. Reminders stay on this phone.",
        confirmText = "Remind me",
        onConfirm = {},
        dismissText = "Not now",
        icon = CycleIcons.Today
    )
}

@Composable
private fun DestructiveSample() {
    CycleDestructiveDialog(
        visible = true,
        onDismissRequest = {},
        title = "Delete this day?",
        text = "This removes the period and notes logged for 14 March. You can't undo it.",
        confirmText = "Delete",
        onConfirm = {},
        dismissText = "Keep it"
    )
}

@Composable
private fun NoteSample() {
    CycleDialog(
        visible = true,
        onDismissRequest = {},
        title = "Add a note",
        confirmText = "Save",
        onConfirm = {},
        text = "A few words about the day. Notes stay on this phone.",
        dismissText = "Cancel"
    ) {
        CycleTextField(rememberTextFieldState("Slept well"), label = "Note", supportingText = "Only on this phone.")
    }
}
