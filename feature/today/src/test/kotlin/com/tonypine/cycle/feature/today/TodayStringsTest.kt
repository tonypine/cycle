package com.tonypine.cycle.feature.today

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every Today string follows the copy rules in `docs/research/product-implications.md` and
 * `docs/design/contraception.md`: no "safe" days, nothing about fertility, no telling her how to feel,
 * no pregnancy questions, no advice on her method and no clinical labels.
 */
@RunWith(AndroidJUnit4::class)
class TodayStringsTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `no string says what the app never says`() {
        val strings = R.string::class.java.fields.map { resources.getString(it.getInt(null)) } +
            R.plurals::class.java.fields.flatMap { field ->
                // The bleeding count takes the episodes as its second argument.
                listOf(1, 2).map { resources.getQuantityString(field.getInt(null), it, it, "$it episodes") }
            }
        assertTrue("Found no strings to check", strings.size > 20)

        val offending = strings.filter { text -> ForbiddenWords.any { text.contains(it, ignoreCase = true) } }
        assertEquals(emptyList<String>(), offending)
    }

    private companion object {
        // On a method too: never a safe or protected day, ovulation, advice on the method or a condition.
        val ForbiddenWords = listOf(
            "safe",
            "fertile",
            "you should feel",
            "pregnan",
            "ovulat",
            "protected",
            "low chance",
            "should",
            "consider",
            "switch",
            "amenorrh",
            "infrequent",
            "prolonged"
        )
    }
}
