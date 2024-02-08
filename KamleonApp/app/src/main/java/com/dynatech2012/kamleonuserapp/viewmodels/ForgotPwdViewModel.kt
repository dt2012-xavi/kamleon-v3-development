package com.dynatech2012.kamleonuserapp.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.camera.QRCodeImageAnalyzerMLKitKotlin
import com.dynatech2012.kamleonuserapp.repositories.CloudFunctions
import com.dynatech2012.kamleonuserapp.repositories.RealtimeRepository
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPwdViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val cloudFunctions: CloudFunctions

) : ViewModel() {

    private val _resetPwdSuccess = MutableLiveData<Boolean>()
    val resetPwdSuccess: LiveData<Boolean> = _resetPwdSuccess
    fun resetPwd(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val response = cloudFunctions.resetPwd(email)
            if (response.isSuccess) {
                _resetPwdSuccess.postValue(true)
            } else {
                _resetPwdSuccess.postValue(false)
            }
        }
    }


    companion object {
        private val TAG = ForgotPwdViewModel::class.java.simpleName
    }
}