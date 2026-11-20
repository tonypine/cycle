package com.tonypine.cycle.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.intl.LocaleList
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What the Language page shows and TalkBack reads, in each language, and what a tap chooses. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class LanguageScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val chosen = mutableListOf<Language?>()
    private var backs = 0
    private var state by mutableStateOf(LanguageUiState(chosen = null, phoneLanguage = Language.English))

    private fun show() = composeRule.setContent {
        Themed { LanguageScreen(state, onSelect = { chosen += it }, onBack = { backs++ }) }
    }

    private val isRadio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    // An option by its title, the first of its texts: "English" is also Phone's language's second line.
    private fun option(title: String) = composeRule.onNode(
        SemanticsMatcher("titled $title") {
            it.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text == title
        } and isRadio
    ).performScrollTo()

    // The option's texts as TalkBack reads them: its title, then its second line if it has one.
    private fun lines(title: String): List<String> =
        option(title).fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }

    @Test
    fun `five options in one group, Phone's language first, each read as a radio button with its place`() {
        show()

        composeRule.onNode(hasText("Language") and isHeading()).assertExists()
        val titles = listOf("Phone's language", "Deutsch", "English", "Español", "Português (Brasil)")
        titles.forEachIndexed { index, title ->
            option(title).assert(
                SemanticsMatcher("${index + 1} of 5") {
                    it.config.getOrNull(SemanticsProperties.CollectionItemInfo)?.rowIndex == index
                }
            )
        }
        composeRule.onAllNodes(isRadio).fetchSemanticsNodes().let { assertEquals(5, it.size) }
        option("Phone's language").assertIsSelected()
        option("Deutsch").assertIsNotSelected()
    }

    @Test
    fun `each language reads under its own name, in its own voice, and in Cycle's language under it`() {
        show()

        assertEquals(listOf("Phone's language", "English"), lines("Phone's language"))
        assertEquals(listOf("Deutsch", "German"), lines("Deutsch"))
        // The language Cycle is in has no second line: it would repeat the title.
        assertEquals(listOf("English"), lines("English"))
        assertEquals(listOf("Español", "Spanish"), lines("Español"))
        assertEquals(listOf("Português (Brasil)", "Portuguese (Brazil)"), lines("Português (Brasil)"))

        val title = option("Deutsch").fetchSemanticsNode().config[SemanticsProperties.Text].first()
        assertEquals(listOf(LocaleList("de")), title.spanStyles.map { it.item.localeList })
        val phoneTitle = option("Phone's language").fetchSemanticsNode().config[SemanticsProperties.Text].first()
        assertEquals(emptyList<Any>(), phoneTitle.spanStyles)
    }

    @Test
    fun `a tap chooses the language, or the phone's, and Back leaves`() {
        state = state.copy(chosen = Language("es"))
        show()
        option("Español").assertIsSelected()

        option("Deutsch").performClick()
        option("Phone's language").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf(Language("de"), null), chosen)
        assertEquals(1, backs)
    }

    @Test
    fun `a language with a region, as Android's per-app page gives it, selects its option`() {
        for ((tag, name) in listOf("es-US" to "Español", "de-AT" to "Deutsch", "pt-PT" to "Português (Brasil)")) {
            state = LanguageUiState(chosen = Language(tag), phoneLanguage = Language("de-DE"))
            if (tag == "es-US") show()
            composeRule.waitForIdle()

            option(name).assertIsSelected()
        }
    }

    @Test
    fun `a phone in French says Phone's language uses English`() {
        state = LanguageUiState(chosen = null, phoneLanguage = Language.English, phoneLocale = Locale.FRANCE)
        show()

        assertEquals(
            listOf("Phone's language", "French isn't in Cycle yet, so this option uses English."),
            lines("Phone's language")
        )
    }

    @Test
    fun `in Spanish, on a phone in French`() {
        renderIn(Language("es"))
        state = LanguageUiState(chosen = Language("es"), phoneLanguage = Language.English, phoneLocale = Locale.FRANCE)
        show()

        composeRule.onNode(hasText("Idioma") and isHeading()).assertExists()
        assertEquals(
            listOf("Idioma del teléfono", "Francés aún no está en Cycle, así que esta opción usa inglés."),
            lines("Idioma del teléfono")
        )
        assertEquals(listOf("Deutsch", "Alemán"), lines("Deutsch"))
        assertEquals(listOf("English", "Inglés"), lines("English"))
        assertEquals(listOf("Español"), lines("Español"))
        assertEquals(listOf("Português (Brasil)", "Portugués (Brasil)"), lines("Português (Brasil)"))
    }

    @Test
    fun `in German, on a phone in French then Spanish`() {
        renderIn(Language("de"))
        state = LanguageUiState(chosen = Language("de"), phoneLanguage = Language("es"), phoneLocale = Locale.FRANCE)
        show()

        composeRule.onNode(hasText("Sprache") and isHeading()).assertExists()
        assertEquals(
            listOf(
                "Sprache des Telefons",
                "Französisch gibt es in Cycle noch nicht, deshalb nutzt diese Option Spanisch."
            ),
            lines("Sprache des Telefons")
        )
        assertEquals(listOf("Deutsch"), lines("Deutsch"))
        assertEquals(listOf("English", "Englisch"), lines("English"))
        assertEquals(listOf("Português (Brasil)", "Portugiesisch (Brasilien)"), lines("Português (Brasil)"))
    }

    @Test
    fun `in Portuguese, on a phone in French then Spanish`() {
        renderIn(Language("pt-BR"))
        state = LanguageUiState(
            chosen = Language("pt-BR"),
            phoneLanguage = Language("es"),
            phoneLocale = Locale.FRANCE
        )
        show()

        composeRule.onNode(hasText("Idioma") and isHeading()).assertExists()
        assertEquals(
            listOf("Idioma do celular", "Francês ainda não está no Cycle, então esta opção usa espanhol."),
            lines("Idioma do celular")
        )
        assertEquals(listOf("Deutsch", "Alemão"), lines("Deutsch"))
        assertEquals(listOf("Español", "Espanhol"), lines("Español"))
        assertEquals(listOf("Português (Brasil)"), lines("Português (Brasil)"))
    }

    @Test
    fun `a phone in one of Cycle's languages names it, in any region`() {
        renderIn(Language("de"))
        state = LanguageUiState(
            chosen = Language("de"),
            phoneLanguage = Language.English,
            phoneLocale = Locale.forLanguageTag("en-US")
        )
        show()

        assertEquals(listOf("Sprache des Telefons", "Englisch"), lines("Sprache des Telefons"))
    }
}
