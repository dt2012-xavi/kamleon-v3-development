package com.dynatech2012.kamleonuserapp.camera

import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.dynatech2012.kamleonuserapp.repositories.Response
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.NullPointerException

/**
 *
 * <font color="teal">
 * Edu:<br></br>
 * This class analyze image and returns a String result to the listener
</font> */
class QRCodeImageAnalyzerMLKitKotlin :
    ImageAnalysis.Analyzer {
    private val mlKit: QRCodeProcessorMLKitKotlin = QRCodeProcessorMLKitKotlin()
    private val formats = arrayListOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)
    private val _qrStingFlow: MutableStateFlow<Response<String>> = MutableStateFlow(Response.Inactive())
    val qrStingFlow: StateFlow<Response<String>> = _qrStingFlow

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        if (imageProxy.format in formats) {

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val rotation = imageProxy.imageInfo.rotationDegrees
                val image = InputImage.fromMediaImage(mediaImage, rotation)
                // Pass image to an ML Kit Vision API
                mlKit.scanBarcodesByImage(image, imageProxy, mediaImage)
            } else {
                Log.d(TAG, "qrScanner ML: fail: mediaImage == null")
                val e: Exception = NullPointerException("mediaImage = null")
                _qrStingFlow.value = Response.Failure(e)
                //listener.onQRCodeException(e)
            }
        } else {
            Log.e("QRCodeAnalyzer", "qrScanner ML: fail: Expected YUV, now = ${imageProxy.format}")
            val e: Exception = IllegalStateException("Expected YUV format = ${imageProxy.format}")
            _qrStingFlow.value = Response.Failure(e)
            //listener.onQRCodeException(e)
        }
    }

    fun stopAnalyzing() {
        _qrStingFlow.value = Response.Inactive()
    }

    fun startAnalyzing() {
        _qrStingFlow.value = Response.Loading()
    }

    companion object {
        private val TAG = QRCodeImageAnalyzerMLKitKotlin::class.java.simpleName
    }


    inner class QRCodeProcessorMLKitKotlin {
        fun scanBarcodesByImage(image: InputImage?, pImage: ImageProxy, mImage: Image) {
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_QR_CODE /*,
                                Barcode.FORMAT_AZTEC*/
                )
                .build()

            // BarcodeScanner scanner = BarcodeScanning.getClient();
            // Or, to specify the formats to recognize:
            val scanner = BarcodeScanning.getClient(options)

            /*Task<List<Barcode>> result = */scanner.process(image!!)
                .addOnSuccessListener { barcodes: List<Barcode> ->
                    if(qrStingFlow.value is Response.Loading) {
                        // Task completed successfully
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            Log.d(TAG, "qrScanner ML: barcode: $rawValue")
                            val valueType = barcode.valueType
                            // See API reference for complete list of supported types
                            if (rawValue != null && valueType == Barcode.TYPE_TEXT) {
                                _qrStingFlow.value = Response.Success(rawValue)
                                //listener.onQRCodeFound(rawValue)
                            } else {
                                val e: Exception =
                                    IllegalStateException("Unexpected value: $valueType")
                                Log.d(TAG, "qrScanner ML: failure with exception: $e")
                                _qrStingFlow.value = Response.Failure(e)
                                //listener.onQRCodeException(e)
                            }
                        }
                    }
                }
                .addOnFailureListener { e: Exception ->
                    // Task failed with an exception
                    Log.d(TAG, "qrScanner ML: failure with exception: $e")
                    _qrStingFlow.value = Response.Failure(e)
                    //listener.onQRCodeException(e)
                }
                .addOnCompleteListener {
                    // Always close ImageMedia and ImageProxy after task. Otherwise -> Error
                    mImage.close()
                    pImage.close()
                }
        }
    }
}

