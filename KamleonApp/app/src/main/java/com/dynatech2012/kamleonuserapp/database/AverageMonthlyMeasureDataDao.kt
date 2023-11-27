package com.dynatech2012.kamleonuserapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AverageMonthlyMeasureDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAverageMonthlyMeasure(averageMonthlyMeasureData: AverageMonthlyMeasureData): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAverageMonthlyMeasures(averageMonthlyMeasureDataList: List<AverageMonthlyMeasureData>)

    @Query("DELETE FROM AverageMonthlyMeasureData WHERE month < :month")
    suspend fun deleteAverageMonthlyMeasuresFromMonth(month: Int)

    @Query("SELECT * FROM AverageMonthlyMeasureData WHERE month <= :month")
    suspend fun getAverageMonthlyMeasuresFromMonth(month: Int): List<AverageMonthlyMeasureData>?

    @Query("DELETE FROM AverageMonthlyMeasureData WHERE year < :year")
    suspend fun deleteAverageMonthlyMeasuresFromYear(year: Int)

    @Query("SELECT * FROM AverageMonthlyMeasureData WHERE year <= :year")
    suspend fun getAverageMonthlyMeasuresFromYear(year: Int): List<AverageMonthlyMeasureData?>?

    @Query("SELECT * FROM AverageMonthlyMeasureData")
    suspend fun allAverageMonthlyMeasures(): List<AverageMonthlyMeasureData>?

    @Query("DELETE FROM AverageMonthlyMeasureData")
    suspend fun deleteAllAverageMonthlyMeasures()
}