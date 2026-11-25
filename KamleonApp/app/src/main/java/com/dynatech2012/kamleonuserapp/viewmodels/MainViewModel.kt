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
import com.dynatech2012.kamleonuserapp.constants.PreferenceConstants.PREF_NEW_MEASURE
import com.dynatech2012.kamleonuserapp.database.AverageDailyMeasureData
import com.dynatech2012.kamleonuserapp.database.AverageMonthlyMeasureData
import com.dynatech2012.kamleonuserapp.database.MeasureData
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.HomeStatus
import com.dynatech2012.kamleonuserapp.models.homeStatusAfterLoad
import com.dynatech2012.kamleonuserapp.models.Event
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.models.InvitationItem
import com.dynatech2012.kamleonuserapp.models.RoleGroup
import com.dynatech2012.kamleonuserapp.repositories.CloudFunctions
import com.dynatech2012.kamleonuserapp.repositories.DatabaseDataSource
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.repositories.InvitationsDataSource
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
    @param:ApplicationContext private val appContext: Context,
    private val userRepository: UserRepository,
    private val firestoreRepo: FirestoreDataSource,
    private val measuresRepository: MeasuresRepository,
    private val databaseRepository: DatabaseDataSource,
    private val realtime: RealtimeDataSource,
    private val cloudFunctions: CloudFunctions,
    private val invitationsSource: InvitationsDataSource,
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
            DebugLog.d(TAG) { "User profile refresh completed" }
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

    /** Live strength feedback for the new password field. */
    suspend fun passwordStrength(password: String): PasswordStrength = passwordPolicy.estimateStrength(password)

    /**
     * Changes the password: the new one must pass the policy before the account is touched, then
     * the account is reauthenticated with [oldPwd]. The sheet waits for the result itself.
     */
    suspend fun changePassword(oldPwd: String, newPwd: String): Response<Unit> = withContext(Dispatchers.IO) {
        when (val policy = passwordPolicy.validate(newPwd)) {
            is PasswordPolicyResult.Rejected -> Response.Failure(PasswordPolicyException(policy.reason))
            else -> userRepository.changePwd(oldPwd, newPwd)
        }
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
        }
        SharedPrefUtil(appContext).saveBoolean(PREF_NEW_MEASURE, false)

    }

    /** The user's roles, grouped by organization, for the roles sheet. */
    suspend fun loadUserRoles(): Response<List<RoleGroup>> = withContext(Dispatchers.IO) {
        firestoreRepo.getUserRoles()
    }

    /** The signed-in user's e-mail, where a password reset link goes; null when there is none. */
    val accountEmail: String? get() = userRepository.email?.takeIf { it.isNotEmpty() }

    /** Emails the signed-in user a link to reset the password; null when there is no e-mail. */
    suspend fun sendPasswordReset(): ActionResult? = withContext(Dispatchers.IO) {
        accountEmail?.let { actionResultOf(cloudFunctions.resetPwd(it)) }
    }

    // NOTIFICATIONS AND INVITATIONS

    fun updateUserToken() {
        viewModelScope.launch(Dispatchers.IO) {
            firestoreRepo.updateToken()
        }
    }

    fun onGetNotification() {
        DebugLog.d(TAG) { "onGetNotification from viewModel" }
        getUserMeasures()
    }

    private val _invitations = MutableLiveData<List<InvitationItem>>()
    /** The invitations the bell lists: pending ones and the ones accepted in the last days. */
    val invitations: LiveData<List<InvitationItem>> = _invitations
    /** The bell's dot: a pending invitation the user has not seen yet. */
    val hasUnreadInvitations: LiveData<Boolean> = _invitations.map { list -> list.any { it.unread } }

    /** Reloads the invitations in the background, for the bell. */
    fun refreshInvitations() {
        viewModelScope.launch { loadInvitations() }
    }

    /** Reloads the invitations; the list keeps its last value when this fails. */
    suspend fun loadInvitations(): Response<List<InvitationItem>> = withContext(Dispatchers.IO) {
        invitationsSource.load().also { if (it is Response.Success) _invitations.postValue(it.data) }
    }

    /** Accepts an invitation; on success the profile is reloaded so the new role shows. */
    suspend fun acceptInvitation(id: String): Response<Unit> = withContext(Dispatchers.IO) {
        invitationsSource.accept(id).also { if (it is Response.Success) getUserData() }
    }

    suspend fun declineInvitation(id: String): Response<Unit> = withContext(Dispatchers.IO) {
        invitationsSource.decline(id)
    }

    suspend fun dismissInvitation(id: String): Response<Unit> = withContext(Dispatchers.IO) {
        invitationsSource.dismiss(id)
    }

    /** Marks these invitations as seen; it outlives the sheet that asks for it. */
    fun markInvitationsRead(ids: List<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            invitationsSource.markRead(ids)
            loadInvitations()
        }
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

    /**
     *
     * <font color="teal">
     * Edu: <br></br>
     * MONTHLY measures <br></br>
     * Mediator live data <br></br>
     * to expose all MONTHLY measures to fragment at once <br></br>
     * (from database and from realtime)
    </font> */

    fun checkNewMeasures() {
        val newMeasures = SharedPrefUtil(appContext).getBoolean(PREF_NEW_MEASURE, false)
        if (newMeasures) {
            getUserMeasures()
        }
    }

    var tutorialComingFromHome: Boolean = true

    companion object {
        val TAG = MainViewModel::class.simpleName
    }
}

