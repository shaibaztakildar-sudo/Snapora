package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class MediaStorageManager(private val context: Context) {

    private val mediaDir: File
        get() {
            val dir = File(context.filesDir, "snapora_media")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val snapsDir: File
        get() {
            val dir = File(mediaDir, "snaps")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val storiesDir: File
        get() {
            val dir = File(mediaDir, "stories")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val avatarsDir: File
        get() {
            val dir = File(mediaDir, "avatars")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun saveBitmap(bitmap: Bitmap, type: String = "snap"): String = withContext(Dispatchers.IO) {
        val targetDir = when (type) {
            "story" -> storiesDir
            "avatar" -> avatarsDir
            else -> snapsDir
        }
        val file = File(targetDir, "${type}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        file.absolutePath
    }

    suspend fun copyUriToInternal(uri: Uri, type: String = "snap"): String? = withContext(Dispatchers.IO) {
        try {
            val targetDir = when (type) {
                "story" -> storiesDir
                "avatar" -> avatarsDir
                else -> snapsDir
            }
            val extension = if (context.contentResolver.getType(uri)?.contains("video") == true) "mp4" else "jpg"
            val file = File(targetDir, "${type}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createVideoOutputFile(type: String = "snap"): File {
        val targetDir = if (type == "story") storiesDir else snapsDir
        return File(targetDir, "${type}_video_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.mp4")
    }

    suspend fun deleteFile(path: String?): Boolean = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext false
        try {
            val file = File(path)
            if (file.exists() && file.startsWith(mediaDir)) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun cleanupFiles(paths: List<String>) = withContext(Dispatchers.IO) {
        for (path in paths) {
            deleteFile(path)
        }
    }
}
