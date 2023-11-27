package com.dynatech2012.kamleonuserapp.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.extensions.sha256
import com.dynatech2012.kamleonuserapp.fragments.SettingFragment
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.repositories.DatabaseDataSource
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.repositories.MeasuresRepository
import com.dynatech2012.kamleonuserapp.repositories.RealtimeRepository
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.nio.channels.Channel
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepository: UserRepository,
    private val firestoreRepo: FirestoreDataSource,
    private val measuresRepository: MeasuresRepository,
    private val databaseRepository: DatabaseDataSource,
    private val realtime: RealtimeRepository
): ViewModel() {

    var graphicType: Int = -1

    var fName: String? = ""
    var lName: String? = ""
    var email: String? = ""
    var pass: String? = ""
    var birthday: Date = Date()//LocalDate = LocalDate.MIN
    var height = -1f
    var weight = -1f
    var gender = Gender.none

    private val _userData = MutableLiveData<CustomUser>()
    val userData: LiveData<CustomUser> = _userData
    private val _userUpdated = MutableLiveData<Boolean>()
    val userUpdated: LiveData<Boolean> = _userUpdated

    private val _userImagePrev = MutableLiveData<Uri?>()
    val userImagePrev: LiveData<Uri?> = _userImagePrev

    private val _userImage = MutableLiveData<Uri?>()
    val userImage: LiveData<Uri?> = _userImage

    private val _measures = MutableLiveData<ArrayList<MeasureData>>()
    val measures: LiveData<ArrayList<MeasureData>> = _measures

    private val _lastMeasure = MutableLiveData<MeasureData>()
    val lastMeasure: LiveData<MeasureData> = _lastMeasure

    fun getUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            val userResult = firestoreRepo.getUserData()
            if (userResult.isSuccess && userResult.dataValue != null)
                _userData.postValue(userResult.dataValue)
            Log.d(TAG, "user data changed got user ${userResult.dataValue}")
        }
    }

    fun updateUserData(data: HashMap<String, Any>) {
        viewModelScope.launch(Dispatchers.IO) {
            val userResult = firestoreRepo.updateUser(data)
            if (userResult.isSuccess && userResult.dataValue != null) {
                getUserData()
                _userUpdated.postValue(userResult.dataValue)
                Log.d(SettingFragment.TAG, "user data changed")
            }
        }
    }

    fun changeHeight(newHeight: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = hashMapOf<String, Any>("height" to newHeight.toFloat())
            updateUserData(data)
        }
    }

    fun changeWeight(newWeight: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = hashMapOf<String, Any>("weight" to newWeight.toFloat())
            updateUserData(data)
        }
    }

    fun changeGender(newGender: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = hashMapOf<String, Any>("gender" to newGender)
            updateUserData(data)
        }
    }

    fun changeBirth(newBirth: Date) {
        viewModelScope.launch(Dispatchers.IO) {
            val data = hashMapOf<String, Any>("dateOfBirth" to newBirth)
            updateUserData(data)
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            databaseRepository.deleteAllMeasures()
            SharedPrefUtil(appContext).removeUser()
            measuresRepository.closeChannels()
            userRepository.logout()
        }
    }

    fun changePin(oldPin: String, newPin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldPinHash = oldPin.sha256()
            if (oldPinHash != userData.value?.pin) {
                throw Exception(Constants.unmatchingPIN)
            }
            else {
                val newPinHash = newPin.sha256()
                val data = hashMapOf<String, Any>("pin" to newPinHash)
                firestoreRepo.updateUser(data)
                _userUpdated.postValue(true)
            }
        }
    }

    fun changeEmail(currentPwd: String, newEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.changeEmail(currentPwd, newEmail)
            val data = hashMapOf<String, Any>("email" to newEmail)
            firestoreRepo.updateUser(data)
            _userUpdated.postValue(true)
        }
    }

    fun changePwd(oldPwd: String, newPwd: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.changePwd(oldPwd, newPwd)
            _userUpdated.postValue(true)
        }
    }

    fun deleteUser() {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.deleteUser()
            _userUpdated.postValue(true)
        }
    }

    fun resetUserUpdated()
    {
        _userUpdated.postValue(false)
    }

    fun setImageUri(uri: Uri?) {
        uri?.let {
            _userImagePrev.postValue(uri)
            viewModelScope.launch(Dispatchers.IO) {
                firestoreRepo.updateUserImage(it)
                _userImage.postValue(uri)
            }
        }
    }

    fun removeUserImage() {
        viewModelScope.launch(Dispatchers.IO) {
            firestoreRepo.removeUserImage()
            _userUpdated.postValue(true)
        }
    }

    fun getUserMeasures() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "will try to get measures from DB and from FS")
            val responseDB = measuresRepository.getUserLastMeasure()//.collect { lastM ->
            if (responseDB.isSuccess && responseDB.dataValue != null) {
                Log.d(TAG, "got measures from DB size: ${responseDB.dataValue?.size}")
                responseDB.dataValue?.let { measuresDB ->
                    _measures.postValue(measuresDB)
                    if (measuresDB.size > 0) {
                        Log.d(TAG, "got measures from DB not empty")
                        _lastMeasure.postValue(measuresDB[0])
                    }
                    else {
                        Log.d(TAG, "got measures from DB EMPTY")
                    }
                }
            }
            val responseMeasures = measuresRepository.getUserMeasures(userRepository.uuid)//.collect { meas ->
            if (responseMeasures.isSuccess && responseMeasures.dataValue != null) {
                Log.d(TAG, "got measures from FS success size: ${responseMeasures.dataValue?.size}")
                responseMeasures.dataValue?.let { measuresFB ->
                    _measures.postValue(measuresFB)
                    if (measuresFB.size > 0) {
                        Log.d(TAG, "got measures from FS not empty")
                        _lastMeasure.postValue(measuresFB[0])
                    }
                    else
                    {
                        Log.d(TAG, "got measures from FS EMPTY")
                    }
                }
            }


                        /*
                        val responseMonthlyDB = databaseRepository.getAllAverageMonthlyMeasures()
                        if (responseMonthlyDB.isSuccess && responseMonthlyDB.dataValue != null) {
                            monthlyLoaded.addAll(0, responseMonthlyDB.dataValue!!)
                            _averageMonthlyMeasures.postValue(monthlyLoaded)
                        }
                        val responseDailyDB = databaseRepository.getAllAverageDailyMeasures()
                        if (responseDailyDB.isSuccess && responseDailyDB.dataValue != null) {
                            dailyLoaded.addAll(0, responseDailyDB.dataValue!!)
                            _averageDailyMeasures.postValue(dailyLoaded)
                        }
                        measuresRepository.getUserMonthlyAverages(userRepository.uuid, measures).collect { avMon ->
                            if (avMon.isSuccess && avMon.dataValue != null) {
                                Log.d(TAG, "got measures daily finally 2")
                                avMon.dataValue?.let { monthlyMeasures ->
                                    Log.d(TAG, "got measures daily finally 2 size ${monthlyMeasures.size}")
                                    monthlyLoaded.addAll(monthlyMeasures)
                                    _averageMonthlyMeasures.postValue(monthlyLoaded)
                                }
                            }
                        }
                        measuresRepository.getUserDailyAverages(userRepository.uuid, measures).collect { avDay ->
                            if (avDay.isSuccess && avDay.dataValue != null) {
                                Log.d(TAG, "got measures daily finally 2")
                                avDay.dataValue?.let { dailyMeasures ->
                                    Log.d(TAG, "got measures daily finally 2 size ${dailyMeasures.size}")
                                    dailyLoaded.addAll(dailyMeasures)
                                    _averageDailyMeasures.postValue(dailyLoaded)                                }
                            }
                        }
                        */
                        measuresRepository.getAllUserDailyAverages(userRepository.uuid)
                            .collect { avDa ->
                            if (avDa.isSuccess && avDa.dataValue != null) {
                                Log.d(TAG, "got measures all daily finally 2")
                                avDa.dataValue?.let { dailyMeasures ->
                                    Log.d(
                                        TAG,
                                        "got measures all daily finally 2 size ${dailyMeasures.size}"
                                    )
                                    dailyLoaded.addAll(dailyMeasures)
                                    _averageDailyMeasures.postValue(dailyLoaded)
                                } } }
                        measuresRepository.getAllUserMonthlyAverages(userRepository.uuid)
                            .collect { avMon ->
                            if (avMon.isSuccess && avMon.dataValue != null) {
                                Log.d(TAG, "got measures all monthly finally 2")
                                avMon.dataValue?.let { monthlyMeasures ->
                                    Log.d(TAG, "got measures all monthly finally 2 size ${monthlyMeasures.size}")
                                    monthlyLoaded.addAll(monthlyMeasures)
                                    _averageMonthlyMeasures.postValue(monthlyLoaded)
                                } } }
        }
    }

    val dailyLoaded = ArrayList<AverageDailyMeasureData>()
    val monthlyLoaded = ArrayList<AverageMonthlyMeasureData>()

    private val _averageDailyMeasures =  MutableLiveData<ArrayList<AverageDailyMeasureData>>()
    val averageDailyMeasures: LiveData<ArrayList<AverageDailyMeasureData>> = _averageDailyMeasures
    private val _averageMonthlyMeasures =  MutableLiveData<ArrayList<AverageMonthlyMeasureData>>()
    val averageMonthlyMeasures: LiveData<ArrayList<AverageMonthlyMeasureData>> = _averageMonthlyMeasures




    // AVERAGES
    /**
     *
     * <font color="teal">
     * Edu: <br></br>
     * DAILY measures <br></br>
     * Mediator live data <br></br>
     * to expose all DAILY measures to fragment at once <br></br>
     * (from database and from realtime)
    </font> */

    /*
        var areDailyDBLoaded: Boolean = false
    var areDailyFirestoreDownloaded = false
        var areMonthlyDBLoaded = false
    var areMonthlyFirestoreDownloaded = false

    private fun insertNewAverageDailyMeasuresToDB(measureDataList: List<AverageDailyMeasureData?>?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (measureDataList != null) {
                val measureDataListNotNull = measureDataList.filterNotNull()
                databaseRepository.saveAverageDailyMeasures(ArrayList(measureDataListNotNull))
            }
        }
    }

    private fun getAllAverageDailyMeasuresDB() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = databaseRepository.getAllAverageDailyMeasures()
            if (response.isSuccess && response.dataValue != null) {
                _averageDailyMeasures.postValue(response.dataValue)
            }
        }
    }
    */


    /*
    fun getAverageDailyMeasures(): LiveData<ArrayList<AverageDailyMeasureData>> {
        val observable: MediatorLiveData<ArrayList<AverageDailyMeasureData>> =
            MediatorLiveData<ArrayList<AverageDailyMeasureData>>()
        if (areDailyDBLoaded && areDailyFirestoreDownloaded) {
            observable.setValue(dailyLoaded)
        } else {
            // Get local measures
            observable.addSource(averageDailyMeasures) { measureData: ArrayList<AverageDailyMeasureData> ->
                //add at beginning of array local measures
                Log.d(
                    TAG,
                    "daily average from DB received: size: " + measureData.size
                )
                dailyLoaded.addAll(0, measureData)
                observable.postValue(dailyLoaded)
            }
            // Get remote measures
            observable.addSource(
                getNewAverageDailyMeasuresRealtime()) { measureData: ArrayList<AverageDailyMeasureData> ->
                    //add to the end of array remote new measures
                    dailyLoaded.addAll(measureData)
                    observable.postValue(dailyLoaded)
                    //Save new measures to local database as new
                    insertNewAverageDailyMeasuresToDB(measureData)
                }
        }
        getAllAverageDailyMeasuresDB()
        return observable
    }

     */

    /**
     *
     * <font color="teal">
     * Edu: <br></br>
     * MONTHLY measures <br></br>
     * Mediator live data <br></br>
     * to expose all MONTHLY measures to fragment at once <br></br>
     * (from database and from realtime)
    </font> */

    /*
    private fun insertNewAverageMonthlyMeasuresToDB(measureDataList: List<AverageMonthlyMeasureData?>?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (measureDataList != null) {
                val measureDataListNotNull = measureDataList.filterNotNull()
                databaseRepository.saveAverageMonthlyMeasures(ArrayList(measureDataListNotNull))
            }
        }
    }

    private val _allAverageMonthlyMeasuresDB =  MutableLiveData<ArrayList<AverageMonthlyMeasureData>>()
    val allAverageMonthlyMeasuresDB: LiveData<ArrayList<AverageMonthlyMeasureData>> = _allAverageMonthlyMeasuresDB
    private fun getAllAverageMonthlyMeasuresDB() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = databaseRepository.getAllAverageMonthlyMeasures()
            if (response.isSuccess && response.dataValue != null) {
                _allAverageMonthlyMeasuresDB.postValue(response.dataValue)
            }
        }
    }
     */

    /*
    fun getAverageMonthlyMeasures(): LiveData<ArrayList<AverageMonthlyMeasureData>> {
        val observable: MediatorLiveData<ArrayList<AverageMonthlyMeasureData>> =
            MediatorLiveData<ArrayList<AverageMonthlyMeasureData>>()
        if (areMonthlyDBLoaded && areMonthlyFirestoreDownloaded) {
            observable.setValue(monthlyLoaded)
        } else {
            // Get local measures
            observable.addSource(
                allAverageMonthlyMeasuresDB
            ) { measureData: ArrayList<AverageMonthlyMeasureData> ->
                //add at beginning of array local measures
                Log.d(
                    TAG,
                    "monthly average from DB received: size: " + measureData.size
                )
                monthlyLoaded.addAll(0, measureData)
                observable.postValue(monthlyLoaded)
            }
            // Get remote measures
            observable.addSource(
                getNewAverageMonthlyMeasuresRealtime()
            ) { measureData: ArrayList<AverageMonthlyMeasureData> ->
                //add to the end of array remote new measures
                monthlyLoaded.addAll(measureData)
                observable.postValue(monthlyLoaded)
                //Save new measures to local database as new
                insertNewAverageMonthlyMeasuresToDB(measureData)
            }
        }
        getAllAverageMonthlyMeasuresDB()
        return observable
    }
     */

    companion object {
        val TAG = MainViewModel::class.simpleName
    }
}








