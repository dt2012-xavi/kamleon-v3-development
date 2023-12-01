package com.dynatech2012.kamleonuserapp.views.graph

import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphViewMode

interface KamleonGraphViewListener {
    fun onViewModeChanged(mode: KamleonGraphViewMode)
    fun onStreakChanged(isStreak: Boolean)
    fun onGraphBarSelected()
}