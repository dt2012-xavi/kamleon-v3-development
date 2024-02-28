package com.dynatech2012.kamleonuserapp.viewmodels

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.camera.QRCodeImageAnalyzerMLKitKotlin
import com.dynatech2012.kamleonuserapp.models.Event
import com.dynatech2012.kamleonuserapp.models.QRResponse
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.repositories.RealtimeRepository
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.dynatech2012.kamleonuserapp.repositories.UserRepository
import com.google.gson.Gson
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
    private val analyzer: QRCodeImageAnalyzerMLKitKotlin,
    private val firestoreDataSource: FirestoreDataSource
) : ViewModel() {
    /*fun uploadQRtoRealtime(qrId: String?) {
        realtime.uploadQrId(qrId)
    }*/

    private var _qrUploaded: MutableLiveData<Event<Boolean>> = MutableLiveData()
    val qrUploaded: LiveData<Event<Boolean>> = _qrUploaded
    fun uploadQRtoFirestore(qrResponse: QRResponse?) {
        viewModelScope.launch {
            firestoreDataSource.uploadQrId(qrResponse)
            val currentValue = qrUploaded.value?.peek() ?: false
            _qrUploaded.postValue(Event(!currentValue))
        }
    }

    private var _qrResponse: MutableLiveData<Response<QRResponse>> = MutableLiveData()
    val qrResponse: LiveData<Response<QRResponse>> = _qrResponse
    val imageAnalysis = ImageAnalysis.Builder()
        .setTargetResolution(Size(720, 1280)) //1280, 720
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    private var _qrDebug: MutableLiveData<Response<String>> = MutableLiveData()
    val qrDebug: LiveData<Response<String>> = _qrDebug

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
                // qr example
                // {"unitId": "1000", "sessionId": "1708532818789", "data": {}}
                if (qrString is Response.Success) {
                    Log.d(TAG, "qqqq4: ${qrString.data}")
                    val gson = Gson()
                    val qrResponse = gson.fromJson(qrString.data, QRResponse::class.java)
                    _qrResponse.postValue(Response.Success(qrResponse))
                }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            analyzer.qrDebugFlow.collect { qrDebug ->
                _qrDebug.postValue(qrDebug)
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

    var cameraStarted = false

    companion object {
        private val TAG = QrViewModel::class.java.simpleName
    }
}