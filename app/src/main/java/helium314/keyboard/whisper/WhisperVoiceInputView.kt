// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.whisper

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import helium314.keyboard.latin.R

class WhisperVoiceInputView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val statusText: TextView
    private val timerText: TextView
    private val stopButton: ImageButton
    private val progressBar: ProgressBar
    private var pulseAnimator: ValueAnimator? = null

    var onStopClicked: Runnable? = null
    var onCancelClicked: Runnable? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(16, 4, 16, 4)

        stopButton = ImageButton(context).apply {
            setImageResource(R.drawable.sym_keyboard_voice_holo)
            setBackgroundResource(android.R.drawable.btn_default)
            contentDescription = "Stop recording"
            setOnClickListener { onStopClicked?.run() }
        }
        addView(stopButton, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        statusText = TextView(context).apply {
            text = "Recording..."
            textSize = 14f
            setPadding(16, 0, 8, 0)
        }
        addView(statusText, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))

        timerText = TextView(context).apply {
            text = "0:00"
            textSize = 14f
        }
        addView(timerText, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))

        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleSmall).apply {
            visibility = GONE
        }
        addView(progressBar, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = 8
        })
    }

    fun showRecording() {
        statusText.text = "Recording..."
        stopButton.visibility = VISIBLE
        timerText.visibility = VISIBLE
        progressBar.visibility = GONE
        startPulseAnimation()
    }

    fun showTranscribing() {
        stopPulseAnimation()
        statusText.text = "Transcribing..."
        stopButton.visibility = GONE
        timerText.visibility = GONE
        progressBar.visibility = VISIBLE
    }

    fun updateTimer(seconds: Int) {
        timerText.text = String.format("%d:%02d", seconds / 60, seconds % 60)
    }

    private fun startPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(1.0f, 0.3f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                stopButton.alpha = animation.animatedValue as Float
            }
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        stopButton.alpha = 1.0f
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPulseAnimation()
    }
}
