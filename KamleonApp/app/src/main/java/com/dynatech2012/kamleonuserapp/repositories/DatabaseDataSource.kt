package com.dynatech2012.kamleonuserapp.repositories

import android.util.Log
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureDataDao
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureDataDao
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureDataDao
import javax.inject.Inject

/**
 *
 * <font color="teal">
 * Edu:<br></br>
 * This repository has the responsibility to access the databases through Daos <br></br>
 * It needs an Executor to execute tasks in other thread different from UIThread
</font> */
class DatabaseDataSource @Inject constructor(
    private val measureDataDao: MeasureDataDao,
    private val averageDailyMeasureDataDao: AverageDailyMeasureDataDao,
    private val averageMonthlyMeasureDataDao: AverageMonthlyMeasureDataDao
) {
    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * ----- MeasureDataDao -----
    </font> */

    suspend fun saveMeasures(data: ArrayList<MeasureData>): ResponseNullable<Nothing> {
        measureDataDao.insertMeasures(data)
        return ResponseNullable.Success()
    }

    suspend fun getMeasures(date: Long): Response<ArrayList<MeasureData>> {
        val data = measureDataDao.getMeasuresFromDate(date)
        return if (data != null)
            Response.Success(ArrayList(data))
        else
            Response.Success(ArrayList())
    }

    suspend fun getAllMeasures(): Response<ArrayList<MeasureData>>
    {
        // get measures from database
        val data = measureDataDao.getAllMeasures()
        Log.d(TAG, "measures data size: " + data?.size)
        return if (data != null)
            Response.Success(ArrayList(data))
        else
            Response.Success(ArrayList())
    }

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * ----- AverageDailyMeasureDataDao -----
    </font> */
    suspend fun saveAverageDailyMeasures(data: ArrayList<AverageDailyMeasureData>): ResponseNullable<Nothing> {
        averageDailyMeasureDataDao.insertAverageDailyMeasures(data)
        return ResponseNullable.Success()
    }

    /*
    suspend fun getAverageDailyMeasures(day: Int): Response<ArrayList<AverageDailyMeasureData>> {
        val data = averageDailyMeasureDataDao.getAverageDailyMeasuresFromDay(day)
        return if (data != null)
            Response.Success(ArrayList(data))
        else
            Response.Success(ArrayList())
    }
     */

    suspend fun getAllAverageDailyMeasures(): Response<ArrayList<AverageDailyMeasureData>> {
        val data = averageDailyMeasureDataDao.getAllAverageDailyMeasures()
        return if (data != null)
            Response.Success(ArrayList(data))
        else
            Response.Success(ArrayList())
    }

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * ----- AverageMonthlyMeasureDataDao -----
    </font> */
    suspend fun saveAverageMonthlyMeasures(data: ArrayList<AverageMonthlyMeasureData>): ResponseNullable<Nothing> {
        averageMonthlyMeasureDataDao.insertAverageMonthlyMeasures(data)
        return ResponseNullable.Success()
    }

    /*
    suspend fun getAverageMonthlyMeasures(month: Int): Response<ArrayList<AverageMonthlyMeasureData>> {
        val data = averageMonthlyMeasureDataDao.getAverageMonthlyMeasuresFromMonth(month)
        return if (data != null)
            Response.Success(ArrayList(data))
        else
            Response.Success(ArrayList())    }
     */

    suspend fun getAllAverageMonthlyMeasures(): Response<ArrayList<AverageMonthlyMeasureData>> {
        val data = averageMonthlyMeasureDataDao.getAllAverageMonthlyMeasures()
        return if (data != null)
            Response.Success(ArrayList(data))
        else
            Response.Success(ArrayList())
    }

    suspend fun deleteAllMeasures() {
        measureDataDao.deleteAllMeasures()
        averageDailyMeasureDataDao.deleteAllAverageDailyMeasures()
        averageMonthlyMeasureDataDao.deleteAllAverageMonthlyMeasures()
    }

    companion object {
        private val TAG = DatabaseDataSource::class.java.simpleName
    }
}