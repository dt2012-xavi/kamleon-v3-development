package com.dynatech2012.kamleonuserapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.scale
import java.io.File

/**
 * Prepares a picked or taken photo for the profile, as the web does: the square from its centre
 * as a [FULL_SIZE] px JPEG for `imageUrl` and a [THUMBNAIL_SIZE] px one for `imageUrlThumbnail`.
 * A smaller photo is scaled up so both always have these sizes.
 */
object ProfilePhoto {
    const val FULL_SIZE = 568
    const val THUMBNAIL_SIZE = 256
    private const val JPEG_QUALITY = 85
    private const val FULL_FILE = "profile_upload.jpg"
    private const val THUMBNAIL_FILE = "profile_upload_thumbnail.jpg"

    /** The two files to upload. */
    data class Prepared(val full: Uri, val thumbnail: Uri)

    /** The size to decode a [width] x [height] photo at so its shorter side is [target] px (never upscaled). */
    fun decodeSize(width: Int, height: Int, target: Int = FULL_SIZE): Pair<Int, Int> {
        val shorter = minOf(width, height)
        if (shorter <= target) return width to height
        val scale = target.toDouble() / shorter
        return maxOf(1, (width * scale).toInt()) to maxOf(1, (height * scale).toInt())
    }

    /** The left, top and side of the centred square of a [width] x [height] image. */
    fun centreSquare(width: Int, height: Int): Triple<Int, Int, Int> {
        val side = minOf(width, height)
        return Triple((width - side) / 2, (height - side) / 2, side)
    }

    /**
     * Writes both versions of [source] to the app's cache, or returns null when the photo cannot
     * be read. Runs off the main thread. The decoder applies the photo's EXIF rotation, so camera
     * photos stay upright.
     */
    fun prepare(context: Context, source: Uri): Prepared? = runCatching {
        val decoder = ImageDecoder.createSource(context.contentResolver, source)
        val decoded = ImageDecoder.decodeBitmap(decoder) { d, info, _ ->
            val (width, height) = decodeSize(info.size.width, info.size.height)
            d.setTargetSize(width, height)
            // A software bitmap can be cropped, scaled and compressed.
            d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        val (left, top, side) = centreSquare(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(decoded, left, top, side, side)
        val full = writeJpeg(context, square, FULL_SIZE, FULL_FILE)
        val thumbnail = writeJpeg(context, square, THUMBNAIL_SIZE, THUMBNAIL_FILE)
        if (square !== decoded) square.recycle()
        decoded.recycle()
        Prepared(full, thumbnail)
    }.getOrNull()

    private fun writeJpeg(context: Context, square: Bitmap, size: Int, name: String): Uri {
        val scaled = square.scale(size, size)
        val file = File(context.cacheDir, name)
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        if (scaled !== square) scaled.recycle()
        return Uri.fromFile(file)
    }
}
