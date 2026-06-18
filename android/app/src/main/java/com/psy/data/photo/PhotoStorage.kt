package com.psy.data.photo

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val photosDir: File
        get() = File(context.filesDir, "photos").also { it.mkdirs() }

    /**
     * Copy the content-URI stream into internal storage under photos/<name>.jpg.
     * Returns the absolute path of the saved file.
     * The caller supplies [name] (e.g. "img_<timestamp>") so PhotoStorage doesn't
     * depend on time/random itself.
     *
     * @throws IllegalStateException if the stream cannot be opened or the copy fails.
     */
    suspend fun savePicked(uri: Uri, name: String): String = withContext(Dispatchers.IO) {
        val dest = File(photosDir, "$name.jpg")
        try {
            context.contentResolver.openInputStream(uri)
                ?.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
                ?: throw IllegalStateException("Cannot open input stream for URI: $uri")
        } catch (e: Exception) {
            // Clean up partial file on failure
            dest.delete()
            throw IllegalStateException("Failed to copy photo from $uri: ${e.message}", e)
        }
        dest.absolutePath
    }

    /**
     * Delete the file at [path] if it exists. Errors are silently ignored.
     */
    fun delete(path: String) {
        try {
            File(path).takeIf { it.exists() }?.delete()
        } catch (_: Exception) {
            // Ignore errors on delete
        }
    }
}
