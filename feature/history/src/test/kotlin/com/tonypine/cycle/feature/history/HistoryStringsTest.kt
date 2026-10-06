package com.tonypine.cycle.feature.history

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.NeverSaid
import com.tonypine.cycle.core.testing.assertNeverSaid
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every History string follows the copy rules in `docs/research/product-implications.md`, in every
 * language Cycle has: her cycles are described, never judged as "normal", "regular" or "abnormal",
 * and nothing about fertility.
 */
@RunWith(AndroidJUnit4::class)
class HistoryStringsTest {
    @Test
    fun `no string says what the app never says`() {
        val strings = assertNeverSaid(R.string::class.java, R.plurals::class.java, also = NeverSaid.history)

        assertTrue("Found no strings to check", strings.getValue(Language.English).size > 20)
    }
}
