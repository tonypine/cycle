package com.tonypine.cycle.core.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Each language's strings carry its own tag, which [cycleLocale] formats dates in. */
@RunWith(AndroidJUnit4::class)
class LanguageTagTest {
    @Test
    fun `each language Cycle has names itself`() {
        Language.Supported.forEach { language ->
            renderIn(language)
            val resources = ApplicationProvider.getApplicationContext<Context>().resources

            assertEquals(language.tag, resources.getString(R.string.cycle_language_tag))
        }
    }
}
