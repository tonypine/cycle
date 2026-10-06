package com.tonypine.cycle.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.tonypine.cycle.core.data.export.ImportProblem
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Settings tab and its pages, in light, dark and at 200% font scale: Settings, after an export,
 * on a screen tall enough for every section, with and without the note that Cycle isn't backed up
 * on a phone with no screen lock; Usual cycle and period; the open-source notices; and,
 * over Settings, "Delete everything?", "Import 42 days?" and a file that can't be imported. Each
 * records `src/test/screenshots/settings_<screen>_<appearance>.png`. Usual cycle and period is also
 * recorded on a phone on its side, at the top and scrolled to Save, which fails if it stops
 * scrolling. Synthetic dates only.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class SettingsScreenshotTest(private val screen: Screen, private val appearance: Appearance) {
    @get:Rule
    val composeRule = createComposeRule()

    @OptIn(ExperimentalRoborazziApi::class)
    @Test
    fun capture() {
        if (screen == Screen.Home || screen == Screen.NotBackedUp) RuntimeEnvironment.setQualifiers("+h1400dp")
        if (appearance == Appearance.Landscape) RuntimeEnvironment.setQualifiers(LANDSCAPE)
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == Appearance.FontScale200) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed(darkTheme = appearance == Appearance.Dark) { screen.content() }
            }
        }
        val path = "src/test/screenshots/settings_${screen.fileName}_${appearance.fileName}.png"
        when (screen) {
            Screen.DeleteDialog -> {
                composeRule.onNodeWithText("Delete everything").performScrollTo().performClick()
                composeRule.waitForIdle()
                captureScreenRoboImage(path)
            }

            Screen.ImportDialog, Screen.RefusedDialog -> captureScreenRoboImage(path)

            Screen.UsualLengths if appearance == Appearance.Landscape -> {
                composeRule.onRoot().captureRoboImage(path)
                composeRule.onNodeWithText("Save").performScrollTo()
                composeRule.onRoot().captureRoboImage(path.replace(".png", "_scrolled.png"))
            }

            else -> composeRule.onRoot().captureRoboImage(path)
        }
    }

    enum class Screen(val fileName: String, val content: @Composable () -> Unit) {
        Home("home", { Settings() }),
        NotBackedUp("not_backed_up", { Settings(showBackupNote = true) }),
        UsualLengths("usual_lengths", {
            UsualLengthsScreen(UsualLengthsUiState.Editing(29, 4), onSave = { _, _ -> }, onBack = {})
        }),
        Notices("notices", { OpenSourceNoticesScreen(onOpen = {}, onBack = {}) }),
        DeleteDialog("delete_dialog", { Settings() }),
        ImportDialog("import_dialog", { Settings(DataDialog.ConfirmImport(newDays = 42)) }),
        RefusedDialog("refused_dialog", {
            Settings(DataDialog.ImportRefused(ImportProblem.UnknownValue(line = 7, column = "flow", value = "lots")))
        })
    }

    enum class Appearance(val fileName: String) {
        Light("light"),
        Dark("dark"),
        FontScale200("font_scale_200"),
        Landscape("landscape")
    }

    companion object {
        private const val LANDSCAPE = "+w800dp-h360dp-land"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun cases(): List<Array<Any>> = Screen.entries.flatMap { screen ->
            listOf(Appearance.Light, Appearance.Dark, Appearance.FontScale200).map { arrayOf<Any>(screen, it) }
        } + listOf(arrayOf<Any>(Screen.UsualLengths, Appearance.Landscape))
    }
}

@Composable
private fun Settings(dialog: DataDialog? = null, showBackupNote: Boolean = false) = SettingsScreen(
    state = SettingsUiState(29, 4, lastExported = LocalDate.of(2027, 3, 20), dialog = dialog),
    versionName = "1.4.27",
    onUsualLengths = {},
    onWhatToLog = {},
    onContraception = {},
    onAppLockChange = {},
    onDismissLockNote = {},
    onExport = {},
    onImport = {},
    onConfirmImport = {},
    onDismissDialog = {},
    onDeleteEverything = {},
    onNotices = {},
    showBackupNote = showBackupNote,
    onHideBackupNote = {}
)
