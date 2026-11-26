package com.dynatech2012.kamleonuserapp.viewmodels

import com.dynatech2012.kamleonuserapp.analytics.Analytics
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
import com.dynatech2012.kamleonuserapp.network.actionResultOf
import com.dynatech2012.kamleonuserapp.repositories.FirestoreDataSource
import com.dynatech2012.kamleonuserapp.utils.DebugLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Links a scan to the user, from the in-app scanner ([imageAnalysis]) or from a /scan app link
 * ([linkFromAppLink]); both report through [linkState], which [com.dynatech2012.kamleonuserapp.views.ScanLinkSheet]
 * shows. The scanner reads codes only while [startAnalyzing] is on and stops by itself once it finds a
 * Kamleon scan link, until the screen resumes it. Shared by the activity and its screens.
 */
@HiltViewModel
class QrViewModel @Inject constructor(
    private val firestoreDataSource: FirestoreDataSource
) : ViewModel() {

    private val _scanRead = MutableLiveData<Event<Unit>>()
    /** The scanner read a Kamleon scan link and started linking it. */
    val scanRead: LiveData<Event<Unit>> = _scanRead

    private val _foreignCode = MutableLiveData<Event<Unit>>()
    /** The scanner read a code that is not a Kamleon scan link; it keeps scanning. */
    val foreignCode: LiveData<Event<Unit>> = _foreignCode

    // Main thread only: the analyzer reports codes there.
    private val gate = ScanGate(BuildConfig.APP_LINK_HOST)

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
        gate.open = _linkState.value != ScanLinkState.Linking
        if (gate.open) Analytics.qr?.scannerReady()
    }

    fun stopAnalyzing() {
        gate.open = false
        Analytics.qr?.scannerLeft()
    }

    private fun onCodeRead(code: String) {
        when (val decision = gate.onCode(code, SystemClock.elapsedRealtime())) {
            ScanGate.Decision.Ignore -> Unit
            ScanGate.Decision.Foreign -> _foreignCode.value = Event(Unit)
            is ScanGate.Decision.Link -> {
                DebugLog.d(TAG) { "Kamleon scan link read; linking it" }
                _scanRead.value = Event(Unit)
                Analytics.qr?.scannerCodeRead()
                link(decision.scan)
            }
        }
    }

    private val _linkState = MutableLiveData<ScanLinkState?>()
    /** Where the scan being linked stands, for [com.dynatech2012.kamleonuserapp.views.ScanLinkSheet]. */
    val linkState: LiveData<ScanLinkState?> = _linkState

    /** Links a scan that arrived through an app link (the phone's own camera read the QR). */
    fun linkFromAppLink(scan: QRResponse) {
        Analytics.qr?.cameraLinkDelivered()
        link(scan)
    }

    private fun link(scan: QRResponse) {
        if (_linkState.value == ScanLinkState.Linking) return
        _linkState.value = ScanLinkState.Linking
        viewModelScope.launch {
            val result = actionResultOf(firestoreDataSource.uploadQrId(scan))
            Analytics.qr?.linked(result.success)
            _linkState.value = if (result.success) ScanLinkState.Linked else ScanLinkState.Failed(result.failure)
        }
    }

    /** An app link arrived that cannot be used: incomplete, or it expired while the user logged in. */
    fun appLinkInvalid() {
        Analytics.qr?.cameraLinkInvalid()
        _linkState.value = ScanLinkState.Invalid
    }

    /** The user has seen how the scan went. */
    fun clearLinkState() {
        if (_linkState.value != ScanLinkState.Linking) _linkState.value = null
    }

    override fun onCleared() {
        if (analyzer != null) imageAnalysis.clearAnalyzer()
        analyzer?.close()
        analysisExecutor?.shutdown()
    }

    companion object {
        private val TAG = QrViewModel::class.java.simpleName
    }
}

/** Where a scan that arrived through an app link stands. */
sealed interface ScanLinkState {
    data object Linking : ScanLinkState
    data object Linked : ScanLinkState
    data class Failed(val failure: Exception?) : ScanLinkState
    /** The link is incomplete or expired: there is nothing to link. */
    data object Invalid : ScanLinkState
}
