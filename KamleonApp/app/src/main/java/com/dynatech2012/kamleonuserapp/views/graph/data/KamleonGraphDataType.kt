package com.dynatech2012.kamleonuserapp.views.graph.data

import androidx.annotation.StringRes
import com.dynatech2012.kamleonuserapp.R
import com.dynatech2012.kamleonuserapp.models.ElectrolyteLevel
import com.dynatech2012.kamleonuserapp.models.HydrationLevel

enum class KamleonGraphDataType(val identifier: String) {
    hydration("HYDRATION"),
    electrolytes("ELECTROLYTES"),
    volume("VOLUME");

    /** The metric's name, as the Analytics cards show it. */
    @StringRes
    fun title(): Int = when (this) {
        hydration -> R.string.analytic_metric_hydration
        electrolytes -> R.string.analytic_metric_electrolytes
        volume -> R.string.analytic_metric_volume
    }

    fun getUnit() : Int {
        return when (this) {
            hydration -> R.string.unit_percentage
            electrolytes -> R.string.unit_electrolites
            volume -> R.string.unit_volume
        }
    }

    companion object {
        /** Lowest hydration that counts for the streak, drawn as the graph's streak line; it has no upper limit. */
        const val HydrationStreakValue = HydrationLevel.HYDRATED_MIN.toDouble()

        const val ElectrolyteStreakValueLower = ElectrolyteLevel.BALANCED_MIN
        const val ElectrolyteStreakValueUpper = ElectrolyteLevel.BALANCED_MAX
    }
}