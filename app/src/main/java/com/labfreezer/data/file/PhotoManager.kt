package com.labfreezer.data.file

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val photosDir: File get() = File(context.filesDir, "photos").also { it.mkdirs() }

    fun getPhotoFileName(boxId: Long, row: Int, col: Int): String {
        return "${boxId}_${row}_${col}.jpg"
    }

    fun createPhotoUri(boxId: Long, row: Int, col: Int): Uri {
        val file = File(photosDir, getPhotoFileName(boxId, row, col))
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun compressAndSave(sourceUri: Uri, boxId: Long, row: Int, col: Int): String? {
        return try {
            val orientation = context.contentResolver
                .openInputStream(sourceUri)
                ?.use { ExifInterface(it) }
                ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                ?: ExifInterface.ORIENTATION_NORMAL

            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap == null) return null

            val rotatedBitmap = rotateBitmap(bitmap, orientation)
            if (rotatedBitmap !== bitmap) bitmap.recycle()

            val maxSize = 1080
            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height, 1f)
            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(rotatedBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                rotatedBitmap
            }

            val file = File(photosDir, getPhotoFileName(boxId, row, col))
            FileOutputStream(file).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            if (scaledBitmap !== rotatedBitmap) scaledBitmap.recycle()
            rotatedBitmap.recycle()

            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun deletePhoto(photoPath: String?) {
        if (photoPath.isNullOrBlank()) return
        try {
            val uri = Uri.parse(photoPath)
            if (uri.scheme == "file") {
                File(uri.path!!).delete()
            } else {
                context.contentResolver.delete(uri, null, null)
            }
        } catch (_: Exception) {}
    }

    fun deletePhotoByPath(photoPath: String?) {
        if (photoPath.isNullOrBlank()) return
        try {
            val file = File(photoPath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }
}
