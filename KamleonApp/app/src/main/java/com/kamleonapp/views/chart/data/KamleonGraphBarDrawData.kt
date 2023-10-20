package com.kamleonapp.views.chart.data

import android.graphics.Point
import android.util.MonthDisplayHelper
import android.util.Size
import com.kamleonapp.views.chart.exts.addDays
import com.kamleonapp.views.chart.exts.addMonths
import com.kamleonapp.views.chart.exts.addYears
import com.kamleonapp.views.chart.exts.beginningOfDay
import com.kamleonapp.views.chart.exts.beginningOfMonth
import com.kamleonapp.views.chart.exts.beginningOfWeek
import com.kamleonapp.views.chart.exts.beginningOfYear
import com.kamleonapp.views.chart.exts.endOfDay
import com.kamleonapp.views.chart.exts.endOfMonth
import com.kamleonapp.views.chart.exts.endOfWeek
import com.kamleonapp.views.chart.exts.endOfYear
import com.kamleonapp.views.chart.exts.with
import java.util.Calendar
import java.util.Date

class KamleonGraphBarDrawData(
    val date: Date,
    val values : ArrayList<KamleonGraphDataXY>,
    val xyRange : KamleonGraphDataXY,
    val xLabels : ArrayList<KamleonGraphAxisLabelItem>,
    val yLabels : ArrayList<KamleonGraphAxisLabelItem>,
    val type : KamleonGraphDataType,
    val mode : KamleonGraphViewMode,
) {
    fun startDate() : Date {
        return KamleonGraphBarDrawData.calcStartDateFrom(mode, date)
    }

    fun endDate() : Date {
        return KamleonGraphBarDrawData.calcEndDateFrom(mode, date)
    }


    fun filterNonZeroValues() : List<KamleonGraphDataXY> {
        return values.filter { it.y > 0 }
    }

    fun averageForStreak(): Double {
        val nonZeroValues = filterNonZeroValues()
        if (nonZeroValues.isEmpty()) { return 0.0 }
        return nonZeroValues.sumOf { it.y } / nonZeroValues.size
    }

    fun minForStreak(): Double {
        val nonZeroValues = filterNonZeroValues()
        if (nonZeroValues.isEmpty()) { return 0.0 }
        return nonZeroValues.minOf { it.y }
    }

    fun maxForStreak(): Double {
        val nonZeroValues = filterNonZeroValues()
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
                KamleonGraphViewMode.Daily
            )

        fun calcStartDateFrom(viewMode: KamleonGraphViewMode, date: Date) : Date {
          return when (viewMode) {
            KamleonGraphViewMode.Daily -> date.beginningOfDay
            KamleonGraphViewMode.Weekly -> date.addDays(-6)
            KamleonGraphViewMode.Monthly -> date.addDays(-30)
            KamleonGraphViewMode.Yearly -> date.addYears(-1)
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