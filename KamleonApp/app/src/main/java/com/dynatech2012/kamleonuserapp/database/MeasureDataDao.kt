package com.dynatech2012.kamleonuserapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MeasureDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasure(measureData: MeasureData): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasures(measureDataList: List<MeasureData>)

    @Query("DELETE FROM MeasureData WHERE analysisDate < :analysisDate")
    suspend fun deleteMeasuresFromDate(analysisDate: Long)

    @Query("SELECT * FROM MeasureData WHERE analysisDate <= :date")
    suspend fun getMeasuresFromDate(date: Long): List<MeasureData>?

    @Query("SELECT * FROM MeasureData ORDER BY analysisDate DESC")
    suspend fun getAllMeasures(): List<MeasureData>?

    @Query("DELETE FROM MeasureData")
    suspend fun deleteAllMeasures()

}