package com.dynatech2012.kamleonuserapp.views.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.application.MatTheme
import com.dynatech2012.kamleonuserapp.extensions.timeText
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphBarDrawData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphViewMode
import com.dynatech2012.kamleonuserapp.views.graph.data.barLabel
import com.dynatech2012.kamleonuserapp.views.graph.data.barSamples
import com.dynatech2012.kamleonuserapp.views.graph.data.averageText
import com.dynatech2012.kamleonuserapp.views.graph.data.barValue
import com.dynatech2012.kamleonuserapp.views.graph.data.periodText
import com.dynatech2012.kamleonuserapp.views.graph.data.countsForStreak
import com.dynatech2012.kamleonuserapp.views.graph.data.isTappable
import java.util.Date
import kotlin.math.roundToInt

/** Height of the strip under the bars that holds the x axis labels. */
private val XLabelStrip = 20.dp

/** Gap between the bars' area and the y axis labels on its right. */
private val YLabelGap = 6.dp

/** Vertical dashed lines across the bars, one per column of the period. */
private fun gridColumns(mode: KamleonGraphViewMode) = when (mode) {
    KamleonGraphViewMode.Weekly -> 7
    KamleonGraphViewMode.Daily, KamleonGraphViewMode.Monthly, KamleonGraphViewMode.Yearly -> 6
}

/** Width of a bar inside its column, as a fraction of the column. */
private fun barFraction(mode: KamleonGraphViewMode) = when (mode) {
    KamleonGraphViewMode.Daily -> 0.6f
    KamleonGraphViewMode.Weekly -> 0.3f
    KamleonGraphViewMode.Monthly -> 0.4f
    KamleonGraphViewMode.Yearly -> 0.5f
}

/**
 * Where everything sits in a chart of [size]: the bars' area on the left, the y labels on the right
 * (as wide as the widest label), the x labels in a strip under the bars.
 */
private class ChartGeometry(val data: KamleonGraphBarDrawData, val size: Size, yLabelColumn: Float, xLabelStrip: Float) {
    val plotWidth = (size.width - yLabelColumn).coerceAtLeast(0f)
    val plotHeight = (size.height - xLabelStrip).coerceAtLeast(0f)
    private val columnWidth = if (data.xyRange.x > 0) plotWidth / data.xyRange.x.toFloat() else 0f
    private val valueStep = if (data.xyRange.y > 0) plotHeight / data.xyRange.y.toFloat() else 0f

    fun yOf(value: Double) = plotHeight - valueStep * value.toFloat()

    fun columnCenter(index: Double) = columnWidth * index.toFloat() + columnWidth / 2f

    fun barRect(index: Int): Rect {
        val width = columnWidth * barFraction(data.mode)
        val left = columnWidth * index + (columnWidth - width) / 2f
        return Rect(left, yOf(data.values[index].y), left + width, plotHeight)
    }

    /** The bar under [point], if it has a height to hit. */
    fun barAt(point: Offset): Int? = data.values.indices.firstOrNull { i ->
        data.isTappable(i) && barRect(i).contains(point)
    }
}

/**
 * The bars of [data] with their grid, axis labels and, with [streak] on, the streak lines; tapping or
 * dragging over a bar shows its value above it.
 */
