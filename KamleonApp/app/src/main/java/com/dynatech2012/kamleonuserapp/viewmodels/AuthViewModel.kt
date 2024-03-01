package com.dynatech2012.kamleonuserapp.viewmodels

import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.fragments.LoginFragment
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.repositories.CloudFunctions
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: UserRepository,
    private val firestoreRepo: FirestoreDataSource,
    private val cloudFuctions: CloudFunctions
): ViewModel() {

    private val _isReady = MutableLiveData<Boolean>()
    val isReady: LiveData<Boolean> = _isReady
    var alreadyLogged = false
    var alreadyVerified = false
    var alreadyPolicy = false
    var alreadySplash = false

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

    fun signup() {
        Log.d(TAG, "Sign up state ${uiState.value}")
        viewModelScope.launch(Dispatchers.IO) {
            if (email != null && pass != null && fName != null && lName != null) {
                val authResult = authRepo.signup(email!!, pass!!)
                if (authResult.isSuccess && authResult.dataValue != null)
                    _uiState.postValue(1)
                val registerResult = firestoreRepo.createUserStep1(email!!, fName!!, lName!!)
                if (registerResult.isSuccess && registerResult.dataValue != null)
                    _uiState.postValue(2)
            }
        }
    }

    fun notificationPermissionGranted() {
        _uiState.postValue(3)
    }

    /*
    fun locationPermissionGranted() {
        _uiState.postValue(4)
    }
    */

    fun finishSignup() {
        viewModelScope.launch(Dispatchers.IO) {
            val registerResult = firestoreRepo.createUserStep2(birthday, height, weight, gender)
            Log.d(TAG, "login step finish sign up")
            if (registerResult.isSuccess && registerResult.dataValue != null) {
                sendVerificationEmail()
            }
        }
    }

    fun sendVerificationEmail() {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = authRepo.uuid ?: return@launch
            val username = "$fName $lName"
            if (email == null) {
                Log.e(TAG, "Email or username is null")
                return@launch
            }
            Log.d(TAG, "login step send verif email")
            val verificationResponse = cloudFuctions.sendVerificationEmail(userId, email!!, username)
            if (verificationResponse.isSuccess) {
                Log.d(TAG, "login step send verif email post 5")
                _uiState.postValue(5)
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
        _uiState.value = 1
        if (!isValidEmail(email)) {
            _uiState.postValue(-4)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val authResult = authRepo.login(email, pass)
            if (authResult.isSuccess && authResult.dataValue != null)
                if (!authRepo.isEmailVerified)
                    _uiState.postValue(-5)
                else {
                    val userResponse = firestoreRepo.getUserData()
                    if (userResponse.isSuccess && userResponse.dataValue?.legal?.privacyPolicyApp == true) {
                        Log.d(TAG, "login:success: ${authResult.dataValue?.legal}")
                        _uiState.postValue(7)
                    }
                    else {
                        Log.d(TAG, "login:success: ${authResult.dataValue?.legal}")
                        _uiState.postValue(6)
                    }
                }
            else if (authResult.isFailure) {
                when (val e = authResult.error) {
                    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> {
                        // user invalid
                        Log.e(UserRepository.TAG, "login:failure: $e _ ${e.cause}", e)
                        _uiState.postValue(-3)
                    }
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                        // Pass wrong
                        Log.e(UserRepository.TAG, "login:failure: $e _ ${e.cause}", e)
                        _uiState.postValue(-2)
                    }
                    else -> {
                        Log.e(UserRepository.TAG, "login:failure: $e _ ${e?.cause}", e)
                        _uiState.postValue(-1)
                    }
                }
            }
            else {
                _uiState.postValue(0)
            }
        }
    }

    fun resetUiState() {
        _uiState.value = 0
    }

    fun resetLogged() {
        alreadyLogged = false
        alreadyVerified = false
        alreadyPolicy = false
        _isReady.value = false
    }

    fun checkLogin() {
        alreadyLogged = authRepo.checkLogged
        alreadyVerified = authRepo.isEmailVerified
        viewModelScope.launch(Dispatchers.IO) {
            val userResponse = firestoreRepo.getUserData()
            if (userResponse.isSuccess) {
                val user = userResponse.dataValue
                if (user?.legal?.privacyPolicyApp == true)
                    alreadyPolicy = true
            }
            Log.d(TAG, "checkLogin: $alreadyLogged, $alreadyVerified, $alreadyPolicy")
            _isReady.postValue(true)
        }
    }


    private fun isValidEmail(target: CharSequence): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    companion object {
        val TAG = AuthViewModel::class.simpleName
    }
}