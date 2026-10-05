package com.tonypine.cycle.feature.onboarding

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every welcome and setup string follows the copy rules in `docs/research/product-implications.md`,
 * like Today's (`TodayStringsTest`).
 */
@RunWith(AndroidJUnit4::class)
class OnboardingStringsTest {
    @Test
    fun `no string says what the app never says`() {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val strings = R.string::class.java.fields.map { resources.getString(it.getInt(null)) }
        assertTrue("Found no strings to check", strings.size > 10)

        val offending = strings.filter { text ->
            listOf("safe", "fertile", "you should feel", "pregnan").any { text.contains(it, ignoreCase = true) }
        }
        assertEquals(emptyList<String>(), offending)
    }
}
