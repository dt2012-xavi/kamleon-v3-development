package com.dynatech2012.kamleonuserapp.views.graph.data

import com.dynatech2012.kamleonuserapp.R

enum class KamleonGraphDataType(val identifier: String) {
    hydration("HYDRATION"),
    electrolytes("ELECTROLYTES"),
    volume("VOLUME");

    fun getUnit() : Int {
        return when (this) {
            hydration -> R.string.unit_percentage
            electrolytes -> R.string.unit_electrolites
            volume -> R.string.unit_volume
        }
    }

    companion object {
        @JvmStatic
        var HydrationStreakValue = 50.0

        var ElectrolyteStreakValueLower = 20.0
        var ElectrolyteStreakValueUpper = 35.0
    }
}