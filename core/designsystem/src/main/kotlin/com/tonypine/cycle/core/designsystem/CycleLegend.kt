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

/** The legend's entries, in reading order: each day cell state, and today. */
private enum class LegendEntry(@StringRes val label: Int, val state: CycleDayState, val isToday: Boolean = false) {
    Period(R.string.legend_period, CycleDayState.Period),
    PredictedPeriod(R.string.legend_predicted_period, CycleDayState.PredictedPeriod),
    Fertile(R.string.legend_fertile, CycleDayState.Fertile),
    Ovulation(R.string.legend_ovulation, CycleDayState.Ovulation),
    Today(R.string.legend_today, CycleDayState.Plain, isToday = true)
}

/**
 * The key to the calendar: one swatch per cycle state (period, predicted period, fertile window,
 * ovulation, today), drawn with the same shapes as [DayCell], each with its label. The entries wrap
 * onto more lines when the text is large, and TalkBack reads them as a list of five.
 */
@Composable
fun CycleLegend(modifier: Modifier = Modifier) {
    val entries = LegendEntry.entries
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
