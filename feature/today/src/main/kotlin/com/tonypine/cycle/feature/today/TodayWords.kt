package com.tonypine.cycle.feature.today

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tonypine.cycle.core.model.BleedBasis
import com.tonypine.cycle.core.model.BleedingSummary
import com.tonypine.cycle.core.model.BleedingWord
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
import com.tonypine.cycle.core.ui.formatDate

/*
 * Today's words on a method, from `docs/design/contraception.md`: "period", "bleed" or "bleeding"
 * ([BleedingWord]), and each method's lines. Each follows [ContraceptionMethod], so a new method
 * fails to compile here until it has its words.
 */

/** The one-tap button that starts it: "My period started", "Bleed started", "Bleeding started". */
@StringRes
internal fun BleedingWord.startButton(): Int = when (this) {
    BleedingWord.PERIOD -> R.string.today_period_started
    BleedingWord.BLEED -> R.string.today_bleed_started
    BleedingWord.BLEEDING -> R.string.today_bleeding_started
}

/** The one-tap button that ends it: "My period ended", "Bleed ended", "Bleeding stopped". */
@StringRes
internal fun BleedingWord.endButton(): Int = when (this) {
    BleedingWord.PERIOD -> R.string.today_period_ended
    BleedingWord.BLEED -> R.string.today_bleed_ended
    BleedingWord.BLEEDING -> R.string.today_bleeding_stopped
}

/** The Undo card after a start: "Period started today". */
@StringRes
internal fun BleedingWord.startedCard(): Int = when (this) {
    BleedingWord.PERIOD -> R.string.today_started_card
    BleedingWord.BLEED -> R.string.today_bleed_started_card
    BleedingWord.BLEEDING -> R.string.today_bleeding_started_card
}

/** The Undo card after an end: "Period ended today". */
@StringRes
internal fun BleedingWord.endedCard(): Int = when (this) {
    BleedingWord.PERIOD -> R.string.today_ended_card
    BleedingWord.BLEED -> R.string.today_bleed_ended_card
    BleedingWord.BLEEDING -> R.string.today_bleeding_stopped_card
}

/** The context line on its first day: "Your period started today". */
@StringRes
internal fun BleedingWord.startedLine(): Int = when (this) {
    BleedingWord.PERIOD -> R.string.today_context_started
    BleedingWord.BLEED -> R.string.today_context_bleed_started
    BleedingWord.BLEEDING -> R.string.today_context_bleeding_started
}

/** The context line while it lasts: "Day 2 of your period", "Day 2 of bleeding". */
@StringRes
internal fun BleedingWord.dayLine(): Int = when (this) {
    BleedingWord.PERIOD -> R.string.today_context_on_period
    BleedingWord.BLEED -> R.string.today_context_on_bleed
    BleedingWord.BLEEDING -> R.string.today_context_on_bleeding
}

/** The context line on the day it ended: "Your period lasted 5 days". */
@PluralsRes
internal fun BleedingWord.endedLine(): Int = when (this) {
    BleedingWord.PERIOD -> R.plurals.today_context_ended
    BleedingWord.BLEED -> R.plurals.today_context_bleed_ended
    BleedingWord.BLEEDING -> R.plurals.today_context_bleeding_ended
}

/** "Still going?", or "Still bleeding?" on a method. */
@StringRes
internal fun BleedingWord.stillGoingTitle(): Int =
    if (this == BleedingWord.PERIOD) R.string.today_still_going_title else R.string.today_still_bleeding_title

/** "Your period has lasted 8 days so far." */
@PluralsRes
internal fun BleedingWord.stillGoingBody(): Int = when (this) {
    BleedingWord.PERIOD -> R.plurals.today_still_going_body
    BleedingWord.BLEED -> R.plurals.today_still_going_bleed_body
    BleedingWord.BLEEDING -> R.plurals.today_still_going_bleeding_body
}

/** "Pick the last day of your period." */
@StringRes
internal fun BleedingWord.lastDayBody(): Int = when (this) {
    BleedingWord.PERIOD -> R.string.today_last_day_body
    BleedingWord.BLEED -> R.string.today_last_day_bleed_body
    BleedingWord.BLEEDING -> R.string.today_last_day_bleeding_body
}

/** Under the days since she stopped [method]: "since your implant came out". */
@Composable
internal fun sinceLine(method: ContraceptionMethod): String = stringResource(
    when (method) {
        COMBINED_PILL -> R.string.today_since_pill
        PROGESTOGEN_PILL -> R.string.today_since_mini_pill
        PATCH -> R.string.today_since_patch
        RING -> R.string.today_since_ring
        IMPLANT -> R.string.today_since_implant
        HORMONAL_IUD, COPPER_IUD -> R.string.today_since_iud
        INJECTION -> R.string.today_since_injection
    }
)

