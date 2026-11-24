package com.dynatech2012.kamleonuserapp.views.graph.data

import com.dynatech2012.kamleonuserapp.database.AveragesData
import com.dynatech2012.kamleonuserapp.extensions.addDays
import com.dynatech2012.kamleonuserapp.extensions.addMonths
import com.dynatech2012.kamleonuserapp.extensions.formatDate
import com.dynatech2012.kamleonuserapp.models.ElectrolyteLevel
import com.dynatech2012.kamleonuserapp.models.HydrationLevel
import java.util.Locale

/** What the graph screen shows about its bars: the tapped bar's label and the streak count. */

/** The span of bar [index]: its hour, day or month. */
fun KamleonGraphBarDrawData.barLabel(index: Int): String {
    val startDate = startDate()
    return when (mode) {
        KamleonGraphViewMode.Daily -> names.hour(index)
        KamleonGraphViewMode.Weekly -> names.days[startDate.addDays(index).day]
        KamleonGraphViewMode.Monthly -> startDate.addDays(index).formatDate("dd MMM, yyyy")
        KamleonGraphViewMode.Yearly -> names.months[startDate.addMonths(index).month]
    }
}

/** Bar [index]'s value with one decimal, as the tapped bar's label shows it. */
fun KamleonGraphBarDrawData.barValue(index: Int): String = String.format(Locale.getDefault(), "%.1f", values[index].y)

/** The samples behind bar [index], newest first. */
fun KamleonGraphBarDrawData.barSamples(index: Int): List<AveragesData> = values[index].data.reversed()

/** Whether bar [index] can be tapped: it has a height to hit. */
fun KamleonGraphBarDrawData.isTappable(index: Int): Boolean = values[index].y > 0

/**
 * The average, unit and dates over the graph. "--" when the period has no precise bar, so imprecise
 * samples never make an average.
 */
fun KamleonGraphBarDrawData.averageText(): String {
    if (!hasPreciseData()) return "--"
    val average = averageForStreak()
    return when (type) {
        KamleonGraphDataType.electrolytes -> String.format(Locale.getDefault(), "%.1f", average)
        KamleonGraphDataType.hydration, KamleonGraphDataType.volume -> average.toInt().toString()
    }
}

/** The period the graph covers: a day, a range of days, of months or of years. */
fun KamleonGraphBarDrawData.periodText(): String = when (mode) {
    KamleonGraphViewMode.Daily -> date.formatDate("dd MMM, yyyy")
    KamleonGraphViewMode.Weekly -> startDate().formatDate("dd MMM, yyyy") + " - " + endDate().formatDate("dd MMM, yyyy")
    KamleonGraphViewMode.Monthly -> startDate().formatDate("MMM") + " - " + endDate().formatDate("MMM")
    KamleonGraphViewMode.Yearly -> startDate().formatDate("yyyy") + " - " + endDate().formatDate("yyyy")
}

/** The streak as "bars in the optimum range" over "bars with precise data". */
data class StreakCount(val optimum: Int, val total: Int)

/** The streak of hydration or electrolytes; volume has limits instead, so it has none. */
fun KamleonGraphBarDrawData.streakCount(): StreakCount? {
    val precise = values.filterIndexed { i, bar -> arePrecise[i] && bar.y > 0 }
    val optimum = when (type) {
        KamleonGraphDataType.hydration -> precise.count { HydrationLevel.isOptimum(it.y) }
        KamleonGraphDataType.electrolytes -> precise.count { ElectrolyteLevel.isOptimum(it.y) }
        KamleonGraphDataType.volume -> return null
    }
    return StreakCount(optimum, precise.size)
}

/**
 * Whether bar [index] keeps its colour with the streak on: in the optimum range for hydration and
 * electrolytes, the highest or lowest bar for volume. Imprecise bars never do.
 */
fun KamleonGraphBarDrawData.countsForStreak(index: Int): Boolean {
    if (!arePrecise[index]) return false
    val y = values[index].y
    return when (type) {
        KamleonGraphDataType.hydration -> HydrationLevel.isOptimum(y)
        KamleonGraphDataType.electrolytes -> ElectrolyteLevel.isOptimum(y)
        KamleonGraphDataType.volume -> y == minForStreak() || y == maxForStreak()
    }
}
