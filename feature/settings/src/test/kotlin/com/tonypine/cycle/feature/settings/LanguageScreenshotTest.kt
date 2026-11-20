package com.tonypine.cycle.feature.settings

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
import java.time.LocalDate
import java.util.Locale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Language page, and Settings' Language row, in each of Cycle's four languages, light and dark,
 * and in German at 200% font size, as the journeys in `docs/design/language.md` draw them: in English
 * on a phone in French (B2), in Spanish chosen on that phone (B3), in German chosen on a phone in
 * English (C2, C6), and in Portuguese chosen on a phone in French then Spanish. Each records
 * `src/test/screenshots/language_<screen>_<language>_<appearance>.png`. At 200% the page is tall
 * enough for every option, so nothing that wraps is cut off.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-mdpi")
class LanguageScreenshotTest(private val screen: String, private val tag: String, private val appearance: String) {
    // A tag rather than a Language: the runner cannot pass a value class to the constructor.
    private val language = Language(tag)

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun capture() {
        if (screen == SETTINGS || appearance == FONT_SCALE_200) RuntimeEnvironment.setQualifiers("+h1400dp")
        renderIn(language)
        val state = stateIn(language)
        composeRule.setContent {
            val density = LocalDensity.current
            val fontScale = if (appearance == FONT_SCALE_200) 2f else 1f
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Themed(darkTheme = appearance == DARK) {
                    if (screen == PAGE) {
                        LanguageScreen(state, onSelect = {}, onBack = {})
                    } else {
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
                            language = state,
                            onLanguage = {},
                            onNotices = {},
                            showBackupNote = false,
                            onHideBackupNote = {}
                        )
                    }
                }
            }
        }
        val path = "src/test/screenshots/language_${screen}_${language.tag}_$appearance.png"
        if (screen == PAGE) {
            composeRule.onRoot().captureRoboImage(path)
        } else {
            // The App section's one row.
            val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
            val title = ApplicationProvider.getApplicationContext<Context>().getString(R.string.language_title)
            composeRule.onNode(hasText(title) and isButton).performScrollTo().captureRoboImage(path)
        }
    }

    companion object {
        private const val PAGE = "page"
        private const val SETTINGS = "settings_row"
        private const val LIGHT = "light"
        private const val DARK = "dark"
        private const val FONT_SCALE_200 = "font_scale_200"

        // The journeys' phones: English following a phone in French, the others chosen.
        private fun stateIn(language: Language): LanguageUiState = when (language.tag) {
            "en" -> LanguageUiState(chosen = null, phoneLanguage = Language.English, phoneLocale = Locale.FRANCE)
            "es" -> LanguageUiState(chosen = language, phoneLanguage = Language.English, phoneLocale = Locale.FRANCE)
            "pt-BR" -> LanguageUiState(chosen = language, phoneLanguage = Language("es"), phoneLocale = Locale.FRANCE)
            else -> LanguageUiState(chosen = language, phoneLanguage = Language.English, phoneLocale = Locale.US)
        }

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}_{2}")
        fun cases(): List<Array<Any>> = listOf(PAGE, SETTINGS).flatMap { screen ->
            Language.Supported.flatMap { language ->
                listOf(LIGHT, DARK).map { arrayOf<Any>(screen, language.tag, it) }
            } + listOf(arrayOf<Any>(screen, "de", FONT_SCALE_200))
        }
    }
}
