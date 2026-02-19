// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.whisper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

private const val LOG_TAG = "WhisperModelDownloader"

object WhisperModelDownloader {
    fun download(
        context: Context,
        url: String,
        fileName: String,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit,
    ) {
        val mainHandler = Handler(Looper.getMainLooper())

        Thread {
            try {
                val modelsDir = WhisperManager.getModelsDir(context)
                val outputFile = File(modelsDir, fileName)
                val tempFile = File(modelsDir, "$fileName.tmp")

                Log.i(LOG_TAG, "Downloading $url to ${outputFile.absolutePath}")

                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                connection.connect()

                val totalBytes = connection.contentLengthLong
                var downloadedBytes = 0L

                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                mainHandler.post { onProgress(progress) }
                            }
                        }
                    }
                }

                tempFile.renameTo(outputFile)
                Log.i(LOG_TAG, "Download complete: ${outputFile.absolutePath}")
                mainHandler.post { onComplete(true) }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Download failed", e)
                mainHandler.post { onComplete(false) }
            }
        }.start()
    }
}
