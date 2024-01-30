package com.dynatech2012.kamleonuserapp.views.graph.data

import android.util.Log
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.extensions.addDays
import com.dynatech2012.kamleonuserapp.extensions.addMonths
import java.util.Date

enum class KamleonGraphViewMode(val identifier: String) {
    Daily("daily"),
    Weekly("weekly"),
    Monthly("monthly"),
    Yearly("yearly");

    fun displayName() : Int {
        return when(this) {
            Daily -> R.string.graph_header_item_daily
            Weekly -> R.string.graph_header_item_weekly
            Monthly -> R.string.graph_header_item_monthly
            Yearly -> R.string.graph_header_item_yearly
        }
    }

    fun xLabelStrings(startDate: Date, steps: Int) : ArrayList<KamleonGraphAxisLabelItem> {
        val aryRet = ArrayList<KamleonGraphAxisLabelItem>()
        if (this == Daily) {
            for (step in 0 until (steps - 1) step 2 ) {
                val labelItem = KamleonGraphAxisLabelItem((step/* - 1*/).toDouble(), step.toString())
                aryRet.add(labelItem)
                Log.d("LLLL", "label item: ${labelItem.toString()}")
            }
        } else if (this == Monthly) {
            // it begins with 1 so that it starts with the second one, the last is not printed
            for (step in 1 until (steps - 1) step 2 ) {
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