/**
 * The next bleed's basis: "In your first pill break. Estimated from the day you started the pill.",
 * or "In your pill break" once the first break passed with no bleed logged.
 */
@Composable
internal fun bleedBasisLine(next: NextBleed): String = stringResource(
    when (next.basis) {
        BleedBasis.START_DATE -> when {
            next.method == PATCH && next.missedBreak -> R.string.today_bleed_basis_start_later_patch
            next.method == PATCH -> R.string.today_bleed_basis_start_patch
            next.method == RING && next.missedBreak -> R.string.today_bleed_basis_start_later_ring
            next.method == RING -> R.string.today_bleed_basis_start_ring
            next.missedBreak -> R.string.today_bleed_basis_start_later_pill
            else -> R.string.today_bleed_basis_start_pill
        }

        BleedBasis.LAST_BLEED -> when (next.method) {
            PATCH -> R.string.today_bleed_basis_last_patch
            RING -> R.string.today_bleed_basis_last_ring
            else -> R.string.today_bleed_basis_last_pill
        }
    }
)

/** The break a combined [method] bleeds in: "break", "patch-free week", "ring-free week". */
@Composable
internal fun breakName(method: ContraceptionMethod): String = stringResource(
    when (method) {
        PATCH -> R.string.bleed_break_patch
        RING -> R.string.bleed_break_ring
        else -> R.string.bleed_break_pill
    }
)

/** The first-months line for [method]: bleeding is often unsettled at first, and usually settles. */
@Composable
internal fun firstMonthsLine(method: ContraceptionMethod, breaks: Breaks?): String? = when (method) {
    COMBINED_PILL, PATCH, RING -> if (breaks == Breaks.MONTHLY) {
        R.string.today_first_months_combined_monthly
    } else {
        R.string.today_first_months_combined
    }

    PROGESTOGEN_PILL -> R.string.today_first_months_mini_pill

    IMPLANT -> R.string.today_first_months_implant

    HORMONAL_IUD -> R.string.today_first_months_hormonal_iud

    INJECTION -> R.string.today_first_months_injection

    // The copper IUD has its own card.
    COPPER_IUD -> null
}?.let { stringResource(it) }

/** "What changes on the implant?": the button that opens [method]'s sheet. */
@Composable
internal fun methodSheetButton(method: ContraceptionMethod): String = stringResource(
    when (method) {
        COMBINED_PILL -> R.string.method_sheet_button_pill
        PROGESTOGEN_PILL -> R.string.method_sheet_button_mini_pill
        PATCH -> R.string.method_sheet_button_patch
        RING -> R.string.method_sheet_button_ring
        IMPLANT -> R.string.method_sheet_button_implant
        HORMONAL_IUD, COPPER_IUD -> R.string.method_sheet_button_hormonal_iud
        INJECTION -> R.string.method_sheet_button_injection
    }
)

/** "Bleeding on the implant": the sheet's title. */
@Composable
internal fun methodSheetTitle(method: ContraceptionMethod): String = stringResource(
    when (method) {
        COMBINED_PILL -> R.string.method_sheet_title_pill
        PROGESTOGEN_PILL -> R.string.method_sheet_title_mini_pill
        PATCH -> R.string.method_sheet_title_patch
        RING -> R.string.method_sheet_title_ring
        IMPLANT -> R.string.method_sheet_title_implant
        HORMONAL_IUD, COPPER_IUD -> R.string.method_sheet_title_hormonal_iud
        INJECTION -> R.string.method_sheet_title_injection
    }
)

/** "Last 90 days", or "Since 6 September" while the method is younger than that. */
@Composable
internal fun bleedingTitle(summary: BleedingSummary): String = if (summary.sinceStart) {
    stringResource(R.string.today_bleeding_since, formatDate(summary.from))
} else {
    stringResource(R.string.today_bleeding_last_90_days)
}

/** "You logged bleeding or spotting on 11 days, in 3 episodes. The longest lasted 6 days." */
@Composable
internal fun bleedingCounts(summary: BleedingSummary): String {
    if (summary.days == 0) return stringResource(R.string.today_bleeding_none)
    val episodes = pluralStringResource(R.plurals.today_bleeding_episodes, summary.episodes, summary.episodes)
    return pluralStringResource(R.plurals.today_bleeding_days, summary.days, summary.days, episodes) + " " +
        pluralStringResource(R.plurals.today_bleeding_longest, summary.longest, summary.longest)
}
