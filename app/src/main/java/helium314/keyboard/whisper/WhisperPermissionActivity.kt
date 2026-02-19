// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.whisper

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import helium314.keyboard.settings.SettingsActivity

class WhisperPermissionActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, "Microphone permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission required for voice input", Toast.LENGTH_LONG).show()
            }
            if (intent.getBooleanExtra("show_setup", false)) {
                openWhisperSettings()
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (intent.getBooleanExtra("show_setup", false)) {
            openWhisperSettings()
            finish()
        } else {
            finish()
        }
    }

    private fun openWhisperSettings() {
        val settingsIntent = Intent(this, SettingsActivity::class.java).apply {
            putExtra("navigate_to", "whisper_voice_input")
        }
        startActivity(settingsIntent)
    }
}
