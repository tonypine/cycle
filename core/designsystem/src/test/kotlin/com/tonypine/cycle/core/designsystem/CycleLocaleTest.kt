package com.tonypine.cycle.core.designsystem

import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateFormat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Cycle's locale and the first day of the week, as `docs/decisions/0008-languages.md` decides. */
@RunWith(AndroidJUnit4::class)
class CycleLocaleTest {
    @get:Rule
    val compose = createComposeRule()

    private fun locales(vararg tags: String) = tags.map(Locale::forLanguageTag)

    @Test
    fun `following the phone, the phone's first locale in the language of the strings`() {
        // French then Spanish: Android shows the Spanish strings, and the dates follow them.
        val frenchThenSpanish = locales("fr-FR", "es-MX")
        assertEquals(Locale.forLanguageTag("es-MX"), cycleLocale("es", frenchThenSpanish, frenchThenSpanish))

        val portugal = locales("pt-PT")
        assertEquals(Locale.forLanguageTag("pt-PT"), cycleLocale("pt-BR", portugal, portugal))
    }

    @Test
    fun `with none of Cycle's languages on the phone, English in the phone's region`() {
        val french = locales("fr-FR")

        assertEquals(Locale.forLanguageTag("en-FR"), cycleLocale("en", french, french))
    }

    @Test
    fun `a chosen language takes the region of the phone's first locale, unless it has its own`() {
        val us = locales("en-US", "de-AT")
        assertEquals(Locale.forLanguageTag("de-US"), cycleLocale("de", locales("de"), us))

        val germany = locales("de-DE")
        assertEquals(Locale.forLanguageTag("en-DE"), cycleLocale("en", locales("en"), germany))

        val portugal = locales("pt-PT")
        assertEquals(Locale.forLanguageTag("pt-BR"), cycleLocale("pt-BR", locales("pt-BR"), portugal))
    }

    @Test
    fun `with German strings chosen, months are German`() {
        val locale = cycleLocale("de", locales("de"), locales("en-US"))
        val pattern = DateFormat.getBestDateTimePattern(locale, "MMMMyyyy")

        assertEquals("März 2027", DateTimeFormatter.ofPattern(pattern, locale).format(YearMonth.of(2027, 3)))
    }

    @Test
    fun `chosen German with only English strings stays English, so a screen never mixes the two`() {
        assertEquals(Locale.forLanguageTag("en-US"), cycleLocale("en", locales("de"), locales("en-US")))
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `on an English phone, the phone's own locale, as before`() {
        lateinit var locale: Locale
        lateinit var firstDay: DayOfWeek
        compose.setContent {
            locale = cycleLocale()
            firstDay = firstDayOfWeek()
        }

        compose.runOnIdle {
            assertEquals(Locale.US, locale)
            assertEquals(DayOfWeek.SUNDAY, firstDay)
        }
    }

    @Test
    @Config(qualifiers = "fr-rFR")
    fun `on a French phone, a language Cycle has no strings for, English dates and French weeks`() {
        lateinit var locale: Locale
        lateinit var firstDay: DayOfWeek
        compose.setContent {
            locale = cycleLocale()
            firstDay = firstDayOfWeek()
        }

        compose.runOnIdle {
            assertEquals(Locale.forLanguageTag("en-FR"), locale)
            assertEquals(DayOfWeek.MONDAY, firstDay)
        }
    }

    @Test
    @Config(qualifiers = "de-rDE")
    fun `on a German phone, German dates and weeks`() {
        lateinit var locale: Locale
        lateinit var firstDay: DayOfWeek
        compose.setContent {
            locale = cycleLocale()
            firstDay = firstDayOfWeek()
        }

        compose.runOnIdle {
            assertEquals(Locale.forLanguageTag("de-DE"), locale)
            assertEquals(DayOfWeek.MONDAY, firstDay)
        }
    }

    @Test
    @Config(sdk = [29], qualifiers = "fr-rFR")
    fun `below Android 13 the phone's languages come from the system`() {
        lateinit var locale: Locale
        lateinit var firstDay: DayOfWeek
        compose.setContent {
            locale = cycleLocale()
            firstDay = firstDayOfWeek()
        }

        compose.runOnIdle {
            assertEquals(Locale.forLanguageTag("en-FR"), locale)
            assertEquals(DayOfWeek.MONDAY, firstDay)
        }
    }

    @Test
    @Config(qualifiers = "fr-rFR")
    fun `a context without a LocaleManager, as in a preview, falls back to the system's languages`() {
        lateinit var locale: Locale
        lateinit var firstDay: DayOfWeek
        compose.setContent {
            CompositionLocalProvider(LocalContext provides NoLocaleManager(LocalContext.current)) {
                locale = cycleLocale()
                firstDay = firstDayOfWeek()
            }
        }

        compose.runOnIdle {
            assertEquals(Locale.forLanguageTag("en-FR"), locale)
            assertEquals(DayOfWeek.MONDAY, firstDay)
        }
    }

    @Test
    fun `a date that starts a line starts with a capital in every language`() {
        fun monthTitle(tag: String): String {
            val locale = Locale.forLanguageTag(tag)
            val pattern = DateFormat.getBestDateTimePattern(locale, "MMMMyyyy")
            return startingLine(DateTimeFormatter.ofPattern(pattern, locale).format(YearMonth.of(2027, 3)), locale)
        }

        assertEquals("Março de 2027", monthTitle("pt-BR"))
        assertEquals("Marzo de 2027", monthTitle("es-ES"))
        assertEquals("März 2027", monthTitle("de-DE"))
        assertEquals("March 2027", monthTitle("en-US"))
    }

    private class NoLocaleManager(base: Context) : ContextWrapper(base) {
        override fun getSystemService(name: String): Any? =
            if (name == Context.LOCALE_SERVICE) null else super.getSystemService(name)
    }
}
