package com.dynatech2012.kamleonuserapp.camera

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.Closeable

/**
 * Reads QR codes from the camera frames with ML Kit and hands each code's text to [onCode], on the
 * main thread. It does not judge the codes; whoever receives them decides what to do. One ML Kit
 * client for its whole life: [close] it when done.
 */
class QrCodeAnalyzer(private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer, Closeable {

    private val scanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue }?.let(onCode) }
            // The next frame only arrives once this one is closed (keep-only-latest backpressure).
            .addOnCompleteListener { imageProxy.close() }
    }

    override fun close() = scanner.close()
}