@Composable
fun GraphChart(data: KamleonGraphBarDrawData, streak: Boolean, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val black = colorResource(R.color.kmln_graph_color_black)
    val gridColor = colorResource(R.color.kamleon_secondary_grey_40)
    val active = colorResource(R.color.kmln_graph_color_active)
    val disabled = colorResource(R.color.kmln_graph_color_disabled)
    val emptyColor = colorResource(R.color.kamleon_secondary_grey_60)
    val xLabelStyle = TextStyle(color = black, fontSize = 12.sp)
    val yLabelStyle = TextStyle(color = black, fontSize = 14.sp)
    val limitStyle = TextStyle(color = black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val emptyStyle = TextStyle(color = emptyColor, fontSize = 10.sp)
    val emptyMessage = stringResource(R.string.graph_no_samples)
    val limitLabels = streakLimitLabels(data)
    // TalkBack reads the metric, the period and its average; tapping a bar shows its own value as text.
    val description = stringResource(
        R.string.graph_chart_description,
        stringResource(data.type.title()),
        data.periodText(),
        data.averageText(),
        if (data.hasPreciseData()) stringResource(data.type.getUnit()) else "",
    ).let { if (data.hasPreciseData()) it else "$it $emptyMessage" }

    val yLabels = remember(data) { data.yLabels.map { it.dataVal to measurer.measure(it.label, yLabelStyle) } }
    val xLabels = remember(data) { data.xLabels.map { it.dataVal to measurer.measure(it.label, xLabelStyle) } }
    val yLabelColumn = with(density) { (yLabels.maxOfOrNull { it.second.size.width } ?: 0) + YLabelGap.toPx() }
    val xLabelStrip = with(density) { XLabelStrip.toPx() }

    // The tapped bar; a new period or metric hides it.
    var selected by remember(data) { mutableStateOf<Int?>(null) }
    var size by remember { mutableStateOf(Size.Zero) }
    val geometry = ChartGeometry(data, size, yLabelColumn, xLabelStrip)

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { size = Size(it.width.toFloat(), it.height.toFloat()) }
                .testTag("graph_chart")
                .semantics { contentDescription = description }
                .pointerInput(data, yLabelColumn) {
                    // Every touch position picks the bar under it, so dragging moves along the bars.
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val chart = ChartGeometry(data, this.size.let { Size(it.width.toFloat(), it.height.toFloat()) }, yLabelColumn, xLabelStrip)
                        selected = chart.barAt(down.position)
                        do {
                            val event = awaitPointerEvent()
                            event.changes.lastOrNull()?.let { selected = chart.barAt(it.position) }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val chart = ChartGeometry(data, this.size, yLabelColumn, xLabelStrip)
            drawGrid(chart, gridColor)
            drawBars(chart, streak, active, disabled)
            drawAxisLabels(chart, xLabels, yLabels)
            if (streak) drawStreakLines(chart, limitLabels, active, measurer, limitStyle)
            if (!data.hasPreciseData()) {
                val message = measurer.measure(emptyMessage, emptyStyle)
                drawText(
                    message,
                    topLeft = Offset(
                        (chart.plotWidth - message.size.width) / 2f,
                        (chart.plotHeight - message.size.height) / 2f,
                    ),
                )
            }
        }
        selected?.let { index ->
            BarLabel(data, index, geometry)
        }
    }
}

private fun DrawScope.drawGrid(chart: ChartGeometry, color: Color) {
    // One line per y axis label, so each label sits on its line.
    for (label in chart.data.yLabels) {
        val y = chart.yOf(label.dataVal)
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
    }
    val columns = gridColumns(chart.data.mode)
    val dash = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
    for (column in 1..columns) {
        val x = chart.plotWidth / columns * column
        drawLine(color, Offset(x, 0f), Offset(x, chart.plotHeight), strokeWidth = 1f, pathEffect = dash)
    }
}

private fun DrawScope.drawBars(chart: ChartGeometry, streak: Boolean, active: Color, disabled: Color) {
    val data = chart.data
    for (index in data.values.indices) {
        val rect = chart.barRect(index)
        val color = when {
            // With the streak on, imprecise bars disappear and the rest keep their colour only if they count.
            streak && !data.arePrecise[index] -> continue
            streak -> if (data.countsForStreak(index)) active else disabled
            !data.arePrecise[index] -> disabled
            else -> active
        }
        drawRect(color, rect.topLeft, rect.size)
    }
}

private fun DrawScope.drawAxisLabels(
    chart: ChartGeometry,
    xLabels: List<Pair<Double, TextLayoutResult>>,
    yLabels: List<Pair<Double, TextLayoutResult>>,
) {
    // Centred under their column, in the strip under the bars.
    val stripTop = chart.plotHeight
    val stripHeight = size.height - stripTop
    for ((column, label) in xLabels) {
        val x = chart.columnCenter(column) - label.size.width / 2f
        drawText(label, topLeft = Offset(x, stripTop + (stripHeight - label.size.height) / 2f))
    }
    // Right of the bars, centred on their value and kept inside the chart.
    val x = size.width - (yLabels.maxOfOrNull { it.second.size.width } ?: 0)
    for ((value, label) in yLabels) {
        val y = (chart.yOf(value) - label.size.height / 2f).coerceIn(0f, size.height - label.size.height)
        drawText(label, topLeft = Offset(x, y))
    }
}

/** One streak line: its value, its label and whether the label goes above the line. */
private data class StreakLimit(val value: Double, val label: String?, val labelAbove: Boolean)

@Composable
private fun streakLimitLabels(data: KamleonGraphBarDrawData): List<StreakLimit> {
    val optimum = stringResource(R.string.graph_label_optimum)
    val minimum = stringResource(R.string.graph_label_minimum)
    val maximum = stringResource(R.string.graph_label_maximum)
    return when (data.type) {
        // Everything from the line up counts for the streak.
        KamleonGraphDataType.hydration -> listOf(StreakLimit(KamleonGraphDataType.HydrationStreakValue, optimum, true))
        KamleonGraphDataType.electrolytes -> listOf(
            StreakLimit(KamleonGraphDataType.ElectrolyteStreakValueLower, null, false),
            StreakLimit(KamleonGraphDataType.ElectrolyteStreakValueUpper, optimum, false),
        )
        KamleonGraphDataType.volume -> listOf(
            StreakLimit(data.minForStreak(), minimum, false),
            StreakLimit(data.maxForStreak(), maximum, true),
        )
    }
}

private fun DrawScope.drawStreakLines(
    chart: ChartGeometry,
    limits: List<StreakLimit>,
    color: Color,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    val gap = 7.dp.toPx()
    for (limit in limits) {
        val y = chart.yOf(limit.value)
        drawLine(color, Offset(0f, y), Offset(chart.plotWidth, y), strokeWidth = 1.5.dp.toPx())
        val label = limit.label ?: continue
        val text = measurer.measure(label, style)
        val top = if (limit.labelAbove) y - gap - text.size.height else y + gap
        drawText(text, topLeft = Offset(chart.plotWidth * 2f / 3f, top))
    }
}

/**
 * The tapped bar's value, span and (on the daily graph) its samples, in a grey box over the bar with a
 * small mark pointing at it. Kept inside the chart's width.
 */
@Composable
private fun BarLabel(data: KamleonGraphBarDrawData, index: Int, geometry: ChartGeometry) {
    val bar = geometry.barRect(index)
    val unit = stringResource(data.type.getUnit())
    val grey = colorResource(R.color.kmln_graph_color_grey)
    val black = colorResource(R.color.kmln_graph_color_black)
    val markColor = colorResource(R.color.kmln_graph_header_item_text_normal)
    val markWidth = with(LocalDensity.current) { 2.dp.toPx() }
    Layout(
        modifier = Modifier.testTag("graph_bar_label"),
        content = {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .background(colorResource(R.color.kmln_graph_label_bkg), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(stringResource(R.string.graph_header_average), color = grey, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = MatTheme.fontFamily)
                Row {
                    Text(data.barValue(index), color = black, fontSize = 23.sp, fontFamily = MatTheme.fontFamily, modifier = Modifier.alignByBaseline())
                    Text(unit, color = black, fontSize = 16.sp, fontFamily = MatTheme.fontFamily, modifier = Modifier.alignByBaseline())
                }
                Text(data.barLabel(index), color = grey, fontSize = 12.sp, fontFamily = MatTheme.fontFamily)
                if (data.mode == KamleonGraphViewMode.Daily) {
                    Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).background(colorResource(R.color.white)))
                    SampleRows(data, index, unit)
                }
            }
            Box(Modifier.width(2.dp).height(8.dp).background(markColor))
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val box = measurables[0].measure(loose)
        val mark = measurables[1].measure(loose)
        val chartWidth = constraints.maxWidth
        val height = box.height + mark.height
        val center = bar.center.x
        val x = (center - box.width / 2f).roundToInt().coerceIn(0, (chartWidth - box.width).coerceAtLeast(0))
        val y = (bar.top - height).roundToInt().coerceAtLeast(0)
        layout(chartWidth, constraints.maxHeight) {
            box.place(IntOffset(x, y))
            mark.place(IntOffset((center - markWidth / 2f).roundToInt(), y + box.height))
        }
    }
}

/** The daily bar's samples, newest first: time and value, or low volume for an imprecise one. */
@Composable
private fun SampleRows(data: KamleonGraphBarDrawData, index: Int, unit: String) {
    val grey = colorResource(R.color.kmln_graph_color_grey)
    val black = colorResource(R.color.kmln_graph_color_black)
    Column(Modifier.fillMaxWidth()) {
        for (sample in data.barSamples(index)) {
            val value = if (sample.isPrecise) sample.scoreValue(data.type).toInt().toString() else stringResource(R.string.graph_label_low_volume)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Date(sample.timestamp).timeText(), color = grey, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MatTheme.fontFamily)
                Text(
                    stringResource(R.string.graph_label_item_value, value, if (sample.isPrecise) unit else ""),
                    color = black, fontSize = 12.sp, fontFamily = MatTheme.fontFamily,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
