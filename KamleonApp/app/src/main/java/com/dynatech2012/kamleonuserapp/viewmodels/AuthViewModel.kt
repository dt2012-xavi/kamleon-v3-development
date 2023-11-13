package com.dynatech2012.kamleonuserapp.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.repositories.AuthRepository
import com.dynatech2012.kamleonuserapp.repositories.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val firestoreRepo: FirestoreRepository
): ViewModel() {

    var isReady = false
    var alreadyLogged = false

    var fName: String? = ""
    var lName: String? = ""
    var email: String? = ""
    var pass: String? = ""
    var birthday: Date = Date()//LocalDate = LocalDate.MIN
    var height = -1f
    var weight = -1f
    var gender = Gender.none

    private val _uiState = MutableLiveData<Int>(0)
    val uiState: LiveData<Int> = _uiState

    fun signup() {
        Log.d(TAG, "Sign up state ${uiState.value}")
        viewModelScope.launch(Dispatchers.Main) {
            if (email != null && pass != null && fName != null && lName != null) {
                val authResult = authRepo.signup(email!!, pass!!)
                if (authResult.isSuccess && authResult.getOrNull() != null)
                    _uiState.postValue(1)
                val registerResult = firestoreRepo.createUserStep1(email!!, fName!!, lName!!)
                if (registerResult.isSuccess && registerResult.getOrNull() != null)
                    _uiState.postValue(2)
            }
        }
    }

    fun finishSignup() {
        viewModelScope.launch(Dispatchers.Main) {
            val registerResult = firestoreRepo.createUserStep2(birthday, height, weight, gender)
            if (registerResult.isSuccess && registerResult.getOrNull() != null)
                _uiState.postValue(3)
        }
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val authResult = authRepo.login(email, pass)
            if (authResult.isSuccess && authResult.getOrNull() != null)
                _uiState.postValue(3)
        }
    }

    fun getUserData(email: String, pass: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val userResult = firestoreRepo.getUserData()
            if (userResult.isSuccess && userResult.getOrNull() != null)
                _uiState.postValue(4)
        }
    }

    fun checkLogin() {
        alreadyLogged = authRepo.checkLogged()
        isReady = true
    }

    companion object {
        val TAG = AuthViewModel::class.simpleName
    }
}