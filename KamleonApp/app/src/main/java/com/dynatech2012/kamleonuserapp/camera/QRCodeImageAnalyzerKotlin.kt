package com.dynatech2012.kamleonuserapp.camera

import android.graphics.ImageFormat.*
import android.os.Build
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.*
import java.nio.ByteBuffer

/**
 *
 * <font color="teal">
 * Edu:<br></br>
 * This class analyze image and returns a String result to the listener
</font> */
class QRCodeImageAnalyzerKotlin(private val listener: QRCodeFoundListener) : ImageAnalysis.Analyzer {
    private val yuvFormats = mutableListOf(YUV_420_888)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            yuvFormats.addAll(listOf(YUV_422_888, YUV_444_888))
        }
    }
    override fun analyze(image: ImageProxy) {
        if (image.format in yuvFormats) {

            /*val byteBuffer = image.planes[0].buffer
            val imageData = ByteArray(byteBuffer.capacity())
            byteBuffer[imageData]*/
            val imageData = image.planes[0].buffer.toByteArray()
            val source = PlanarYUVLuminanceSource(
                imageData,
                image.width, image.height,
                0, 0,
                image.width, image.height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            /**
             * Locates and decodes a QR code in an image.
             *
             * @return a String representing the content encoded by the QR code
             * @throws NotFoundException if a QR code cannot be found
             * @throws FormatException if a QR code cannot be decoded
             * @throws ChecksumException if error correction fails
             */
            try {
                //val result = QRCodeMultiReader().decode(binaryBitmap)

                val result = reader.decode(binaryBitmap)
                Log.d(TAG, "QRCodeAnalyzer Analyzed result")
                listener.onQRCodeFound(result.text)
            } catch (e: FormatException) {
                Log.d(TAG, "QRCodeAnalyzer exception: format exception")
                listener.onQRCodeException(e)
            } catch (e: ChecksumException) {
                Log.d(TAG, "QRCodeAnalyzer exception: checksum exception")
                listener.onQRCodeException(e)
            } catch (e: NotFoundException) {
                Log.d(TAG, "QRCodeAnalyzer QR not found")
                listener.onQRCodeNotFound(e)
            }
        }
        else {
            Log.e("QRCodeAnalyzer", "Expected YUV, now = ${image.format}")
            return
        }
        image.close()
    }
    private val reader = MultiFormatReader().apply {
        val map = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE)
        )
        setHints(map)
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    companion object {
        private val TAG = QRCodeImageAnalyzerKotlin::class.java.simpleName
    }
}