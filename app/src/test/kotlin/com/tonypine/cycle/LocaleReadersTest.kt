package com.tonypine.cycle

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every date and number is formatted in Cycle's locale, `cycleLocale()` in `core:designsystem`, never
 * in a configuration's first locale: on a phone in French then Spanish, Android shows the Spanish
 * strings while that locale stays French (`docs/decisions/0008-languages.md`).
 */
class LocaleReadersTest {
    @Test
    fun `no main source reads a configuration's first locale`() {
        val root = File("").absoluteFile.parentFile
        val sources = root.walkTopDown()
            .onEnter { it.name != "build" && !it.name.startsWith(".") }
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
            .toList()
        assertTrue("Found no sources", sources.size > 50)

        val offending = sources
            .map { it.relativeTo(root).invariantSeparatorsPath to it.readText() }
            .filter { (path, text) -> path != HELPER && ("locales[0]" in text || "locales.get(0)" in text) }
            .map { (path, _) -> path }
        assertEquals(emptyList<String>(), offending)
    }

    private companion object {
        const val HELPER = "core/designsystem/src/main/kotlin/com/tonypine/cycle/core/designsystem/CycleLocale.kt"
    }
}
