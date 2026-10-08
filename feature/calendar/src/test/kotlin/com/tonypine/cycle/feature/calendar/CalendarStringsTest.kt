package com.tonypine.cycle.feature.calendar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.assertNeverSaid
import com.tonypine.cycle.core.testing.renderIn
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Every calendar string follows the copy rules in `docs/research/product-implications.md`, in every language. */
@RunWith(AndroidJUnit4::class)
class CalendarStringsTest {
    @Test
    fun `no string says what the app never says, and the predictions read as estimates`() {
        val strings = assertNeverSaid(R.string::class.java, also = MoreWords)

        assertTrue("Found no strings to check", strings.getValue(Language.English).size >= 3)
        Estimates.forEach { (language, word) ->
            renderIn(language)
            val resources = ApplicationProvider.getApplicationContext<Context>().resources
            assertTrue(resources.getString(R.string.calendar_hint).contains(word))
            assertTrue(resources.getString(R.string.calendar_hint_bleeds).contains(word))
        }
    }

    private companion object {
        // On a method too: never a safe or protected day, ovulation or advice on the method.
        val MoreWords = mapOf(
            Language.English to listOf("ovulat", "protected", "should", "consider", "switch"),
            Language("pt-BR") to listOf("ovulaç", "protegid", "deveria", "você deve", "considere", "troque"),
            Language("es") to listOf("ovulac", "protegid", "deberías", "debes", "considera", "cambia de"),
            Language("de") to listOf("eisprung", "geschützt", "solltest", "sollte", "erwäge", "wechsle")
        )

        // The predictions read as estimates, in each language's own word.
        val Estimates = mapOf(
            Language.English to "estimates",
            Language("pt-BR") to "estimativas",
            Language("es") to "estimaciones",
            Language("de") to "Schätzungen"
        )
    }
}
