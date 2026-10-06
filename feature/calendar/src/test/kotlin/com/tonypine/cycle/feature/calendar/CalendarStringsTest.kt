package com.tonypine.cycle.feature.calendar

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Every calendar string follows the copy rules in `docs/research/product-implications.md`. */
@RunWith(AndroidJUnit4::class)
class CalendarStringsTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `no string says what the app never says, and the predictions read as estimates`() {
        val strings = R.string::class.java.fields.map { resources.getString(it.getInt(null)) }
        assertTrue("Found no strings to check", strings.size >= 3)

        val offending = strings.filter { text -> ForbiddenWords.any { text.contains(it, ignoreCase = true) } }
        assertEquals(emptyList<String>(), offending)
        assertTrue(resources.getString(R.string.calendar_hint).contains("estimates"))
        assertTrue(resources.getString(R.string.calendar_hint_bleeds).contains("estimates"))
    }

    private companion object {
        // On a method too: never a safe or protected day, ovulation or advice on the method.
        val ForbiddenWords = listOf(
            "safe",
            "fertile",
            "you should feel",
            "pregnan",
            "ovulat",
            "protected",
            "should",
            "consider",
            "switch"
        )
    }
}
