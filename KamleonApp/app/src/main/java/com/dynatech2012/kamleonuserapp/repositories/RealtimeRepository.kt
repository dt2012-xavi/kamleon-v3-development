package com.dynatech2012.kamleonuserapp.repositories

import android.util.Log
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.REALTIME_COLLECTION_AVERAGES
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.REALTIME_COLLECTION_QR_LOGIN
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.REALTIME_FOLDER_DAILY
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.REALTIME_FOLDER_MONTHLY
import com.dynatech2012.kamleonuserapp.constants.FirebaseConstants.USER_UID_DEBUG
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.SendChannel
import java.text.DecimalFormat
import java.time.LocalDate
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 *
 * <font color="teal">
 * Edu:<br></br>
 * This repository has the responsibility
 * to manage access to Realtime Database <br></br>
 * It holds a reference to Realtime FirebaseDatabase instance
</font> */
class RealtimeRepository @Inject constructor(private val userRepository: UserRepository){
    private val db =  Firebase.database
    private val averagesRef = db.reference.child(REALTIME_COLLECTION_AVERAGES)

    private var channel1: SendChannel<ArrayList<AverageDailyMeasureData>>? = null
    private var channel2: SendChannel<ArrayList<AverageMonthlyMeasureData>>? = null
    private val uuid: String?
        get() = if (Constants.DEBUG_MODE) USER_UID_DEBUG else userRepository.uuid

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * Upload a new scanned QR ID to Realtime
    </font> */
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

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * ------- DAILY AVERAGE MEASURES -------
    </font> */
    suspend fun getDaysAverages(userId: String?, dayArray: List<LocalDate>) = suspendCoroutine { continuation ->
        Log.d(TAG, "getMonthAverage")
        if (userId == null) {
            continuation.resume(Response.Failure(Exception()))
            return@suspendCoroutine
        }
        val ref = averagesRef.child(userId)
        Log.d(TAG, "averagesRef = $ref")
        val daysAverages = ArrayList<AverageDailyMeasureData>()
        dayArray.forEachIndexed { i, dayItem ->
            val year = dayItem.year
            val monthInt = dayItem.monthValue
            val numberFormat = DecimalFormat("00")
            val month = numberFormat.format(monthInt)
            val dayInt = dayItem.dayOfMonth
            val day = numberFormat.format(dayInt)
            ref.child(REALTIME_FOLDER_DAILY).child(year.toString()).child(month.toString())
                .child(day.toString()).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //Get map of users in dataSnapshot
                        Log.d(TAG, "getMonthAverage: on data change")
                        try {
                            //val b = dataSnapshot.child("averageScore").value.toString()
                            //Log.d(TAG, "bbbbbbbb d date: ${day}/${month}/${year} averageScore: $b")
                            val dailyAverage = AverageDailyMeasureData(dataSnapshot, year, monthInt, dayInt)
                            daysAverages.add(dailyAverage)
                        } catch (e: Exception) {
                            Log.e(TAG, "Daily average measure with null parameters date: ${day}/${month}/${year}")
                        }
                        if (i == dayArray.size - 1)
                            continuation.resume(Response.Success(daysAverages))
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        //handle databaseError
                        Log.d(TAG, "getMonthAverage: error")
                    }
                })
        }
    }

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * ------- MONTHLY AVERAGE MEASURES -------
    </font> */

    suspend fun getMonthsAverages(userId: String?, monthArray: List<LocalDate>) = suspendCoroutine { continuation ->
        Log.d(TAG, "getMonthAverage")
        if (userId == null) {
            continuation.resume(Response.Failure(Exception()))
            return@suspendCoroutine
        }
        val ref = averagesRef.child(userId)
        Log.d(TAG, "averagesRef = $ref")
        val monthsAverages = ArrayList<AverageMonthlyMeasureData>()
        monthArray.forEachIndexed { i, monthItem ->
            val year = monthItem.year
            val monthInt = monthItem.monthValue
            val numberFormat = DecimalFormat("00")
            val month = numberFormat.format(monthInt)
            ref.child(REALTIME_FOLDER_MONTHLY).child(year.toString()).child(month.toString())
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //Get map of users in dataSnapshot
                        Log.d(TAG, "getMonthAverage: on data change")
                        try {
                            //val c = dataSnapshot.child("averageScore").value.toString()
                            //Log.d(TAG, "bbbbbbbb c: date: ${month}/${year} _ averageScore: $c")
                            val monthAverage = AverageMonthlyMeasureData(dataSnapshot, year, monthInt)
                            monthsAverages.add(monthAverage)
                        } catch (e: Exception) {
                            Log.e(TAG, "Monthly average measure with null parameters date: ${month}/${year}")
                        }
                        if (i == monthArray.size - 1)
                            continuation.resume(Response.Success(monthsAverages))
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        //handle databaseError
                        Log.d(TAG, "getMonthAverage: error")
                    }
                })
        }
    }



    fun closeChannels() {
        /*
        channel1?.close()
        channel1 = null
        channel2?.close()
        channel2 = null
        */
    }

    companion object {
        private val TAG = RealtimeRepository::class.java.simpleName
    }
}