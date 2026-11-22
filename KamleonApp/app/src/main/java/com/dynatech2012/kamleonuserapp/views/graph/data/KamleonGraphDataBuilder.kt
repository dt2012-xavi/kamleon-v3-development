package com.dynatech2012.kamleonuserapp.views.graph.data

import com.dynatech2012.kamleonuserapp.database.AveragesData
import com.dynatech2012.kamleonuserapp.extensions.addDays
import com.dynatech2012.kamleonuserapp.extensions.addHours
import com.dynatech2012.kamleonuserapp.extensions.addMonths
import com.dynatech2012.kamleonuserapp.extensions.beginningOfDay
import com.dynatech2012.kamleonuserapp.extensions.beginningOfMonth
import com.dynatech2012.kamleonuserapp.extensions.endOfDay
import com.dynatech2012.kamleonuserapp.extensions.endOfMonth
import java.util.Date

/**
 * Turns samples (or their daily/monthly averages) into the bars, axis range and labels the graph draws.
 * Pure: no Android view, so it can be unit tested. KamleonGraphView feeds it and draws the result.
 */
object KamleonGraphDataBuilder {

    /** How many bars a mode has: hours of a day, days of a week or month, months of a year. */
    fun xSteps(mode: KamleonGraphViewMode): Int = when (mode) {
        KamleonGraphViewMode.Daily -> 24
        KamleonGraphViewMode.Weekly -> 7
        KamleonGraphViewMode.Monthly -> 30
        KamleonGraphViewMode.Yearly -> 12
    }

    /** The time span, inclusive and in milliseconds, of bar [xStep] in a graph that starts at [startDate]. */
    fun xRange(mode: KamleonGraphViewMode, startDate: Date, xStep: Int): LongArray = when (mode) {
        KamleonGraphViewMode.Daily -> longArrayOf(
            startDate.beginningOfDay.addHours(xStep).time,
            startDate.beginningOfDay.addHours(xStep + 1).time - 1L,
        )
        KamleonGraphViewMode.Weekly, KamleonGraphViewMode.Monthly -> longArrayOf(
            startDate.beginningOfDay.addDays(xStep).time,
            startDate.beginningOfDay.addDays(xStep).endOfDay.time,
        )
        KamleonGraphViewMode.Yearly -> longArrayOf(
            startDate.beginningOfDay.addMonths(xStep).beginningOfMonth.time,
            startDate.beginningOfDay.addMonths(xStep).endOfMonth().time,
        )
    }

    /**
     * The data each mode is drawn from: single samples for a day, daily averages for a week or a month,
     * monthly averages for a year.
     */
    fun sourceFor(
        mode: KamleonGraphViewMode,
        measures: List<AveragesData>,
        daily: List<AveragesData>,
        monthly: List<AveragesData>,
    ): List<AveragesData> = when (mode) {
        KamleonGraphViewMode.Daily -> measures
        KamleonGraphViewMode.Weekly, KamleonGraphViewMode.Monthly -> daily
        KamleonGraphViewMode.Yearly -> monthly
    }

    /**
     * The graph ending on [date]. Each bar is the average of the precise items in its span; when a span
     * only has imprecise items, their average is drawn but the bar is marked as not precise. Empty spans
     * are 0. [percentUnit] is appended to the hydration axis labels.
     */
    fun build(
        date: Date,
        mode: KamleonGraphViewMode,
        type: KamleonGraphDataType,
        source: List<AveragesData>,
        percentUnit: String,
    ): KamleonGraphBarDrawData {
        val startDate = KamleonGraphBarDrawData.calcStartDateFrom(mode, date)
        val endDate = KamleonGraphBarDrawData.calcEndDateFrom(mode, date)
        val inRange = source.filter { it.timestamp >= startDate.time && it.timestamp <= endDate.time }
        val xStepCount = xSteps(mode)

        val xyValues = ArrayList<KamleonGraphDataXY>()
        val precisionValues = ArrayList<Boolean>()
        var maxYVal = 0.0
        for (i in 0 until xStepCount) {
            val range = xRange(mode, startDate, i)
            val items = inRange.filter { it.timestamp >= range[0] && it.timestamp <= range[1] }
            val preciseItems = items.filter { it.isPrecise }
            if (items.isEmpty()) {
                xyValues.add(KamleonGraphDataXY(i.toDouble(), 0.0))
                precisionValues.add(false)
                continue
            }
            // With no precise item, the imprecise ones are averaged instead.
            val counted = preciseItems.ifEmpty { items }
            val y = counted.sumOf { it.scoreValue(type) } / counted.size
            if (maxYVal < y) maxYVal = y
            xyValues.add(KamleonGraphDataXY(i.toDouble(), y).apply { data = ArrayList(items) })
            precisionValues.add(preciseItems.isNotEmpty())
        }

        val maxY = yAxisMax(type, maxYVal)
        val xyRange = KamleonGraphDataXY(xStepCount.toDouble(), maxY)
        val yLabelStep = maxY.toInt() / 5
        val yLabels = ArrayList<KamleonGraphAxisLabelItem>()
        for (tempY in 0..maxY.toInt() step yLabelStep) {
            val label = if (type == KamleonGraphDataType.hydration) "$tempY$percentUnit" else tempY.toString()
            yLabels.add(KamleonGraphAxisLabelItem(tempY.toDouble(), label))
        }

        return KamleonGraphBarDrawData(
            date,
            xyValues,
            xyRange,
            mode.xLabelStrings(startDate, xStepCount),
            yLabels,
            type,
            mode,
            precisionValues,
        )
    }

    /** Top of the y axis: 100% for hydration, 500 ml for volume, and a step that fits the electrolytes. */
    fun yAxisMax(type: KamleonGraphDataType, maxValue: Double): Double = when (type) {
        KamleonGraphDataType.hydration -> 100.0
        KamleonGraphDataType.volume -> 500.0
        KamleonGraphDataType.electrolytes -> when {
            maxValue <= 50.0 -> 50.0
            maxValue <= 100.0 -> 100.0
            maxValue <= 200.0 -> 200.0
            else -> 500.0
        }
    }
}
