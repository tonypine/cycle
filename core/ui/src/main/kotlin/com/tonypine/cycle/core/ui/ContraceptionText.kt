package com.tonypine.cycle.core.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tonypine.cycle.core.designsystem.RadioGroup
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionMethod.COMBINED_PILL
import com.tonypine.cycle.core.model.ContraceptionMethod.COPPER_IUD
import com.tonypine.cycle.core.model.ContraceptionMethod.HORMONAL_IUD
import com.tonypine.cycle.core.model.ContraceptionMethod.IMPLANT
import com.tonypine.cycle.core.model.ContraceptionMethod.INJECTION
import com.tonypine.cycle.core.model.ContraceptionMethod.PATCH
import com.tonypine.cycle.core.model.ContraceptionMethod.PROGESTOGEN_PILL
import com.tonypine.cycle.core.model.ContraceptionMethod.RING
import com.tonypine.cycle.core.model.ContraceptionStretch

/*
 * Her contraception in words, for setup and Settings alike: the method list, since when, breaks and
 * the calm line, from `docs/design/contraception.md`. Each method's words follow
 * [ContraceptionMethod], the one list of methods, so a new method fails to compile here until it has
 * its words.
 */

/** A row of the method list: None, or a method. */
sealed interface MethodChoice {
    data object None : MethodChoice

    data class Method(val method: ContraceptionMethod) : MethodChoice
}

/** The method list in its order: None, then every method. */
val MethodChoices: List<MethodChoice> =
    listOf(MethodChoice.None) + ContraceptionMethod.entries.map(MethodChoice::Method)

/**
 * "Which method?": one [com.tonypine.cycle.core.designsystem.RadioRow] per [MethodChoices], each
 * with its line ("A rod in the arm, such as Nexplanon"), nothing chosen while [selected] is null.
 */
@Composable
fun MethodList(selected: MethodChoice?, onSelect: (MethodChoice) -> Unit, modifier: Modifier = Modifier) {
    RadioGroup(
        options = MethodChoices,
        selected = selected,
        onSelect = onSelect,
        title = { choice ->
            when (choice) {
                MethodChoice.None -> stringResource(R.string.method_none)
                is MethodChoice.Method -> methodTitle(choice.method)
            }
        },
        modifier = modifier,
        body = { choice -> stringResource(choice.line) }
    )
}

/** "Do you take a break between packs?" on a combined [method]: every month, every few packs or none. */
@Composable
fun BreaksList(
    method: ContraceptionMethod,
    selected: Breaks?,
    onSelect: (Breaks) -> Unit,
    modifier: Modifier = Modifier
) {
    RadioGroup(
        options = Breaks.entries,
        selected = selected,
        onSelect = onSelect,
        title = { stringResource(it.title) },
        modifier = modifier,
        body = { stringResource(breaksLine(method, it)) }
    )
}

/** Its name in the method list: "Combined pill". */
@Composable
fun methodTitle(method: ContraceptionMethod): String = stringResource(
    when (method) {
        COMBINED_PILL -> R.string.method_combined_pill
        PROGESTOGEN_PILL -> R.string.method_progestogen_pill
        PATCH -> R.string.method_patch
        RING -> R.string.method_ring
        IMPLANT -> R.string.method_implant
        HORMONAL_IUD -> R.string.method_hormonal_iud
        COPPER_IUD -> R.string.method_copper_iud
        INJECTION -> R.string.method_injection
    }
)

/** Its short name, for Today and History: "Pill". */
@Composable
fun methodShortName(method: ContraceptionMethod): String = stringResource(
    when (method) {
        COMBINED_PILL -> R.string.method_short_combined_pill
        PROGESTOGEN_PILL -> R.string.method_short_progestogen_pill
        PATCH -> R.string.method_short_patch
        RING -> R.string.method_short_ring
        IMPLANT -> R.string.method_short_implant
        HORMONAL_IUD -> R.string.method_short_hormonal_iud
        COPPER_IUD -> R.string.method_short_copper_iud
        INJECTION -> R.string.method_short_injection
    }
)

/** Its short name inside a sentence: "pill", as in "Move the end of your pill?". */
@Composable
fun methodInSentence(method: ContraceptionMethod): String = stringResource(
    when (method) {
        COMBINED_PILL -> R.string.method_sentence_combined_pill
        PROGESTOGEN_PILL -> R.string.method_sentence_progestogen_pill
        PATCH -> R.string.method_sentence_patch
        RING -> R.string.method_sentence_ring
        IMPLANT -> R.string.method_sentence_implant
        HORMONAL_IUD -> R.string.method_sentence_hormonal_iud
        COPPER_IUD -> R.string.method_sentence_copper_iud
        INJECTION -> R.string.method_sentence_injection
    }
)

/** Its full name inside a sentence: "combined pill", as in "Your combined pill would end on 5 September". */
@Composable
fun methodInFull(method: ContraceptionMethod): String = stringResource(
    when (method) {
        COMBINED_PILL -> R.string.method_full_combined_pill
        PROGESTOGEN_PILL -> R.string.method_full_progestogen_pill
        PATCH -> R.string.method_full_patch
        RING -> R.string.method_full_ring
        IMPLANT -> R.string.method_full_implant
        HORMONAL_IUD -> R.string.method_full_hormonal_iud
        COPPER_IUD -> R.string.method_full_copper_iud
        INJECTION -> R.string.method_full_injection
    }
)

