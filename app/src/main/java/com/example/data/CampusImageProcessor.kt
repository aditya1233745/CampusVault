package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

class CampusImageProcessor(
    private val context: Context
) {
    suspend fun encodePhotosAsDataUris(imageUris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        imageUris.take(3).map { uri ->
            encodeSinglePhoto(uri)
                ?: throw IllegalStateException("Could not process one of the selected photos.")
        }
    }

    suspend fun decodeSmallBitmap(uri: Uri, maxDim: Int = 640): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val largest = max(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (largest > 0 && largest / sampleSize > maxDim * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            }
        }.getOrNull()
    }

    suspend fun decodeDataUriBitmap(dataUriOrUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        if (!dataUriOrUrl.startsWith(DATA_URI_PREFIX_MARKER)) return@withContext null
        val commaIndex = dataUriOrUrl.indexOf(',')
        if (commaIndex <= 0) return@withContext null
        runCatching {
            val bytes = Base64.decode(dataUriOrUrl.substring(commaIndex + 1), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private suspend fun encodeSinglePhoto(uri: Uri): String? {
        val decoded = decodeSmallBitmap(uri, maxDim = 640) ?: return null
        return try {
            var current = scaleDownIfNeeded(decoded, maxDim = 640)
            if (current !== decoded) decoded.recycle()

            var quality = 76
            var jpegBytes = compressJpeg(current, quality)

            // Keep each thumbnail compact (~<= 135 KB binary -> ~180 KB base64) so 3 photos fit easily inside Firestore's 1 MB doc limit.
            while (jpegBytes.size > MAX_JPEG_BYTES && quality > 38) {
                quality -= 12
                jpegBytes = compressJpeg(current, quality)
            }
            if (jpegBytes.size > MAX_JPEG_BYTES) {
                val smaller = scaleDownIfNeeded(current, maxDim = 420)
                if (smaller !== current) {
                    current.recycle()
                    current = smaller
                }
                jpegBytes = compressJpeg(current, 48)
            }
            current.recycle()

            val encoded = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        } catch (e: Exception) {
            Log.e(TAG, "Failed encoding image thumbnail", e)
            null
        }
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val largest = max(w, h)
        if (largest <= maxDim || largest <= 0) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    companion object {
        private const val TAG = "CampusImageProcessor"
        private const val MAX_JPEG_BYTES = 135 * 1024
        private const val DATA_URI_PREFIX_MARKER = "data:image"

        fun toCoilModel(source: String): Any {
            if (source.startsWith(DATA_URI_PREFIX_MARKER)) {
                val commaIdx = source.indexOf(',')
                if (commaIdx > 0) {
                    val decoded = runCatching {
                        Base64.decode(source.substring(commaIdx + 1), Base64.DEFAULT)
                    }.getOrNull()
                    if (decoded != null) return decoded
                }
            }
            return source
        }
    }
}
