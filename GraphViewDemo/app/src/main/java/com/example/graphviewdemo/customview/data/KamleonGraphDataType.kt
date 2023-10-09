package com.example.graphviewdemo.customview.data

import kotlin.random.Random

enum class KamleonGraphDataType(val identifier: String) {
    hydration("HYDRATION"),
    electrolytes("ELECTROLYTES"),
    volume("VOLUME");

    fun getUnit() : String {
        return when (this) {
            hydration -> "%"
            electrolytes -> "mS/cm"
            volume -> "ml"
        }
    }

    companion object {
        @JvmStatic
        var HydrationStreakValue = 50.0

        var ElectrolyteStreakValueLower = 20.0
        var ElectrolyteStreakValueUpper = 35.0
    }
}