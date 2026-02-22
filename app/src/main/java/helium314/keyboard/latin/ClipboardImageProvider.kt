// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.latin

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import helium314.keyboard.latin.common.FileUtils
import helium314.keyboard.latin.utils.Log
import java.io.File

/** Manages clipboard image files in internal storage. */
object ClipboardImageProvider {
    private const val TAG = "ClipboardImageProvider"
    private const val DIR_NAME = "clipboard_images"

    private fun getDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Saves a clipboard image URI to internal storage and returns a FileProvider URI for it. */
    fun saveImage(context: Context, sourceUri: Uri, mimeType: String): String? {
        return try {
            val extension = extensionForMimeType(mimeType)
            val file = File(getDir(context), "${System.currentTimeMillis()}$extension")
            FileUtils.copyContentUriToNewFile(sourceUri, context, file)
            val providerUri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            providerUri.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save clipboard image", e)
            null
        }
    }

    /** Deletes the image file associated with the given FileProvider URI string. */
    fun deleteImage(context: Context, imageUriString: String) {
        try {
            val file = fileForUri(context, imageUriString) ?: return
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete clipboard image", e)
        }
    }

    /** Removes image files that are not referenced by any of the given URI strings. */
    fun cleanupOrphaned(context: Context, activeUris: Set<String>) {
        val dir = getDir(context)
        val activeFiles = activeUris.mapNotNull { fileForUri(context, it)?.name }.toSet()
        dir.listFiles()?.forEach { file ->
            if (file.name !in activeFiles) {
                file.delete()
            }
        }
    }

    private fun fileForUri(context: Context, uriString: String): File? {
        val uri = Uri.parse(uriString)
        // FileProvider URIs have format: content://authority/name/filename
        val path = uri.lastPathSegment ?: return null
        return File(getDir(context), path)
    }

    private fun extensionForMimeType(mimeType: String): String = when {
        mimeType.contains("png") -> ".png"
        mimeType.contains("gif") -> ".gif"
        mimeType.contains("webp") -> ".webp"
        mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
        else -> ".img"
    }
}
