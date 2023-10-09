package com.example.graphviewdemo.customview

import com.example.graphviewdemo.customview.data.KamleonGraphViewMode

interface KamleonGraphViewListener {
    fun onViewModeChanged(mode: KamleonGraphViewMode)
    fun onStreakChanged(isStreak: Boolean)
    fun onGraphBarSelected()
}