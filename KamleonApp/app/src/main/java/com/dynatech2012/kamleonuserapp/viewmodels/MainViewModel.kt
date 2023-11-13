package com.dynatech2012.kamleonuserapp.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.constants.Constants
import com.dynatech2012.kamleonuserapp.extensions.sha256
import com.dynatech2012.kamleonuserapp.fragments.SettingFragment
import com.dynatech2012.kamleonuserapp.models.CustomUser
import com.dynatech2012.kamleonuserapp.models.Gender
import com.dynatech2012.kamleonuserapp.repositories.AuthRepository
import com.dynatech2012.kamleonuserapp.repositories.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val firestoreRepo: FirestoreRepository
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

    fun getUserData() {
        viewModelScope.launch(Dispatchers.Main) {
            val userResult = firestoreRepo.getUserData()
            if (userResult.isSuccess && userResult.getOrNull() != null)
                _userData.postValue(userResult.getOrNull())
            Log.d(SettingFragment.TAG, "user data changed got user")
        }
    }

    fun updateUserData(data: HashMap<String, Any>) {
        viewModelScope.launch(Dispatchers.Main) {
            val userResult = firestoreRepo.updateUser(data)
            if (userResult.isSuccess && userResult.getOrNull() != null) {
                getUserData()
                _userUpdated.postValue(userResult.getOrNull())
                Log.d(SettingFragment.TAG, "user data changed")
            }
        }
    }

    fun changeHeight(newHeight: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val data = hashMapOf<String, Any>("height" to newHeight.toFloat())
            updateUserData(data)
        }
    }

    fun changeWeight(newWeight: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val data = hashMapOf<String, Any>("weight" to newWeight.toFloat())
            updateUserData(data)
        }
    }

    fun changeGender(newGender: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val data = hashMapOf<String, Any>("gender" to newGender)
            updateUserData(data)
        }
    }

    fun changeBirth(newBirth: Date) {
        viewModelScope.launch(Dispatchers.Main) {
            val data = hashMapOf<String, Any>("dateOfBirth" to newBirth)
            updateUserData(data)
        }
    }

    fun logOut() {
        authRepo.logout()
    }

    fun changePin(oldPin: String, newPin: String) {
        viewModelScope.launch(Dispatchers.Main) {
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
        viewModelScope.launch(Dispatchers.Main) {
            authRepo.changeEmail(currentPwd, newEmail)
            val data = hashMapOf<String, Any>("email" to newEmail)
            firestoreRepo.updateUser(data)
            _userUpdated.postValue(true)
        }
    }

    fun changePwd(oldPwd: String, newPwd: String) {
        viewModelScope.launch(Dispatchers.Main) {
            authRepo.changePwd(oldPwd, newPwd)
            _userUpdated.postValue(true)
        }
    }

    fun deleteUser() {
        viewModelScope.launch(Dispatchers.Main) {
            // TODO: Delete user in auth and in firestore
            _userUpdated.postValue(true)
        }
    }

    fun resetUserUpdated()
    {
        _userUpdated.postValue(false)
    }



    companion object {
        val TAG = MainViewModel::class.simpleName
    }
}