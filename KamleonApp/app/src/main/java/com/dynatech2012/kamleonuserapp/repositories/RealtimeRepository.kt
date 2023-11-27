package com.dynatech2012.kamleonuserapp.repositories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.RealtimeConstants.REALTIME_COLLECTION_AVERAGES
import com.dynatech2012.kamleonuserapp.constants.RealtimeConstants.REALTIME_COLLECTION_QR_LOGIN
import com.dynatech2012.kamleonuserapp.constants.RealtimeConstants.REALTIME_FOLDER_DAILY
import com.dynatech2012.kamleonuserapp.constants.RealtimeConstants.REALTIME_FOLDER_MONTHLY
import com.dynatech2012.kamleonuserapp.constants.RealtimeConstants.USER_UID_DEBUG
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.views.chart.exts.day
import com.dynatech2012.kamleonuserapp.views.chart.exts.month
import com.dynatech2012.kamleonuserapp.views.chart.exts.year
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
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
    var allAverageDailyMeasures_Observable: MutableLiveData<ArrayList<AverageDailyMeasureData>>? =
        null
        get() {
            if (field == null) {
                field = MutableLiveData()
            }
            return field
        }
        private set
    /*
    fun getAllAverageDailyMeasures(userId: String?): LiveData<ArrayList<AverageDailyMeasureData>> {
        val observable = MutableLiveData<ArrayList<AverageDailyMeasureData>>()
        val ref = averagesRef.child(userId!!)
        if (ref != null) {
            ref.child(REALTIME_FOLDER_DAILY).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //Get map of users in dataSnapshot
                    val data = collectAverageDailyMeasures(dataSnapshot.value as Map<String, Any>?)
                    observable.postValue(data)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    //handle databaseError
                }
            })
        }
        return observable
    }
     */

    private var allAverageDailyMeasuresAfterDate_Observable: MutableLiveData<ArrayList<AverageDailyMeasureData>>? =
        null

    fun getAllAverageDailyMeasuresAfterDate_Observable(): LiveData<ArrayList<AverageDailyMeasureData>> {
        if (allAverageDailyMeasuresAfterDate_Observable == null) {
            allAverageDailyMeasuresAfterDate_Observable = MutableLiveData()
        }
        return allAverageDailyMeasuresAfterDate_Observable!!
    }

    private var dailyDatesInRangeToCheck: MutableList<Date> = ArrayList()
    private val newAverageDailyMeasures: ArrayList<AverageDailyMeasureData>? = ArrayList()
    fun getAllAverageDailyMeasuresAfterDate(
        lastDateStr: String,
        currentDateStr: String,
        userId: String
    ): LiveData<ArrayList<AverageDailyMeasureData>> {
        //date = "dd/MM/yyyy HH:mm:ss"
        //realtime:     user
        //              L__ daily
        //                  L__ 2021
        //                      L__ 10
        //                          L__ 22
        //                          L__ 29
        //we'll have to download all the days included between the lastDateCheckRealtimeDaily and now (inclusive)

        //get all the days included between the lastDateCheckRealtimeDaily and now (inclusive)
        val observable = MutableLiveData<ArrayList<AverageDailyMeasureData>>()
        try {
            val lastDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(lastDateStr)
            val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(currentDateStr)
            dailyDatesInRangeToCheck = ArrayList()
            dailyDatesInRangeToCheck.clear()
            newAverageDailyMeasures!!.clear()
            val calendar = getCalendarWithoutTime(lastDate)
            val endCalendar = getCalendarWithoutTime(currentDate)
            while (calendar.before(endCalendar) || calendar == endCalendar) {
                val result = calendar.time
                dailyDatesInRangeToCheck.add(result)
                calendar.add(Calendar.DATE, 1)
            }
            Log.d(TAG, "PLAYING: lastDateStr: $lastDateStr")
            Log.d(TAG, "PLAYING: currentDateStr: $currentDateStr")
            Log.d(TAG, "PLAYING: dailyDatesInRangeToCheck: $dailyDatesInRangeToCheck")
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        //download from firebase all such days if available
        checkNextDateDaily(userId)
        return observable
    }

    private fun checkNextDateDaily(mUserId: String) {
        if (0 < dailyDatesInRangeToCheck.size) {
            val date = dailyDatesInRangeToCheck[0]
            dailyDatesInRangeToCheck.removeAt(0)
            val localDate = date.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate() //Tue Jan 11 00:00:00 GMT+01:00 2022 -----> 2022->01->11
            var dtf = DateTimeFormatter.ofPattern("yyyy")
            val year = dtf.format(localDate)
            dtf = DateTimeFormatter.ofPattern("MM")
            val month = dtf.format(localDate)
            dtf = DateTimeFormatter.ofPattern("dd")
            val day = dtf.format(localDate)
            Log.d(TAG, "PLAYING: year,month,day: $year,$month,$day")
            val ref = averagesRef.child(mUserId)
            if (ref != null) {
                ref.child(REALTIME_FOLDER_DAILY).child(year).child(month).child(day)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            Log.d(TAG, "PLAYING: " + "DOWNLOADED: " + dataSnapshot.value)
                            if (dataSnapshot.value != null) {
                                val a = AverageDailyMeasureData(
                                    dataSnapshot,
                                    Integer.valueOf(year),
                                    Integer.valueOf(month),
                                    Integer.valueOf(day)
                                )
                                Log.d(TAG, "PLAYING: " + "DOWNLOADED and ADDED: " + a.allParams)
                                newAverageDailyMeasures!!.add(a)
                            } else {
                                Log.d(TAG, "PLAYING" + "NOT ADDED")
                            }
                            checkNextDateDaily(mUserId)
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            //handle databaseError
                        }
                    })
            }
        } else {
            if (allAverageDailyMeasuresAfterDate_Observable != null && newAverageDailyMeasures != null && !newAverageDailyMeasures.isEmpty()) {
                Log.d(
                    TAG,
                    "PLAYING" + "newAverageDailyMeasures.size(): " + newAverageDailyMeasures.size
                )
                allAverageDailyMeasuresAfterDate_Observable!!.postValue(newAverageDailyMeasures)
            }
        }
    }

    private fun collectAverageDailyMeasures(all: Map<String, Any>?): ArrayList<AverageDailyMeasureData> {
        val averageDailyMeasures = ArrayList<AverageDailyMeasureData>()
        if (all != null && !all.isEmpty()) {
            Log.d(TAG, " ")
            Log.d(TAG, "DAILY")
            Log.d(TAG, "--------------------------")
            Log.d(TAG, "RAW: $all")
            Log.d(TAG, "--------------------------")

            //all years
            for ((key, value) in all) {
                val yearNum = key.toInt()
                Log.d(TAG, "YEAR: $yearNum")

                //all months
                val yearData: Map<String, Any> = value as Map<String, Any>
                for ((key1, value1) in yearData) {
                    val monthNum = key1.toInt()
                    Log.d(TAG, "     MONTH: $monthNum")

                    //all days
                    val monthData: Map<String, Any> = value1 as Map<String, Any>
                    for ((key2, value2) in monthData) {
                        val dayNum = key2.toInt()
                        Log.d(TAG, "          DAY: $dayNum")

                        //all raw values
                        val dayData: Map<String, Any> = value2 as Map<String, Any>
                        val averageScore = dayData["averageScore"].toString().toDouble()
                        val colorChart = dayData["colorChart"].toString().toFloat()
                        val maxScore = dayData["maxScore"].toString().toInt()
                        val minScore = dayData["minScore"].toString().toInt()
                        val msCond = dayData["msCond"].toString().toDouble()
                        val num = dayData["num"].toString().toInt()
                        val usg = dayData["usg"].toString().toFloat()
                        val volume = dayData["volume"].toString().toDouble()

                        //date values
                        val year = yearNum.toString().toInt()
                        var monthStr = monthNum.toString()
                        if (monthNum < 10) {
                            monthStr = "0$monthStr"
                        }
                        var dayStr = dayNum.toString()
                        if (dayNum < 10) {
                            dayStr = "0$dayStr"
                        }
                        val month = (yearNum.toString() + monthStr).toInt()
                        val day = (yearNum.toString() + monthStr + dayStr).toInt()

                        //save all in an object
                        val singleMeasure = AverageDailyMeasureData(
                            averageScore,
                            colorChart,
                            maxScore,
                            minScore,
                            msCond,
                            num,
                            usg,
                            volume,
                            year,
                            month,
                            day,
                            GregorianCalendar(year, monthNum - 1, dayNum).time.time
                        )
                        Log.d(TAG, "               VALUES: " + singleMeasure.allParams)
                        averageDailyMeasures.add(singleMeasure)
                    }
                }
            }
            /*
             * if (allAverageDailyMeasures_Observable != null) {
             *    allAverageDailyMeasures_Observable.postValue(averageDailyMeasures);
             * }
             */
        }
        return averageDailyMeasures
    }

    /**
     *
     * <font color="teal">
     * Edu:<br></br>
     * ------- MONTHLY AVERAGE MEASURES -------
    </font> */
    private var allAverageMonthlyMeasures_Observable: MutableLiveData<ArrayList<AverageMonthlyMeasureData>>? =
        null

    fun getAllAverageMonthlyMeasures_Observable(): LiveData<ArrayList<AverageMonthlyMeasureData>> {
        if (allAverageMonthlyMeasures_Observable == null) {
            allAverageMonthlyMeasures_Observable = MutableLiveData()
        }
        return allAverageMonthlyMeasures_Observable!!
    }

    private var allAverageMonthlyMeasuresAfterDate_Observable: MutableLiveData<ArrayList<AverageMonthlyMeasureData>>? =
        null

    fun getAllAverageMonthlyMeasuresAfterDate_Observable(): LiveData<ArrayList<AverageMonthlyMeasureData>> {
        if (allAverageMonthlyMeasuresAfterDate_Observable == null) {
            allAverageMonthlyMeasuresAfterDate_Observable = MutableLiveData()
        }
        return allAverageMonthlyMeasuresAfterDate_Observable!!
    }

    private val monthlyDatesInRangeToCheck: MutableList<Date> = ArrayList()
    private val newAverageMonthlyMeasures: ArrayList<AverageMonthlyMeasureData>? = ArrayList()

    fun getAllAverageMonthlyMeasuresAfterDate(
        lastDateStr: String,
        currentDateStr: String,
        userId: String
    ): LiveData<ArrayList<AverageMonthlyMeasureData>> {
        //date = "dd/MM/yyyy HH:mm:ss"
        //realtime:     user
        //              L__ monthly
        //                  L__ 2021
        //                      L__ 10
        //                      L__ 12
        //we'll have to download all the months included between the lastDateCheckRealtimeDaily and now (inclusive)

        //get all the days included between the lastDateCheckRealtimeDaily and now (inclusive)
        val observable = MutableLiveData<ArrayList<AverageMonthlyMeasureData>>()
        try {
            val lastDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(lastDateStr)!!
            val currentDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).parse(currentDateStr)!!
            monthlyDatesInRangeToCheck.clear()
            newAverageMonthlyMeasures!!.clear()
            val calendar = getCalendarWithoutTime(lastDate)
            // TODO add last measure dates not in database
            val endCalendar = getCalendarWithoutTime(currentDate)
            while (calendar.before(endCalendar) || calendar == endCalendar) {
                val result = calendar.time
                monthlyDatesInRangeToCheck.add(result)
                calendar.add(Calendar.DATE, 1)
            }
            Log.d(TAG, "PLAYINGlastDateStr: $lastDateStr")
            Log.d(TAG, "PLAYINGcurrentDateStr: $currentDateStr")
            Log.d(TAG, "PLAYINGmonthlyDatesInRangeToCheck: $monthlyDatesInRangeToCheck")
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        //download from firebase all such days if available
        checkNextDateMonthly(userId)
        return observable
    }

    private fun checkNextDateMonthly(userId: String) {
        if (0 < monthlyDatesInRangeToCheck.size) {
            val date = monthlyDatesInRangeToCheck[0]
            monthlyDatesInRangeToCheck.removeAt(0)
            val localDate = date.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDate() //Tue Jan 11 00:00:00 GMT+01:00 2022 -----> 2022->01->11
            var dtf = DateTimeFormatter.ofPattern("yyyy")
            val year = dtf.format(localDate)
            dtf = DateTimeFormatter.ofPattern("MM")
            val month = dtf.format(localDate)
            val ref = averagesRef.child(userId)
            if (ref != null) {
                ref.child(REALTIME_FOLDER_MONTHLY).child(year).child(month)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            Log.d(TAG, "PLAYING" + "DOWNLOADED: " + dataSnapshot.value)
                            if (dataSnapshot.value != null) {
                                val a = AverageMonthlyMeasureData(
                                    dataSnapshot,
                                    Integer.valueOf(year),
                                    Integer.valueOf(month)
                                )
                                Log.d(TAG, "PLAYING" + "DOWNLOADED and ADDED: " + a.allParams)
                                newAverageMonthlyMeasures!!.add(a)
                            } else {
                                Log.d(TAG, "PLAYING" + "NOT ADDED")
                            }
                            checkNextDateMonthly(userId)
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            //handle databaseError
                        }
                    })
            }
        } else {
            if (allAverageMonthlyMeasuresAfterDate_Observable != null && newAverageMonthlyMeasures != null && !newAverageMonthlyMeasures.isEmpty()) {
                Log.d(
                    TAG,
                    "PLAYING" + "newAverageMonthlyMeasures.size(): " + newAverageMonthlyMeasures.size
                )
                allAverageMonthlyMeasuresAfterDate_Observable!!.postValue(newAverageMonthlyMeasures)
            }
        }
    }

    /*
    fun getAllAverageMonthlyMeasures(userId: String?): LiveData<ArrayList<AverageMonthlyMeasureData>> {
        Log.d(TAG, "getAllAverageMonthlyMeasures")
        val observable = MutableLiveData<ArrayList<AverageMonthlyMeasureData>>()
        val ref = averagesRef.child(userId!!)
        if (ref != null) {
            Log.d(TAG, "averagesRef = $ref")
            ref.child(REALTIME_FOLDER_MONTHLY).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //Get map of users in dataSnapshot
                    Log.d(TAG, "getAllAverageMonthlyMeasures: on data change")
                    val data =
                        collectAverageMonthlyMeasures(dataSnapshot.value as Map<String, Any>?)
                    Log.d(TAG, "getAllAverageMonthlyMeasures: data = ${data.size}")
                    observable.postValue(data)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    //handle databaseError
                    Log.d(TAG, "getAllAverageMonthlyMeasures: error")
                }
            })
        }
        return observable
    }
     */

    fun getMonthsAverages(userId: String?, monthArray: List<LocalDate>) = callbackFlow {
        Log.d(TAG, "getMonthAverage")
        if (userId == null) {
            cancel()
            return@callbackFlow
        }
        val ref = averagesRef.child(userId)
        Log.d(TAG, "averagesRef = $ref")
        val monthsAverages = ArrayList<AverageMonthlyMeasureData>()
        monthArray.forEachIndexed { i, monthItem ->
            val year = monthItem.year
            val month = monthItem.monthValue
            ref.child(REALTIME_FOLDER_MONTHLY).child(year.toString()).child(month.toString())
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //Get map of users in dataSnapshot
                        Log.d(TAG, "getMonthAverage: on data change")
                        val monthAverage = AverageMonthlyMeasureData(dataSnapshot, year, month)
                        monthsAverages.add(monthAverage)
                        if (i == monthArray.size - 1)
                            trySend(monthsAverages)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        //handle databaseError
                        Log.d(TAG, "getMonthAverage: error")
                    }
                })
        }
        awaitClose { }
    }

    suspend fun getDaysAverages(userId: String?, dayArray: List<LocalDate>) = callbackFlow {
        Log.d(TAG, "getMonthAverage")
        if (userId == null) {
            cancel()
            return@callbackFlow
        }
        val ref = averagesRef.child(userId)
        Log.d(TAG, "averagesRef = $ref")
        val daysAverages = ArrayList<AverageDailyMeasureData>()
        dayArray.forEachIndexed { i, dayItem ->
            val year = dayItem.year
            val month = dayItem.monthValue
            val day = dayItem.dayOfMonth
            ref.child(REALTIME_FOLDER_MONTHLY).child(year.toString()).child(month.toString())
                .child(day.toString()).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        //Get map of users in dataSnapshot
                        Log.d(TAG, "getMonthAverage: on data change")
                        val dailyAverage = AverageDailyMeasureData(dataSnapshot, year, month, day)
                        daysAverages.add(dailyAverage)
                        if (i == dayArray.size - 1)
                            trySend(daysAverages)
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        //handle databaseError
                        Log.d(TAG, "getMonthAverage: error")
                    }
            })
        }
        awaitClose { }
    }

    fun getAllMonthsAverages(userId: String?) = callbackFlow {
        Log.d(TAG, "getMonthAverage")
        channel2 = this.channel
        //i if user == nul, then cancel flow
        if (userId == null) {
            cancel()
            return@callbackFlow
        }
        val ref = averagesRef.child(userId)
        Log.d(TAG, "averagesRef = $ref")
        ref.child(REALTIME_FOLDER_MONTHLY)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //Get map of users in dataSnapshot
                    Log.d(TAG, "getMonthAverage: on data change")
                    val monthsAverages = collectAverageMonthlyMeasures(dataSnapshot.value as Map<String, Any>?)
                    trySend(monthsAverages)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    //handle databaseError
                    Log.d(TAG, "getMonthAverage: error")
                }
            })
        awaitClose { channel2?.close() }
    }

    private fun collectAverageMonthlyMeasures(all: Map<String, Any>?): ArrayList<AverageMonthlyMeasureData> {
        val averageMonthlyMeasures = ArrayList<AverageMonthlyMeasureData>()
        if (!all.isNullOrEmpty()) {
            Log.d(TAG, " ")
            Log.d(TAG, "MONTHLY")
            Log.d(TAG, "--------------------------")
            Log.d(TAG, "RAW: $all")
            Log.d(TAG, "--------------------------")

            //all years
            for ((key, value) in all) {
                val yearNum = key.toInt()
                Log.d(TAG, "YEAR: $yearNum")

                //all months
                val yearData: Map<String, Any> = value as Map<String, Any>
                for ((key1, value1) in yearData) {
                    val monthNum = key1.toInt()
                    Log.d(TAG, "     MONTH: $monthNum")

                    //all raw values
                    val dayData: Map<String, Any> = value1 as Map<String, Any>
                    val averageScore = dayData["averageScore"].toString().toDouble()
                    val colorChart = dayData["colorChart"].toString().toFloat()
                    val maxScore = dayData["maxScore"].toString().toInt()
                    val minScore = dayData["minScore"].toString().toInt()
                    val msCond = dayData["msCond"].toString().toDouble()
                    val num = dayData["num"].toString().toInt()
                    val usg = dayData["usg"].toString().toFloat()
                    val volume = dayData["volume"].toString().toDouble()

                    //date values
                    val year = yearNum.toString().toInt()
                    var monthStr = monthNum.toString()
                    if (monthNum < 10) {
                        monthStr = "0$monthStr"
                    }
                    val month = (yearNum.toString() + monthStr).toInt()

                    //save all in an object
                    val singleMeasure = AverageMonthlyMeasureData(
                        averageScore,
                        colorChart,
                        maxScore,
                        minScore,
                        msCond,
                        num,
                        usg,
                        volume,
                        year,
                        month,
                        GregorianCalendar(year, monthNum - 1, 1).time.time
                    )
                    Log.d(TAG, "          VALUES: " + singleMeasure.allParams)
                    averageMonthlyMeasures.add(singleMeasure)
                }
            }
            if (allAverageMonthlyMeasures_Observable != null) {
                allAverageMonthlyMeasures_Observable!!.postValue(averageMonthlyMeasures)
            }
        } else {
            if (allAverageMonthlyMeasures_Observable != null) {
                allAverageMonthlyMeasures_Observable!!.setValue(ArrayList())
            }
        }
        return averageMonthlyMeasures
    }

    fun getAllDaysAverages(userId: String?) = callbackFlow {
        Log.d(TAG, "getMonthAverage")
        channel1 = this.channel
        if (userId == null) {
            cancel()
            return@callbackFlow
        }
        val ref = averagesRef.child(userId)
        Log.d(TAG, "averagesRef = $ref")
        ref.child(REALTIME_FOLDER_DAILY)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    //Get map of users in dataSnapshot
                    Log.d(TAG, "getMonthAverage: on data change")
                    val daysAverages = collectAverageDailyMeasures(dataSnapshot.value as Map<String, Any>?)
                    trySend(daysAverages)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    //handle databaseError
                    Log.d(TAG, "getMonthAverage: error")
                }
            })
        awaitClose { channel1?.close() }
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
        private fun getCalendarWithoutTime(date: Date): Calendar {
            val calendar: Calendar = GregorianCalendar()
            calendar.time = date
            calendar[Calendar.HOUR] = 0
            calendar[Calendar.HOUR_OF_DAY] = 0
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
            return calendar
        }
    }
}