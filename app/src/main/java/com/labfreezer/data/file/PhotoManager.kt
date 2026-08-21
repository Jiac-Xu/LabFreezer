package com.labfreezer.data.file

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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

            val maxSize = 1080

            // 1. 读取原图尺寸
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOptions)
            }
            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

            // 2. 计算 inSampleSize 降采样解码
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(boundsOptions, maxSize, maxSize)
            }
            val decodedBitmap = context.contentResolver.openInputStream(sourceUri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            } ?: return null

            // 3. 旋转校正
            val rotatedBitmap = rotateBitmap(decodedBitmap, orientation)
            if (rotatedBitmap !== decodedBitmap) {
                decodedBitmap.recycle()
            }

            // 4. 精确缩放到 maxSize 范围
            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height, 1f)
            val scaledBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(rotatedBitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                rotatedBitmap
            }
            if (scaledBitmap !== rotatedBitmap) {
                rotatedBitmap.recycle()
            }

            val file = File(photosDir, getPhotoFileName(boxId, row, col))
            FileOutputStream(file).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            scaledBitmap.recycle()

            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
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

    fun rotatePhoto(photoPath: String): String? {
        return try {
            val uri = Uri.parse(photoPath)
            val file = if (uri.scheme == "file") File(uri.path!!) else return null
            if (!file.exists()) return null

            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val matrix = Matrix().apply { postRotate(90f) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotatedBitmap !== bitmap) bitmap.recycle()

            FileOutputStream(file).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            rotatedBitmap.recycle()

            photoPath
        } catch (e: Exception) {
            null
        }
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

    /**
     * 将样本照片复制到系统相册（Pictures/LabFreezer）。
     * Android 10+ 走 MediaStore（无需权限）；9 及以下直接写公共目录，需要 WRITE_EXTERNAL_STORAGE。
     *
     * @param displayName 相册中的显示名；为空时沿用原文件名，自动补 .jpg 后缀
     * @return 是否保存成功
     */
    fun savePhotoToGallery(photoPath: String, displayName: String? = null): Boolean {
        return try {
            val uri = Uri.parse(photoPath)
            if (uri.scheme != "file") return false
            val file = File(uri.path!!)
            if (!file.exists()) return false

            val base = if (displayName.isNullOrBlank()) file.nameWithoutExtension else displayName
            val name = if (base.endsWith(".jpg", ignoreCase = true)) base else "$base.jpg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LabFreezer")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = context.contentResolver.insert(collection, values) ?: return false
                val output = context.contentResolver.openOutputStream(itemUri)
                    ?: run {
                        context.contentResolver.delete(itemUri, null, null)
                        return false
                    }
                output.use { out -> file.inputStream().use { it.copyTo(out) } }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(itemUri, values, null, null)
                true
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "LabFreezer"
                ).apply { mkdirs() }
                val dest = File(dir, name)
                FileOutputStream(dest).use { out -> file.inputStream().use { it.copyTo(out) } }
                MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("image/jpeg"), null)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
