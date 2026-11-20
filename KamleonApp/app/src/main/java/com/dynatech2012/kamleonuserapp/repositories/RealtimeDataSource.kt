package com.dynatech2012.kamleonuserapp.repositories

import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.REALTIME_COLLECTION_AVERAGES
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.REALTIME_FOLDER_DAILY
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.REALTIME_FOLDER_MONTHLY
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USER_UID_DEBUG
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.network.CallKind
import com.dynatech2012.kamleonuserapp.network.NetworkGate
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.text.DecimalFormat
import java.time.LocalDate
import javax.inject.Inject
import com.dynatech2012.kamleonuserapp.utils.DebugLog

/**
 *
 * <font color="teal">
 * Edu:<br></br>
 * This repository has the responsibility
 * to manage access to Realtime Database <br></br>
 * It holds a reference to Realtime FirebaseDatabase instance
</font> */
class RealtimeDataSource @Inject constructor(
    private val userRepository: UserRepository,
    private val gate: NetworkGate
) {
    private val db =  Firebase.database
    private val averagesRef = db.reference.child(REALTIME_COLLECTION_AVERAGES)

    /*
    private var channel1: SendChannel<ArrayList<AverageDailyMeasureData>>? = null
    private var channel2: SendChannel<ArrayList<AverageMonthlyMeasureData>>? = null
    */
    private val uuid: String?
        get() = if (Constants.DEBUG_MODE) USER_UID_DEBUG else userRepository.uuid

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * Upload a new scanned QR ID to Realtime
    </font> */
    /*
    fun uploadQrId(qrId: String?) {
        //DatabaseReference qrRef = db.getReference(REALTIME_COLLECTION_QR_LOGIN + qrId);
        try {
            val qrRef = db.reference.child(REALTIME_COLLECTION_QR_LOGIN).child(
                qrId!!
            )
            Log.d(TAG, "qr upload will begin: \n$qrRef\nuser id: ${this.uuid}")
            qrRef.setValue(this.uuid)
                .addOnSuccessListener { Log.d(TAG, "qr upload success") }
                .addOnFailureListener { e: Exception -> Log.d(TAG, "qr upload failure: $e") }
        } catch (e: Exception) {
            Log.d(TAG, "QR upload exception: $e")
        }
    }
    */

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * ------- DAILY AVERAGE MEASURES -------
    </font> */
    suspend fun getDaysAverages(userId: String?, dayArray: List<LocalDate>): Response<ArrayList<AverageDailyMeasureData>> {
        DebugLog.d(TAG) { "getDaysAverage" }
        if (userId == null) return Response.Failure(Exception())
        val ref = averagesRef.child(userId).child(REALTIME_FOLDER_DAILY)
        val numberFormat = DecimalFormat("00")
        val results = coroutineScope {
            dayArray.map { dayItem ->
                async {
                    val year = dayItem.year
                    val monthInt = dayItem.monthValue
                    val dayInt = dayItem.dayOfMonth
                    val month = numberFormat.format(monthInt)
                    val day = numberFormat.format(dayInt)
                    val read = gate.safeCall(CallKind.Read) {
                        val map = ref.child(year.toString()).child(month).child(day).get().await()
                        AverageDailyMeasureData(map, year, monthInt, dayInt)
                    }
                    read.dataValue?.let { Result.success(it) }
                        ?: Result.failure(read.error ?: IllegalStateException("No daily average"))
                }
            }.awaitAll()
        }
        return averagesResult(results)
    }

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * ------- MONTHLY AVERAGE MEASURES -------
    </font> */

    suspend fun getMonthsAverages(userId: String?, monthArray: List<LocalDate>): Response<ArrayList<AverageMonthlyMeasureData>> {
        DebugLog.d(TAG) { "getMonthAverage" }
        if (userId == null) return Response.Failure(Exception())
        val ref = averagesRef.child(userId).child(REALTIME_FOLDER_MONTHLY)
        val numberFormat = DecimalFormat("00")
        val results = coroutineScope {
            monthArray.map { monthItem ->
                async {
                    val year = monthItem.year
                    val monthInt = monthItem.monthValue
                    val month = numberFormat.format(monthInt)
                    val read = gate.safeCall(CallKind.Read) {
                        val map = ref.child(year.toString()).child(month).get().await()
                        AverageMonthlyMeasureData(map, year, monthInt)
                    }
                    read.dataValue?.let { Result.success(it) }
                        ?: Result.failure(read.error ?: IllegalStateException("No monthly average"))
                }
            }.awaitAll()
        }
        return averagesResult(results)
    }

    /*
    fun closeChannels() {
        channel1?.close()
        channel1 = null
        channel2?.close()
        channel2 = null
    }
    */

    companion object {
        private val TAG = RealtimeDataSource::class.java.simpleName
    }
}

/**
 * The outcome of reading each day or month on its own: what was read, or the first failure when
 * **every** read failed. A network failure must not look like "no data" (an empty success).
 */
internal fun <T : Any> averagesResult(results: List<Result<T>>): Response<ArrayList<T>> {
    val values = results.mapNotNull { it.getOrNull() }
    val firstFailure = results.firstNotNullOfOrNull { it.exceptionOrNull() }
    return if (values.isEmpty() && firstFailure != null) {
        Response.Failure(firstFailure as Exception)
    } else {
        Response.Success(ArrayList(values))
    }
}
