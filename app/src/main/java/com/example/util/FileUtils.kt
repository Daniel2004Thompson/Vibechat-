package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.Locale

object FileUtils {

    data class FileMetadata(
        val displayName: String,
        val sizeBytes: Long,
        val formattedSize: String,
        val extension: String,
        val localFilePath: String
    )

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[index])
    }

    suspend fun savePickedFileLocally(
        context: Context,
        sourceUri: Uri
    ): FileMetadata = withContext(Dispatchers.IO) {
        var fileName = "attachment_${System.currentTimeMillis()}"
        var fileSize = 0L

        try {
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
        }

        val chatFilesDir = File(context.filesDir, "chat_files").apply { mkdirs() }
        // Sanitize file name for file system safety
        val safeName = fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
        val destFile = File(chatFilesDir, "${System.currentTimeMillis()}_$safeName")

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            if (inputStream != null) {
                FileOutputStream(destFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                if (fileSize <= 0) {
                    fileSize = destFile.length()
                }
            }
        } catch (_: Exception) {
        }

        val extension = fileName.substringAfterLast(".", "").lowercase()

        FileMetadata(
            displayName = fileName,
            sizeBytes = fileSize,
            formattedSize = formatFileSize(fileSize),
            extension = extension,
            localFilePath = destFile.absolutePath
        )
    }

    fun openFile(context: Context, filePathOrUri: String) {
        try {
            val file = File(filePathOrUri)
            val uri = if (file.exists()) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.parse(filePathOrUri)
            }

            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: try sharing intent or general view
            try {
                val file = File(filePathOrUri)
                if (file.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Open file with...").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            } catch (_: Exception) {}
        }
    }
}
