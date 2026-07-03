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
import com.hytranslator.translator.Language
import com.hytranslator.web.HtmlTranslator
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun WebTranslateScreen(
    state: WebTranslateState,
    onStateChange: (WebTranslateState) -> Unit,
    modelReady: Boolean,
    translate: suspend (String, Language, Language) -> String
) {
    val scope = rememberCoroutineScope()
    val appContext = LocalContext.current
    val scrollState = rememberScrollState()

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val folderName = uri.lastPathSegment ?: "文件夹"
            onStateChange(state.copy(selectedFolderUri = uri, selectedFolderName = folderName))
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

        // Folder picker
        OutlinedButton(
            onClick = { folderPicker.launch(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.selectedFolderName.isEmpty()) "选择网页文件夹" else state.selectedFolderName)
        }

        // Translate button
        Button(
            onClick = {
                if (state.selectedFolderUri == null) return@Button
                scope.launch {
                    onStateChange(state.copy(uiState = UiState.Loading, progress = 0))
                    try {
                        // Convert URI to DocumentFile
                        val docTree = androidx.documentfile.provider.DocumentFile.fromTreeUri(
                            appContext, state.selectedFolderUri!!
                        )
                        if (docTree == null || !docTree.isDirectory) {
                            onStateChange(state.copy(uiState = UiState.Error("无法访问文件夹")))
                            return@launch
                        }

                        // Count HTML files
                        val htmlFiles = docTree.listFiles().filter { it.name?.endsWith(".html") == true || it.name?.endsWith(".htm") == true }
                        onStateChange(state.copy(fileCount = htmlFiles.size))

                        if (htmlFiles.isEmpty()) {
                            onStateChange(state.copy(uiState = UiState.Error("未找到 HTML 文件")))
                            return@launch
                        }

                        // Process each file
                        val outputFiles = mutableListOf<String>()
                        val translator = HtmlTranslator()
                        val sourceLang = Language.fromCode(state.sourceLang)
                        val targetLang = Language.fromCode(state.targetLang)

                        htmlFiles.forEachIndexed { index, docFile ->
                            // Read HTML content
                            val content = appContext.contentResolver.openInputStream(docFile.uri)
                                ?.bufferedReader()?.use { it.readText() } ?: ""

                            // Translate using the model
                            val translated = translate(content, sourceLang, targetLang)

                            // Write to new file
                            val outputName = "${docFile.name?.removeSuffix(".html")?.removeSuffix(".htm")}_${state.targetLang}.html"
                            val outputUri = docTree.createFile("text/html", outputName)?.uri
                            if (outputUri != null) {
                                appContext.contentResolver.openOutputStream(outputUri)?.use { out ->
                                    out.write(translated.toByteArray())
                                }
                                outputFiles.add(outputName)
                            }

                            // Update progress
                            onStateChange(state.copy(progress = (index + 1) * 100 / htmlFiles.size))
                        }

                        onStateChange(state.copy(
                            uiState = UiState.Success(TranslationResult("", "完成", true)),
                            outputFiles = outputFiles
                        ))
                    } catch (e: Exception) {
                        onStateChange(state.copy(uiState = UiState.Error(e.message ?: "翻译失败")))
                    }
                }
            },
            enabled = state.selectedFolderUri != null && modelReady,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.uiState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text("翻译网页")
        }

        // Progress
        when (val s = state.uiState) {
            is UiState.Loading -> {
                LinearProgressIndicator(
                    progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("已翻译 ${state.progress}% (${state.fileCount} 个文件)", style = MaterialTheme.typography.bodySmall)
            }
            is UiState.Success -> {
                Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("翻译完成", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        if (state.outputFiles.isNotEmpty()) {
                            Text("生成文件：", style = MaterialTheme.typography.labelMedium)
                            state.outputFiles.forEach { file ->
                                Text("• $file", style = MaterialTheme.typography.bodySmall)
                            }
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
