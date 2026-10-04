package com.tonypine.cycle.core.designsystem

import android.text.format.DateFormat
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Where a day sits in the cycle. Today, selection and disabled are separate, and combine with every state. */
enum class CycleDayState {
    /** No cycle event: the number on its own. */
    Plain,

    /** A logged period day: a solid `period` squircle. */
    Period,

    /** A predicted period day: a pale `predicted` squircle with a dashed `predictedEdge`. */
    PredictedPeriod,

    /** A day in the fertile window: a `fertile` circle with an `ovulation` marker dot. */
    Fertile,

    /** The ovulation day: a soft eight-point `ovulation` sun. */
    Ovulation
}

/**
 * One day of the calendar, drawn in Zest's shape for its cycle [state] so no state relies on colour
 * alone: a squircle for period days (dashed when predicted), a circle with a dot for the fertile
 * window, a sun for ovulation. [isToday] adds a `today` ring and a bolder number, [selected] a 2dp
 * `accent` rounded-square frame, and a disabled day (outside the month, or in the future) draws at
 * the disabled alpha and ignores taps.
 *
 * When [state] becomes [CycleDayState.Period], the shape morphs from the plain circle to the period
 * squircle on the spatial spring, a small celebration of logging the day; under reduce motion it
 * jumps to the squircle.
 *
 * The cell is at least 48dp square and grows with large text, so its shape always holds the number.
 * It is a `Role.Button` that exposes [selected], and reads its date and state to TalkBack
 * ("20 March, today, period").
 */
@Composable
fun DayCell(
    date: LocalDate,
    state: CycleDayState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isToday: Boolean = false,
    selected: Boolean = false,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = CycleTheme.colors
    val tile = CycleTheme.shapes.medium
    val periodShape = animatedMorphShape(
        start = CyclePolygons.circle,
        end = CyclePolygons.squircle,
        atEnd = state == CycleDayState.Period
    )
    val description = dayDescription(date, state, isToday)
    Box(
        modifier = modifier
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = cycleIndication(tile),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = description }
            .sizeIn(minWidth = DayCellMinSize, minHeight = DayCellMinSize)
            .alpha(if (enabled) 1f else CycleTheme.stateAlpha.disabledContent)
            .drawBehind {
                if (selected) drawSelectionFrame(tile, colors.accent)
                drawDay(state, isToday, colors, periodShape)
            },
        contentAlignment = Alignment.Center
    ) {
        val typography = CycleTheme.typography
        BasicText(
            text = dayNumber(date),
            modifier = Modifier
                .clearAndSetSemantics {}
                .growCellToFit(),
            style = (if (isToday) typography.dayNumberEmphasized else typography.dayNumber)
                .copy(color = state.numberColor(colors)),
            softWrap = false,
            maxLines = 1
        )
    }
}

private val DayCellMinSize = 48.dp

/**
 * Lays the number out in a square big enough for the shape to hold it, so with large text the cell
 * grows past 48dp instead of the number spilling over its shape.
 */
private fun Modifier.growCellToFit() = layout { measurable, constraints ->
    val number = measurable.measure(constraints)
    val fill = maxOf(number.width, number.height) / DayGeometry.NUMBER_SHARE
    val side = (fill * DayGeometry.BASE / DayGeometry.FILL).roundToInt()
    val width = constraints.constrainWidth(side)
    val height = constraints.constrainHeight(side)
    layout(width, height) { number.place((width - number.width) / 2, (height - number.height) / 2) }
}

/**
 * The day cell's geometry at its 48dp base size, in dp. Everything scales with the cell's smaller
 * side, so the legend's swatches draw the same shapes smaller.
 */
private object DayGeometry {
    const val BASE = 48f
    const val FILL = 36f

    /** The most of the fill's width the number takes before the cell grows: 80%. */
    const val NUMBER_SHARE = 0.8f
    const val SUN = 40f
    const val RING = 39f
    const val RING_WIDTH = 3f

    /** On today, the shape shrinks to sit inside the ring, so the ring never hides its corners or edge. */
    const val TODAY_SCALE = 0.85f
    const val EDGE_WIDTH = 2f
    const val EDGE_DASH = 4f
    const val EDGE_GAP = 3f
    const val DOT = 5f
    const val DOT_OFFSET = 12f
    const val FRAME_WIDTH = 2f
}

private val SquircleShape = PolygonShape(CyclePolygons.squircle)
private val SunShape = PolygonShape(CyclePolygons.sun)

/**
 * Draws [state]'s shape, its marker dot and the today ring, centred and scaled to this draw scope.
 * [periodShape] is the period fill: the log morph in a cell, the plain squircle in a swatch.
 */
internal fun DrawScope.drawDay(
    state: CycleDayState,
    isToday: Boolean,
    colors: CycleColors,
    periodShape: Shape = SquircleShape
) {
    val unit = size.minDimension / DayGeometry.BASE
    val shape = if (isToday) unit * DayGeometry.TODAY_SCALE else unit
    when (state) {
        CycleDayState.Plain -> Unit

        CycleDayState.Period -> drawCentred(periodShape, DayGeometry.FILL * shape, colors.period)

        CycleDayState.PredictedPeriod -> {
            drawCentred(SquircleShape, DayGeometry.FILL * shape, colors.predicted)
            val edge = DayGeometry.EDGE_WIDTH * shape
            drawCentred(
                SquircleShape,
                DayGeometry.FILL * shape - edge,
                colors.predictedEdge,
                Stroke(
                    width = edge,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(DayGeometry.EDGE_DASH * shape, DayGeometry.EDGE_GAP * shape)
                    )
                )
            )
        }

        CycleDayState.Fertile -> {
            drawCentred(CircleShape, DayGeometry.FILL * shape, colors.fertile)
            drawCircle(
                colors.ovulation,
                radius = DayGeometry.DOT / 2 * shape,
                center = center + Offset(0f, DayGeometry.DOT_OFFSET * shape)
            )
        }

        CycleDayState.Ovulation -> drawCentred(SunShape, DayGeometry.SUN * shape, colors.ovulation)
    }
    if (isToday) {
        drawCircle(
            colors.today,
            radius = DayGeometry.RING / 2 * unit,
            style = Stroke(width = DayGeometry.RING_WIDTH * unit)
        )
    }
}

