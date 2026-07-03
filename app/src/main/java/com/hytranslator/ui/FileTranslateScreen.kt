package com.hytranslator.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hytranslator.file.DocumentExtractor
import com.hytranslator.translator.Language
import kotlinx.coroutines.launch

@Composable
fun FileTranslateScreen(
    state: FileTranslateState,
    onStateChange: (FileTranslateState) -> Unit,
    modelReady: Boolean,
    context: String,
    translate: suspend (String, Language, Language) -> String
) {
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current
    val scrollState = rememberScrollState()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Take persistent permission
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val fileName = uri.lastPathSegment ?: "unknown"
            onStateChange(state.copy(selectedFileUri = uri, selectedFileName = fileName))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!modelReady) {
            Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                Text("模型未就绪", modifier = Modifier.padding(16.dp))
            }
        }

        // Language pair
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageDropdown(
                label = "源语言",
                selected = Language.fromCode(state.sourceLang),
                onSelect = { onStateChange(state.copy(sourceLang = it.code)) },
                modifier = Modifier.weight(1f)
            )
            Text(" → ", style = MaterialTheme.typography.headlineSmall)
            LanguageDropdown(
                label = "目标语言",
                selected = Language.fromCode(state.targetLang),
                onSelect = { onStateChange(state.copy(targetLang = it.code)) },
                modifier = Modifier.weight(1f)
            )
        }

        // File picker
        OutlinedButton(
            onClick = {
                filePicker.launch(arrayOf(
                    // Supported MIME types
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "text/plain",
                    "*/*"
                ))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.selectedFileName.isEmpty()) "选择文件" else state.selectedFileName)
        }

        // Supported formats hint
        Text(
            "支持 PDF / Word / Excel / PPT / TXT",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Translate button
        Button(
            onClick = {
                if (state.selectedFileUri == null) return@Button
                scope.launch {
                    onStateChange(state.copy(uiState = UiState.Loading))
                    try {
                        // Extract text
                        val extractor = DocumentExtractor()
                        val inputStream = appContext.contentResolver.openInputStream(state.selectedFileUri!!)
                        val mimeType = appContext.contentResolver.getType(state.selectedFileUri!!)
                        val extractedText = if (inputStream != null) {
                            extractor.extractText(state.selectedFileUri!!, inputStream, mimeType)
                        } else "无法读取文件"

                        inputStream?.close()

                        // Translate
                        val result = translate(
                            extractedText,
                            Language.fromCode(state.sourceLang),
                            Language.fromCode(state.targetLang)
                        )
                        onStateChange(state.copy(
                            extractedText = extractedText,
                            translatedText = result,
                            uiState = UiState.Success(
                                TranslationResult(extractedText.take(200) + "...", result.take(200) + "...", true)
                            )
                        ))
                    } catch (e: Exception) {
                        onStateChange(state.copy(
                            uiState = UiState.Error(e.message ?: "翻译失败")
                        ))
                    }
                }
            },
            enabled = state.selectedFileUri != null && modelReady,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.uiState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("翻译文件")
        }

        // Progress / Results
        when (val s = state.uiState) {
            is UiState.Loading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("正在翻译…", style = MaterialTheme.typography.bodySmall)
            }
            is UiState.Success -> {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("翻译结果：", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        SelectionContainer {
                            Text(state.translatedText)
                        }
                    }
                }
            }
            is UiState.Error -> {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                    Text(s.message, modifier = Modifier.padding(12.dp))
                }
            }
            else -> {}
        }
    }
}
