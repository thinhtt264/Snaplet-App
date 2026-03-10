package com.thinh.snaplet.platform.photo_picker

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface PhotoPickerManager {

    suspend fun processPickedImage(uri: Uri): Uri?
}

@Singleton
class PhotoPickerManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : PhotoPickerManager {

    override suspend fun processPickedImage(uri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@withContext null

                val avatarDir = File(context.cacheDir, AVATAR_CACHE_DIR).apply { mkdirs() }
                avatarDir.listFiles()?.forEach { it.delete() }

                val file = File(avatarDir, "avatar_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { output -> inputStream.copyTo(output) }
                inputStream.close()

                file.toUri()
            } catch (e: Exception) {
                Timber.e(e, "Failed to process picked image")
                null
            }
        }
    }

    private companion object {
        const val AVATAR_CACHE_DIR = "picked_avatars"
    }
}
