package com.tonypine.cycle.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tonypine.cycle.core.designsystem.CycleTextField
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.domain.LengthCheck
import com.tonypine.cycle.core.domain.UsualLengths

/**
 * Her usual cycle and period lengths, as two number fields one above the other, each with what it
 * means under it: setup's second step and Settings' "Usual cycle and period". With [showErrors],
 * which the screen turns on once she saves a value that does not fit, each field says how to fix
 * its value, following what she types from then on.
 */
@Composable
fun UsualLengthFields(
    cycleLength: TextFieldState,
    periodLength: TextFieldState,
    showErrors: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)) {
        val margin = Modifier
            .fillMaxWidth()
            .padding(horizontal = CycleTheme.spacing.large)
        CycleTextField(
            state = cycleLength,
            label = stringResource(R.string.usual_cycle_length),
            modifier = margin,
            supportingText = stringResource(R.string.usual_cycle_length_supporting),
            errorMessage = if (showErrors) {
                lengthError(UsualLengths.checkCycle(cycleLength.text.toString()), R.string.usual_cycle_length_empty)
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            inputTransformation = DaysInput
        )
        CycleTextField(
            state = periodLength,
            label = stringResource(R.string.usual_period_length),
            modifier = margin,
            supportingText = stringResource(R.string.usual_period_length_supporting),
            errorMessage = if (showErrors) {
                lengthError(UsualLengths.checkPeriod(periodLength.text.toString()), R.string.usual_period_length_empty)
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            inputTransformation = DaysInput
        )
    }
}

/** Her two lengths in days, as typed in [cycleLength] and [periodLength], or null if either does not fit. */
fun checkedLengths(cycleLength: TextFieldState, periodLength: TextFieldState): Pair<Int, Int>? {
    val cycle = UsualLengths.checkCycle(cycleLength.text.toString())
    val period = UsualLengths.checkPeriod(periodLength.text.toString())
    return if (cycle is LengthCheck.Valid && period is LengthCheck.Valid) cycle.days to period.days else null
}

/** What to do about [check], or null when it is fine. [empty] asks for a number in this field's words. */
@Composable
private fun lengthError(check: LengthCheck, empty: Int): String? = when (check) {
    is LengthCheck.Valid -> null
    LengthCheck.Empty -> stringResource(empty)
    is LengthCheck.OutOfRange -> stringResource(R.string.usual_length_out_of_range, check.range.first, check.range.last)
}

/** Digits only, up to two: every length she can give fits, and the keyboard has no other keys. */
private val DaysInput = InputTransformation.maxLength(2).then(
    InputTransformation { if (!asCharSequence().all(Char::isDigit)) revertAllChanges() }
)
