package com.tonypine.cycle

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app's own strings follow the copy rules in `docs/research/product-implications.md`, like each
 * feature's (`TodayStringsTest`).
 */
@RunWith(AndroidJUnit4::class)
class AppStringsTest {
    @Test
    fun `no string says what the app never says`() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val strings = R.string::class.java.fields.map { resources.getString(it.getInt(null)) }

        val offending = strings.filter { text ->
            listOf("safe", "fertile", "you should feel", "pregnan").any { text.contains(it, ignoreCase = true) }
        }
        assertEquals(emptyList<String>(), offending)
    }
}
