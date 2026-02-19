// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Theme
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.previewDark
import helium314.keyboard.whisper.WhisperManager
import helium314.keyboard.whisper.WhisperModelDownloader
import helium314.keyboard.latin.utils.prefs
import androidx.core.content.edit

private const val PREF_WHISPER_LANGUAGE = "whisper_language"
private const val PREF_WHISPER_MODEL = "whisper_model"

data class WhisperModelInfo(
    val name: String,
    val fileName: String,
    val description: String,
    val sizeDescription: String,
    val url: String,
)

val AVAILABLE_MODELS = listOf(
    WhisperModelInfo(
        name = "Tiny (Multilingual)",
        fileName = "ggml-tiny.bin",
        description = "Fastest, lowest quality",
        sizeDescription = "75 MB",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.bin"
    ),
    WhisperModelInfo(
        name = "Base (Multilingual)",
        fileName = "ggml-base.bin",
        description = "Fast, decent quality",
        sizeDescription = "142 MB",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
    ),
    WhisperModelInfo(
        name = "Small (Multilingual)",
        fileName = "ggml-small.bin",
        description = "Best quality for PL+EN (recommended)",
        sizeDescription = "466 MB",
        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-small.bin"
    ),
)

val LANGUAGE_OPTIONS = listOf(
    "auto" to "Auto-detect",
    "pl" to "Polski",
    "en" to "English",
)

@Composable
fun WhisperScreen(
    onClickBack: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = context.prefs()
    var selectedLanguage by remember { mutableStateOf(prefs.getString(PREF_WHISPER_LANGUAGE, "auto") ?: "auto") }
    var selectedModel by remember { mutableStateOf(prefs.getString(PREF_WHISPER_MODEL, "") ?: "") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadingModel by remember { mutableStateOf("") }
    val downloadedModels = remember { mutableStateOf(WhisperManager.getDownloadedModels(context).map { it.name }) }
    val modelLoaded = remember { mutableStateOf(WhisperManager.getInstance().isModelLoaded()) }

    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = "Voice Input (Whisper)",
        settings = emptyList(),
    ) {
        Scaffold(contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)) { innerPadding ->
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Model status
                Text(
                    text = "Model Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = if (modelLoaded.value) "Model loaded and ready"
                    else if (selectedModel.isNotEmpty() && selectedModel in downloadedModels.value) "Model downloaded, tap Load to activate"
                    else "No model loaded. Download a model below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (modelLoaded.value) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (selectedModel.isNotEmpty() && selectedModel in downloadedModels.value && !modelLoaded.value) {
                    Button(
                        onClick = {
                            WhisperManager.getInstance().loadModel(context, selectedModel) { success ->
                                modelLoaded.value = success
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Load Model")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language selection
                Text(
                    text = "Dictation Language",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LANGUAGE_OPTIONS.forEach { (code, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedLanguage == code,
                            onClick = {
                                selectedLanguage = code
                                prefs.edit { putString(PREF_WHISPER_LANGUAGE, code) }
                            }
                        )
                        Text(text = label, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Models section
                Text(
                    text = "Available Models",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                AVAILABLE_MODELS.forEach { model ->
                    val isDownloaded = model.fileName in downloadedModels.value
                    val isSelected = model.fileName == selectedModel
                    val isCurrentlyDownloading = isDownloading && downloadingModel == model.fileName

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (isDownloaded) {
                                            selectedModel = model.fileName
                                            prefs.edit { putString(PREF_WHISPER_MODEL, model.fileName) }
                                        }
                                    },
                                    enabled = isDownloaded
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = model.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${model.description} (${model.sizeDescription})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isCurrentlyDownloading) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            Row(modifier = Modifier.padding(top = 8.dp)) {
                                if (!isDownloaded && !isCurrentlyDownloading) {
                                    Button(onClick = {
                                        isDownloading = true
                                        downloadingModel = model.fileName
                                        downloadProgress = 0f
                                        WhisperModelDownloader.download(
                                            context = context,
                                            url = model.url,
                                            fileName = model.fileName,
                                            onProgress = { progress -> downloadProgress = progress },
                                            onComplete = { success ->
                                                isDownloading = false
                                                if (success) {
                                                    downloadedModels.value = WhisperManager.getDownloadedModels(context).map { it.name }
                                                    selectedModel = model.fileName
                                                    prefs.edit { putString(PREF_WHISPER_MODEL, model.fileName) }
                                                }
                                            }
                                        )
                                    }) {
                                        Text("Download")
                                    }
                                } else if (isDownloaded) {
                                    Text(
                                        text = "Downloaded",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(onClick = {
                                        val file = java.io.File(WhisperManager.getModelsDir(context), model.fileName)
                                        if (file.exists()) file.delete()
                                        downloadedModels.value = WhisperManager.getDownloadedModels(context).map { it.name }
                                        if (selectedModel == model.fileName) {
                                            selectedModel = ""
                                            prefs.edit { putString(PREF_WHISPER_MODEL, "") }
                                        }
                                    }) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Preview
@Composable
private fun PreviewScreen() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            WhisperScreen({})
        }
    }
}
