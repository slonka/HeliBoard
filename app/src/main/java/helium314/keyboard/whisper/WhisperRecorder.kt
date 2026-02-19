// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.whisper

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

private const val LOG_TAG = "WhisperRecorder"
private const val SAMPLE_RATE = 16000
private const val MAX_RECORDING_SECONDS = 30
private const val MAX_SAMPLES = SAMPLE_RATE * MAX_RECORDING_SECONDS

class WhisperRecorder {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var buffer = ShortArray(MAX_SAMPLES)
    private var samplesRecorded = 0

    @Volatile
    var isRecording = false
        private set

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "AudioRecord failed to initialize")
            audioRecord?.release()
            audioRecord = null
            return
        }

        buffer = ShortArray(MAX_SAMPLES)
        samplesRecorded = 0
        isRecording = true
        audioRecord?.startRecording()

        recordingThread = Thread {
            val readBuffer = ShortArray(bufferSize / 2)
            while (isRecording && samplesRecorded < MAX_SAMPLES) {
                val read = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: -1
                if (read > 0) {
                    val toCopy = minOf(read, MAX_SAMPLES - samplesRecorded)
                    System.arraycopy(readBuffer, 0, buffer, samplesRecorded, toCopy)
                    samplesRecorded += toCopy
                }
            }
            if (samplesRecorded >= MAX_SAMPLES) {
                Log.i(LOG_TAG, "Max recording time reached (${MAX_RECORDING_SECONDS}s)")
                isRecording = false
            }
        }
        recordingThread?.start()
        Log.i(LOG_TAG, "Recording started")
    }

    fun stopRecording(): FloatArray {
        isRecording = false
        recordingThread?.join(1000)
        recordingThread = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        Log.i(LOG_TAG, "Recording stopped, $samplesRecorded samples recorded")
        return shortToFloat(buffer, samplesRecorded)
    }

    fun cancel() {
        isRecording = false
        recordingThread?.join(1000)
        recordingThread = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        samplesRecorded = 0
    }

    private fun shortToFloat(shortArray: ShortArray, length: Int): FloatArray {
        val floatArray = FloatArray(length)
        for (i in 0 until length) {
            floatArray[i] = shortArray[i] / 32768.0f
        }
        return floatArray
    }
}
