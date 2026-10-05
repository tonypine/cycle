package com.tonypine.cycle.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * "Lock Cycle" in Settings, in light, dark, at 200% font scale and right to left: the switch on, the
 * note after Cycle turned it off itself, and the dialog on a phone with no screen lock. Each records
 * `src/test/screenshots/lock_cycle_<state>_<appearance>.png`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class LockCycleScreenshotTest(private val state: State, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun capture() {
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale),
                LocalLayoutDirection provides
                    if (appearance == Appearance.Rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                Themed(darkTheme = appearance == Appearance.Dark) { state.content() }
            }
        }
        val path = "src/test/screenshots/lock_cycle_${state.fileName}_${appearance.fileName}.png"
        when (state) {
            State.On -> {
                composeRule.onNodeWithText("Lock Cycle").performScrollTo()
                composeRule.onRoot().captureRoboImage(path)
            }

            State.TurnedOff -> {
                composeRule.onNodeWithText("OK").performScrollTo()
                composeRule.onRoot().captureRoboImage(path)
            }

            State.NoScreenLock -> captureScreenRoboImage(path)
        }
    }

    enum class State(val fileName: String, val content: @Composable () -> Unit) {
        On("on", { Settings(SettingsUiState(29, 4, lastExported = null, appLock = true)) }),
        TurnedOff("turned_off", { Settings(SettingsUiState(29, 4, lastExported = null, appLockTurnedOff = true)) }),
        NoScreenLock("no_screen_lock", {
            Settings(SettingsUiState(29, 4, lastExported = LocalDate.of(2027, 3, 20), dialog = DataDialog.NoScreenLock))
        })
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200"),
        Rtl("rtl")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = State.entries.flatMap { state ->
            Appearance.entries.map { arrayOf<Any>(state, it) }
        }
    }
}

@Composable
private fun Settings(state: SettingsUiState) = SettingsScreen(
    state = state,
    versionName = "1.4.27",
    onUsualLengths = {},
    onWhatToLog = {},
    onAppLockChange = {},
    onDismissLockNote = {},
    onExport = {},
    onImport = {},
    onConfirmImport = {},
    onDismissDialog = {},
    onDeleteEverything = {},
    onNotices = {},
    showBackupNote = false,
    onHideBackupNote = {}
)
