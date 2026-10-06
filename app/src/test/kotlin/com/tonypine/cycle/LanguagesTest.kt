package com.tonypine.cycle

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.model.Language
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The languages Cycle has are named the same everywhere (`docs/decisions/0008-languages.md`): in
 * `localeConfig`, in the build's `localeFilters`, in `Language.Supported` and in every module's string
 * folders. A fifth language cannot be half added.
 */
@RunWith(AndroidJUnit4::class)
class LanguagesTest {
    private val root = File("").absoluteFile.parentFile
    private val supported = Language.Supported.map { it.tag }.toSet()

    @Test
    fun `localeConfig lists the languages Cycle has`() {
        val localeConfig = File("src/main/res/xml/locales_config.xml").readText()

        assertEquals(
            supported,
            Regex("""android:name="([^"]+)"""").findAll(localeConfig).map {
                it.groupValues[1]
            }.toSet()
        )
    }

    @Test
    fun `the APK keeps only the languages Cycle has`() {
        val filters = System.getProperty("cycle.localeFilters").orEmpty().split(",").mapNotNull(::tagOfQualifiers)

        assertEquals(supported, filters.toSet())
    }

    @Test
    fun `every module has its strings in each language Cycle has, and in no other`() {
        val modules = root.walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.isDirectory && it.invariantSeparatorsPath.endsWith("/src/main/res") }
            // The review catalog keeps its few strings in English.
            .filterNot { it.invariantSeparatorsPath.contains("/app-catalog/") }
            .toList()
        assertTrue("Found no modules", modules.size >= 8)

        modules.forEach { res ->
            val languages = res.listFiles().orEmpty()
                .filter { File(it, "strings.xml").exists() }
                .map { tagOfQualifiers(it.name.removePrefix("values").removePrefix("-")) ?: "en" }
            if (languages.isNotEmpty()) assertEquals(res.relativeTo(root).path, supported, languages.toSet())
        }
    }

    @Test
    fun `a library's strings in another language never mix into Cycle's`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val french = context.createConfigurationContext(Configuration().apply { setLocale(Locale.FRANCE) })

        // AndroidX Biometric has French strings of its own; the filter leaves them out.
        assertEquals(
            context.getString(androidx.biometric.R.string.default_error_msg),
            french.getString(androidx.biometric.R.string.default_error_msg)
        )
    }

    /** The language in resource qualifiers such as `de` or `pt-rBR`, as a tag; null with none (`night`). */
    private fun tagOfQualifiers(qualifiers: String): String? {
        val parts = qualifiers.split("-")
        val language = parts.firstOrNull()?.takeIf { it.matches(Regex("[a-z]{2,3}")) } ?: return null
        val region = parts.getOrNull(1)?.takeIf { it.matches(Regex("r[A-Z]{2}")) }?.drop(1)
        return if (region == null) language else "$language-$region"
    }
}