private fun DrawScope.drawSelectionFrame(shape: Shape, color: Color) {
    val width = DayGeometry.FRAME_WIDTH.dp.toPx()
    val frame = Size(size.width - width, size.height - width)
    translate(width / 2, width / 2) {
        drawOutline(shape.createOutline(frame, layoutDirection, this), color, style = Stroke(width))
    }
}

private fun DrawScope.drawCentred(shape: Shape, side: Float, color: Color, style: DrawStyle = Fill) {
    val outline = shape.createOutline(Size(side, side), layoutDirection, this)
    translate(center.x - side / 2, center.y - side / 2) { drawOutline(outline, color, style = style) }
}

/** A normalized [RoundedPolygon] stretched to the outline's size. */
@Stable
private class PolygonShape(polygon: RoundedPolygon) : Shape {
    private val path = polygon.normalized().toPath().asComposePath()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val scaled = Path().apply { addPath(path) }
        scaled.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(scaled)
    }
}

private fun CycleDayState.numberColor(colors: CycleColors): Color = when (this) {
    CycleDayState.Plain, CycleDayState.PredictedPeriod -> colors.onSurface
    CycleDayState.Period -> colors.onPeriod
    CycleDayState.Fertile -> colors.onFertile
    CycleDayState.Ovulation -> colors.onOvulation
}

@Composable
private fun locale(): Locale = LocalConfiguration.current.locales[0]

@Composable
private fun dayNumber(date: LocalDate): String {
    val locale = locale()
    return remember(date, locale) { String.format(locale, "%d", date.dayOfMonth) }
}

/** The date in the locale's day-and-month form ("20 March", "March 20"), then today and the state. */
@Composable
private fun dayDescription(date: LocalDate, state: CycleDayState, isToday: Boolean): String {
    val locale = locale()
    val dayMonth = remember(date, locale) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "dMMMM"), locale).format(date)
    }
    val today = if (isToday) stringResource(R.string.day_cell_today) else null
    val stateLabel = when (state) {
        CycleDayState.Plain -> null
        CycleDayState.Period -> stringResource(R.string.day_cell_period)
        CycleDayState.PredictedPeriod -> stringResource(R.string.day_cell_predicted_period)
        CycleDayState.Fertile -> stringResource(R.string.day_cell_fertile)
        CycleDayState.Ovulation -> stringResource(R.string.day_cell_ovulation)
    }
    return listOfNotNull(dayMonth, today, stateLabel).joinToString(", ")
}

/** The synthetic week of 15 to 21 March 2027 from the Zest board, for previews, tests and the catalog. */
internal val SampleWeek: List<Pair<LocalDate, CycleDayState>> = listOf(
    CycleDayState.Fertile,
    CycleDayState.Fertile,
    CycleDayState.Ovulation,
    CycleDayState.Fertile,
    CycleDayState.Plain,
    CycleDayState.Plain,
    CycleDayState.Plain
).mapIndexed { index, state -> LocalDate.of(2027, 3, 15 + index) to state }

/** Today in [SampleWeek]: the days after it are in the future, so disabled. */
internal val SampleToday: LocalDate = LocalDate.of(2027, 3, 20)

/** [SampleWeek] as a row of cells, with today on the 20th and the 19th selected. */
@Composable
internal fun SampleWeekRow(modifier: Modifier = Modifier) {
    Row(modifier) {
        SampleWeek.forEach { (date, state) ->
            DayCell(
                date = date,
                state = state,
                onClick = {},
                isToday = date == SampleToday,
                selected = date == SampleToday.minusDays(1),
                enabled = !date.isAfter(SampleToday)
            )
        }
    }
}

/** A day in each state with the dates of the Zest board, as a cell would show them. */
internal val SampleStateDates: Map<CycleDayState, LocalDate> = mapOf(
    CycleDayState.Plain to LocalDate.of(2027, 3, 9),
    CycleDayState.Period to LocalDate.of(2027, 3, 4),
    CycleDayState.PredictedPeriod to LocalDate.of(2027, 3, 30),
    CycleDayState.Fertile to LocalDate.of(2027, 3, 14),
    CycleDayState.Ovulation to LocalDate.of(2027, 3, 17)
)

@Composable
private fun DayCellStates() {
    Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        CycleDayState.entries.forEach { state ->
            Row(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
                val date = SampleStateDates.getValue(state)
                DayCell(date, state, onClick = {})
                DayCell(date, state, onClick = {}, isToday = true)
                DayCell(date, state, onClick = {}, selected = true)
                DayCell(date, state, onClick = {}, enabled = false)
                listOf<Interaction>(PressInteraction.Press(Offset.Zero), FocusInteraction.Focus()).forEach {
                    DayCell(date, state, onClick = {}, interactionSource = rememberInteractionSourceIn(it))
                }
            }
        }
        SampleWeekRow()
    }
}

@Preview(name = "Day cell · light")
@Composable
private fun DayCellLightPreview() = PreviewSurface(darkTheme = false) { DayCellStates() }

@Preview(name = "Day cell · dark")
@Composable
private fun DayCellDarkPreview() = PreviewSurface(darkTheme = true) { DayCellStates() }
