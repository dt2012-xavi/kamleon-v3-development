package com.dynatech2012.kamleonuserapp.viewmodels

import android.os.SystemClock
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dynatech2012.kamleonuserapp.BuildConfig
import com.dynatech2012.kamleonuserapp.camera.QrCodeAnalyzer
import com.dynatech2012.kamleonuserapp.models.Event
import com.dynatech2012.kamleonuserapp.models.QRResponse
import com.dynatech2012.kamleonuserapp.network.ActionResult
import com.dynatech2012.kamleonuserapp.network.actionResultOf
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Links a scan to the user: from the in-app scanner ([imageAnalysis]) or from a /scan app link
 * ([uploadQRtoFirestore]). The scanner reads codes only while [startAnalyzing] is on and stops by
 * itself once it finds a Kamleon scan link, until the screen resumes it.
 */
@HiltViewModel
class QrViewModel @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) : ViewModel() {

    private val _qrUploaded = MutableLiveData<Event<ActionResult>>()
    /** One event per upload: whether Firestore saved the scan and, if not, why. */
    val qrUploaded: LiveData<Event<ActionResult>> = _qrUploaded

    private val _uploading = MutableLiveData(false)
    /** True while a scan read by the scanner is being linked. */
    val uploading: LiveData<Boolean> = _uploading

    private val _foreignCode = MutableLiveData<Event<Unit>>()
    /** The scanner read a code that is not a Kamleon scan link; it keeps scanning. */
    val foreignCode: LiveData<Event<Unit>> = _foreignCode

    // Main thread only: the analyzer reports codes there.
    private var analyzing = false
    private var lastForeignNoticeAt = 0L

    private var analysisExecutor: ExecutorService? = null
    private var analyzer: QrCodeAnalyzer? = null

    /** The camera use case that feeds the scanner; built on first use. */
    val imageAnalysis: ImageAnalysis by lazy {
        val executor = Executors.newSingleThreadExecutor().also { analysisExecutor = it }
        val codeAnalyzer = QrCodeAnalyzer(::onCodeRead).also { analyzer = it }
        ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, codeAnalyzer) }
    }

    fun startAnalyzing() {
        analyzing = _uploading.value != true
    }

    fun stopAnalyzing() {
        analyzing = false
    }

    private fun onCodeRead(code: String) {
        if (!analyzing) return
        val scan = scanLinkFrom(code, BuildConfig.APP_LINK_HOST)
        if (scan == null) {
            // The same code arrives with every frame: say it once every few seconds.
            val now = SystemClock.elapsedRealtime()
            if (now - lastForeignNoticeAt >= FOREIGN_NOTICE_INTERVAL_MS) {
                lastForeignNoticeAt = now
                _foreignCode.value = Event(Unit)
            }
            return
        }
        DebugLog.d(TAG) { "Kamleon scan link read; linking it" }
        analyzing = false
        _uploading.value = true
        upload(scan)
    }

    /** Links a scan that arrived through an app link. */
    fun uploadQRtoFirestore(qrResponse: QRResponse) = upload(qrResponse)

    private fun upload(scan: QRResponse) {
        viewModelScope.launch {
            val result = firestoreDataSource.uploadQrId(scan)
            _uploading.value = false
            _qrUploaded.value = Event(actionResultOf(result))
        }
    }

    override fun onCleared() {
        if (analyzer != null) imageAnalysis.clearAnalyzer()
        analyzer?.close()
        analysisExecutor?.shutdown()
    }

    companion object {
        private val TAG = QrViewModel::class.java.simpleName
        private const val FOREIGN_NOTICE_INTERVAL_MS = 3_000L
    }
}
