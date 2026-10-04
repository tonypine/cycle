package com.tonypine.cycle.core.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OpenSourceNoticesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun everyBundledFontShipsItsCopyrightAndTheOflText() {
        val copyrights = mapOf(
            "Bricolage Grotesque" to "Copyright 2022 The Bricolage Grotesque Project Authors",
            "DM Sans" to "Copyright 2014 The DM Sans Project Authors"
        )
        copyrights.forEach { (name, copyright) ->
            val notice = OpenSourceNotices.single { it.name == name }
            val text = context.resources.openRawResource(notice.text).bufferedReader().use { it.readText() }
            assertTrue("$name is missing its copyright", text.startsWith(copyright))
            assertTrue("$name is missing the OFL 1.1 text", "SIL OPEN FONT LICENSE Version 1.1" in text)
            assertTrue("$name is missing the OFL terms", "TERMINATION" in text)
        }
    }

    @Test
    fun materialSymbolsShipTheApacheLicence() {
        val notice = OpenSourceNotices.single { it.name == "Material Symbols Rounded" }
        val text = context.resources.openRawResource(notice.text).bufferedReader().use { it.readText() }
        assertTrue(
            "Material Symbols is missing its copyright",
            text.startsWith("Material Symbols Rounded\nCopyright Google LLC")
        )
        assertTrue(
            "Material Symbols is missing the Apache 2.0 text",
            "Apache License\n                           Version 2.0" in text
        )
        assertTrue("Material Symbols is missing the Apache terms", "END OF TERMS AND CONDITIONS" in text)
    }
}
