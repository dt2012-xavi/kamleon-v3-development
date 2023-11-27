package com.dynatech2012.kamleonuserapp.views.chart.data

import android.util.Log
import com.dynatech2012.kamleonuserapp.views.chart.exts.addDays
import com.dynatech2012.kamleonuserapp.views.chart.exts.addMonths
import java.util.Date

enum class KamleonGraphViewMode(val identifier: String) {
    Daily("daily"),
    Weekly("weekly"),
    Monthly("monthly"),
    Yearly("yearly");

    fun displayName() : String {
        return when(this) {
            Daily -> "Daily"
            Weekly -> "Weekly"
            Monthly -> "Monthly"
            Yearly -> "Yearly"
        }
    }

    fun xLabelStrings(startDate: Date, steps: Int) : ArrayList<KamleonGraphAxisLabelItem> {
        val aryRet = ArrayList<KamleonGraphAxisLabelItem>()
        if (this == Daily) {
            for (step in 2 .. steps step 2 ) {
                aryRet.add(KamleonGraphAxisLabelItem((step - 1).toDouble(), step.toString()))
            }
        } else if (this == Monthly) {
            for (step in 0 until steps step 2 ) {
                aryRet.add(KamleonGraphAxisLabelItem(step.toDouble(), (startDate.addDays(step).date).toString()))
            }
        } else if (this == Weekly) {
            Log.d("LLL", "Weekly..")
            val dayNames = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            for (step in dayNames.indices) {
                aryRet.add(KamleonGraphAxisLabelItem(step.toDouble(), dayNames[startDate.addDays(step).day]))
            }
        } else if (this == Yearly) {
            val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            for (step in monthNames.indices) {
                aryRet.add(KamleonGraphAxisLabelItem(step.toDouble(), monthNames[startDate.addMonths(step).month]))
            }
        }

        return aryRet
    }

    companion object {
        fun viewMode(fromIndex: Int) : KamleonGraphViewMode {
            return when(fromIndex) {
                1 -> Weekly
                2 -> Monthly
                3 -> Yearly
                else -> Daily
            }
        }
    }
}