package com.tonypine.cycle.core.testing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LanguagesTest {
    @Test
    fun `qualifiers in Android's resource form`() {
        assertEquals(listOf("en", "de", "pt-rBR"), listOf("en", "de", "pt-BR").map { Language(it).qualifier })
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp-night")
    fun `renders in the language, keeping the other qualifiers`() {
        renderIn(Language("pt-BR"))

        val configuration = ApplicationProvider.getApplicationContext<Context>().resources.configuration
        assertEquals(Locale.forLanguageTag("pt-BR"), configuration.locales.get(0))
        assertEquals(360, configuration.screenWidthDp)
        assertEquals(true, configuration.isNightModeActive)
    }
}
