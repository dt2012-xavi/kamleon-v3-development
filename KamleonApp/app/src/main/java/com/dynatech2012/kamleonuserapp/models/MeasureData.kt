package com.dynatech2012.kamleonuserapp.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class MeasureData3(
    val colorChart: Int,
    @PropertyName(value="analysisDate")
    var date: Date?,
    var day: Int,
    var month: Int,
    var year: Int,
    var msCond: Float,
    var read: Boolean = false,
    var score: Int,
    var temp: Float,
    @DocumentId
    var id: String?,
    var usage: Int,
    var usg: Float,
    var vol: Float
    ): Comparable<MeasureData3> {
    constructor() : this(0, null, 0, 0, 0, 0f, false, 0, 0f, null, 0, 0f, 0f)
    constructor(colorChart: Int, date: Date?, msCond: Float, read: Boolean, score: Int, temp: Float, theID: String?, usage: Int, usg: Float, vol: Float)
    : this(colorChart, date, 0,0,0, msCond, read, score, temp, theID, usage, usg, vol)


    //Sort by date a list of MeasureData (ArrayList<MeasureData> measuresData)
    override fun compareTo(other: MeasureData3): Int {
        val date2 = other.date
        if (date == null || date2 == null) return 0
        return date!!.compareTo(date2)
    }

    override fun toString(): String {
        return "MeasureData{" +
                "id=" + id +
                ", date=" + date +
                ", score=" + score +
                '}'
    }

    val hydrationLevel: HydrationLevel
        get() {
            return when (score) {
                in 0..30 -> HydrationLevel.VERYDEHYDRATED
                in 31..64 -> HydrationLevel.DEHYDRATED
                in 66..90 -> HydrationLevel.HYDRATED
                else -> HydrationLevel.VERYHYDRATED
            }
        }
    
    enum class HydrationLevel(val message: String, val subtitle: String) {
        VERYHYDRATED("Very Well Hydrated", "Your last urine hydration is on the recommended level."),
        HYDRATED("Hydrated", "Your last urine hydration is on the recommended level."),
        DEHYDRATED("Dehydrated", "Your last urine hydration is under the recommended level."),
        VERYDEHYDRATED("Severely dehydrated", "Your last urine hydration is under the recommended level.")
    }

}