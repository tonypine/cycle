package com.tonypine.cycle.core.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.assertNeverSaid
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The shared strings follow the copy rules in `docs/research/product-implications.md`, in every language. */
@RunWith(AndroidJUnit4::class)
class CoreUiStringsTest {
    @Test
    fun `no string says what the app never says`() {
        val strings = assertNeverSaid(R.string::class.java, R.plurals::class.java)

        assertTrue("Found no strings to check", strings.getValue(Language.English).size > 10)
    }
}
