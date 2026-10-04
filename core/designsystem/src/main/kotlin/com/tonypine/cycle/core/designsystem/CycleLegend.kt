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
 * fertility estimates are off. The default is all five. The entries wrap onto more lines when the text
 * is large, and TalkBack reads them as a list of as many items as there are entries.
 */
@Composable
fun CycleLegend(modifier: Modifier = Modifier, entries: List<CycleLegendEntry> = CycleLegendEntry.entries) {
    FlowRow(
        modifier = modifier.semantics { collectionInfo = CollectionInfo(rowCount = entries.size, columnCount = 1) },
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        entries.forEachIndexed { index, entry ->
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
                    stringResource(entry.label),
                    style = CycleTheme.typography.bodySmall.copy(color = CycleTheme.colors.onSurfaceVariant)
                )
            }
        }
    }
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
