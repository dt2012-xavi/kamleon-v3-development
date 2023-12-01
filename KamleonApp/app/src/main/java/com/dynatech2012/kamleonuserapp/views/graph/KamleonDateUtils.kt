package com.dynatech2012.kamleonuserapp.views.graph

import com.dynatech2012.kamleonuserapp.extensions.toDate
import java.util.Date

class KamleonUtils {
    companion object {
        const val DEFAULT_FORMAT_DATE_WITHOUT_TIME = "yyyy-MM-dd"

        fun dateFrom(year: Int, month: Int, day: Int) : Date {
            return "$year-$month-$day".toDate("yyyy-MM-dd")
        }
    }
}