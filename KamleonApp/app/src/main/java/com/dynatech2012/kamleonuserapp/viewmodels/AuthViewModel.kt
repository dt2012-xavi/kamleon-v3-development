package com.dynatech2012.kamleonuserapp.viewmodels

import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.models.Gender
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
    private val firestoreRepo: FirestoreDataSource
): ViewModel() {

    var isReady = false
    var alreadyLogged = false
    var alreadySplash = false

    var fName: String? = ""
    var lName: String? = ""
    var email: String? = ""
    var pass: String? = ""
    var birthday: Date = Date()//LocalDate = LocalDate.MIN
    var height = -1f
    var weight = -1f
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
    fun locationPermissionGranted() {
        _uiState.postValue(4)
    }

    fun finishSignup() {
        viewModelScope.launch(Dispatchers.IO) {
            val registerResult = firestoreRepo.createUserStep2(birthday, height, weight, gender)
            if (registerResult.isSuccess && registerResult.dataValue != null)
                _uiState.postValue(5)
        }
    }

    fun login(email: String, pass: String) {
        _uiState.value = 1
        if (!isValidEmail(email)) {
            _uiState.postValue(-2)
            return
        }
        if (pass.count() < 6) {
            _uiState.postValue(-3)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val authResult = authRepo.login(email, pass)
            if (authResult.isSuccess && authResult.dataValue != null)
                _uiState.postValue(5)
            else if (authResult.isFailure)
                _uiState.postValue(-1)
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
        isReady = false
    }

    fun checkLogin() {
        alreadyLogged = authRepo.checkLogged
        isReady = true
    }

    private fun isValidEmail(target: CharSequence): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    companion object {
        val TAG = AuthViewModel::class.simpleName
    }
}