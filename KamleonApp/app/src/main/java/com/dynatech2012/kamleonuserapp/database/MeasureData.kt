package com.dynatech2012.kamleonuserapp.database

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dynatech2012.kamleonuserapp.models.IntVectorTypeConverter
import com.dynatech2012.kamleonuserapp.models.MeasurePrecision
import com.dynatech2012.kamleonuserapp.models.MeasureType
import com.dynatech2012.kamleonuserapp.models.RawMeasureData
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import java.util.Date

@Entity
data class MeasureData (
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var isNew: Boolean = false,

    //    private long analysisDate; //Date
    var analysisDate: Long = 0, //Date

    //    private long GS_lastUpdateAnalysis; //Timestamp
    var gS_lastUpdateAnalysis: Long = 0, //Timestamp
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
    var type: MeasureType? = null, //String
    var precision: MeasurePrecision? = null, //String

    @TypeConverters( IntVectorTypeConverter::class )
    var xyz: IntArray? = null, //ArrayList<Integer> xyz = new ArrayList<Integer>();

    @TypeConverters(IntVectorTypeConverter::class)
    var minxyz: IntArray? = null,

    @TypeConverters(IntVectorTypeConverter::class)
    var maxxyz: IntArray? = null,

    @TypeConverters(IntVectorTypeConverter::class)
    var stdxyz: IntArray? = null,

    @TypeConverters(IntVectorTypeConverter::class)
    var waterXYZ: IntArray? = null,

    @TypeConverters(IntVectorTypeConverter::class)
    var colorUrineRGB //[int,int,int]
            : IntArray? = null,

    @TypeConverters(IntVectorTypeConverter::class)
    var a11Band: IntArray? = null,

    @TypeConverters(IntVectorTypeConverter::class)
    var min11Band: IntArray? = null,

    @TypeConverters(IntVectorTypeConverter::class)
    var max11Band: IntArray? = null,

    @TypeConverters(IntVectorTypeConverter::class)
    var std11Band: IntArray? = null,

    @TypeConverters(IntVectorTypeConverter::class)
    var water11Band //[int,...,int]
            : IntArray? = null,
    override var timestamp: Long,
    ) : AveragesData, Parcelable, Comparable<MeasureData> {

    constructor() : this (
        id = 0,
        isNew = false,
        analysisDate = 0,
        gS_lastUpdateAnalysis = 0,
        measEventDuration = 0,
        volTime = 0,
        cond = 0,
        weight = 0,
        usg = 0,
        score = 60,
        colorChart = 0,
        humidity = 0,
        gS_ColorChart = 0,
        gS_USG = 0,
        gS_Volume = 0,
        weightTime = 0,
        vol = 0f,
        msCond = 0f,
        temp = 0f,
        centerID = null,
        deviceID = null,
        orgID = null,
        userID = null,
        teamID = null,
        waterReference = null,
        measType = null,
        usgModel = null,
        volModel = null,
        condModel = null,
        waterDetectModel = null,
        waterDetect = null,
        gS_Chemist = null,
        gS_Observation = null,
        type = MeasureType.urine,
        precision = MeasurePrecision.Good,
        xyz = null,
        minxyz = null,
        maxxyz = null,
        stdxyz = null,
        waterXYZ = null,
        colorUrineRGB = null,
        a11Band = null,
        min11Band = null,
        max11Band = null,
        std11Band = null,
        water11Band = null,
        timestamp = 0,
    )

    constructor(rawMeasureData: RawMeasureData) : this (
        analysisDate = rawMeasureData.analysisDate?.time ?: 0,
        gS_lastUpdateAnalysis = 0,//rawMeasureData.gS_lastUpdateAnalysis.time,
        measEventDuration = rawMeasureData.measEventDuration,
        volTime = rawMeasureData.volTime,
        cond = rawMeasureData.cond,
        weight = rawMeasureData.weight,
        usg = rawMeasureData.usg,
        score = rawMeasureData.score,
        colorChart = rawMeasureData.colorChart,
        humidity = rawMeasureData.humidity,
        gS_ColorChart = rawMeasureData.gS_ColorChart,
        gS_USG = rawMeasureData.gS_USG,
        gS_Volume = rawMeasureData.gS_Volume,
        weightTime = rawMeasureData.weightTime,
        vol = rawMeasureData.vol,
        msCond = rawMeasureData.msCond,
        temp = rawMeasureData.temp,
        centerID = rawMeasureData.centerID,
        deviceID = rawMeasureData.deviceID,
        orgID = rawMeasureData.orgID,
        userID = rawMeasureData.userID,
        teamID = rawMeasureData.teamID,
        waterReference = rawMeasureData.waterReference,
        measType = rawMeasureData.measType,
        usgModel = rawMeasureData.usgModel,
        volModel = rawMeasureData.volModel,
        condModel = rawMeasureData.condModel,
        waterDetectModel = rawMeasureData.waterDetectModel,
        waterDetect = rawMeasureData.waterDetect,
        gS_Chemist = rawMeasureData.gS_Chemist,
        gS_Observation = rawMeasureData.gS_Observation,
        type = rawMeasureData.type,
        precision = rawMeasureData.precision,
        xyz = rawMeasureData.xyz,
        minxyz = rawMeasureData.minxyz,
        maxxyz = rawMeasureData.maxxyz,
        stdxyz = rawMeasureData.stdxyz,
        waterXYZ = rawMeasureData.waterXYZ,
        colorUrineRGB = rawMeasureData.colorUrineRGB,
        a11Band = rawMeasureData.a11Band,
        min11Band = rawMeasureData.min11Band,
        max11Band = rawMeasureData.max11Band,
        std11Band = rawMeasureData.std11Band,
        water11Band = rawMeasureData.water11Band,
        timestamp = rawMeasureData.analysisDate?.time ?: 0,
        )

    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readByte() != 0.toByte(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        MeasureType.fromString(parcel.readString()),
        MeasurePrecision.fromString(parcel.readString()),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.createIntArray(),
        parcel.readLong(),
    ) {
    }

    //Sort by date a list of MeasureData (ArrayList<MeasureData> measuresData)
    override fun compareTo(other: MeasureData): Int {
        val date1 = Date(analysisDate)
        val date2 = Date(other.analysisDate)
        //        if (getAnalysisDate() == null || o.getAnalysisDate() == null)
        return if (date1 == null || date2 == null) 0 else date1.compareTo(date2)
    }

    override fun toString(): String {
        return "MeasureData{" +
                "id=" + id +
                ", isNew=" + isNew +
                ", analysisDate=" + analysisDate +
                ", score=" + score +  /*", GS_lastUpdateAnalysis=" + GS_lastUpdateAnalysis +
                ", measEventDuration=" + measEventDuration +
                ", volTime=" + volTime +
                ", cond=" + cond +
                ", weight=" + weight +
                ", usg=" + usg +
                ", score=" + score +
                ", colorChart=" + colorChart +
                ", humidity=" + humidity +
                ", GS_ColorChart=" + GS_ColorChart +
                ", GS_USG=" + GS_USG +
                ", GS_Volume=" + GS_Volume +
                ", weightTime=" + weightTime +
                ", vol=" + vol +
                ", msCond=" + msCond +
                ", temp=" + temp +
                ", centerID='" + centerID + '\'' +
                ", deviceID='" + deviceID + '\'' +
                ", orgID='" + orgID + '\'' +
                ", userID='" + userID + '\'' +
                ", teamID='" + teamID + '\'' +
                ", waterReference='" + waterReference + '\'' +
                ", measType='" + measType + '\'' +
                ", usgModel='" + usgModel + '\'' +
                ", volModel='" + volModel + '\'' +
                ", condModel='" + condModel + '\'' +
                ", waterDetectModel='" + waterDetectModel + '\'' +
                ", waterDetect='" + waterDetect + '\'' +
                ", GS_Chemist='" + GS_Chemist + '\'' +
                ", GS_Observation='" + GS_Observation + '\'' +
                ", xyz=" + Arrays.toString(xyz) +
                ", minxyz=" + Arrays.toString(minxyz) +
                ", maxxyz=" + Arrays.toString(maxxyz) +
                ", stdxyz=" + Arrays.toString(stdxyz) +
                ", waterXYZ=" + Arrays.toString(waterXYZ) +
                ", colorUrineRGB=" + Arrays.toString(colorUrineRGB) +
                ", a11Band=" + Arrays.toString(a11Band) +
                ", min11Band=" + Arrays.toString(min11Band) +
                ", max11Band=" + Arrays.toString(max11Band) +
                ", std11Band=" + Arrays.toString(std11Band) +
                ", water11Band=" + Arrays.toString(water11Band) +*/
                '}'
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeByte(if (isNew) 1 else 0)
        parcel.writeLong(analysisDate)
        parcel.writeLong(gS_lastUpdateAnalysis)
        parcel.writeInt(measEventDuration)
        parcel.writeInt(volTime)
        parcel.writeInt(cond)
        parcel.writeInt(weight)
        parcel.writeInt(usg)
        parcel.writeInt(score)
        parcel.writeInt(colorChart)
        parcel.writeInt(humidity)
        parcel.writeInt(gS_ColorChart)
        parcel.writeInt(gS_USG)
        parcel.writeInt(gS_Volume)
        parcel.writeInt(weightTime)
        parcel.writeFloat(vol)
        parcel.writeFloat(msCond)
        parcel.writeFloat(temp)
        parcel.writeString(centerID)
        parcel.writeString(deviceID)
        parcel.writeString(orgID)
        parcel.writeString(userID)
        parcel.writeString(teamID)
        parcel.writeString(waterReference)
        parcel.writeString(measType)
        parcel.writeString(usgModel)
        parcel.writeString(volModel)
        parcel.writeString(condModel)
        parcel.writeString(waterDetectModel)
        parcel.writeString(waterDetect)
        parcel.writeString(gS_Chemist)
        parcel.writeString(gS_Observation)
        parcel.writeString(type.toString())
        parcel.writeString(precision.toString())
        parcel.writeIntArray(xyz)
        parcel.writeIntArray(minxyz)
        parcel.writeIntArray(maxxyz)
        parcel.writeIntArray(stdxyz)
        parcel.writeIntArray(waterXYZ)
        parcel.writeIntArray(colorUrineRGB)
        parcel.writeIntArray(a11Band)
        parcel.writeIntArray(min11Band)
        parcel.writeIntArray(max11Band)
        parcel.writeIntArray(std11Band)
        parcel.writeIntArray(water11Band)
        parcel.writeLong(timestamp)
    }

    override fun describeContents(): Int {
        return 0
    }

    override val isPrecise
        get() = precision != MeasurePrecision.Bad

    override fun scoreValue(type: KamleonGraphDataType): Double {
        return when (type) {
            KamleonGraphDataType.hydration -> score.toDouble()
            KamleonGraphDataType.electrolytes -> msCond.toDouble()
            KamleonGraphDataType.volume -> vol.toDouble()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MeasureData

        if (id != other.id) return false
        if (isNew != other.isNew) return false
        if (analysisDate != other.analysisDate) return false
        if (gS_lastUpdateAnalysis != other.gS_lastUpdateAnalysis) return false
        if (measEventDuration != other.measEventDuration) return false
        if (volTime != other.volTime) return false
        if (cond != other.cond) return false
        if (weight != other.weight) return false
        if (usg != other.usg) return false
        if (score != other.score) return false
        if (colorChart != other.colorChart) return false
        if (humidity != other.humidity) return false
        if (gS_ColorChart != other.gS_ColorChart) return false
        if (gS_USG != other.gS_USG) return false
        if (gS_Volume != other.gS_Volume) return false
        if (weightTime != other.weightTime) return false
        if (vol != other.vol) return false
        if (msCond != other.msCond) return false
        if (temp != other.temp) return false
        if (centerID != other.centerID) return false
        if (deviceID != other.deviceID) return false
        if (orgID != other.orgID) return false
        if (userID != other.userID) return false
        if (teamID != other.teamID) return false
        if (waterReference != other.waterReference) return false
        if (measType != other.measType) return false
        if (usgModel != other.usgModel) return false
        if (volModel != other.volModel) return false
        if (condModel != other.condModel) return false
        if (waterDetectModel != other.waterDetectModel) return false
        if (waterDetect != other.waterDetect) return false
        if (gS_Chemist != other.gS_Chemist) return false
        if (gS_Observation != other.gS_Observation) return false
        if (type != other.type) return false
        if (precision != other.precision) return false
        if (xyz != null) {
            if (other.xyz == null) return false
            if (!xyz.contentEquals(other.xyz)) return false
        } else if (other.xyz != null) return false
        if (minxyz != null) {
            if (other.minxyz == null) return false
            if (!minxyz.contentEquals(other.minxyz)) return false
        } else if (other.minxyz != null) return false
        if (maxxyz != null) {
            if (other.maxxyz == null) return false
            if (!maxxyz.contentEquals(other.maxxyz)) return false
        } else if (other.maxxyz != null) return false
        if (stdxyz != null) {
            if (other.stdxyz == null) return false
            if (!stdxyz.contentEquals(other.stdxyz)) return false
        } else if (other.stdxyz != null) return false
        if (waterXYZ != null) {
            if (other.waterXYZ == null) return false
            if (!waterXYZ.contentEquals(other.waterXYZ)) return false
        } else if (other.waterXYZ != null) return false
        if (colorUrineRGB != null) {
            if (other.colorUrineRGB == null) return false
            if (!colorUrineRGB.contentEquals(other.colorUrineRGB)) return false
        } else if (other.colorUrineRGB != null) return false
        if (a11Band != null) {
            if (other.a11Band == null) return false
            if (!a11Band.contentEquals(other.a11Band)) return false
        } else if (other.a11Band != null) return false
        if (min11Band != null) {
            if (other.min11Band == null) return false
            if (!min11Band.contentEquals(other.min11Band)) return false
        } else if (other.min11Band != null) return false
        if (max11Band != null) {
            if (other.max11Band == null) return false
            if (!max11Band.contentEquals(other.max11Band)) return false
        } else if (other.max11Band != null) return false
        if (std11Band != null) {
            if (other.std11Band == null) return false
            if (!std11Band.contentEquals(other.std11Band)) return false
        } else if (other.std11Band != null) return false
        if (water11Band != null) {
            if (other.water11Band == null) return false
            if (!water11Band.contentEquals(other.water11Band)) return false
        } else if (other.water11Band != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + isNew.hashCode()
        result = 31 * result + analysisDate.hashCode()
        result = 31 * result + gS_lastUpdateAnalysis.hashCode()
        result = 31 * result + measEventDuration
        result = 31 * result + volTime
        result = 31 * result + cond
        result = 31 * result + weight
        result = 31 * result + usg
        result = 31 * result + score
        result = 31 * result + colorChart
        result = 31 * result + humidity
        result = 31 * result + gS_ColorChart
        result = 31 * result + gS_USG
        result = 31 * result + gS_Volume
        result = 31 * result + weightTime
        result = 31 * result + vol.hashCode()
        result = 31 * result + msCond.hashCode()
        result = 31 * result + temp.hashCode()
        result = 31 * result + (centerID?.hashCode() ?: 0)
        result = 31 * result + (deviceID?.hashCode() ?: 0)
        result = 31 * result + (orgID?.hashCode() ?: 0)
        result = 31 * result + (userID?.hashCode() ?: 0)
        result = 31 * result + (teamID?.hashCode() ?: 0)
        result = 31 * result + (waterReference?.hashCode() ?: 0)
        result = 31 * result + (measType?.hashCode() ?: 0)
        result = 31 * result + (usgModel?.hashCode() ?: 0)
        result = 31 * result + (volModel?.hashCode() ?: 0)
        result = 31 * result + (condModel?.hashCode() ?: 0)
        result = 31 * result + (waterDetectModel?.hashCode() ?: 0)
        result = 31 * result + (waterDetect?.hashCode() ?: 0)
        result = 31 * result + (gS_Chemist?.hashCode() ?: 0)
        result = 31 * result + (gS_Observation?.hashCode() ?: 0)
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + (precision?.hashCode() ?: 0)
        result = 31 * result + (xyz?.contentHashCode() ?: 0)
        result = 31 * result + (minxyz?.contentHashCode() ?: 0)
        result = 31 * result + (maxxyz?.contentHashCode() ?: 0)
        result = 31 * result + (stdxyz?.contentHashCode() ?: 0)
        result = 31 * result + (waterXYZ?.contentHashCode() ?: 0)
        result = 31 * result + (colorUrineRGB?.contentHashCode() ?: 0)
        result = 31 * result + (a11Band?.contentHashCode() ?: 0)
        result = 31 * result + (min11Band?.contentHashCode() ?: 0)
        result = 31 * result + (max11Band?.contentHashCode() ?: 0)
        result = 31 * result + (std11Band?.contentHashCode() ?: 0)
        result = 31 * result + (water11Band?.contentHashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<MeasureData> {
        override fun createFromParcel(parcel: Parcel): MeasureData {
            return MeasureData(parcel)
        }

        override fun newArray(size: Int): Array<MeasureData?> {
            return arrayOfNulls(size)
        }
    }

    val hydrationLevel: HydrationLevel
        get() {
            return when (score) {
                in 0..30 -> HydrationLevel.VERYDEHYDRATED
                in 31..64 -> HydrationLevel.DEHYDRATED
                in 65..90 -> HydrationLevel.HYDRATED
                else -> HydrationLevel.VERYHYDRATED
            }
        }

    enum class HydrationLevel {
        VERYHYDRATED,
        HYDRATED,
        DEHYDRATED,
        VERYDEHYDRATED
    }
}