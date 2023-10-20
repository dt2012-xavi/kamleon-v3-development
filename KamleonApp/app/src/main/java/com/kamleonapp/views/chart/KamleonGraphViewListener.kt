package com.kamleonapp.views.chart

import com.kamleonapp.views.chart.data.KamleonGraphViewMode

interface KamleonGraphViewListener {
    fun onViewModeChanged(mode: KamleonGraphViewMode)
    fun onStreakChanged(isStreak: Boolean)
    fun onGraphBarSelected()
}