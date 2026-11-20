package com.tonypine.cycle.feature.settings

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Settings, on a screen tall enough for every section, in German at 100% and 200% font size and in
 * Brazilian Portuguese and Spanish; and "Delete everything?" over it in German at both sizes. Nothing
 * clips or overlaps, and the disclaimer reads in each language. Each records
 * `src/test/screenshots/settings_<screen>_<language>_<scale>.png`. Synthetic dates only.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class SettingsLanguagesScreenshotTest(
    private val screen: String,
    private val tag: String,
    private val fontScale: Float
) {
    // A tag rather than a Language: the runner cannot pass a value class to the constructor.
    private val language = Language(tag)

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        if (screen == HOME) RuntimeEnvironment.setQualifiers("+h1400dp")
        renderIn(language)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed {
                    SettingsScreen(
                        state = SettingsUiState(29, 4, lastExported = LocalDate.of(2027, 3, 20)),
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
                        showBackupNote = false,
                        onHideBackupNote = {}
                    )
                }
            }
        }
        val scale = if (fontScale == 2f) "font_scale_200" else "light"
        val path = "src/test/screenshots/settings_${screen}_${language.tag}_$scale.png"
        if (screen == DELETE_DIALOG) {
            val deleteEverything = ApplicationProvider.getApplicationContext<Context>().getString(
                R.string.settings_delete_title
            )
            composeRule.onNodeWithText(deleteEverything).performScrollTo().performClick()
            composeRule.waitForIdle()
            captureScreenRoboImage(path)
        } else {
            composeRule.onRoot().captureRoboImage(path)
        }
    }

    companion object {
        private const val HOME = "home"
        private const val DELETE_DIALOG = "delete_dialog"

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases(): List<Array<Any>> = listOf(HOME, DELETE_DIALOG).flatMap { screen ->
            listOf(1f, 2f).map { arrayOf<Any>(screen, "de", it) }
        } + listOf("pt-BR", "es").map { arrayOf<Any>(HOME, it, 1f) }
    }
}
