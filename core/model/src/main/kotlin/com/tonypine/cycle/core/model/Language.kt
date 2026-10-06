package com.tonypine.cycle.core.model

import java.util.Locale

/**
 * A language Cycle is shown in, by its BCP 47 tag: `en`, `pt-BR`, `es`, `de`
 * (`docs/decisions/0008-languages.md`). Her choice is one of [Supported], or none for the phone's
 * language.
 */
@JvmInline
value class Language(val tag: String) {
    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        val English = Language("en")

        /**
         * The languages Cycle has strings for, as `localeConfig` and `localeFilters` list them. A
         * language is added here in the PR that adds its strings.
         */
        val Supported: List<Language> = listOf(English)

        /**
         * What "the phone's language" is in Cycle: the first of the phone's [locales] that Cycle has,
         * in any region ("Español (México)" is Spanish, "Português (Portugal)" Brazilian Portuguese),
         * as Android picks the strings. English when it has none of them.
         */
        fun ofPhone(locales: List<Locale>, supported: List<Language> = Supported): Language =
            locales.firstNotNullOfOrNull { phone -> supported.firstOrNull { it.locale.language == phone.language } }
                ?: English
    }
}
