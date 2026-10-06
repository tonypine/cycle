package com.tonypine.cycle.core.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageTest {
    private val withSpanish = listOf(Language.English, Language("pt-BR"), Language("es"), Language("de"))

    @Test
    fun `a phone in French then Spanish is in Spanish once Cycle has Spanish, English until then`() {
        val phone = listOf(Locale.forLanguageTag("fr-FR"), Locale.forLanguageTag("es-ES"))

        assertEquals(Language("es"), Language.ofPhone(phone, withSpanish))
        assertEquals(Language.English, Language.ofPhone(phone))
    }

    @Test
    fun `any region of a language Cycle has picks it`() {
        assertEquals(Language("pt-BR"), Language.ofPhone(listOf(Locale.forLanguageTag("pt-PT")), withSpanish))
        assertEquals(Language("es"), Language.ofPhone(listOf(Locale.forLanguageTag("es-MX")), withSpanish))
        assertEquals(Language("de"), Language.ofPhone(listOf(Locale.forLanguageTag("de-AT")), withSpanish))
    }

    @Test
    fun `the phone's order wins, and a phone with none of them is in English`() {
        val phone = listOf(Locale.forLanguageTag("de-DE"), Locale.forLanguageTag("en-US"))

        assertEquals(Language("de"), Language.ofPhone(phone, withSpanish))
        assertEquals(Language.English, Language.ofPhone(phone))
        assertEquals(Language.English, Language.ofPhone(listOf(Locale.FRENCH), withSpanish))
        assertEquals(Language.English, Language.ofPhone(emptyList(), withSpanish))
    }

    @Test
    fun `English, the language of the default strings, is always one Cycle has`() {
        assertEquals(Language.English, Language.Supported.first())
        assertEquals(Locale.ENGLISH, Language.English.locale)
    }
}
