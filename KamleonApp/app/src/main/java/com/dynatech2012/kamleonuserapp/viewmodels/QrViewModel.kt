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
import com.dynatech2012.kamleonuserapp.repositories.RealtimeRepository
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val userRepository: UserRepository,
    private val realtime: RealtimeRepository,
    private val analyzer: QRCodeImageAnalyzerMLKitKotlin
) : ViewModel() {
    fun uploadQRtoRealtime(qrId: String?) {
        realtime.uploadQrId(qrId)
    }

    private var _qrString: MutableLiveData<Response<String>> = MutableLiveData()
    val qrString: LiveData<Response<String>> = _qrString
    val imageAnalysis = ImageAnalysis.Builder()
        .setTargetResolution(Size(720, 1280)) //1280, 720
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    fun startScanner() {
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(appContext),
            analyzer
        )
        Log.d(TAG, "qqqq1")
        val ioDispatcher = Dispatchers.IO
        viewModelScope.launch(ioDispatcher) {
            analyzer.qrStingFlow.collect { qrString ->
                Log.d(TAG, "qqqq3 status: $qrString")
                if (qrString is Response.Success && qrString.data != null) {
                    Log.d(TAG, "qqqq4: ${qrString.data}")
                }
                _qrString.postValue(qrString)
            }
        }
    }

    fun stopAnalyzing() {
        Log.d(TAG, "qqqq5 qrScanner: resetting qrString to null")
        analyzer.stopAnalyzing()
        //_qrString.value = null
    }

    fun startAnalyzing() {
        Log.d(TAG, "qqqq5 qrScanner: resetting qrString to null")
        analyzer.startAnalyzing()
        //_qrString.value = null
    }

    companion object {
        private val TAG = QrViewModel::class.java.simpleName
    }
}