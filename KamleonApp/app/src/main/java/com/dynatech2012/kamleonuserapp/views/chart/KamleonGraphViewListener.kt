package com.dynatech2012.kamleonuserapp.views.chart

import com.dynatech2012.kamleonuserapp.views.chart.data.KamleonGraphViewMode

interface KamleonGraphViewListener {
    fun onViewModeChanged(mode: KamleonGraphViewMode)
    fun onStreakChanged(isStreak: Boolean)
    fun onGraphBarSelected()
}