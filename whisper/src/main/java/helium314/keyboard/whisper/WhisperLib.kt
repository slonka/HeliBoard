// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.whisper

import android.os.Build
import android.util.Log
import java.io.File

private const val LOG_TAG = "WhisperLib"

class WhisperLib {
    companion object {
        init {
            Log.d(LOG_TAG, "Primary ABI: ${Build.SUPPORTED_ABIS[0]}")
            var loadVfpv4 = false
            var loadV8fp16 = false
            if (isArmEabiV7a()) {
                val cpuInfo = cpuInfo()
                cpuInfo?.let {
                    Log.d(LOG_TAG, "CPU info: $cpuInfo")
                    if (cpuInfo.contains("vfpv4")) {
                        Log.d(LOG_TAG, "CPU supports vfpv4")
                        loadVfpv4 = true
                    }
                }
            } else if (isArmEabiV8a()) {
                val cpuInfo = cpuInfo()
                cpuInfo?.let {
                    Log.d(LOG_TAG, "CPU info: $cpuInfo")
                    if (cpuInfo.contains("fphp")) {
                        Log.d(LOG_TAG, "CPU supports fp16 arithmetic")
                        loadV8fp16 = true
                    }
                }
            }

            if (loadVfpv4) {
                Log.d(LOG_TAG, "Loading libwhisper_vfpv4.so")
                System.loadLibrary("whisper_vfpv4")
            } else if (loadV8fp16) {
                Log.d(LOG_TAG, "Loading libwhisper_v8fp16_va.so")
                System.loadLibrary("whisper_v8fp16_va")
            } else {
                Log.d(LOG_TAG, "Loading libwhisper.so")
                System.loadLibrary("whisper")
            }
        }

        external fun initContext(modelPath: String): Long
        external fun freeContext(contextPtr: Long)
        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray, language: String?)
        external fun getTextSegmentCount(contextPtr: Long): Int
        external fun getTextSegment(contextPtr: Long, index: Int): String
        external fun getSystemInfo(): String
    }
}

private fun isArmEabiV7a(): Boolean = Build.SUPPORTED_ABIS[0] == "armeabi-v7a"

private fun isArmEabiV8a(): Boolean = Build.SUPPORTED_ABIS[0] == "arm64-v8a"

private fun cpuInfo(): String? {
    return try {
        File("/proc/cpuinfo").inputStream().bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        Log.w(LOG_TAG, "Couldn't read /proc/cpuinfo", e)
        null
    }
}
