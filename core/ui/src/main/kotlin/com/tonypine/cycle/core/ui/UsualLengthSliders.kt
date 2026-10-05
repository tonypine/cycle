package com.tonypine.cycle.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.SliderField
import com.tonypine.cycle.core.domain.CycleRules

/**
 * Her usual cycle and period lengths, as two slider fields one above the other, each with what it
 * means under it: setup's second step and Settings' "Usual cycle and period". Each shows its value as
 * "28 days", moves a day at a time with − and +, and stays within the lengths she can give, so there
 * is nothing to correct.
 */
@Composable
fun UsualLengthSliders(
    cycleLength: Int,
    onCycleLengthChange: (Int) -> Unit,
    periodLength: Int,
    onPeriodLengthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(horizontal = CycleTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
    ) {
        SliderField(
            label = stringResource(R.string.usual_cycle_length),
            value = cycleLength,
            onValueChange = onCycleLengthChange,
            valueRange = CycleRules.USUAL_CYCLE_LENGTHS,
            valueText = pluralStringResource(R.plurals.usual_length_days, cycleLength, cycleLength),
            decreaseDescription = stringResource(R.string.usual_cycle_length_shorter),
            increaseDescription = stringResource(R.string.usual_cycle_length_longer),
            supportingText = stringResource(R.string.usual_cycle_length_supporting)
        )
        SliderField(
            label = stringResource(R.string.usual_period_length),
            value = periodLength,
            onValueChange = onPeriodLengthChange,
            valueRange = CycleRules.USUAL_PERIOD_LENGTHS,
            valueText = pluralStringResource(R.plurals.usual_length_days, periodLength, periodLength),
            decreaseDescription = stringResource(R.string.usual_period_length_shorter),
            increaseDescription = stringResource(R.string.usual_period_length_longer),
            supportingText = stringResource(R.string.usual_period_length_supporting)
        )
    }
}
