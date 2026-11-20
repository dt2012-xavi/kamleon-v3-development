package com.dynatech2012.kamleonuserapp.repositories

import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject


class MeasuresRepository @Inject constructor(
    private val database: DatabaseDataSource,
    private val firestore: FirestoreDataSource,
    private val realtime: RealtimeDataSource
) {

    private var lastDateLong = 0L       // last date in DB
    private var newMeasuresFS = ArrayList<MeasureData>()
    suspend fun getUserLastMeasureFromDB(): Response<ArrayList<MeasureData>> {
        DebugLog.d(TAG) { "will try to get measures from DB 1" }
        val response = database.getAllMeasures()
        if (response.isSuccess) {
            val measures = response.dataValue
            DebugLog.d(TAG) { "got measures from DB 2 size ${measures?.size}" }
            if (measures?.isNotEmpty() == true) {
                DebugLog.d(TAG) { "got measures from DB not empty" }
                lastDateLong = measures[0].analysisDate
                DebugLog.d(TAG) { "Latest local measure found" }

            }
            else {
                DebugLog.w(TAG) { "got measures from DB EMPTY" }
                lastDateLong = 0L
            }
        }
        return response
    }

    suspend fun getUserMeasuresFromFS(userId: String?): Response<ArrayList<MeasureData>> {
        DebugLog.d(TAG) { "will try to get measures from FS 1" }
        val response = firestore.getUserMeasuresNoPag(userId, lastDateLong)
        if (response.isSuccess) {
            val measures = response.dataValue
            DebugLog.d(TAG) { "got measures from FS 2 size ${measures?.size}" }

            if (measures?.isNotEmpty() == true) {
                DebugLog.d(TAG) { "got measures from FS not empty" }
                database.saveMeasures(measures)
                lastDateLong = measures[0].analysisDate
                newMeasuresFS = measures
                DebugLog.d(TAG) { "Latest remote measure found" }
            }
            else {
                DebugLog.w(TAG) { "got measures from FS EMPTY" }
            }
        }
        return response
    }

    suspend fun getDailyAveragesFromDB(): Response<ArrayList<AverageDailyMeasureData>> {
        DebugLog.d(TAG) { "will try to get average daily from DB 1" }
        val response = database.getAllAverageDailyMeasures()
        if (response.isSuccess) {
            val avDaily = response.dataValue
            DebugLog.d(TAG) { "got average daily from DB 2 size ${avDaily?.size}" }
            if (avDaily?.isNotEmpty() == true) {
                DebugLog.d(TAG) { "got average daily from DB not empty" }
            }
            else {
                DebugLog.w(TAG) { "got average daily from DB EMPTY" }
            }
        }
        return response
    }

    suspend fun getMonthlyAveragesFromDB(): Response<ArrayList<AverageMonthlyMeasureData>> {
        DebugLog.d(TAG) { "will try to get average monthly from DB 1" }
        val response = database.getAllAverageMonthlyMeasures()
        if (response.isSuccess) {
            val avMonthly = response.dataValue
            DebugLog.d(TAG) { "got average monthly from DB 2 size ${avMonthly?.size}" }
            if (avMonthly?.isNotEmpty() == true) {
                DebugLog.d(TAG) { "got average monthly from DB not empty" }
            }
            else {
                DebugLog.w(TAG) { "got average monthly from DB EMPTY" }
            }
        }
        return response
    }

    suspend fun getUserDailyAverages(userId: String?): Response<ArrayList<AverageDailyMeasureData>> {
        if (newMeasuresFS.isEmpty()) return Response.Success(ArrayList())
        val dates = newMeasuresFS.map { Date(it.analysisDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
        val newDays = dates.distinct()
        val response = realtime.getDaysAverages(userId, newDays)
        if (response.isSuccess) {
            val measures = response.dataValue
            if (measures?.isNotEmpty() == true) {
                DebugLog.d(TAG) { "got measures from FS not empty" }
                saveNewAverageDailyMeasuresToDB(measures)
                /*
                val saveJob = coroutineScope { async {
                       saveNewAverageDailyMeasuresToDB(measures)
                    } }
                saveJob.await()
                */
            } }
        return response
    }
    private suspend fun saveNewAverageDailyMeasuresToDB(measureDataList: List<AverageDailyMeasureData?>?) {
        if (measureDataList != null) {
            val measureDataListNotNull = measureDataList.filterNotNull()
            database.saveAverageDailyMeasures(ArrayList(measureDataListNotNull))
        }
    }

    suspend fun getUserMonthlyAverages(userId: String?): Response<ArrayList<AverageMonthlyMeasureData>> {
        if (newMeasuresFS.isEmpty()) return Response.Success(ArrayList())
        val dates = newMeasuresFS.map { Date(it.analysisDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
        val newMonths = dates.distinctBy { it.year to it.month }
        val response = realtime.getMonthsAverages(userId, newMonths)
        if (response.isSuccess) {
            val measures = response.dataValue
            if (measures?.isNotEmpty() == true) {
                DebugLog.d(TAG) { "got measures from FS not empty" }
                saveNewAverageMonthlyMeasuresToDB(measures)
                /*
                val saveJob = coroutineScope { async {
                       saveNewAverageMonthlyMeasuresToDB(measures)
                    } }
                saveJob.await()
                */
            } }
        return response
    }

    private suspend fun saveNewAverageMonthlyMeasuresToDB(measureDataList: List<AverageMonthlyMeasureData?>?) {
        if (measureDataList != null) {
            val measureDataListNotNull = measureDataList.filterNotNull()
            database.saveAverageMonthlyMeasures(ArrayList(measureDataListNotNull))
        }
    }

    /*
    fun closeChannels() {
        realtimeRepository.closeChannels()
    }
    */

    /*
    fun getAllUserDailyAverages(userId: String?): Flow<Response<ArrayList<AverageDailyMeasureData>>> = callbackFlow {
        realtime.getAllDaysAverages(userId).collect {
            insertNewAverageDailyMeasuresToDB(it)
            trySend(Response.Success(it))
        }
        awaitClose {  }
    }
    fun getAllUserMonthlyAverages(userId: String?): Flow<Response<ArrayList<AverageMonthlyMeasureData>>> = callbackFlow {
        realtime.getAllMonthsAverages(userId).collect {
            insertNewAverageMonthlyMeasuresToDB(it)
            trySend(Response.Success(it))
        }
        awaitClose {  }
    }
    */

    companion object {
        val TAG = MeasuresRepository::class.simpleName
    }
}