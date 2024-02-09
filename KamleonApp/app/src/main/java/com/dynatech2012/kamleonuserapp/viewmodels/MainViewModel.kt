package com.dynatech2012.kamleonuserapp.viewmodels

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.extensions.addDays
import com.dynatech2012.kamleonuserapp.extensions.sha256
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.InvitationStatus
import com.dynatech2012.kamleonuserapp.models.Organization
import com.dynatech2012.kamleonuserapp.repositories.CloudFunctions
import com.dynatech2012.kamleonuserapp.repositories.DatabaseDataSource
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.repositories.MeasuresRepository
import com.dynatech2012.kamleonuserapp.repositories.RealtimeRepository
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepository: UserRepository,
    private val firestoreRepo: FirestoreDataSource,
    private val measuresRepository: MeasuresRepository,
    private val databaseRepository: DatabaseDataSource,
    private val realtime: RealtimeRepository,
    private val cloudFunctions: CloudFunctions
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
    private val _userProfiles = MutableLiveData<ArrayList<Organization>>()
    val userProfiles: LiveData<ArrayList<Organization>> = _userProfiles
    private val _userUpdated = MutableLiveData<Boolean>()
    val userUpdated: LiveData<Boolean> = _userUpdated

    private val _userImagePrevUri = MutableLiveData<Uri?>()
    val userImagePrevUri: LiveData<Uri?> = _userImagePrevUri

    private val _userImageUri = MutableLiveData<Uri?>()
    val userImageUri: LiveData<Uri?> = _userImageUri

    private val _userImageDrawable = MutableLiveData<Drawable?>()
    val userImageDrawable: LiveData<Drawable?> = _userImageDrawable

    private val _measures = MutableLiveData<ArrayList<MeasureData>>()
    val measures: LiveData<ArrayList<MeasureData>> = _measures

    private val _lastMeasure = MutableLiveData<MeasureData>()
    val lastMeasure: LiveData<MeasureData> = _lastMeasure

    fun getUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            val userResult = firestoreRepo.getUserData()
            if (userResult.isSuccess && userResult.dataValue != null) {
                val user = userResult.dataValue!!
                _userData.postValue(user)
                val imageLoader = ImageLoader(appContext)
                if (user.imageUrl.isNotBlank()) {
                    val request = ImageRequest.Builder(appContext)
                        .data(user.imageUrl)
                        .allowHardware(false)
                        .size(100,100)
                        .target { drawable ->
                            // Handle the result.
                            _userImageDrawable.postValue(drawable)
                        }
                        .build()
                    val disposable = imageLoader.enqueue(request)
                }
                else {
                    _userImageDrawable.postValue(null)
                }
            }
            val userProfilesResult = cloudFunctions.getUserProfiles()
            if (userProfilesResult.isSuccess && userProfilesResult.dataValue != null) {
                val userProfiles = userProfilesResult.dataValue!!
                _userProfiles.postValue(userProfiles)
            }
            Log.d(TAG, "user data changed got user ${userResult.dataValue}")
        }
    }

    fun updateUserData(data: HashMap<String, Any>) {
        viewModelScope.launch(Dispatchers.IO) {
            val userResult = firestoreRepo.updateUser(data)
            if (userResult.isSuccess && userResult.dataValue != null) {
                getUserData()
                _userUpdated.postValue(userResult.dataValue)
                Log.d(TAG, "user data changed")
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
                throw Exception(Constants.UNMATCHING_PIN)
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
        _userImagePrevUri.postValue(uri)
        if (uri == null) {
            _userImageUri.postValue(null)
        }
        else {
            viewModelScope.launch(Dispatchers.IO) {
                val response = firestoreRepo.updateUserImage(uri)
                if (response.isSuccess && response.dataValue != null) {
                    getUserData()
                }
                _userImageUri.postValue(uri)
            }
        }
    }

    fun removeUserImage() {
        setImageUri(null)
        viewModelScope.launch(Dispatchers.IO) {
            firestoreRepo.removeUserImage()
            //_userUpdated.postValue(true)
            getUserData()
        }
    }

    fun getUserMeasures() {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d(TAG, "will try to get measures from DB and from FS")
            val measuresLoaded = ArrayList<MeasureData>()
            val dailyLoaded = ArrayList<AverageDailyMeasureData>()
            val monthlyLoaded = ArrayList<AverageMonthlyMeasureData>()
            val responseMeasuresDB = measuresRepository.getUserLastMeasureFromDB()//.collect { lastM ->
            if (responseMeasuresDB.isSuccess && responseMeasuresDB.dataValue != null) {
                Log.d(TAG, "got measures from DB size: ${responseMeasuresDB.dataValue?.size}")
                responseMeasuresDB.dataValue?.let { measuresDB ->
                    measuresLoaded.clear() // not necessary
                    measuresLoaded.addAll(measuresDB)
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
            val responseMeasuresFS = measuresRepository.getUserMeasuresFromFS(userRepository.uuid)//.collect { meas ->
            if (responseMeasuresFS.isSuccess && responseMeasuresFS.dataValue != null) {
                Log.d(TAG, "got measures from FS success size: ${responseMeasuresFS.dataValue?.size}")
                responseMeasuresFS.dataValue?.let { measuresFB ->
                    measuresLoaded.addAll(measuresFB)
                    //_measures.postValue(measuresFB)
                    _measures.postValue(measuresLoaded)
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
            val responseDailyDB = measuresRepository.getDailyAveragesFromDB()
            if (responseDailyDB.isSuccess && responseDailyDB.dataValue != null) {
                Log.d(TAG, "got daily from DB size: ${responseDailyDB.dataValue?.size}")
                responseDailyDB.dataValue?.let { dailyDB ->
                    dailyLoaded.clear() // not necessary
                    dailyLoaded.addAll(dailyDB)
                    _averageDailyMeasures.postValue(dailyDB)
                    if (dailyDB.size == 0) {
                        Log.d(TAG, "got daily from DB EMPTY")
                    }
                }
            }
            val responseMonthlyDB = measuresRepository.getMonthlyAveragesFromDB()
            if (responseMonthlyDB.isSuccess && responseMonthlyDB.dataValue != null) {
                Log.d(TAG, "got monthly from DB size: ${responseMonthlyDB.dataValue?.size}")
                responseMonthlyDB.dataValue?.let { monthlyDB ->
                    monthlyLoaded.clear() // not necessary
                    monthlyLoaded.addAll(monthlyDB)
                    _averageMonthlyMeasures.postValue(monthlyDB)
                    if (monthlyDB.size == 0) {
                        Log.d(TAG, "got monthly from DB EMPTY")
                    }
                }
            }
            measuresRepository.getUserDailyAverages(userRepository.uuid).collect { avDay ->
                if (avDay.isSuccess && avDay.dataValue != null) {
                    Log.d(TAG, "got measures daily finally 2")
                    avDay.dataValue?.let { dailyMeasures ->
                        Log.d(TAG, "got measures daily finally 2 size ${dailyMeasures.size}")
                        dailyLoaded.addAll(dailyMeasures)
                        _averageDailyMeasures.postValue(dailyLoaded)
                    }
                }
                measuresRepository.getUserMonthlyAverages(userRepository.uuid).collect { avMon ->
                    if (avMon.isSuccess && avMon.dataValue != null) {
                        Log.d(TAG, "got measures monthly finally 2")
                        avMon.dataValue?.let { monthlyMeasures ->
                            Log.d(TAG, "got measures monthly finally 2 size ${monthlyMeasures.size}")
                            monthlyLoaded.addAll(monthlyMeasures)
                            _averageMonthlyMeasures.postValue(monthlyLoaded)
                        }
                    }
                }
            }
        /*
        // get all averages from FS
            measuresRepository.getAllUserDailyAverages(userRepository.uuid)
                .collect { avDa ->
                    avDa.dataValue?.let { dailyMeasures ->
                        Log.d(
                            TAG,
                            "got measures all daily finally 2 size ${dailyMeasures.size}"
                        )
                        dailyLoaded.addAll(dailyMeasures)
                        _averageDailyMeasures.postValue(dailyLoaded)
                    } }
            measuresRepository.getAllUserMonthlyAverages(userRepository.uuid)
                .collect { avMon ->
                    avMon.dataValue?.let { monthlyMeasures ->
                        Log.d(TAG, "got measures all monthly finally 2 size ${monthlyMeasures.size}")
                        monthlyLoaded.addAll(monthlyMeasures)
                        _averageMonthlyMeasures.postValue(monthlyLoaded)
                    } }
        */
        }
    }


    // NOTIFICATIONS AND INVITATIONS

    /*
    private val _newNotificationMeasure =  MutableLiveData<Boolean>()
    val newNotificationMeasure: LiveData<Boolean> = _newNotificationMeasure
    */
    private val _newNotificationToken =  MutableLiveData<String>()
    val newNotificationToken: LiveData<String> = _newNotificationToken
    fun updateUserToken() {
        viewModelScope.launch(Dispatchers.IO) {
            firestoreRepo.updateToken()
            //_newNotificationToken.postValue(token)
        }
    }
    fun onGetNotification() {
        Log.d(TAG, "onGetNotification from viewModel")
        getUserMeasures()
        //_newNotificationMeasure.postValue(true)
    }
    fun resetNewNotification() {
        //_newNotificationMeasure.postValue(false)
    }

    private val _newInvitations =  MutableLiveData<Boolean>()
    val newInvitations: LiveData<Boolean> = _newInvitations
    fun updateInvitationsCount() {
        viewModelScope.launch(Dispatchers.IO) {
            val invitationsCount = firestoreRepo.getNewInvitations()
            _newInvitations.postValue(invitationsCount > 0)
        }
    }
    private val _pendingInvitations =  MutableLiveData<ArrayList<Invitation>>()
    val pendingInvitations: LiveData<ArrayList<Invitation>> = _pendingInvitations
    private val _oldInvitations =  MutableLiveData<ArrayList<Invitation>>()
    val oldInvitations: LiveData<ArrayList<Invitation>> = _oldInvitations
    private val _recentInvitations =  MutableLiveData<ArrayList<Invitation>>()
    val recentInvitations: LiveData<ArrayList<Invitation>> = _recentInvitations
    private val _recentInvitationModified =  MutableLiveData<Boolean>()
    val recentInvitationModified: LiveData<Boolean> = _recentInvitationModified
    private var gettingInvitationsAfterModifyingOne = false
    fun resetGettingInvitationsAfterModifyingOne() {
        gettingInvitationsAfterModifyingOne = false
    }
    fun getInvitations() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = cloudFunctions.getInvitations()
            Log.d(TAG, "HHH getInvitations: ${response.dataValue?.size}")
            val invitations = response.dataValue ?: ArrayList()
            val pendingInvitations = invitations.filter { it.status == InvitationStatus.SENT } as ArrayList<Invitation>
            _pendingInvitations.postValue(pendingInvitations)
            if (gettingInvitationsAfterModifyingOne) {
                gettingInvitationsAfterModifyingOne = false
                _recentInvitationModified.postValue(true)
            }
            // 30 days ago
            Log.d(TAG, "HHH getInvitations pending: ${pendingInvitations.size}")
            val thirtyDaysAgo = Date().addDays(-30)
            _recentInvitations.postValue(invitations.filter { it.status != InvitationStatus.SENT && it.dateSent >= thirtyDaysAgo } as ArrayList<Invitation>?)
            val oldInvitations = invitations.filter { it.status != InvitationStatus.SENT } as ArrayList<Invitation>
            //val oldInvitations = invitations.filter { it.status != InvitationStatus.SENT && it.dateSent < thirtyDaysAgo } as ArrayList<Invitation>
            /*val inFake = arrayListOf(Invitation("1", "1", "1", "1", Date(), Date(), Date().addDays(-40), InvitationStatus.SENT, InvitationRole.CENTERSTAFF_ADMIN))
            oldInvitations.addAll(inFake)*/
            _oldInvitations.postValue(oldInvitations)
        }
    }

    fun acceptInvitation(invitationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = cloudFunctions.acceptInvitation(invitationId)
            if (response.isSuccess) {
                Log.d(TAG, "HHH acceptInvitation: success")
                gettingInvitationsAfterModifyingOne = true
                getInvitations()
            }
            else {
                Log.d(TAG, "HHH acceptInvitation: failure")
            }
        }
    }

    fun rejectInvitation(invitationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = cloudFunctions.rejectInvitation(invitationId)
            if (response.isSuccess) {
                gettingInvitationsAfterModifyingOne = true
                Log.d(TAG, "HHH rejectInvitation: success")
                getInvitations()
            }
            else {
                Log.d(TAG, "HHH rejectInvitation: failure")
            }
        }
    }

    fun updateNotificationStatus(on: Boolean) {
        val data = hashMapOf<String, Any>()
        data["notifications"] = hashMapOf("analytics" to on)
        updateUserData(data)
    }


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