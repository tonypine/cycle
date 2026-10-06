package com.tonypine.cycle.feature.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.assertNeverSaid
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Every Settings string follows the copy rules in `docs/research/product-implications.md`, in every language. */
@RunWith(AndroidJUnit4::class)
class SettingsStringsTest {
    @Test
    fun `no string says what the app never says`() {
        val strings = assertNeverSaid(R.string::class.java)

        assertTrue("Found no strings to check", strings.getValue(Language.English).size > 10)
    }
}