/** "When did you start the pill?": the question for its start date. */
@Composable
fun sinceWhenTitle(method: ContraceptionMethod): String = stringResource(
    when (method) {
        COMBINED_PILL -> R.string.method_since_combined_pill
        PROGESTOGEN_PILL -> R.string.method_since_progestogen_pill
        PATCH -> R.string.method_since_patch
        RING -> R.string.method_since_ring
        IMPLANT -> R.string.method_since_implant
        HORMONAL_IUD, COPPER_IUD -> R.string.method_since_iud
        INJECTION -> R.string.method_since_injection
    }
)

/** "When did you take your last pill?": the question when she marks it as stopped. */
@Composable
fun stopTitle(method: ContraceptionMethod): String = stringResource(
    when (method) {
        COMBINED_PILL, PROGESTOGEN_PILL -> R.string.method_stop_pill
        PATCH -> R.string.method_stop_patch
        RING -> R.string.method_stop_ring
        IMPLANT -> R.string.method_stop_implant
        HORMONAL_IUD, COPPER_IUD -> R.string.method_stop_iud
        INJECTION -> R.string.method_stop_injection
    }
)

/** The line under [stopTitle]: "Roughly is fine.", or on the injection that Cycle counts 13 weeks. */
@Composable
fun stopBody(method: ContraceptionMethod): String =
    stringResource(if (method == INJECTION) R.string.method_stop_injection_body else R.string.method_since_body)

/** "Do you take a break between packs?", or the patch's and the ring's own question. */
@Composable
fun breaksQuestion(method: ContraceptionMethod): String = stringResource(
    when (method) {
        PATCH -> R.string.breaks_question_patch
        RING -> R.string.breaks_question_ring
        else -> R.string.breaks_question_pill
    }
)

/** "Every month", "Every few packs" or "No breaks". */
@Composable
fun breaksTitle(breaks: Breaks): String = stringResource(breaks.title)

/**
 * The calm line: one sentence on what Cycle estimates on [method] with [breaks], and why. With no
 * method, that Cycle estimates from her own cycle.
 */
@Composable
fun calmLine(method: ContraceptionMethod?, breaks: Breaks?): String = stringResource(
    when (method) {
        null -> R.string.calm_none

        COPPER_IUD -> R.string.calm_copper_iud

        PROGESTOGEN_PILL -> R.string.calm_progestogen_pill

        IMPLANT -> R.string.calm_implant

        HORMONAL_IUD -> R.string.calm_hormonal_iud

        INJECTION -> R.string.calm_injection

        COMBINED_PILL, PATCH, RING -> when (breaks) {
            Breaks.EVERY_FEW_PACKS -> R.string.calm_every_few_packs

            Breaks.NONE -> R.string.calm_no_breaks

            Breaks.MONTHLY, null -> when (method) {
                PATCH -> R.string.calm_patch_monthly
                RING -> R.string.calm_ring_monthly
                else -> R.string.calm_pill_monthly
            }
        }
    }
)

@get:StringRes
private val MethodChoice.line: Int
    get() = when (this) {
        MethodChoice.None -> R.string.method_none_line

        is MethodChoice.Method -> when (method) {
            COMBINED_PILL -> R.string.method_combined_pill_line
            PROGESTOGEN_PILL -> R.string.method_progestogen_pill_line
            PATCH -> R.string.method_patch_line
            RING -> R.string.method_ring_line
            IMPLANT -> R.string.method_implant_line
            HORMONAL_IUD -> R.string.method_hormonal_iud_line
            COPPER_IUD -> R.string.method_copper_iud_line
            INJECTION -> R.string.method_injection_line
        }
    }

@get:StringRes
private val Breaks.title: Int
    get() = when (this) {
        Breaks.MONTHLY -> R.string.breaks_monthly
        Breaks.EVERY_FEW_PACKS -> R.string.breaks_every_few_packs
        Breaks.NONE -> R.string.breaks_none
    }

@StringRes
private fun breaksLine(method: ContraceptionMethod, breaks: Breaks): Int = when (breaks) {
    Breaks.MONTHLY -> when (method) {
        PATCH -> R.string.breaks_monthly_patch_line
        RING -> R.string.breaks_monthly_ring_line
        else -> R.string.breaks_monthly_pill_line
    }

    Breaks.EVERY_FEW_PACKS -> R.string.breaks_every_few_packs_line

    Breaks.NONE -> R.string.breaks_none_line
}

/**
 * A stretch's dates, short: "Since 13 Sep 2027" while she is on it, "3 May to 12 Sep 2027" (the year
 * once when both share it) once it has a stop date, and with no start date "Start not known", or
 * "Until 3 Nov 2027" once it has stopped.
 */
@Composable
fun stretchDates(stretch: ContraceptionStretch): String {
    val started = stretch.started
    val stopped = stretch.stopped
    return when {
        stopped == null ->
            started
                ?.let { stringResource(R.string.stretch_since, formatDate(it, DAY_SHORT_MONTH_AND_YEAR)) }
                ?: stringResource(R.string.stretch_start_not_known)

        started == null -> stringResource(R.string.stretch_until, formatDate(stopped, DAY_SHORT_MONTH_AND_YEAR))

        else -> stringResource(
            R.string.stretch_from_to,
            formatDate(started, if (started.year == stopped.year) DAY_AND_SHORT_MONTH else DAY_SHORT_MONTH_AND_YEAR),
            formatDate(stopped, DAY_SHORT_MONTH_AND_YEAR)
        )
    }
}
