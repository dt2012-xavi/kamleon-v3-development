package com.dynatech2012.kamleonuserapp.views.graph.data

import java.util.Date

class KamleonGraphBarItemData(val dataVal: Double, var timestamp: Long) {
    fun toDate(): Date {
        return Date(timestamp)
    }
}