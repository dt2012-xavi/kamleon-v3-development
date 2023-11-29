package com.dynatech2012.kamleonuserapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AverageDailyMeasureDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAverageDailyMeasure(averageDailyMeasureData: AverageDailyMeasureData): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAverageDailyMeasures(averageDailyMeasureDataList: List<AverageDailyMeasureData>)

    @Query("DELETE FROM AverageDailyMeasureData WHERE day < :day")
    suspend fun deleteAverageDailyMeasuresFromDay(day: Int)

    @Query("SELECT * FROM AverageDailyMeasureData WHERE day <= :day")
    suspend fun getAverageDailyMeasuresFromDay(day: Int): List<AverageDailyMeasureData>?

    @Query("DELETE FROM AverageDailyMeasureData WHERE month < :month")
    suspend fun deleteAverageDailyMeasuresFromMonth(month: Int)

    @Query("SELECT * FROM AverageDailyMeasureData WHERE month <= :month")
    suspend fun getAverageDailyMeasuresFromMonth(month: Int): List<AverageDailyMeasureData?>?

    @Query("DELETE FROM AverageDailyMeasureData WHERE year < :year")
    suspend fun deleteAverageDailyMeasuresFromYear(year: Int)

    @Query("SELECT * FROM AverageDailyMeasureData WHERE year <= :year")
    suspend fun getAverageDailyMeasuresFromYear(year: Int): List<AverageDailyMeasureData?>?

    @Query("SELECT * FROM AverageDailyMeasureData")
    suspend fun getAllAverageDailyMeasures(): List<AverageDailyMeasureData>?

    @Query("DELETE FROM AverageDailyMeasureData")
    suspend fun deleteAllAverageDailyMeasures()
}