package com.tonypine.cycle.core.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** The shared strings follow the copy rules in `docs/research/product-implications.md`. */
@RunWith(AndroidJUnit4::class)
class CoreUiStringsTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun `no string says what the app never says`() {
        val strings = R.string::class.java.fields.map { resources.getString(it.getInt(null)) } +
            R.plurals::class.java.fields.flatMap { field ->
                listOf(1, 2).map { resources.getQuantityString(field.getInt(null), it, it) }
            }
        assertTrue("Found no strings to check", strings.size > 10)

        val offending = strings.filter { text -> ForbiddenWords.any { text.contains(it, ignoreCase = true) } }
        assertEquals(emptyList<String>(), offending)
    }

    private companion object {
        val ForbiddenWords = listOf("safe", "fertile", "you should feel", "pregnan")
    }
}
