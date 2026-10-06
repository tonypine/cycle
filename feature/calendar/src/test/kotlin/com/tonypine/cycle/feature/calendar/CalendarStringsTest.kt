package com.tonypine.cycle.feature.calendar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.assertNeverSaid
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Every calendar string follows the copy rules in `docs/research/product-implications.md`, in every language. */
@RunWith(AndroidJUnit4::class)
class CalendarStringsTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `no string says what the app never says, and the predictions read as estimates`() {
        val strings = assertNeverSaid(R.string::class.java, also = mapOf(Language.English to MoreWords))

        assertTrue("Found no strings to check", strings.getValue(Language.English).size >= 3)
        assertTrue(resources.getString(R.string.calendar_hint).contains("estimates"))
        assertTrue(resources.getString(R.string.calendar_hint_bleeds).contains("estimates"))
    }

    private companion object {
        // On a method too: never a safe or protected day, ovulation or advice on the method.
        val MoreWords = listOf("ovulat", "protected", "should", "consider", "switch")
    }
}
