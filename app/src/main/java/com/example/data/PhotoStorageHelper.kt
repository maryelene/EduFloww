package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PhotoStorageHelper(private val context: Context) {

    companion object {
        private const val TAG = "PhotoStorageHelper"
        private const val PHOTOS_DIR = "note_photos"
        private const val CAMERA_CACHE_DIR = "camera_cache"
        private const val MAX_AI_DIMENSION = 1600
    }

    fun createTempCameraUri(): Uri {
        val cacheDir = File(context.cacheDir, CAMERA_CACHE_DIR).apply { mkdirs() }
        val tempFile = File(cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    suspend fun savePhotoToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val photosDir = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
            val destFile = File(photosDir, "note_${System.currentTimeMillis()}.jpg")
            val bitmap = decodeScaledBitmapFromUri(uri, 1920) ?: return@withContext null
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            bitmap.recycle()
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save photo to internal storage", e)
            null
        }
    }

    suspend fun loadBitmapForAi(photoPath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(photoPath)
            if (!file.exists()) return@withContext null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            val sampleSize = calculateInSampleSize(options, MAX_AI_DIMENSION, MAX_AI_DIMENSION)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return@withContext null
            scaleDownIfNeeded(raw, MAX_AI_DIMENSION)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap for AI", e)
            null
        }
    }

    fun deletePhotoFile(photoPath: String?) {
        if (photoPath.isNullOrBlank()) return
        try {
            val file = File(photoPath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not delete photo file: $photoPath", e)
        }
    }

    private fun decodeScaledBitmapFromUri(uri: Uri, maxDim: Int): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

        val sampleSize = calculateInSampleSize(boundsOpts, maxDim, maxDim)
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return null

        return scaleDownIfNeeded(decoded, maxDim)
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val matrix = Matrix().apply { postScale(ratio, ratio) }
        val scaled = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        if (scaled != bitmap) {
            bitmap.recycle()
        }
        return scaled
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
