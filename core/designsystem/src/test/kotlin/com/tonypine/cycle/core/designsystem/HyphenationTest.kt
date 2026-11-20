package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.style.Hyphens
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * German text hyphenates where a line breaks, so a long compound such as "Periodenlänge" breaks at a
 * syllable at 200% font size rather than between any two letters. The other languages wrap between
 * words, as before.
 */
@RunWith(AndroidJUnit4::class)
class HyphenationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `only German hyphenates`() {
        val hyphens = mutableMapOf<String, List<Hyphens>>()
        Language.Supported.forEach { language ->
            renderIn(language)
            val roles = mutableListOf<Hyphens>()
            compose.setContent {
                CycleTheme {
                    val type = CycleTheme.typography
                    roles += listOf(type.display, type.title, type.titleSmall, type.body, type.bodySmall, type.label)
                        .map { it.hyphens }
                }
            }
            compose.waitForIdle()
            hyphens[language.tag] = roles.distinct()
        }

        assertEquals(listOf(Hyphens.Auto), hyphens.getValue("de"))
        listOf("en", "pt-BR", "es").forEach { assertEquals(it, listOf(Hyphens.Unspecified), hyphens.getValue(it)) }
    }
}
