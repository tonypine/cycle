package com.tonypine.cycle.core.designsystem

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** An entry of [CycleLegend]: a day cell state, or today. [entries] lists them in reading order. */
enum class CycleLegendEntry(
    @param:StringRes internal val label: Int,
    internal val state: CycleDayState,
    internal val isToday: Boolean = false
) {
    Period(R.string.legend_period, CycleDayState.Period),
    PredictedPeriod(R.string.legend_predicted_period, CycleDayState.PredictedPeriod),
    Fertile(R.string.legend_fertile, CycleDayState.Fertile),
    Ovulation(R.string.legend_ovulation, CycleDayState.Ovulation),
    Today(R.string.legend_today, CycleDayState.Plain, isToday = true);

    companion object {
        /** Period, predicted period and today: the legend for a calendar with fertility estimates off. */
        val WithoutFertility: List<CycleLegendEntry> = listOf(Period, PredictedPeriod, Today)
    }
}

/**
 * The key to the calendar: one swatch per entry, drawn with the same shapes as [DayCell], each with
 * its label. Show only the [entries] the calendar can draw: [CycleLegendEntry.WithoutFertility] when
 * fertility estimates are off, no [CycleLegendEntry.PredictedPeriod] when nothing is predicted. The
 * default is all five. The entries wrap onto more lines when the text is large, and TalkBack reads
 * them as a list of as many items as there are entries.
 *
 * [words] are what the calendar calls its logged days, in order ([BleedingWords]): the
 * [CycleLegendEntry.Period] entry shows once per word, "Period" then "Bleeding" in a month where she
 * started the implant. [predictedWords] name its predicted days: [CycleLegendEntry.PredictedPeriod]
 * reads "Expected bleed" on a combined pill with a break every month, even in a month whose only
 * logged days are a period before it.
 */
@Composable
fun CycleLegend(
    modifier: Modifier = Modifier,
    entries: List<CycleLegendEntry> = CycleLegendEntry.entries,
    words: List<BleedingWords> = listOf(BleedingWords.Period),
    predictedWords: BleedingWords = BleedingWords.Period
) {
    val shown = entries.flatMap { entry ->
        when (entry) {
            CycleLegendEntry.Period -> words.distinct().map { entry to it }
            CycleLegendEntry.PredictedPeriod -> listOf(entry to predictedWords)
            else -> listOf(entry to BleedingWords.Period)
        }
    }
    FlowRow(
        modifier = modifier.semantics { collectionInfo = CollectionInfo(rowCount = shown.size, columnCount = 1) },
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        shown.forEachIndexed { index, (entry, word) ->
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    collectionItemInfo =
                        CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1)
                },
                horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DaySwatch(entry.state, entry.isToday)
                BasicText(
                    stringResource(entry.label(word)),
                    style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant)
                )
            }
        }
    }
}

/** The entry's label with its day cells called [words]. */
@StringRes
private fun CycleLegendEntry.label(words: BleedingWords): Int = when (this) {
    CycleLegendEntry.Period -> when (words) {
        BleedingWords.Period -> label
        BleedingWords.Bleed -> R.string.legend_bleed
        BleedingWords.Bleeding -> R.string.legend_bleeding
    }

    CycleLegendEntry.PredictedPeriod -> when (words) {
        BleedingWords.Period -> label
        BleedingWords.Bleed -> R.string.legend_expected_bleed
        BleedingWords.Bleeding -> R.string.legend_expected_bleeding
    }

    else -> label
}

/** A day cell's shape without its number, for the legend. Decorative: the label next to it says what it is. */
@Composable
private fun DaySwatch(state: CycleDayState, isToday: Boolean) {
    val colors = CycleTheme.colors
    Canvas(Modifier.size(SwatchSize)) { drawDay(state, isToday, colors) }
}

private val SwatchSize = 32.dp

@Preview(name = "Cycle legend · light", widthDp = 360)
@Composable
private fun CycleLegendLightPreview() = PreviewSurface(darkTheme = false) { CycleLegend() }

@Preview(name = "Cycle legend · dark", widthDp = 360)
@Composable
private fun CycleLegendDarkPreview() = PreviewSurface(darkTheme = true) { CycleLegend() }

@Preview(name = "Cycle legend · 200%", widthDp = 360, fontScale = 2f)
@Composable
private fun CycleLegendLargeTextPreview() = PreviewSurface(darkTheme = false) { CycleLegend() }

@Preview(name = "Cycle legend · without fertility · light", widthDp = 360)
@Composable
private fun CycleLegendWithoutFertilityLightPreview() = PreviewSurface(darkTheme = false) {
    CycleLegend(entries = CycleLegendEntry.WithoutFertility)
}

@Preview(name = "Cycle legend · without fertility · dark", widthDp = 360)
@Composable
private fun CycleLegendWithoutFertilityDarkPreview() = PreviewSurface(darkTheme = true) {
    CycleLegend(entries = CycleLegendEntry.WithoutFertility)
}

@Preview(name = "Cycle legend · bleed · light", widthDp = 360)
@Composable
private fun CycleLegendBleedPreview() = PreviewSurface(darkTheme = false) {
    CycleLegend(
        entries = CycleLegendEntry.WithoutFertility,
        words = listOf(BleedingWords.Bleed),
        predictedWords = BleedingWords.Bleed
    )
}

@Preview(name = "Cycle legend · period then expected bleed · light", widthDp = 360)
@Composable
private fun CycleLegendPeriodThenExpectedBleedPreview() = PreviewSurface(darkTheme = false) {
    CycleLegend(
        entries = CycleLegendEntry.WithoutFertility,
        words = listOf(BleedingWords.Period),
        predictedWords = BleedingWords.Bleed
    )
}

@Preview(name = "Cycle legend · period then bleeding · light", widthDp = 360)
@Composable
private fun CycleLegendPeriodThenBleedingPreview() = PreviewSurface(darkTheme = false) {
    CycleLegend(
        entries = listOf(CycleLegendEntry.Period, CycleLegendEntry.Today),
        words = listOf(BleedingWords.Period, BleedingWords.Bleeding)
    )
}
