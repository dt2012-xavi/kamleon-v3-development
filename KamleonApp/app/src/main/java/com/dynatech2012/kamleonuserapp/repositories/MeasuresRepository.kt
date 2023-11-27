package com.dynatech2012.kamleonuserapp.repositories

import android.util.Log
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class MeasuresRepository @Inject constructor(
    private val database: DatabaseDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val realtimeRepository: RealtimeRepository) {

    private val db = Firebase.firestore
    private lateinit var user: CustomUser
    private val storage = Firebase.storage

    var lastDateLong = 0L       // last date in DB
    suspend fun getUserLastMeasure(): Response<ArrayList<MeasureData>> {
        Log.d(TAG, "will try to get measures from DB 1")
        val response = database.getAllMeasures()
        if (response.isSuccess) {
            val measures = response.dataValue
            Log.d(TAG, "got measures from DB 2 size ${measures?.size}")
            if (measures?.isNotEmpty() == true) {
                Log.d(TAG, "got measures from DB not empty")
                lastDateLong = measures[0].analysisDate
                Log.d(TAG, "got measures from DB 4 score last: ${measures[0].score}")
            }
            else {
                Log.w(TAG, "got measures from DB EMPTY")
            }
        }
        return response
    }

    suspend fun getUserMeasures(userId: String?): Response<ArrayList<MeasureData>> {
        Log.d(TAG, "will try to get measures from FS 1")
        val response = firestoreDataSource.getUserMeasuresNoPag(userId, lastDateLong, null)
        if (response.isSuccess) {
            val measures = response.dataValue
            Log.d(TAG, "got measures from FS 2 size ${measures?.size}")
            if (measures?.isNotEmpty() == true) {
                Log.d(TAG, "got measures from FS not empty")
                database.saveMeasures(measures)
                Log.d(TAG, "got measures from FS 4 score last: ${measures[0].score}")
            }
            else {
                Log.w(TAG, "got measures from FS EMPTY")
            }
        }
        return response
    }

    fun getUserDailyAverages(userId: String?, measures: ArrayList<MeasureData>): Flow<Response<ArrayList<AverageDailyMeasureData>>> = callbackFlow {
        val dates = measures.map { Date(it.analysisDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
        val newDays = dates.distinct()
        realtimeRepository.getDaysAverages(userId, newDays).collect {
            insertNewAverageDailyMeasuresToDB(it)
            trySend(Response.Success(it))
        }
        awaitClose {  }
    }
    private suspend fun insertNewAverageDailyMeasuresToDB(measureDataList: List<AverageDailyMeasureData?>?) {
        if (measureDataList != null) {
            val measureDataListNotNull = measureDataList.filterNotNull()
            database.saveAverageDailyMeasures(ArrayList(measureDataListNotNull))
        }
    }

    fun getUserMonthlyAverages(userId: String?, measures: ArrayList<MeasureData>): Flow<Response<ArrayList<AverageMonthlyMeasureData>>> = callbackFlow {
        val dates = measures.map { Date(it.analysisDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate() }
        val newMonths = dates.distinctBy { it.year to it.month }
        realtimeRepository.getMonthsAverages(userId, newMonths).collect {
            insertNewAverageMonthlyMeasuresToDB(it)
            trySend(Response.Success(it))
        }
        awaitClose {  }
    }

    private suspend fun insertNewAverageMonthlyMeasuresToDB(measureDataList: List<AverageMonthlyMeasureData?>?) {
        if (measureDataList != null) {
            val measureDataListNotNull = measureDataList.filterNotNull()
            database.saveAverageMonthlyMeasures(ArrayList(measureDataListNotNull))
        }
    }

    fun getAllUserDailyAverages(userId: String?): Flow<Response<ArrayList<AverageDailyMeasureData>>> = callbackFlow {
        realtimeRepository.getAllDaysAverages(userId).collect {
            trySend(Response.Success(it))
        }
        awaitClose {  }
    }

    fun closeChannels() {
        realtimeRepository.closeChannels()
    }

    fun getAllUserMonthlyAverages(userId: String?): Flow<Response<ArrayList<AverageMonthlyMeasureData>>> = callbackFlow {
        realtimeRepository.getAllMonthsAverages(userId).collect {
            trySend(Response.Success(it))
        }
        awaitClose {  }
    }


    companion object {
        val TAG = MeasuresRepository::class.simpleName
    }
}