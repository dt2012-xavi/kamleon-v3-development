package com.dynatech2012.kamleonuserapp.models

import androidx.room.TypeConverters
import com.google.firebase.firestore.PropertyName
import java.sql.Timestamp
import java.util.Date

data class RawMeasureData(
    var analysisDate: Date? = null, //Date
    var gS_lastUpdateAnalysis: Timestamp? = null, //Timestamp
    var measEventDuration: Int = 0,
    var volTime: Int = 0,
    var cond: Int = 0,
    var weight: Int = 0,
    var usg: Int = 0,
    var score: Int = 0,
    var colorChart: Int = 0,
    var humidity: Int = 0,
    var gS_ColorChart: Int = 0,
    var gS_USG: Int = 0,
    var gS_Volume: Int = 0, //int
    var weightTime: Int = 0, //ed int
    var vol: Float = 0f,
    var msCond: Float = 0f,
    var temp: Float = 0f, //float
    var centerID: String? = null,
    var deviceID: String? = null,
    var orgID: String? = null,
    var userID: String? = null,
    var teamID: String? = null,
    var waterReference: String? = null, //stringID
    var measType: String? = null,
    var usgModel: String? = null,
    var volModel: String? = null,
    var condModel: String? = null,
    var waterDetectModel: String? = null,
    var waterDetect: String? = null,
    var gS_Chemist: String? = null,
    var gS_Observation: String? = null, //String
    @PropertyName(value="ID")
    var type: MeasureType? = null, //String
    @PropertyName(value="Prediction_Precision")
    var precision: MeasurePrecision? = null, //String

    @TypeConverters(IntVectorTypeConverter::class)
    var xyz: IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var minxyz: IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var maxxyz: IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var stdxyz: IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var waterXYZ: IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var colorUrineRGB //[int,int,int]
            : IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var a11Band: IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var min11Band: IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var max11Band: IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var std11Band: IntArray,

    @TypeConverters(IntVectorTypeConverter::class)
    var water11Band: IntArray
    ) : Comparable<RawMeasureData> {

    constructor() : this(null, null,
        0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0f, 0f, 0f,
        null, null, null, null, null, null, null,
        null, null, null, null, null, null, null,
        MeasureType.urine, MeasurePrecision.Good,
        IntArray(0), IntArray(0), IntArray(0), IntArray(0), IntArray(0), IntArray(0),
        IntArray(0), IntArray(0), IntArray(0), IntArray(0), IntArray(0))

    //Sort by date a list of MeasureData (ArrayList<MeasureData> measuresData)
    override fun compareTo(other: RawMeasureData): Int {
        return if (this.analysisDate == null || other.analysisDate == null) 0 else analysisDate!!.compareTo(other.analysisDate)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawMeasureData

        if (!xyz.contentEquals(other.xyz)) return false
        if (!minxyz.contentEquals(other.minxyz)) return false
        if (!maxxyz.contentEquals(other.maxxyz)) return false
        if (!stdxyz.contentEquals(other.stdxyz)) return false
        if (!waterXYZ.contentEquals(other.waterXYZ)) return false
        if (!colorUrineRGB.contentEquals(other.colorUrineRGB)) return false
        if (!a11Band.contentEquals(other.a11Band)) return false
        if (!min11Band.contentEquals(other.min11Band)) return false
        if (!max11Band.contentEquals(other.max11Band)) return false
        if (!std11Band.contentEquals(other.std11Band)) return false
        if (!water11Band.contentEquals(other.water11Band)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = xyz.contentHashCode()
        result = 31 * result + minxyz.contentHashCode()
        result = 31 * result + maxxyz.contentHashCode()
        result = 31 * result + stdxyz.contentHashCode()
        result = 31 * result + waterXYZ.contentHashCode()
        result = 31 * result + colorUrineRGB.contentHashCode()
        result = 31 * result + a11Band.contentHashCode()
        result = 31 * result + min11Band.contentHashCode()
        result = 31 * result + max11Band.contentHashCode()
        result = 31 * result + std11Band.contentHashCode()
        result = 31 * result + water11Band.contentHashCode()
        return result
    }
}

enum class MeasureType {
    urine;
    companion object {
        fun fromString(value: String?): MeasureType? {
            if (value == null) return null
            return MeasureType.valueOf(value)
        }
    }
}
enum class MeasurePrecision {
    Good, Bad;
    companion object {
        fun fromString(value: String?): MeasurePrecision? {
            if (value == null) return null
            return MeasurePrecision.valueOf(value)
        }
    }
}