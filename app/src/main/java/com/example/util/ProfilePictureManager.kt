package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ProfilePictureManager {

    /**
     * Copies and optimizes a picked image Uri (e.g. from PhotoPicker content://)
     * to a permanent private app storage file in context.filesDir.
     * Returns the persistent file URI string ("file:///...").
     */
    suspend fun savePickedImageLocally(
        context: Context,
        sourceUri: Uri,
        userId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val avatarsDir = File(context.filesDir, "avatars").apply { mkdirs() }
            val cleanUid = userId.replace("[^a-zA-Z0-9_]".toRegex(), "_")
            val destFile = File(avatarsDir, "avatar_${cleanUid}.jpg")

            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap != null) {
                // Check EXIF orientation
                val rotatedBitmap = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        val exifStream = context.contentResolver.openInputStream(sourceUri)
                        val exif = exifStream?.let { ExifInterface(it) }
                        val orientation = exif?.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        ) ?: ExifInterface.ORIENTATION_NORMAL
                        exifStream?.close()
                        rotateBitmapIfNeeded(originalBitmap, orientation)
                    } else {
                        originalBitmap
                    }
                } catch (_: Exception) {
                    originalBitmap
                }

                // Scale down to max 800x800 for optimal memory and storage
                val scaledBitmap = scaleBitmapToMax(rotatedBitmap, 800)

                FileOutputStream(destFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                }
                Uri.fromFile(destFile).toString()
            } else {
                sourceUri.toString()
            }
        } catch (e: Exception) {
            sourceUri.toString()
        }
    }

    /**
     * Converts a local image (file:// or content://) into a compact Base64 data URI
     * (~10-18KB) suitable for storing in Supabase `profiles.avatar_url` TEXT column,
     * allowing all other contacts to display the user's avatar.
     */
    suspend fun createBase64Thumbnail(
        context: Context,
        imageUriString: String?,
        maxDim: Int = 160
    ): String? = withContext(Dispatchers.IO) {
        if (imageUriString.isNullOrBlank()) return@withContext null
        // If it's already an http/https url (e.g. from Google Account), return as is
        if (imageUriString.startsWith("http://") || imageUriString.startsWith("https://") || imageUriString.startsWith("data:image")) {
            return@withContext imageUriString
        }

        try {
            val uri = Uri.parse(imageUriString)
            val inputStream: InputStream? = if (uri.scheme == "file") {
                File(uri.path ?: "").inputStream()
            } else {
                context.contentResolver.openInputStream(uri)
            }
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close() ?: return@withContext null

            val thumbBitmap = scaleBitmapToMax(originalBitmap, maxDim)
            val baos = ByteArrayOutputStream()
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 78, baos)
            val bytes = baos.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64"
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmapToMax(source: Bitmap, maxDim: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source

        val scale = maxDim.toFloat() / max(width, height).toFloat()
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
