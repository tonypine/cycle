package com.tonypine.cycle.feature.today

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.assertNeverSaid
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every Today string follows the copy rules in `docs/research/product-implications.md` and
 * `docs/design/contraception.md`, in every language: no "safe" days, nothing about fertility, no
 * telling her how to feel, no pregnancy questions, no advice on her method and no clinical labels.
 */
@RunWith(AndroidJUnit4::class)
class TodayStringsTest {
    @Test
    fun `no string says what the app never says`() {
        val strings =
            assertNeverSaid(R.string::class.java, R.plurals::class.java, also = mapOf(Language.English to MoreWords))

        assertTrue("Found no strings to check", strings.getValue(Language.English).size > 20)
    }

    private companion object {
        // On a method too: never a safe or protected day, ovulation, advice on the method or a condition.
        val MoreWords = listOf(
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
