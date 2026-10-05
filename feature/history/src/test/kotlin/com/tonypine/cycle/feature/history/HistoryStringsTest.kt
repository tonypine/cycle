package com.tonypine.cycle.feature.history

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every History string follows the copy rules in `docs/research/product-implications.md`: her cycles
 * are described, never judged as "normal", "regular" or "abnormal", and nothing about fertility.
 */
@RunWith(AndroidJUnit4::class)
class HistoryStringsTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `no string says what the app never says`() {
        val strings = R.string::class.java.fields.map { resources.getString(it.getInt(null)) } +
            R.plurals::class.java.fields.flatMap { field ->
                listOf(1, 2).map { resources.getQuantityString(field.getInt(null), it, it, it) }
            }
        assertTrue("Found no strings to check", strings.size > 20)

        val offending = strings.filter { text -> ForbiddenWords.any { text.contains(it, ignoreCase = true) } }
        assertEquals(emptyList<String>(), offending)
    }

    private companion object {
        val ForbiddenWords = listOf("normal", "regular", "safe", "fertile", "you should feel", "pregnan")
    }
}