/*
    private fun getNewAverageDailyMeasuresRealtime(): LiveData<ArrayList<AverageDailyMeasureData>> {
        val lastDateCheckRealtimeDaily: String? =
            InternalStorage.readLastRealtimeDailyDateOnInternalStorage(appContext)
        Log.d(TAG, "lastDateCheckRealtimeDaily: $lastDateCheckRealtimeDaily"
        )
        //2.- update last read measure (internal storage)
        val dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        val now = dtf.format(LocalDateTime.now())
        InternalStorage.writeLastRealtimeDailyDateOnInternalStorage(
            appContext,
            now
        )
        Log.d(TAG, "now: $now")
        val userId = userRepository.uuid ?: throw Exception("no user")
        // if there is a date saved in Shared Preferences -> download only from last date check
        return if (lastDateCheckRealtimeDaily != null) {
            //download only the NEW measures
            Log.d(TAG, "download only NEW")
            realtime.getAllAverageDailyMeasuresAfterDate(lastDateCheckRealtimeDaily, now, userId)
        } else {
            realtime.getAllAverageDailyMeasures(userId)
        }
    }


    private fun getNewAverageMonthlyMeasuresRealtime(): LiveData<ArrayList<AverageMonthlyMeasureData>> {

        val lastDateCheckRealtimeMonthly: String? =
            InternalStorage.readLastRealtimeMonthlyDateOnInternalStorage(appContext)
        Log.d(TAG, "lastDateCheckRealtimeMonthly: $lastDateCheckRealtimeMonthly")
        //2.- update last read measure (internal storage)
        val dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        val now = dtf.format(LocalDateTime.now())
        InternalStorage.writeLastRealtimeMonthlyDateOnInternalStorage(
            appContext,
            now
        )
        Log.d(TAG, "now: $now")
        val userId = userRepository.uuid ?: throw Exception("no user")
        // if there is a date saved in Shared Preferences -> download only from last date check
        return if (lastDateCheckRealtimeMonthly != null) {
            //download only the NEW measures
            Log.d(TAG, "download only NEW")
            realtime.getAllAverageMonthlyMeasuresAfterDate(
                lastDateCheckRealtimeMonthly,
                now,
                userId
            )
        } else {
            realtime.getAllAverageMonthlyMeasures(userId)
        }
    }
 */