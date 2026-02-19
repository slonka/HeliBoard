// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.whisper

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

private const val LOG_TAG = "WhisperContext"

class WhisperContext private constructor(private var ptr: Long) {
    private val scope: CoroutineScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )

    suspend fun transcribe(data: FloatArray, language: String? = null): String =
        withContext(scope.coroutineContext) {
            require(ptr != 0L) { "WhisperContext has been released" }
            val numThreads = WhisperCpuConfig.preferredThreadCount
            Log.d(LOG_TAG, "Transcribing with $numThreads threads, language=$language")
            WhisperLib.fullTranscribe(ptr, numThreads, data, language)
            val textCount = WhisperLib.getTextSegmentCount(ptr)
            return@withContext buildString {
                for (i in 0 until textCount) {
                    append(WhisperLib.getTextSegment(ptr, i))
                }
            }.trim()
        }

    suspend fun release() = withContext(scope.coroutineContext) {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0
        }
    }

    protected fun finalize() {
        runBlocking { release() }
    }

    companion object {
        fun createFromFile(filePath: String): WhisperContext {
            val ptr = WhisperLib.initContext(filePath)
            if (ptr == 0L) {
                throw RuntimeException("Couldn't create WhisperContext from $filePath")
            }
            return WhisperContext(ptr)
        }
    }
}
