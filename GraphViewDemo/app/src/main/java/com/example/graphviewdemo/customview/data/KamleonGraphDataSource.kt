package com.example.graphviewdemo.customview.data

import java.util.Date
import kotlin.random.Random

class KamleonGraphBarItemData(val dataVal: Double, var timestamp: Long) {
    fun toDate(): Date {
        return Date(timestamp)
    }

    companion object {
        @JvmStatic
        fun randomDataValueWith(from: Double, to: Double, timestamp: Long) =
            KamleonGraphBarItemData(Random.nextDouble(from, to), timestamp)
    }
}