package com.tonypine.cycle.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import com.tonypine.cycle.core.data.export.ImportProblem
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** What Settings and Usual cycle and period show, what TalkBack reads, and what each row does. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rGB-w360dp-h800dp-mdpi")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private var state by mutableStateOf(SettingsUiState(28, 5, lastExported = null))
    private var showBackupNote by mutableStateOf(false)

    private fun show() = composeRule.setContent {
        Themed {
            SettingsScreen(
                state = state,
                versionName = "1.4.27",
                onUsualLengths = { calls += "usual lengths" },
                onWhatToLog = { calls += "what to log" },
                onAppLockChange = { on -> calls += "lock $on" },
                onDismissLockNote = { calls += "dismiss lock note" },
                onExport = { calls += "export" },
                onImport = { calls += "import" },
                onConfirmImport = { calls += "confirm import" },
                onDismissDialog = { calls += "dismiss" },
                onDeleteEverything = { calls += "delete everything" },
                onNotices = { calls += "notices" },
                showBackupNote = showBackupNote,
                onHideBackupNote = { calls += "hide backup note" }
            )
        }
    }

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
    private val isSwitch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

    private fun row(title: String) = composeRule.onNode(hasText(title) and isButton).performScrollTo()

    @Test
    fun `the sections are headings, in order, and each row is one button`() {
        show()

        composeRule.onNode(hasText("Settings") and isHeading()).assertExists()
        listOf("Your cycle", "Your data", "About").forEach {
            composeRule.onNode(hasText(it) and isHeading()).assertExists()
        }
        row("Usual cycle and period").assert(hasText("28-day cycle, 5-day period")).performClick()
        row("What to log").performClick()
        row("Export my data").assert(hasText("Save a file with every day you logged")).performClick()
        row("Import from a file").performClick()
        row("Open-source notices").performClick()

        assertEquals(listOf("usual lengths", "what to log", "export", "import", "notices"), calls)
    }

    @Test
    fun `her data stays on the phone, and the app is no contraceptive or diagnosis`() {
        show()

        composeRule.onNodeWithText("Everything stays on this phone").assertIsDisplayed()
        composeRule.onNodeWithText("No account, no cloud, no tracking", substring = true).assertExists()
        composeRule.onNodeWithText("Not a contraceptive, and not a diagnosis.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Version 1.4.27").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `lock cycle is one switch under your data, and a tap asks to change it`() {
        show()

        val lock = composeRule.onNode(hasText("Lock Cycle") and isSwitch).performScrollTo()
        lock.assert(hasText("Open Cycle with your fingerprint, face or screen lock.")).assertIsOff()
        lock.performClick()
        // The switch follows the setting, not the tap: it changes once the phone's prompt says so.
        lock.assertIsOff()

        state = state.copy(appLock = true)
        lock.assertIsOn().performClick()

        assertEquals(listOf("lock true", "lock false"), calls)
    }

    @Test
    fun `a lock turned off for want of a screen lock says why until she dismisses it`() {
        state = state.copy(appLockTurnedOff = true)
        show()

        composeRule.onNodeWithText("Lock Cycle is off").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Your phone has no screen lock now", substring = true).assertExists()
        composeRule.onNodeWithText("Everything you logged is still here", substring = true).assertExists()
        composeRule.onNode(hasText("OK") and isButton).performScrollTo().performClick()

        assertEquals(listOf("dismiss lock note"), calls)
        state = state.copy(appLockTurnedOff = false)
        composeRule.onNodeWithText("Lock Cycle is off").assertDoesNotExist()
    }

    @Test
    fun `with no screen lock on the phone, lock cycle says to set one first`() {
        state = state.copy(dialog = DataDialog.NoScreenLock)
        show()

        composeRule.onNode(hasText("Set a screen lock first") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Add one in your phone's settings", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("OK").performClick()
        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun `without a screen lock, a note says Cycle isn't backed up, why, and that an export keeps a copy`() {
        showBackupNote = true
        show()

        composeRule.onNodeWithText("Cycle isn't backed up").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("only when this phone has a screen lock", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("To keep a copy, export your data.", substring = true).assertIsDisplayed()
        composeRule.onNode(hasText("Hide for now") and isButton).performScrollTo().performClick()

        assertEquals(listOf("hide backup note"), calls)
    }

    @Test
    fun `with a screen lock, there is no note`() {
        show()

        composeRule.onNodeWithText("Everything stays on this phone").assertIsDisplayed()
        composeRule.onNodeWithText("Cycle isn't backed up").assertDoesNotExist()
        composeRule.onNodeWithText("Hide for now").assertDoesNotExist()
    }

    @Test
    fun `after an export or an import, the rows say so`() {
        state = SettingsUiState(28, 5, lastExported = LocalDate.of(2027, 3, 20), importedDays = 12)
        show()

        row("Export my data").assert(hasText("Last exported 20 March 2027"))
        row("Import from a file").assert(hasText("Added 12 days"))
    }

    @Test
    fun `after an import of only methods, the row says how many`() {
        state = SettingsUiState(28, 5, lastExported = null, importedMethods = 2)
        show()

        row("Import from a file").assert(hasText("Added 2 methods"))
    }

    @Test
    fun `delete everything asks first, and Keep it keeps everything`() {
        show()

        row("Delete everything").performClick()
        composeRule.onNode(hasText("Delete everything?") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("This can't be undone", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Keep it").performClick()
        composeRule.waitForIdle()
        assertEquals(emptyList<String>(), calls)

        row("Delete everything").performClick()
        composeRule.onNode(hasText("Delete everything") and isButton and hasAnyAncestor(isDialog())).performClick()
        assertEquals(listOf("delete everything"), calls)
    }

    @Test
    fun `a picked file asks how many days to import`() {
        state = state.copy(dialog = DataDialog.ConfirmImport(newDays = 42))
        show()

        composeRule.onNode(hasText("Import 42 days?") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("Days already logged on this phone stay as they are.").assertIsDisplayed()
        composeRule.onNodeWithText("Import").performClick()
        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(listOf("confirm import", "dismiss"), calls)
    }

    @Test
    fun `a picked file with only the usual lengths asks to import them`() {
        state = state.copy(dialog = DataDialog.ConfirmImport(newDays = 0, restoresLengths = true))
        show()

        composeRule.onNode(hasText("Import your usual lengths?") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("only your usual cycle and period lengths", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Import").performClick()

        assertEquals(listOf("confirm import"), calls)
    }

    @Test
    fun `a picked file with only methods asks to import them, whatever its lengths do`() {
        state = state.copy(dialog = DataDialog.ConfirmImport(newDays = 0, newStretches = 1))
        show()

        composeRule.onNode(hasText("Import 1 method?") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("only contraception methods and their dates", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("usual lengths", substring = true).assertDoesNotExist()

        state = state.copy(dialog = DataDialog.ConfirmImport(newDays = 0, newStretches = 2, restoresLengths = true))
        composeRule.onNode(hasText("Import 2 methods?") and isHeading()).assertIsDisplayed()
        composeRule.onNodeWithText("only contraception methods and your usual", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a malformed file is refused with a sentence that says what is wrong`() {
        val sentences = mapOf(
            ImportProblem.Unreadable to "Cycle couldn't open this file. Try picking it again.",
            ImportProblem.NotText to "This file isn't a Cycle export: it isn't text.",
            ImportProblem.TooLarge to "This file is too large to be a Cycle export.",
            ImportProblem.Empty to "This file is empty.",
            ImportProblem.NotAnExport to
                "This file isn't a Cycle export: its first line doesn't name the columns Cycle writes.",
            ImportProblem.UnclosedQuote(4) to "Line 4 opens a quote that never closes.",
            ImportProblem.WrongValueCount(5, found = 11) to "Line 5 has 11 values instead of 12.",
            ImportProblem.BadDate(
                6,
                "02/03/2027"
            ) to "Line 6 has “02/03/2027” as its date. Dates look like 2027-03-02.",
            ImportProblem.RepeatedDate(7, LocalDate.of(2027, 3, 2)) to "Line 7 is a second line for 2027-03-02.",
            ImportProblem.UnknownValue(8, "flow", "lots") to
                "Line 8 has “lots” in the flow column, which isn't something Cycle logs there.",
            ImportProblem.PainWhereWithoutPain(9) to "Line 9 says where it hurt but not how much.",
            ImportProblem.NoteTooLong(10) to "Line 10 has a note longer than 500 characters.",
            ImportProblem.BadLength(13, "usual_cycle_length", "12", 15..90) to
                "Line 13 has “12” as usual_cycle_length. Cycle takes a number of days from 15 to 90.",
            ImportProblem.RepeatedSetting(14, "usual_period_length") to
                "Line 14 is a second line for usual_period_length.",
            ImportProblem.WrongValueCount(15, found = 3, expected = 2) to "Line 15 has 3 values instead of 2.",
            ImportProblem.OneUsualLength to
                "This file gives only one of usual_cycle_length and usual_period_length. A Cycle export has both or neither."
        )
        state = state.copy(dialog = DataDialog.ImportRefused(ImportProblem.Empty))
        show()

        sentences.forEach { (problem, sentence) ->
            state = state.copy(dialog = DataDialog.ImportRefused(problem))
            composeRule.onNode(hasText("This file can't be imported") and isHeading()).assertIsDisplayed()
            composeRule.onNodeWithText(sentence).assertIsDisplayed()
        }
        composeRule.onNodeWithText("OK").performClick()
        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun `nothing new and a failed export each say so`() {
        state = state.copy(dialog = DataDialog.NothingToImport)
        show()
        composeRule.onNodeWithText("Every day in this file is already on this phone.").assertIsDisplayed()

        state = state.copy(dialog = DataDialog.ExportFailed)
        composeRule.onNodeWithText("Cycle couldn't write to that file", substring = true).assertIsDisplayed()
    }

    @Test
    fun `usual cycle and period starts from what is saved, and minus and plus save a day either way`() {
        val saved = mutableListOf<Pair<Int, Int>>()
        showUsualLengths(saved)
        composeRule.onNodeWithText("Cycle length").assertExists()
        composeRule.onNodeWithText("29 days").assertIsDisplayed()
        composeRule.onNodeWithText("4 days").assertIsDisplayed()
        lengthSlider("Cycle length").assert(hasStateDescription("29 days"))
        // No number to type, and nothing to correct.
        composeRule.onNode(hasSetTextAction()).assertDoesNotExist()

        composeRule.onNodeWithContentDescription("Cycle one day longer").performClick()
        composeRule.onNodeWithContentDescription("Period one day shorter").performScrollTo().performClick()
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        assertEquals(listOf(30 to 3), saved)
    }

    @Test
    fun `dragging the usual lengths saves the ends of their ranges`() {
        val saved = mutableListOf<Pair<Int, Int>>()
        showUsualLengths(saved)
        lengthSlider("Cycle length").performTouchInput { swipeLeft(startX = centerX, endX = left - 100f) }
        lengthSlider("Period length").performScrollTo()
            .performTouchInput { swipeRight(startX = centerX, endX = right + 100f) }
        lengthSlider("Cycle length").assert(hasStateDescription("15 days"))
        lengthSlider("Period length").assert(hasStateDescription("14 days"))
        composeRule.onNodeWithText("Save").performScrollTo().performClick()

        assertEquals(listOf(15 to 14), saved)
    }

    private fun showUsualLengths(saved: MutableList<Pair<Int, Int>>) = composeRule.setContent {
        Themed {
            UsualLengthsScreen(
                UsualLengthsUiState.Editing(29, 4),
                onSave = { cycle, period -> saved += cycle to period },
                onBack = { calls += "back" }
            )
        }
    }

    private fun lengthSlider(label: String) =
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress) and hasContentDescription(label))
}
