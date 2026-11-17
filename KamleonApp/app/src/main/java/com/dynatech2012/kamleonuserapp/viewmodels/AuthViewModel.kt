package com.dynatech2012.kamleonuserapp.viewmodels

import android.os.SystemClock
import android.text.TextUtils
import timber.log.Timber
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.repositories.CloudFunctions
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
import com.dynatech2012.kamleonuserapp.repositories.UserProfileMissingException
import com.dynatech2012.kamleonuserapp.security.PasswordLength
import com.dynatech2012.kamleonuserapp.security.PasswordPolicy
import com.dynatech2012.kamleonuserapp.security.PasswordPolicyReason
import com.dynatech2012.kamleonuserapp.security.PasswordPolicyResult
import com.dynatech2012.kamleonuserapp.security.PasswordStrength
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.firebase.firestore.Source
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: UserRepository,
    private val firestoreRepo: FirestoreDataSource,
    private val cloudFuctions: CloudFunctions,
    private val passwordPolicy: PasswordPolicy,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    enum class StartupState { Checking, NeedsLogin, NeedsProfile, NeedsPolicy, Ready, RetryableError }

    private val _startupState = MutableLiveData(StartupState.Checking)
    val startupState: LiveData<StartupState> = _startupState
    @Volatile
    var recoveringProfile = false
        private set
    val recoveryEmail: String? get() = if (recoveringProfile) authRepo.email else null

    fun pendingScan(unitId: String, sessionId: String) {
        savedStateHandle["scanUnitId"] = unitId
        savedStateHandle["scanSessionId"] = sessionId
        savedStateHandle["scanSavedAt"] = SystemClock.elapsedRealtime()
    }

    /** The scan still waiting for login or registration, if it has not expired. */
    private fun currentPendingScan(): Pair<String, String>? = pendingScanFor(
        savedStateHandle.get<String>("scanUnitId"),
        savedStateHandle.get<String>("scanSessionId"),
        savedStateHandle.get<Long>("scanSavedAt"),
        SystemClock.elapsedRealtime()
    )

    val hasPendingScan: Boolean get() = currentPendingScan() != null

    /** Changes whenever a scan link is saved or consumed, so screens can react to it. */
    val pendingScanSavedAt: LiveData<Long?> = savedStateHandle.getLiveData("scanSavedAt")

    fun consumePendingScan(): Pair<String, String>? {
        val scan = currentPendingScan()
        savedStateHandle.remove<String>("scanUnitId")
        savedStateHandle.remove<String>("scanSessionId")
        savedStateHandle.remove<Long>("scanSavedAt")
        if (scan == null) Timber.d("No pending scan to deliver, or it expired")
        return scan
    }

    @Volatile
    private var startupCheck = 0

    var fName: String? = ""
    var lName: String? = ""
    var email: String? = ""
    var pass: String? = ""
    var birthday: Date = Date()//LocalDate = LocalDate.MIN
    var height: Float? = null
    var weight: Float? = null
    var gender = Gender.none

    private val _uiState = MutableLiveData(0)
    val uiState: LiveData<Int> = _uiState

    val splashDurationCompleted = MutableLiveData<Boolean>()
    var appInitializationStarted = false // To prevent re-initialization

    /**
     * MainActivity was already started from this InitActivity. It lives here, not in the activity:
     * a recreated InitActivity (theme, language or font size change) re-observes the Ready state
     * and the login result, and would otherwise start Main a second time.
     */
    var mainOpened = false

    fun signalSplashDurationCompleted() {
        splashDurationCompleted.postValue(true)
    }

    fun resetSplashStateForInit() {
        splashDurationCompleted.value = false // Or null
        appInitializationStarted = false
    }

    /**
     * True once the profile document exists. From then on, going back in the onboarding and
     * changing the birth date must update that document ([updateBirthDate]) instead of running the
     * registration again, which would also send another verification email.
     */
    var profileCreated = false
        private set

    /** Saves a birth date chosen after the profile was created; reports state 2 or -1. */
    fun updateBirthDate() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = firestoreRepo.updateUser(hashMapOf("dateOfBirth" to birthday))
            Timber.d("Birth date update succeeded: %s", result.isSuccess)
            _uiState.postValue(if (result.isSuccess) 2 else -1)
        }
    }

    fun signup() {
        Timber.d("Registration requested")
        viewModelScope.launch(Dispatchers.IO) {
            if (email != null && fName != null && lName != null &&
                (recoveringProfile || pass != null)) {
                val resumingProfile = recoveringProfile
                val authResult = if (resumingProfile) null else authRepo.signup(email!!, pass!!)
                val authSucceeded = if (resumingProfile) {
                    authRepo.checkLogged && authRepo.email == email
                } else {
                    authResult?.isSuccess == true
                }
                Timber.d("Registration authentication available: %s", authSucceeded)
                if (authSucceeded) {
                    recoveringProfile = true
                    pass = ""
                    _uiState.postValue(1)
                    val registerResult =
                        firestoreRepo.createUserStep1(
                            email!!,
                            fName!!,
                            lName!!,
                            birthday,
                            healthConsent = true,
                            appPrivacy = true
                        )
                    if (registerResult.isSuccess && registerResult.dataValue != null) {
                        profileCreated = true
                        _uiState.postValue(2)
                        if (authRepo.isEmailVerified) _uiState.postValue(3) else sendVerificationEmail()
                    } else {
                        recoveringProfile = authRepo.checkLogged && authRepo.email == email
                        _uiState.postValue(-1)
                    }
                } else {
                    _uiState.postValue(
                        if (resumingProfile || authResult?.error is FirebaseAuthUserCollisionException) -7
                        else -1
                    )
                }
            } else {
                _uiState.postValue(-1)
            }
        }
    }

    // Result of the last registration validation: 0/1 name missing, 2/3 email missing/invalid,
    // 4 password too short, 5 too long, 7 too weak, 8 found in a breach, 6 all good, -3 session lost.
    private val _registerInputs = MutableLiveData(-1)
    val registerInputs: LiveData<Int> = _registerInputs

    // True while the password policy (which may query the breach service) is being evaluated.
    private val _registerChecking = MutableLiveData(false)
    val registerChecking: LiveData<Boolean> = _registerChecking

    fun resetRegisterInputs() {
        _registerInputs.value = -1
    }

    /** Live strength feedback for the password field. */
    suspend fun passwordStrength(password: String): PasswordStrength = passwordPolicy.estimateStrength(password)

    fun checkRegisterInputs() {
        Timber.d("Validating registration inputs")
        if (_registerChecking.value == true) return
        //check every input
        if (fName.isNullOrEmpty()) { //first name is empty or null
            _registerInputs.postValue(0)
            return
        }
        if (lName.isNullOrEmpty()) { //last name is empty or null
            _registerInputs.postValue(1)
            return
        }
        if (email.isNullOrEmpty()) { //email is empty or null
            _registerInputs.postValue(2)
            return
        }
        //check if an email is valid email
        if (!isValidEmail(email!!)) {
            _registerInputs.postValue(3)
            return
        }
        if (recoveringProfile) {
            // A signed-in account with no profile does not need a new password.
            _registerInputs.value = if (!authRepo.checkLogged || authRepo.email != email) -3 else 6
            return
        }
        val password = pass.orEmpty()
        _registerChecking.value = true
        viewModelScope.launch {
            val code = try {
                when (val result = passwordPolicy.validate(password)) {
                    PasswordPolicyResult.Valid -> 6
                    is PasswordPolicyResult.Rejected -> when (result.reason) {
                        PasswordPolicyReason.TOO_SHORT -> 4
                        PasswordPolicyReason.TOO_LONG -> 5
                        PasswordPolicyReason.WEAK -> 7
                        PasswordPolicyReason.BREACHED -> 8
                    }
                }
            } finally {
                _registerChecking.value = false
            }
            _registerInputs.value = code
        }
    }

    fun notificationPermissionGranted() {
        _uiState.postValue(4)
    }

    /*
    fun locationPermissionGranted() {
        _uiState.postValue(4)
    }
    */

    fun finishSignup() {
        viewModelScope.launch(Dispatchers.IO) {
            val registerResult = firestoreRepo.createUserStep2(height, weight, gender)
            Timber.d("Registration profile update succeeded: %s", registerResult.isSuccess)
            _uiState.postValue(
                if (!registerResult.isSuccess) -1
                else if (authRepo.isEmailVerified) 7
                else 5
            )
        }
    }

    fun sendVerificationEmail() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = authRepo.uuid ?: return@launch
            val emailAddress = authRepo.email ?: return@launch
            val username = if (!fName.isNullOrBlank() && !lName.isNullOrBlank()) {
                "$fName $lName"
            } else {
                val profile = firestoreRepo.getUserData().dataValue
                if (profile == null) {
                    _uiState.postValue(-6)
                    return@launch
                }
                "${profile.name} ${profile.lastName}"
            }
            Timber.d("Requesting verification email")
            val verificationResponse =
                cloudFuctions.sendVerificationEmail(userId, emailAddress, username)
            if (verificationResponse.isSuccess) {
                Timber.d("Verification email request succeeded")
                _uiState.postValue(3)
            } else {
                _uiState.postValue(-6)
            }
        }
    }

    fun acceptPolicy() {
        viewModelScope.launch(Dispatchers.IO) {
            val policyResponse = firestoreRepo.updateLegal(false, true, false)
            if (policyResponse.isSuccess) {
                _uiState.postValue(7)
            }
        }
    }

    fun login(email: String, pass: String) {
        recoveringProfile = false
        profileCreated = false
        Timber.d("Login requested")
        _uiState.value = 1
        if (!isValidEmail(email)) {
            _uiState.postValue(-4)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val authResult = authRepo.login(email, pass)
            if (authResult.isSuccess && authResult.dataValue != null) {
                when (val userResponse = firestoreRepo.getUserData(Source.SERVER)) {
                    is com.dynatech2012.kamleonuserapp.repositories.Response.Success -> {
                        fName = userResponse.data.name
                        lName = userResponse.data.lastName
                        _uiState.postValue(
                            if (!authRepo.isEmailVerified) -5
                            else if (userResponse.data.legal.privacyPolicyApp) 7
                            else 6
                        )
                    }
                    is com.dynatech2012.kamleonuserapp.repositories.Response.Failure -> {
                        if (userResponse.exception is UserProfileMissingException) {
                            recoveringProfile = true
                            _uiState.postValue(8)
                        } else {
                            _uiState.postValue(-6)
                        }
                    }
                    else -> _uiState.postValue(-6)
                }
            }
            else if (authResult.isFailure) {
                when (val e = authResult.error) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                        // user invalid
                        Timber.w("Login failed: user invalid")
                        _uiState.postValue(-3)
                    }

                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                        // Pass wrong
                        Timber.w("Login failed: credentials invalid")
                        _uiState.postValue(-2)
                    }

                    else -> {
                        Timber.w("Login failed: %s", e?.javaClass?.simpleName ?: "Unknown")
                        _uiState.postValue(-1)
                    }
                }
            } else {
                _uiState.postValue(0)
            }
        }
    }

    fun resetUiState() {
        _uiState.value = 0
    }

    fun checkLogin() {
        val request = ++startupCheck
        _startupState.value = StartupState.Checking
        viewModelScope.launch(Dispatchers.IO) {
            val authenticated = authRepo.checkLogged
            val profile = if (authenticated) {
                profileOrTimeout(PROFILE_CHECK_TIMEOUT_MS) { firestoreRepo.getUserData(Source.SERVER) }
            } else {
                null
            }
            val state = startupStateFor(authenticated, authRepo.isEmailVerified, profile)
            if (request == startupCheck) {
                val flags = registrationFlagsAfterCheck(
                    authenticated, state, RegistrationFlags(recoveringProfile, profileCreated)
                )
                recoveringProfile = flags.recoveringProfile
                profileCreated = flags.profileCreated
                Timber.d("Startup authentication state: %s", state)
                _startupState.postValue(state)
            }
        }
    }


    private fun isValidEmail(target: CharSequence): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    companion object {
        val TAG = AuthViewModel::class.simpleName
    }
}
