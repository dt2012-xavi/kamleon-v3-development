package com.dynatech2012.kamleonuserapp.views.chart

import android.annotation.SuppressLint
import com.dynatech2012.kamleonuserapp.views.chart.exts.toDate
import java.text.SimpleDateFormat
import java.util.Date

class KamleonUtils {
    companion object {
        const val DEFAULT_FORMAT_DATE_WITHOUT_TIME = "yyyy-MM-dd"

        fun dateFrom(year: Int, month: Int, day: Int) : Date {
            return "$year-$month-$day".toDate("yyyy-MM-dd")
        }
    }
}