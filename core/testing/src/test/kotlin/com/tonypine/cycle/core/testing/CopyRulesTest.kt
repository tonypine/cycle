package com.tonypine.cycle.core.testing

import com.tonypine.cycle.core.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CopyRulesTest {
    @Test
    fun `each language is checked against its own words`() {
        val texts = mapOf(
            Language("en") to listOf("Your safe days", "Your next period is expected in about 10 days"),
            Language("de") to listOf("Deine fruchtbaren Tage", "Sicherung", "Deine nächste Periode wird erwartet"),
            Language("es") to listOf("¿Estás embarazada?", "Tu próximo periodo se espera en unos 10 días"),
            Language("pt-BR") to listOf("Dias férteis", "Tem certeza?")
        )

        assertEquals(
            listOf("en: Your safe days", "de: Deine fruchtbaren Tage", "es: ¿Estás embarazada?", "pt-BR: Dias férteis"),
            neverSaidIn(texts)
        )
    }

    @Test
    fun `a module's own words are added in each language, and stems catch the other forms`() {
        val texts = mapOf(
            Language("en") to listOf("An abnormal cycle"),
            Language("de") to listOf("Ein unregelmäßiger Zyklus")
        )

        assertEquals(emptyList<String>(), neverSaidIn(texts))
        assertEquals(
            listOf("en: An abnormal cycle", "de: Ein unregelmäßiger Zyklus"),
            neverSaidIn(texts, also = NeverSaid.history)
        )
    }

    @Test
    fun `a language with no words to check fails, so a new language cannot skip the check`() {
        assertThrows(AssertionError::class.java) { neverSaidIn(mapOf(Language("fr") to listOf("Bonjour"))) }
    }

    @Test
    fun `every language Cycle has has its words`() {
        Language.Supported.forEach { assertEquals(true, it in NeverSaid.everywhere) }
    }
}
