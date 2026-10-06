package com.tonypine.cycle

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.app.ActivityOptionsCompat
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.FlowLevel
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Settings' "Your data" and "Your cycle" in the app, on her phone's storage, as the ticket's
 * walkthrough: export, delete everything, restore from the welcome, and a new usual cycle length.
 * Android's save screen and file picker are replaced by one file in a temporary folder. Synthetic
 * data only: made-up periods in 2027, never anyone's real cycle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rGB-w360dp-h800dp")
class YourDataJourneyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val folder = TemporaryFolder()

    private val data get() = (composeRule.activity.application as CycleApplication).data
    private val isTab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    private fun tab(label: String): SemanticsNodeInteraction = composeRule.onNode(hasText(label) and isTab)

    // Settings shows once its settings and her methods are read.
    private fun row(title: String): SemanticsNodeInteraction {
        waitFor(hasText(title) and isButton)
        return composeRule.onNode(hasText(title) and isButton).performScrollTo()
    }

    private fun dialogButton(text: String) = composeRule.onNode(hasText(text) and hasAnyAncestor(isDialog()))

    private fun waitFor(matcher: SemanticsMatcher) =
        composeRule.waitUntil(WAIT_MILLIS) { composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForText(text: String) = waitFor(hasText(text, substring = true))

    /** Android's save screen and file picker, both answering with [file]. */
    private class OneFile(private val file: File) : ActivityResultRegistry() {
        val asked = mutableListOf<Any?>()

        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            asked += input
            dispatchResult(requestCode, Uri.fromFile(file))
        }
    }

    private fun showApp(periods: List<Pair<String, Int>>, today: LocalDate, files: OneFile) {
        runBlocking {
            periods.forEach { (start, length) ->
                (0 until length).forEach {
                    data.dayLogRepository.save(DayLog(LocalDate.parse(start).plusDays(it.toLong()), FlowLevel.MEDIUM))
                }
            }
        }
        val owner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry: ActivityResultRegistry = files
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner) {
                CycleTheme(reduceMotion = true) { CycleApp(data, FakeDeviceLock(), today = { today }) }
            }
        }
        // Her logs skip the first-run welcome; the tabs show once the settings are read.
        waitFor(hasText("Settings") and isTab)
    }

    @Test
    fun `export, delete everything and restore bring Today and History back as before`() {
        val file = File(folder.root, "cycle-export-2027-09-10.csv")
        val files = OneFile(file)
        showApp(listOf("2027-07-09" to 5, "2027-08-05" to 6, "2027-09-02" to 4), LocalDate.of(2027, 9, 10), files)
        runBlocking { data.settingsRepository.saveSetup(cycleLength = 30, periodLength = 5) }
        waitForText("Day 9")
        val logsBefore = runBlocking { data.dayLogRepository.observeDayLogs().first() }
        val settingsBefore = runBlocking { data.settingsRepository.settings.first() }

        // 1. Settings shows its sections.
        tab("Settings").performClick()
        listOf("Your cycle", "Your data", "About").forEach { waitFor(hasText(it) and isHeading()) }
        composeRule.onNodeWithText("Everything stays on this phone").assertExists()

        // 2. Export my data: the save screen suggests today's name, and the row says today.
        row("Export my data").performClick()
        waitForText("Last exported 10 September 2027")
        assertEquals("cycle-export-2027-09-10.csv", files.asked.single())
        // The header, 15 days, a blank line and the usual lengths' table.
        assertEquals(20, file.readLines().size)

        // 3. Delete everything, confirmed: the welcome.
        row("Delete everything").performClick()
        dialogButton("Delete everything").performClick()
        waitForText("Get started")
        assertEquals(emptyList<DayLog>(), runBlocking { data.dayLogRepository.observeDayLogs().first() })

        // 4. Restore from a Cycle export, the file, Import: Today and History as before.
        composeRule.onNodeWithText("Restore from a Cycle export").performScrollTo().performClick()
        waitFor(hasText("Import 15 days?") and isHeading())
        dialogButton("Import").performClick()
        waitForText("Day 9")
        tab("Today").assertExists()
        assertEquals(logsBefore, runBlocking { data.dayLogRepository.observeDayLogs().first() })
        assertEquals(settingsBefore, runBlocking { data.settingsRepository.settings.first() })
        tab("History").performClick()
        waitForText("Your typical cycle")
        tab("Settings").performClick()
        waitForText("30-day cycle, 5-day period")
    }

    @Test
    fun `a new usual cycle length moves the estimate that uses it`() {
        showApp(listOf("2027-09-02" to 4), LocalDate.of(2027, 9, 10), OneFile(File(folder.root, "unused.csv")))
        waitForText("Around 30 September")
        composeRule.onNodeWithText("Estimated from a typical 28-day cycle", substring = true).performScrollTo()

        // 5. Usual cycle and period: 30 days.
        tab("Settings").performClick()
        row("Usual cycle and period").performClick()
        waitForText("28 days")
        repeat(2) { composeRule.onNodeWithContentDescription("Cycle one day longer").performClick() }
        composeRule.onNodeWithText("30 days").assertIsDisplayed()
        composeRule.onNodeWithText("Save").performScrollTo().performClick()
        waitForText("30-day cycle, 5-day period")

        tab("Today").performClick()
        waitForText("Around 2 October")
        composeRule.onNodeWithText("Estimated from the lengths you gave", substring = true).performScrollTo()
        assertTrue(runBlocking { data.settingsRepository.settings.first().setupDone })
    }

    private companion object {
        // Room and DataStore write on their own threads.
        const val WAIT_MILLIS = 5_000L
    }
}
