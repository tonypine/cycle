package com.tonypine.cycle.feature.settings

import androidx.compose.ui.test.junit4.createComposeRule
import com.tonypine.cycle.core.data.export.ImportProblem
import com.tonypine.cycle.core.model.Language
import com.tonypine.cycle.core.testing.renderIn
import java.time.LocalDate
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/**
 * Why a file can't be imported, in each language: the sentence is translated, and quotes the export's
 * own column names, setting keys, values and ISO dates unchanged, since the file is the same in every
 * language (`docs/decisions/0008-languages.md`).
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class ImportProblemLanguagesTest(private val tag: String) {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `import problems quote the file's own words`() {
        renderIn(Language(tag))
        val sentences = mutableMapOf<ImportProblem, String>()
        composeRule.setContent { Quoted.keys.forEach { sentences[it] = problemSentence(it) } }
        composeRule.waitForIdle()

        Quoted.forEach { (problem, words) ->
            val sentence = sentences.getValue(problem)
            words.forEach { assertTrue("$tag: “$sentence” should quote $it", sentence.contains(it)) }
            assertNotEquals("$tag: not translated", English.getValue(problem), sentence)
        }
    }

    companion object {
        private val Quoted = mapOf(
            ImportProblem.BadDate(6, "02/03/2027") to listOf("6", "02/03/2027", "2027-03-02"),
            ImportProblem.RepeatedDate(7, LocalDate.of(2027, 3, 2)) to listOf("7", "2027-03-02"),
            ImportProblem.UnknownValue(8, "flow", "lots") to listOf("8", "flow", "lots"),
            ImportProblem.BadLength(13, "usual_cycle_length", "12", 15..90) to
                listOf("13", "usual_cycle_length", "12", "15", "90"),
            ImportProblem.RepeatedSetting(14, "usual_period_length") to listOf("14", "usual_period_length"),
            ImportProblem.OneUsualLength to listOf("usual_cycle_length", "usual_period_length")
        )

        private val English = mapOf(
            Quoted.keys.elementAt(0) to "Line 6 has “02/03/2027” as its date. Dates look like 2027-03-02.",
            Quoted.keys.elementAt(1) to "Line 7 is a second line for 2027-03-02.",
            Quoted.keys.elementAt(2) to "Line 8 has “lots” in the flow column, which isn't something Cycle logs there.",
            Quoted.keys.elementAt(3) to
                "Line 13 has “12” as usual_cycle_length. Cycle takes a number of days from 15 to 90.",
            Quoted.keys.elementAt(4) to "Line 14 is a second line for usual_period_length.",
            Quoted.keys.elementAt(5) to
                "This file gives only one of usual_cycle_length and usual_period_length. A Cycle export has both or neither."
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> = Language.Supported.drop(1).map { arrayOf(it.tag) }
    }
}
