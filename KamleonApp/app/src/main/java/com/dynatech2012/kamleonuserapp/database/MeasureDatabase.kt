package com.dynatech2012.kamleonuserapp.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dynatech2012.kamleonuserapp.models.IntVectorTypeConverter

@Database(entities = [MeasureData::class], version = 2)
@TypeConverters(IntVectorTypeConverter::class)
abstract class MeasureDatabase : RoomDatabase() {
    abstract fun measureDao(): MeasureDataDao
}