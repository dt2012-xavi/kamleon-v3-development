package com.dynatech2012.kamleonuserapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dynatech2012.kamleonuserapp.models.IntVectorTypeConverter

@Database(entities = [AverageMonthlyMeasureData::class], version = 2)
abstract class AverageMonthlyMeasureDatabase : RoomDatabase() {
    abstract fun averageMonthlyMeasureDao(): AverageMonthlyMeasureDataDao
}