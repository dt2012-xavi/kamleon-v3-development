package com.dynatech2012.kamleonuserapp.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import androidx.lifecycle.map
import com.dynatech2012.kamleonuserapp.views.forgetCachedImages
import com.dynatech2012.kamleonuserapp.views.profileImageUrlOf
import com.dynatech2012.kamleonuserapp.network.ActionResult
import com.dynatech2012.kamleonuserapp.network.actionResultOf
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_FIRST_LOGIN
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_NEW_MEASURE
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.extensions.addDays
import com.dynatech2012.kamleonuserapp.extensions.sha256
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.HomeStatus
import com.dynatech2012.kamleonuserapp.models.homeStatusAfterLoad
import com.dynatech2012.kamleonuserapp.models.Event
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.models.Invitation
import com.dynatech2012.kamleonuserapp.models.InvitationStatus
import com.dynatech2012.kamleonuserapp.models.Organization
import com.dynatech2012.kamleonuserapp.repositories.CloudFunctions
import com.dynatech2012.kamleonuserapp.repositories.DatabaseDataSource
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.repositories.MeasuresRepository
import com.dynatech2012.kamleonuserapp.repositories.RealtimeDataSource
import com.dynatech2012.kamleonuserapp.network.SessionWatcher
import com.dynatech2012.kamleonuserapp.repositories.SessionCleaner
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
import com.dynatech2012.kamleonuserapp.security.PasswordPolicy
import com.dynatech2012.kamleonuserapp.security.PasswordPolicyException
import com.dynatech2012.kamleonuserapp.security.PasswordPolicyResult
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import com.dynatech2012.kamleonuserapp.utils.ProfilePhoto
import com.dynatech2012.kamleonuserapp.utils.SharedPrefUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepository: UserRepository,
    private val firestoreRepo: FirestoreDataSource,
    private val measuresRepository: MeasuresRepository,
    private val databaseRepository: DatabaseDataSource,
    private val realtime: RealtimeDataSource,
    private val cloudFunctions: CloudFunctions,
    private val passwordPolicy: PasswordPolicy,
    private val sessionCleaner: SessionCleaner,
    sessionWatcher: SessionWatcher,
    /** The app-wide image loader; the screens that show the avatar load it with this one. */
    val imageLoader: ImageLoader
) : ViewModel() {


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
    private val _userUpdated = MutableLiveData<Response<Unit>?>()
    val userUpdated: LiveData<Response<Unit>?> = _userUpdated

    private val _userImagePrevUri = MutableLiveData<Uri?>()
    val userImagePrevUri: LiveData<Uri?> = _userImagePrevUri

    /** The photo sheet is done (saved, removed or failed). Not a Uri: an event with null content is never delivered. */
    private val _userImageUri = MutableLiveData<Event<Unit>>()
    val userImageUri: LiveData<Event<Unit>> = _userImageUri

    /** The avatar address, or null when the account has no photo. The screens load it themselves. */
    val userImageUrl: LiveData<String?> = _userData.map { profileImageUrlOf(it.imageUrl) }

    /** A new photo is being prepared and uploaded. */
    private val _userImageUploading = MutableLiveData(false)
    val userImageUploading: LiveData<Boolean> = _userImageUploading

    private val _userImageFailed = MutableLiveData<Event<ActionResult>>()
    val userImageFailed: LiveData<Event<ActionResult>> = _userImageFailed

    private val _measures = MutableLiveData<ArrayList<MeasureData>>()
    val measures: LiveData<ArrayList<MeasureData>> = _measures

    private val _lastMeasure = MutableLiveData<MeasureData>()
    val lastMeasure: LiveData<MeasureData> = _lastMeasure

    private val _homeStatus = MutableLiveData(HomeStatus.Loading)
    /** What Home shows while it has no sample; once a sample arrives [lastMeasure] takes over. */
    val homeStatus: LiveData<HomeStatus> = _homeStatus

    fun getUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            val userResult = firestoreRepo.getUserData()
            if (userResult.isSuccess && userResult.dataValue != null) {
                val user = userResult.dataValue!!
                _userData.postValue(user)
                DebugLog.d(TAG) { "User profile has photo: ${profileImageUrlOf(user.imageUrl) != null}" }
            }
            val userProfilesResult = cloudFunctions.getUserProfiles()
            if (userProfilesResult.isSuccess && userProfilesResult.dataValue != null) {
                val userProfiles = userProfilesResult.dataValue!!
                _userProfiles.postValue(userProfiles)
            }
            DebugLog.d(TAG) { "User profile refresh completed" }
        }
    }

    fun updateUserData(data: HashMap<String, Any>) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = firestoreRepo.updateUser(data)
            if (response.isSuccess && response.dataValue != null) {
                getUserData()
            }
            _userUpdated.postValue(response)
            DebugLog.d(TAG) { "user data changed" }
        }
    }

    /**
     * Saves Personal data [fields] from a profile sheet and reloads the profile when it worked.
     * The sheet waits for the result itself, so no other screen sees it.
     */
    suspend fun saveProfile(fields: Map<String, Any>): Response<Unit> = withContext(Dispatchers.IO) {
        val response = firestoreRepo.updateUser(HashMap(fields))
        if (response.isSuccess) getUserData()
        response
    }

    /** The code to tell the user when the server ended their session; null while it is fine. */
    internal val sessionEnded: LiveData<ErrorCode?> = sessionWatcher.ended
    private val watcher = sessionWatcher

    val logoutStatus = MutableLiveData<Boolean>()

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionCleaner.clearLocalData()
            // Synchronous: there is no session left once it returns, so the screen is told only
            // after the sign-out and can never open the startup check while still signed in.
            userRepository.logout()
            watcher.reset()
            logoutStatus.postValue(true)
        }
    }

    fun changePin(oldPin: String, newPin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldPinHash = oldPin.sha256()
            DebugLog.i("PIN") { "PIN change requested" }

            if (oldPinHash != userData.value?.pin) {
                DebugLog.i("PIN") { "PIN change rejected: current PIN mismatch" }
                val exception = Exception(Constants.UNMATCHING_PIN)
                val response = Response.Failure(exception)
                _userUpdated.postValue(response)
            } else {
                val newPinHash = newPin.sha256()
                DebugLog.i("PIN") { "PIN change accepted; updating profile" }
                val data = hashMapOf<String, Any>("pin" to newPinHash)
                val response = firestoreRepo.updateUser(data)
                DebugLog.i("PIN") { "PIN update completed: success=${response.isSuccess}" }
                //_userUpdated.postValue(true)
                _userUpdated.postValue(response)
            }
        }
    }

    fun changeEmail(currentPwd: String, newEmail: String) {
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.changeEmail(currentPwd, newEmail)
            val data = hashMapOf<String, Any>("email" to newEmail)
            val response = firestoreRepo.updateUser(data)
            //_userUpdated.postValue(true)
            _userUpdated.postValue(response)
        }
    }

    private val _pwdChanging = MutableLiveData(false)
    /** True while a password change (policy check plus reauthentication) is in flight. */
    val pwdChanging: LiveData<Boolean> = _pwdChanging

    /** Live strength feedback for the new password field. */
    suspend fun passwordStrength(password: String): PasswordStrength = passwordPolicy.estimateStrength(password)

    fun changePwd(oldPwd: String, newPwd: String) {
        if (_pwdChanging.value == true) return
        _pwdChanging.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // The new password must pass the policy before the account is touched.
                val policy = passwordPolicy.validate(newPwd)
                if (policy is PasswordPolicyResult.Rejected) {
                    _userUpdated.postValue(Response.Failure(PasswordPolicyException(policy.reason)))
                    return@launch
                }
                val response = userRepository.changePwd(oldPwd, newPwd)
                //_userUpdated.postValue(true)
                _userUpdated.postValue(response)
            } finally {
                _pwdChanging.postValue(false)
            }
        }
    }

    fun deleteUser() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = userRepository.deleteUser()
            //_userUpdated.postValue(true)
            _userUpdated.postValue(response)
        }
    }

    fun resetUserUpdated() {
        _userUpdated.postValue(null)
    }

    fun setImageUri(uri: Uri?) {
        _userImagePrevUri.postValue(uri)
        if (uri == null) {
            _userImageUri.postValue(Event(Unit))
        } else {
            _userImageUploading.postValue(true)
            viewModelScope.launch(Dispatchers.IO) {
                // The 568 px photo and its 256 px thumbnail, as the web saves them.
                val prepared = ProfilePhoto.prepare(appContext, uri)
                val response = if (prepared == null) Response.Failure(Exception("Unreadable photo"))
                else firestoreRepo.updateUserImage(prepared.full, prepared.thumbnail)
                if (response.isSuccess && response.dataValue != null) {
                    // Same storage path every time: forget the cached copy so the new photo is fetched.
                    imageLoader.forgetCachedImages(_userData.value?.imageUrl)
                    getUserData()
                } else {
                    // Nothing was saved: stop showing the photo that was picked as if it were the avatar.
                    _userImagePrevUri.postValue(null)
                    _userImageFailed.postValue(Event(actionResultOf(response)))
                }
                _userImageUploading.postValue(false)
                _userImageUri.postValue(Event(Unit))
            }
        }
    }

    fun removeUserImage() {
        setImageUri(null)
        viewModelScope.launch(Dispatchers.IO) {
            val response = firestoreRepo.removeUserImage()
            if (response.isSuccess) {
                imageLoader.forgetCachedImages(_userData.value?.imageUrl)
            } else {
                _userImageFailed.postValue(Event(actionResultOf(response)))
            }
            getUserData()
        }
    }

    /** Home's "Retry": show the loading state again and reload the samples. Call from the main thread. */
    fun retryUserMeasures() {
        _homeStatus.value = HomeStatus.Loading
        getUserMeasures()
    }

    fun getUserMeasures() {
        viewModelScope.launch(Dispatchers.IO) {
            DebugLog.d(TAG) { "will try to get measures from DB and from FS" }
            val measuresLoaded = ArrayList<MeasureData>()
            val dailyLoaded = ArrayList<AverageDailyMeasureData>()
            val monthlyLoaded = ArrayList<AverageMonthlyMeasureData>()
            val responseMeasuresDB =
                measuresRepository.getUserLastMeasureFromDB()//.collect { lastM ->
            if (responseMeasuresDB.isSuccess && responseMeasuresDB.dataValue != null) {
                DebugLog.d(TAG) { "got measures from DB size: ${responseMeasuresDB.dataValue?.size}" }
                responseMeasuresDB.dataValue?.let { measuresDB ->
                    measuresLoaded.clear() // not necessary
                    measuresLoaded.addAll(measuresDB)
                    _measures.postValue(measuresDB)
                    if (measuresDB.size > 0) {
                        DebugLog.d(TAG) { "got measures from DB not empty" }
                        _lastMeasure.postValue(measuresDB[0])
                    } else {
                        DebugLog.d(TAG) { "got measures from DB EMPTY" }
                    }
                }
            }
            val responseMeasuresFS =
                measuresRepository.getUserMeasuresFromFS(userRepository.uuid)//.collect { meas ->
            if (responseMeasuresFS.isSuccess && responseMeasuresFS.dataValue != null) {
                DebugLog.d(TAG) { "got measures from FS success size: ${responseMeasuresFS.dataValue?.size}" }
                responseMeasuresFS.dataValue?.let { measuresFB ->
                    measuresLoaded.addAll(measuresFB)
                    //_measures.postValue(measuresFB)
                    _measures.postValue(measuresLoaded)
                    if (measuresFB.size > 0) {
                        DebugLog.d(TAG) { "got measures from FS not empty" }
                        _lastMeasure.postValue(measuresFB[0])
                    } else {
                        DebugLog.d(TAG) { "got measures from FS EMPTY" }
                    }
                }
            }
            // Only matters while nothing is shown: Home ignores it once it has a sample on screen.
            homeStatusAfterLoad(measuresLoaded.size, responseMeasuresFS.isSuccess)?.let { _homeStatus.postValue(it) }
            val responseDailyDB = measuresRepository.getDailyAveragesFromDB()
            if (responseDailyDB.isSuccess && responseDailyDB.dataValue != null) {
                DebugLog.d(TAG) { "got daily from DB size: ${responseDailyDB.dataValue?.size}" }
                responseDailyDB.dataValue?.let { dailyDB ->
                    dailyLoaded.clear() // not necessary
                    dailyLoaded.addAll(dailyDB)
                    _averageDailyMeasures.postValue(dailyDB)
                    if (dailyDB.size == 0) {
                        DebugLog.d(TAG) { "got daily from DB EMPTY" }
                    }
                }
            }
            val responseMonthlyDB = measuresRepository.getMonthlyAveragesFromDB()
            if (responseMonthlyDB.isSuccess && responseMonthlyDB.dataValue != null) {
                DebugLog.d(TAG) { "got monthly from DB size: ${responseMonthlyDB.dataValue?.size}" }
                responseMonthlyDB.dataValue?.let { monthlyDB ->
                    monthlyLoaded.clear() // not necessary
                    monthlyLoaded.addAll(monthlyDB)
                    _averageMonthlyMeasures.postValue(monthlyDB)
                    if (monthlyDB.size == 0) {
                        DebugLog.d(TAG) { "got monthly from DB EMPTY" }
                    }
                }
            }
            val avDay =
                measuresRepository.getUserDailyAverages(userRepository.uuid)//.collect { avDay ->
            if (avDay.isSuccess && avDay.dataValue != null) {
                DebugLog.d(TAG) { "got measures daily finally 2" }
                avDay.dataValue?.let { dailyMeasures ->
                    DebugLog.d(TAG) { "got measures daily finally 2 size ${dailyMeasures.size}" }
                    dailyLoaded.addAll(dailyMeasures)
                    _averageDailyMeasures.postValue(dailyLoaded)
                }
            }
            val avMon =
                measuresRepository.getUserMonthlyAverages(userRepository.uuid)//.collect { avMon ->
            if (avMon.isSuccess && avMon.dataValue != null) {
                DebugLog.d(TAG) { "got measures monthly finally 2" }
                avMon.dataValue?.let { monthlyMeasures ->
                    DebugLog.d(TAG) { "got measures monthly finally 2 size ${monthlyMeasures.size}" }
                    monthlyLoaded.addAll(monthlyMeasures)
                    _averageMonthlyMeasures.postValue(monthlyLoaded)
                }
            }
            //}
            //}
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
        SharedPrefUtil(appContext).saveBoolean(PREF_NEW_MEASURE, false)

    }

    private val _resetPwdSuccess = MutableLiveData<ActionResult>()
    val resetPwdSuccess: LiveData<ActionResult> = _resetPwdSuccess
    fun resetPwd() {
        viewModelScope.launch(Dispatchers.IO) {
            val userEmail = userRepository.email
            if (!userEmail.isNullOrEmpty()) {
                _resetPwdSuccess.postValue(actionResultOf(cloudFunctions.resetPwd(userEmail)))
            }
        }
    }

    private val _resetPINSuccess = MutableLiveData<ActionResult>()
    val resetPINSuccess: LiveData<ActionResult> = _resetPINSuccess

    fun resetPIN() {
        viewModelScope.launch(Dispatchers.IO) {
            val userEmail = userRepository.email
            val userUUid = userRepository.uuid
            if (!userEmail.isNullOrEmpty() && !userUUid.isNullOrEmpty()) {
                _resetPINSuccess.postValue(actionResultOf(cloudFunctions.resetPin(userEmail, userUUid)))
            }
        }
    }


    // NOTIFICATIONS AND INVITATIONS

    /*
    private val _newNotificationMeasure =  MutableLiveData<Boolean>()
    val newNotificationMeasure: LiveData<Boolean> = _newNotificationMeasure
    */
    private val _newNotificationToken = MutableLiveData<String>()
    val newNotificationToken: LiveData<String> = _newNotificationToken
    fun updateUserToken() {
        viewModelScope.launch(Dispatchers.IO) {
            firestoreRepo.updateToken()
            //_newNotificationToken.postValue(token)
        }
    }

    fun onGetNotification() {
        DebugLog.d(TAG) { "onGetNotification from viewModel" }
        getUserMeasures()
        //_newNotificationMeasure.postValue(true)
    }

    fun resetNewNotification() {
        //_newNotificationMeasure.postValue(false)
    }

    private val _newInvitations = MutableLiveData<Boolean>()
    val newInvitations: LiveData<Boolean> = _newInvitations
    fun updateInvitationsCount() {
        viewModelScope.launch(Dispatchers.IO) {
            val invitationsCount = firestoreRepo.getNewInvitations()
            _newInvitations.postValue(invitationsCount > 0)
        }
    }

    private val _pendingInvitations = MutableLiveData<ArrayList<Invitation>>()
    val pendingInvitations: LiveData<ArrayList<Invitation>> = _pendingInvitations
    private val _oldInvitations = MutableLiveData<ArrayList<Invitation>>()
    val oldInvitations: LiveData<ArrayList<Invitation>> = _oldInvitations
    private val _recentInvitations = MutableLiveData<ArrayList<Invitation>>()
    val recentInvitations: LiveData<ArrayList<Invitation>> = _recentInvitations
    private val _recentInvitationModified = MutableLiveData<Boolean>()
    val recentInvitationModified: LiveData<Boolean> = _recentInvitationModified
    private var gettingInvitationsAfterModifyingOne = false
    fun resetGettingInvitationsAfterModifyingOne() {
        gettingInvitationsAfterModifyingOne = false
        _recentInvitationModified.postValue(false)
    }

    fun getInvitations() {
        viewModelScope.launch(Dispatchers.IO) {
            val response = cloudFunctions.getInvitations()
            DebugLog.d(TAG) { "HHH getInvitations: ${response.dataValue?.size}" }
            val invitations = response.dataValue ?: ArrayList()
            val pendingInvitations =
                invitations.filter { it.status == InvitationStatus.SENT } as ArrayList<Invitation>
            _pendingInvitations.postValue(pendingInvitations)
            if (gettingInvitationsAfterModifyingOne) {
                gettingInvitationsAfterModifyingOne = false
                _recentInvitationModified.postValue(true)
            }
            // 30 days ago
            DebugLog.d(TAG) { "HHH getInvitations pending: ${pendingInvitations.size}" }
            val thirtyDaysAgo = Date().addDays(-30)
            _recentInvitations.postValue(invitations.filter { it.status != InvitationStatus.SENT && it.dateSent >= thirtyDaysAgo } as ArrayList<Invitation>?)
            val oldInvitations =
                invitations.filter { it.status != InvitationStatus.SENT } as ArrayList<Invitation>
            //val oldInvitations = invitations.filter { it.status != InvitationStatus.SENT && it.dateSent < thirtyDaysAgo } as ArrayList<Invitation>
            /*val inFake = arrayListOf(Invitation("1", "1", "1", "1", Date(), Date(), Date().addDays(-40), InvitationStatus.SENT, InvitationRole.CENTERSTAFF_ADMIN))
            oldInvitations.addAll(inFake)*/
            _oldInvitations.postValue(oldInvitations)
        }
    }

    fun acceptInvitation(invitation: Invitation, optional: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val invitationId = invitation.id
            val isAdmin = invitation.isAdmin
            val opt = optional && !isAdmin
            DebugLog.d(TAG) { "Accepting invitation" }
            val response = cloudFunctions.acceptInvitation(invitationId, opt)
            if (response.isSuccess) {
                DebugLog.d(TAG) { "HHH acceptInvitation: success" }
                if (isAdmin)
                    firestoreRepo.updateLegal(true, false, false)
                else
                    firestoreRepo.updateLegal(false, true, true)
                gettingInvitationsAfterModifyingOne = true
                getInvitations()
            } else {
                DebugLog.d(TAG) { "HHH acceptInvitation: failure" }
            }
        }
    }

    fun rejectInvitation(invitationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = cloudFunctions.rejectInvitation(invitationId)
            if (response.isSuccess) {
                gettingInvitationsAfterModifyingOne = true
                DebugLog.d(TAG) { "HHH rejectInvitation: success" }
                getInvitations()
            } else {
                DebugLog.d(TAG) { "HHH rejectInvitation: failure" }
            }
        }
    }

    fun updateNotificationStatus(on: Boolean) {
        val data = hashMapOf<String, Any>()
        data["notifications"] = hashMapOf("analytics" to on)
        updateUserData(data)
    }


    private val _averageDailyMeasures = MutableLiveData<ArrayList<AverageDailyMeasureData>>()
    val averageDailyMeasures: LiveData<ArrayList<AverageDailyMeasureData>> =
        _averageDailyMeasures
    private val _averageMonthlyMeasures =
        MutableLiveData<ArrayList<AverageMonthlyMeasureData>>()
    val averageMonthlyMeasures: LiveData<ArrayList<AverageMonthlyMeasureData>> =
        _averageMonthlyMeasures


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

    fun getFirstLogin(): Boolean {
        val firstLogin = SharedPrefUtil(appContext).getBoolean(PREF_FIRST_LOGIN, true)
        SharedPrefUtil(appContext).saveBoolean(PREF_FIRST_LOGIN, false)
        return firstLogin
    }


    fun checkNewMeasures() {
        val newMeasures = SharedPrefUtil(appContext).getBoolean(PREF_NEW_MEASURE, false)
        if (newMeasures) {
            getUserMeasures()
        }
    }

    var tutorialComingFromHome: Boolean = true


    private val _trialSent = MutableLiveData<Boolean>()
    val trialSent: LiveData<Boolean> = _trialSent
    fun sendTrialEmail() {
        DebugLog.d(TAG) { "sendTrialEmail" }
        val email = userRepository.email
        if (email == null) {
            _trialSent.postValue(false)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            cloudFunctions.sendTrialEmail(email)
            _trialSent.postValue(true)
        }
    }

    fun resetTrialSent() {
        _trialSent.postValue(false)
    }

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
