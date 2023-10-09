package com.example.graphviewdemo.customview.data

import android.graphics.Point
import android.util.MonthDisplayHelper
import android.util.Size
import com.example.graphviewdemo.customview.exts.beginningOfDay
import com.example.graphviewdemo.customview.exts.beginningOfMonth
import com.example.graphviewdemo.customview.exts.beginningOfWeek
import com.example.graphviewdemo.customview.exts.beginningOfYear
import com.example.graphviewdemo.customview.exts.endOfDay
import com.example.graphviewdemo.customview.exts.endOfMonth
import com.example.graphviewdemo.customview.exts.endOfWeek
import com.example.graphviewdemo.customview.exts.endOfYear
import com.example.graphviewdemo.customview.exts.with
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
            KamleonGraphViewMode.Weekly -> date.beginningOfWeek()
            KamleonGraphViewMode.Monthly -> date.beginningOfMonth
            KamleonGraphViewMode.Yearly -> date.beginningOfYear
            }
        }

        fun calcEndDateFrom(viewMode: KamleonGraphViewMode, date: Date) : Date {
            return when (viewMode) {
                KamleonGraphViewMode.Daily -> date.endOfDay
                KamleonGraphViewMode.Weekly -> date.endOfWeek()
                KamleonGraphViewMode.Monthly -> date.endOfMonth()
                KamleonGraphViewMode.Yearly -> date.endOfYear()
            }
        }
    }
}