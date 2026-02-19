// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.whisper

import android.content.Context
import android.util.Log
import helium314.keyboard.whisper.WhisperContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

private const val LOG_TAG = "WhisperManager"
private const val MODELS_DIR = "whisper-models"

class WhisperManager private constructor() {
    private var whisperContext: WhisperContext? = null
    private var currentModelPath: String? = null
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)

    @Volatile
    var isLoading = false
        private set

    fun isModelLoaded(): Boolean = whisperContext != null

    fun loadModel(context: Context, modelFileName: String, onComplete: ((Boolean) -> Unit)? = null) {
        scope.launch {
            mutex.withLock {
                try {
                    isLoading = true
                    val modelFile = File(getModelsDir(context), modelFileName)
                    if (!modelFile.exists()) {
                        Log.w(LOG_TAG, "Model file not found: ${modelFile.absolutePath}")
                        onComplete?.invoke(false)
                        return@launch
                    }

                    // Release existing context
                    whisperContext?.release()
                    whisperContext = null

                    Log.i(LOG_TAG, "Loading model: ${modelFile.absolutePath}")
                    whisperContext = WhisperContext.createFromFile(modelFile.absolutePath)
                    currentModelPath = modelFile.absolutePath
                    Log.i(LOG_TAG, "Model loaded successfully")
                    onComplete?.invoke(true)
                } catch (e: Exception) {
                    Log.e(LOG_TAG, "Failed to load model", e)
                    onComplete?.invoke(false)
                } finally {
                    isLoading = false
                }
            }
        }
    }

    suspend fun transcribe(audioData: FloatArray, language: String? = null): String {
        val ctx = whisperContext ?: throw IllegalStateException("Model not loaded")
        return ctx.transcribe(audioData, language)
    }

    fun transcribeAsync(audioData: FloatArray, language: String? = null, onResult: (String?) -> Unit) {
        scope.launch {
            try {
                val result = transcribe(audioData, language)
                onResult(result)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Transcription failed", e)
                onResult(null)
            }
        }
    }

    fun release() {
        scope.launch {
            mutex.withLock {
                whisperContext?.release()
                whisperContext = null
                currentModelPath = null
            }
        }
    }

    companion object {
        @Volatile
        private var instance: WhisperManager? = null

        @JvmStatic
        fun getInstance(): WhisperManager {
            return instance ?: synchronized(this) {
                instance ?: WhisperManager().also { instance = it }
            }
        }

        @JvmStatic
        fun getModelsDir(context: Context): File {
            val dir = File(context.getExternalFilesDir(null), MODELS_DIR)
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

        @JvmStatic
        fun getDownloadedModels(context: Context): List<File> {
            val dir = getModelsDir(context)
            return dir.listFiles()?.filter { it.name.endsWith(".bin") }?.toList() ?: emptyList()
        }
    }
}
