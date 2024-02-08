package com.dynatech2012.kamleonuserapp.views.graph.data

import com.dynatech2012.kamleonuserapp.extensions.addDays
import com.dynatech2012.kamleonuserapp.extensions.addMonths
import com.dynatech2012.kamleonuserapp.extensions.addYears
import com.dynatech2012.kamleonuserapp.extensions.beginningOfDay
import com.dynatech2012.kamleonuserapp.extensions.endOfDay
import java.util.Date

class KamleonGraphBarDrawData(
    val date: Date,
    val values : ArrayList<KamleonGraphDataXY>,
    val xyRange : KamleonGraphDataXY,
    val xLabels : ArrayList<KamleonGraphAxisLabelItem>,
    val yLabels : ArrayList<KamleonGraphAxisLabelItem>,
    val type : KamleonGraphDataType,
    val mode : KamleonGraphViewMode,
    val arePrecise : ArrayList<Boolean>
) {
    fun startDate() : Date {
        return calcStartDateFrom(mode, date)
    }

    fun endDate() : Date {
        return calcEndDateFrom(mode, date)
    }


    private fun filterNonZeroValues() : List<KamleonGraphDataXY> {
        return values.filter { it.y > 0 }
    }

    private fun filterNonZeroValuesAndNonLowVolumeValues() : List<KamleonGraphDataXY> {
        val preciseData = ArrayList<KamleonGraphDataXY>()
        for (i in 0 until values.size) {
            if (arePrecise[i] && values[i].y > 0) {
                preciseData.add(values[i])
            }
        }
        return preciseData
    }

    fun averageForStreak(): Double {
        val nonZeroValues = filterNonZeroValuesAndNonLowVolumeValues()
        if (nonZeroValues.isEmpty()) { return 0.0 }
        return nonZeroValues.sumOf { it.y } / nonZeroValues.size
    }

    fun minForStreak(): Double {
        val nonZeroValues = filterNonZeroValuesAndNonLowVolumeValues()
        if (nonZeroValues.isEmpty()) { return 0.0 }
        return nonZeroValues.minOf { it.y }
    }

    fun maxForStreak(): Double {
        val nonZeroValues = filterNonZeroValuesAndNonLowVolumeValues()
        if (nonZeroValues.isEmpty()) { return 0.0 }
        return nonZeroValues.maxOf { it.y }
    }

    companion object {
        @JvmStatic
        fun empty() =
            KamleonGraphBarDrawData(
                Date(),
                ArrayList<KamleonGraphDataXY>(),
                KamleonGraphDataXY(0.0, 0.0),
                ArrayList<KamleonGraphAxisLabelItem>(),
                ArrayList<KamleonGraphAxisLabelItem>(),
                KamleonGraphDataType.hydration,
                KamleonGraphViewMode.Daily,
                ArrayList()
            )

        fun calcStartDateFrom(viewMode: KamleonGraphViewMode, date: Date) : Date {
          return when (viewMode) {
            KamleonGraphViewMode.Daily -> date.beginningOfDay
            KamleonGraphViewMode.Weekly -> date.addDays(-6).beginningOfDay
            KamleonGraphViewMode.Monthly -> date.addDays(-29).beginningOfDay
            KamleonGraphViewMode.Yearly -> date.addMonths(-11).beginningOfDay
            }
        }

        fun calcEndDateFrom(viewMode: KamleonGraphViewMode, date: Date) : Date {
            return when (viewMode) {
                KamleonGraphViewMode.Daily -> date.endOfDay
                KamleonGraphViewMode.Weekly -> date.endOfDay
                KamleonGraphViewMode.Monthly -> date.endOfDay
                KamleonGraphViewMode.Yearly -> date.endOfDay
            }
        }
    }
}