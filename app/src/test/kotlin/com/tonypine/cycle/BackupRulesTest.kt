package com.tonypine.cycle

import android.content.res.XmlResourceParser
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Her log and settings reach Google's backup only end-to-end encrypted
 * (`docs/decisions/0004-backup-encryption.md`), and every file they are written to is in the backup.
 */
@RunWith(AndroidJUnit4::class)
class BackupRulesTest {
    private val app = ApplicationProvider.getApplicationContext<CycleApplication>()

    @Test
    fun `on Android 12 and later, the cloud backup needs end-to-end encryption`() {
        val cloudBackup = rules(R.xml.data_extraction_rules).single { it.name == "cloud-backup" }

        assertEquals("true", cloudBackup.attributes["disableIfNoEncryptionCapabilities"])
    }

    @Test
    fun `on Android 10 and 11, every file in the backup needs end-to-end encryption`() {
        val includes = rules(R.xml.backup_rules).filter { it.name == "include" }

        assertTrue(includes.isNotEmpty())
        includes.forEach { assertEquals("clientSideEncryption", it.attributes["requireFlags"]) }
    }

    @Test
    fun `every file her log and settings are written to is backed up`() = runTest {
        app.data.dayLogRepository.setPeriodStarted(LocalDate.of(2001, 1, 1), true)
        app.data.settingsRepository.setSetupDone(true)
        val written = listOf(app.getDatabasePath("any").parentFile!!, app.filesDir)
            .flatMap { dir -> dir.walk().filter { it.isFile }.toList() }
        assertTrue(written.any { it.name == "cycle.db" })
        assertTrue(written.any { it.parentFile?.name == "datastore" })

        val extractionRules = rules(R.xml.data_extraction_rules)
        val includeSets = mapOf(
            "cloud-backup" to extractionRules.filter { it.name == "include" && it.parent == "cloud-backup" },
            "device-transfer" to extractionRules.filter { it.name == "include" && it.parent == "device-transfer" },
            "full-backup-content" to rules(R.xml.backup_rules).filter { it.name == "include" }
        )
        includeSets.forEach { (section, includes) ->
            val missed = written.filterNot { file -> includes.any { it.covers(file) } }
            assertEquals("not in $section", emptyList<File>(), missed)
        }
    }

    private fun Rule.covers(file: File): Boolean {
        val domainDir = when (attributes["domain"]) {
            "database" -> app.getDatabasePath("any").parentFile!!
            "file" -> app.filesDir
            else -> return false
        }
        val included = domainDir.resolve(attributes.getValue("path")).normalize()
        return file.normalize().startsWith(included)
    }

    private data class Rule(val name: String, val parent: String?, val attributes: Map<String, String>)

    /** Every element of an XML resource, with the name of the element it sits in. */
    private fun rules(id: Int): List<Rule> {
        val parser = app.resources.getXml(id)
        val rules = mutableListOf<Rule>()
        val open = ArrayDeque<String>()
        while (parser.next() != XmlResourceParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlResourceParser.START_TAG -> {
                    val attributes = (0 until parser.attributeCount)
                        .associate { parser.getAttributeName(it) to parser.getAttributeValue(it) }
                    rules += Rule(parser.name, open.lastOrNull(), attributes)
                    open.addLast(parser.name)
                }

                XmlResourceParser.END_TAG -> open.removeLast()
            }
        }
        parser.close()
        return rules
    }
}
