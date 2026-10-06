package com.tonypine.cycle.core.testing

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.tonypine.cycle.core.model.Language
import org.junit.Assert.assertEquals

/**
 * The words the app never says, in each language Cycle has: the copy checks of
 * `docs/decisions/0008-languages.md`, which carry the copy rules of
 * `docs/research/product-implications.md` into every language. A list changes only with that record.
 * Each word is looked for anywhere in a string, ignoring case, so a stem catches its other forms
 * ("fertil" catches "fertilidade", "regelmäßig" catches "unregelmäßig").
 */
object NeverSaid {
    /** In every module: the "safe day" wording, fertility, pregnancy, telling her how to feel. */
    val everywhere: Map<Language, List<String>> = mapOf(
        Language("en") to listOf("safe", "fertile", "you should feel", "pregnan"),
        Language("pt-BR") to listOf(
            "seguro",
            "segura",
            "fértil",
            "fertil",
            "férteis",
            "grávid",
            "gravidez",
            "engravid",
            "deveria se sentir",
            "deve se sentir"
        ),
        Language("es") to listOf("seguro", "segura", "fértil", "fertil", "embaraz", "deberías sentir", "debes sentir"),
        Language("de") to listOf("sichere", "fruchtbar", "schwanger", "solltest dich", "fühlen solltest")
    )

    /** In History too: her cycles are described, never judged. */
    val history: Map<Language, List<String>> = mapOf(
        Language("en") to listOf("normal", "regular"),
        Language("pt-BR") to listOf("normal", "regular"),
        Language("es") to listOf("normal", "regular"),
        Language("de") to listOf("normal", "regelmäßig")
    )
}

/**
 * Reads every string and plural of a module in each of [languages], and fails on any that says one of
 * that language's [NeverSaid.everywhere] words, or of [also] (a module's own words, such as
 * [NeverSaid.history]). Pass the module's `R.string::class.java` and `R.plurals::class.java`. A
 * module with words of its own has them in every language checked, or the check fails.
 *
 * Returns what it read in each language, for the module's own checks on it.
 */
fun assertNeverSaid(
    strings: Class<*>,
    plurals: Class<*>? = null,
    also: Map<Language, List<String>> = emptyMap(),
    languages: List<Language> = Language.Supported
): Map<Language, List<String>> {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val read = languages.associateWith { language ->
        val resources = context.resourcesIn(language)
        strings.ids().map { resources.getString(it) } +
            plurals?.ids().orEmpty().flatMap { id ->
                PLURAL_QUANTITIES.map { resources.getQuantityText(id, it).toString() }
            }
    }
    assertEquals(emptyList<String>(), neverSaidIn(read, also))
    return read
}

/**
 * Each of [texts] that says a word of its language's [NeverSaid.everywhere] or [also], as "tag: text".
 * Fails on a language with no words in either, so a new language cannot skip a module's own words.
 */
fun neverSaidIn(texts: Map<Language, List<String>>, also: Map<Language, List<String>> = emptyMap()): List<String> =
    texts.flatMap { (language, inLanguage) ->
        val words = NeverSaid.everywhere[language]
            ?: throw AssertionError("No words to check in ${language.tag}: add them to NeverSaid, with 0008")
        val extra = if (also.isEmpty()) {
            emptyList()
        } else {
            also[language] ?: throw AssertionError("No module words in ${language.tag}: add them in every language")
        }
        val all = words + extra
        inLanguage.distinct()
            .filter { text -> all.any { text.contains(it, ignoreCase = true) } }
            .map { "${language.tag}: $it" }
    }

private fun Context.resourcesIn(language: Language): Resources =
    createConfigurationContext(Configuration(resources.configuration).apply { setLocale(language.locale) }).resources

private fun Class<*>.ids(): List<Int> = fields.map { it.getInt(null) }

// One of each plural category the four languages use: zero, one, two, few, many and other.
private val PLURAL_QUANTITIES = listOf(0, 1, 2, 3, 5, 1_000_000)
