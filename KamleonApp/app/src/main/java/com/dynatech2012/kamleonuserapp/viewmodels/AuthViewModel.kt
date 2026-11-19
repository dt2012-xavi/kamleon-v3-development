package com.dynatech2012.kamleonuserapp.viewmodels

import android.os.SystemClock
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.SessionCleaner
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
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
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: UserRepository,
    private val firestoreRepo: FirestoreDataSource,
    private val cloudFuctions: CloudFunctions,
    private val passwordPolicy: PasswordPolicy,
    private val sessionCleaner: SessionCleaner,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    enum class StartupState {
        Checking, NeedsLogin, NeedsProfile, NeedsPolicy, Ready, RetryableError, ServerError, SessionRejected,
        NeedsVerification
    }

    private val _startupState = MutableLiveData(StartupState.Checking)
    val startupState: LiveData<StartupState> = _startupState

    /** The code to show with the startup error sheet that has no way out; set before the state is posted. */
    @Volatile
    internal var startupErrorCode: ErrorCode? = null
        private set

    /** The code to show with a login sheet that has no way out; set before the state is posted. */
    @Volatile
    internal var loginErrorCode: ErrorCode? = null
        private set

    private fun report(code: ErrorCode, e: Exception) {
        // Only the code and the kind of failure: no account data.
        Timber.w("Error %s (%s)", code.label, e.javaClass.simpleName)
        FirebaseCrashlytics.getInstance().setCustomKey("error_code", code.label)
    }
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
            val sent = requestVerificationEmail() ?: return@launch
            _uiState.postValue(if (sent) 3 else -6)
        }
    }

    /** Asks for a new verification email: whether it was sent, or null when nobody is signed in. */
    suspend fun requestVerificationEmail(): Boolean? = withContext(Dispatchers.IO) {
        val userId = authRepo.uuid ?: return@withContext null
        val emailAddress = authRepo.email ?: return@withContext null
        val username = if (!fName.isNullOrBlank() && !lName.isNullOrBlank()) {
            "$fName $lName"
        } else {
            val profile = firestoreRepo.getUserData().dataValue ?: return@withContext false
            "${profile.name} ${profile.lastName}"
        }
        Timber.d("Requesting verification email")
        val sent = cloudFuctions.sendVerificationEmail(userId, emailAddress, username).isSuccess
        Timber.d("Verification email request succeeded: %s", sent)
        sent
    }

    /** The startup check found a signed-in user who has not accepted the policy: ask, no password needed. */
    fun showPolicy() {
        _uiState.value = 6
    }

    fun acceptPolicy() {
        _uiState.value = 1
        viewModelScope.launch(Dispatchers.IO) {
            val policyResponse = firestoreRepo.updateLegal(false, true, false)
            // Without an answer on failure the sign-in button kept spinning for ever.
            _uiState.postValue(if (policyResponse.isSuccess) 7 else LoginState.POLICY_RETRY)
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
                loadProfileAfterSignIn()
            } else if (authResult.isFailure) {
                val kind = authErrorKindOf(authResult.error)
                Timber.w("Login failed: %s", kind)
                if (kind == AuthErrorKind.UserDisabled) {
                    errorCodeOf(authResult.error!!).also { code ->
                        loginErrorCode = code
                        report(code, authResult.error!!)
                    }
                }
                _uiState.postValue(loginStateFor(kind))
            } else {
                _uiState.postValue(0)
            }
        }
    }

    /** The user signed in: read their profile and say where they go next. */
    private suspend fun loadProfileAfterSignIn(trustMonitor: Boolean = true) {
        val profile = profileOrTimeout(PROFILE_CHECK_TIMEOUT_MS) { firestoreRepo.getUserData(Source.SERVER, trustMonitor) }
        when (profile) {
            is Response.Success -> {
                fName = profile.data.name
                lName = profile.data.lastName
                _uiState.postValue(
                    if (!authRepo.isEmailVerified) LoginState.EMAIL_NOT_VERIFIED
                    else if (profile.data.legal.privacyPolicyApp) 7
                    else 6
                )
            }
            is Response.Failure -> {
                val state = loginStateForProfileFailure(profile.exception)
                Timber.w("Profile after sign-in not available: %s", state)
                if (state == LoginState.ACCOUNT_REJECTED) {
                    errorCodeOf(profile.exception).also { code ->
                        loginErrorCode = code
                        report(code, profile.exception)
                    }
                }
                if (state == LoginState.PROFILE_MISSING) recoveringProfile = true
                _uiState.postValue(state)
            }
            else -> _uiState.postValue(LoginState.PROFILE_RETRY)
        }
    }

    /** Another try at the profile after a sign-in that worked: no need to type the password again. */
    fun retryProfileAfterLogin() {
        _uiState.value = 1
        viewModelScope.launch(Dispatchers.IO) { loadProfileAfterSignIn(trustMonitor = false) }
    }

    /**
     * Ends the session and wipes what belongs to the user on this phone; the login screen, where
     * they already are, shows the progress. Used when staying signed in makes no sense.
     * [restartStartupCheck]: from the splash, check again once signed out so that it leads to Login.
     */
    fun logOut(restartStartupCheck: Boolean = false) {
        _uiState.value = 1
        viewModelScope.launch(Dispatchers.IO) {
            sessionCleaner.clearLocalData()
            authRepo.signOutSuspend()
            fName = ""
            lName = ""
            _uiState.postValue(0)
            // checkLogin() sets LiveData values, which must happen on the main thread.
            if (restartStartupCheck) withContext(Dispatchers.Main) { checkLogin() }
        }
    }

    fun resetUiState() {
        _uiState.value = 0
    }

    /**
     * Checks the session and the account. [trustMonitor] false is for a retry the user asked for: the
     * server is tried for real even if the monitor says there is no network.
     */
    fun checkLogin(trustMonitor: Boolean = true) {
        val request = ++startupCheck
        _startupState.value = StartupState.Checking
        viewModelScope.launch(Dispatchers.IO) {
            val authenticated = authRepo.checkLogged
            val profile = if (authenticated) {
                profileOrTimeout(PROFILE_CHECK_TIMEOUT_MS) { firestoreRepo.getUserData(Source.SERVER, trustMonitor) }
            } else {
                null
            }
            // The email is verified outside the app: ask the server instead of trusting the cached user.
            val verified = if (authenticated && profile is Response.Success) {
                verifiedAfterReload(authRepo.isEmailVerified, EMAIL_RELOAD_TIMEOUT_MS) { authRepo.reloadEmailVerified() }
            } else {
                authRepo.isEmailVerified
            }
            val state = startupStateFor(authenticated, verified, profile)
            if (request == startupCheck) {
                val flags = registrationFlagsAfterCheck(
                    authenticated, state, RegistrationFlags(recoveringProfile, profileCreated)
                )
                recoveringProfile = flags.recoveringProfile
                profileCreated = flags.profileCreated
                Timber.d("Startup authentication state: %s", state)
                if ((state == StartupState.SessionRejected || state == StartupState.ServerError) &&
                    profile is Response.Failure
                ) {
                    errorCodeOf(profile.exception).also { code ->
                        startupErrorCode = code
                        report(code, profile.exception)
                    }
                }
                // The server could not be asked: see whether the phone has a complete account to open with.
                continueOffline = if (state == StartupState.RetryableError && authenticated) {
                    canContinueOffline(
                        state,
                        // Reading the cache needs no network: the monitor must not block it.
                        firestoreRepo.getUserData(Source.CACHE, trustMonitor = false),
                        authRepo.isEmailVerified
                    )
                } else {
                    false
                }
                // Before the state, so a screen that reacts to it (the policy sheet) is not overwritten.
                if (emailRecheck) {
                    emailRecheck = false
                    _uiState.postValue(0)
                }
                _startupState.postValue(state)
            }
        }
    }

    @Volatile
    private var emailRecheck = false

    /** Whether the startup error sheet can offer to open the app with the data on this phone; set before the state. */
    @Volatile
    var continueOffline = false
        private set

    /** The user says they verified their email: check again, with the login's spinner meanwhile. */
    fun recheckVerification() {
        emailRecheck = true
        _uiState.value = 1
        checkLogin(trustMonitor = false)
    }

    /** The email of the signed-in account, to tell the user where the verification email went. */
    val signedInEmail: String? get() = authRepo.email


    private fun isValidEmail(target: CharSequence): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    companion object {
        val TAG = AuthViewModel::class.simpleName
    }
}
