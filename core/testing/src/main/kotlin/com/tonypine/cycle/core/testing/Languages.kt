package com.tonypine.cycle.core.testing

import com.tonypine.cycle.core.model.Language
import org.robolectric.RuntimeEnvironment

/**
 * Robolectric's resource qualifier for this language: `en`, `de`, `pt-rBR`. For a whole test class or
 * test in one language, `@Config(qualifiers = "+de")`; within a test, [renderIn].
 */
val Language.qualifier: String
    get() = if (locale.country.isEmpty()) locale.language else "${locale.language}-r${locale.country}"

/**
 * Renders what follows in [language], as on a phone set to it, keeping the other qualifiers (screen
 * size, night). Call it before `setContent`, for instance in a test parameterized over
 * [Language.Supported] that records a screenshot in each language, named with [Language.tag].
 */
fun renderIn(language: Language) {
    RuntimeEnvironment.setQualifiers("+${language.qualifier}")
}
