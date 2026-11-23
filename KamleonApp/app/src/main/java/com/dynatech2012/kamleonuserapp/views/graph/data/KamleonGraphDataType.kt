package com.dynatech2012.kamleonuserapp.views.graph.data

import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.models.ElectrolyteLevel
import com.dynatech2012.kamleonuserapp.models.HydrationLevel

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
        /** Optimum hydration band, drawn as the graph's streak lines. */
        const val HydrationStreakValueLower = HydrationLevel.HYDRATED_MIN.toDouble()
        const val HydrationStreakValueUpper = HydrationLevel.HYDRATED_MAX.toDouble()

        const val ElectrolyteStreakValueLower = ElectrolyteLevel.BALANCED_MIN
        const val ElectrolyteStreakValueUpper = ElectrolyteLevel.BALANCED_MAX
    }
}