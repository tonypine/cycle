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
        val Supported: List<Language> = listOf(English, Language("pt-BR"), Language("es"), Language("de"))

        /**
         * What "the phone's language" is in Cycle: the first of the phone's [locales] that Cycle has,
         * in any region ("Español (México)" is Spanish, "Português (Portugal)" Brazilian Portuguese),
         * as Android picks the strings. English when it has none of them.
         */
        fun ofPhone(locales: List<Locale>, supported: List<Language> = Supported): Language =
            locales.firstNotNullOfOrNull { of(it, supported) } ?: English

        /**
         * The one of [supported] that [locale] is, in any region, as Android picks the strings: Android's
         * per-app page gives `es-ES` or `es-US` for Español, `de-AT` for Deutsch (Österreich) and
         * `pt-PT` for Português (Portugal), which are Spanish, German and Brazilian Portuguese in Cycle.
         * The exact tag first, so `pt-BR` stays `pt-BR`. Null when Cycle doesn't have the language.
         */
        fun of(locale: Locale, supported: List<Language> = Supported): Language? =
            supported.firstOrNull { it.locale == locale }
                ?: supported.firstOrNull { it.locale.language == locale.language }

        /** [of] for a BCP 47 tag, as the settings keep it: a stored `es-ES` is [Supported]'s `es`. */
        fun ofTag(tag: String, supported: List<Language> = Supported): Language? =
            of(Locale.forLanguageTag(tag), supported)
    }
}
