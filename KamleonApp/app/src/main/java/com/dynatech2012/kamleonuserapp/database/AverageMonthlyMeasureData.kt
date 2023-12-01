package com.dynatech2012.kamleonuserapp.database

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dynatech2012.kamleonuserapp.views.graph.data.KamleonGraphDataType
import com.google.firebase.database.DataSnapshot
import java.util.GregorianCalendar

@Entity
data class AverageMonthlyMeasureData(
    @JvmField
    @PrimaryKey(autoGenerate = false)
    var id: String,

    //date: 10/2021
    //year = 2021
    //month = 202110
    var hydration: Double,
    var colorChart: Float,
    var maxScore: Int,
    var minScore: Int,
    var num: Int,
    var usg: Float,
    var volume: Double,
    var year: Int,
    var month: Int,
    override var timestamp: Long,
    var msCond: Double
): Parcelable, AveragesData {

    constructor(
        averageScore: Double,
        colorChart: Float,
        maxScore: Int,
        minScore: Int,
        msCond: Double,
        num: Int,
        usg: Float,
        volume: Double,
        year: Int,
        month: Int,
        timestamp: Long
    ) : this(year.toString() + month.toString(), averageScore, colorChart, maxScore, minScore, num, usg, volume, year, month, timestamp, msCond)

    constructor(dataSnapshot: DataSnapshot, year: Int, month: Int) : this(
        year.toString() + month.toString(),
        dataSnapshot.child("averageScore").value.toString().toDouble(),
        dataSnapshot.child("colorChart").value.toString().toFloat(),
        dataSnapshot.child("maxScore").value.toString().toInt(),
        dataSnapshot.child("minScore").value.toString().toInt(),
        dataSnapshot.child("num").value.toString().toInt(),
        dataSnapshot.child("usg").value.toString().toFloat(),
        dataSnapshot.child("volume").value.toString().toDouble(),
        year, month,
        GregorianCalendar(year, month -1, 1).time.time,
        dataSnapshot.child("msCond").value.toString().toDouble(),
    )


    constructor(parcel: Parcel) : this(
        parcel.readString()!!, //id
        parcel.readDouble(),
        parcel.readFloat(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readDouble(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readLong(),
        parcel.readDouble()
    )


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeDouble(hydration)
        parcel.writeFloat(colorChart)
        parcel.writeInt(maxScore)
        parcel.writeInt(minScore)
        parcel.writeInt(num)
        parcel.writeFloat(usg)
        parcel.writeDouble(volume)
        parcel.writeInt(year)
        parcel.writeInt(month)
        parcel.writeLong(timestamp)
        parcel.writeDouble(msCond)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun scoreValue(type: KamleonGraphDataType): Double {
        return when(type) {
            KamleonGraphDataType.hydration -> hydration
            KamleonGraphDataType.electrolytes -> msCond
            KamleonGraphDataType.volume -> volume
        }
    }

    val allParams: String
        get() = "averageScore=$hydration, colorChart=$colorChart, maxScore=$maxScore, minScore=$minScore, msCond=$msCond, num=$num, usg=$usg, volume=$volume, year=$year, month=$month"

    companion object CREATOR : Parcelable.Creator<AverageMonthlyMeasureData> {
        override fun createFromParcel(parcel: Parcel): AverageMonthlyMeasureData {
            return AverageMonthlyMeasureData(parcel)
        }

        override fun newArray(size: Int): Array<AverageMonthlyMeasureData?> {
            return arrayOfNulls(size)
        }
    }
}