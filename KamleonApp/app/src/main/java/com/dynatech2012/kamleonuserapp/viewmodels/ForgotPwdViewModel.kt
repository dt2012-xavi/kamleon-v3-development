package com.dynatech2012.kamleonuserapp.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.network.ActionResult
import com.dynatech2012.kamleonuserapp.network.actionResultOf
import com.dynatech2012.kamleonuserapp.repositories.CloudFunctions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class ForgotPwdViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val cloudFunctions: CloudFunctions

) : ViewModel() {

    private val _resetPwdSuccess = MutableLiveData<ActionResult>()
    val resetPwdSuccess: LiveData<ActionResult> = _resetPwdSuccess
    fun resetPwd(email: String) {
        Timber.d("Password reset requested")
        viewModelScope.launch(Dispatchers.IO) {
            val response = cloudFunctions.resetPwd(email)
            Timber.d("Password reset request completed: success=%s", response.isSuccess)
            _resetPwdSuccess.postValue(actionResultOf(response))
        }
    }


    companion object {
        private val TAG = ForgotPwdViewModel::class.java.simpleName
    }
}