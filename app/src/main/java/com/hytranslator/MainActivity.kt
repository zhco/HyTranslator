package com.hytranslator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hytranslator.ui.*
import com.hytranslator.translator.DownloadState
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme()
            ) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var modelReady by remember { mutableStateOf(false) }
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    val scope = rememberCoroutineScope()
    val app = HyTranslatorApp.instance

    LaunchedEffect(Unit) {
        if (app.engine.isModelDownloaded()) {
            modelReady = app.engine.initialize()
        }
    }

    if (!modelReady) {
        DownloadScreen(
            state = downloadState,
            onStartDownload = {
                scope.launch {
                    app.engine.downloadModel { ds -> downloadState = ds }
                    if (app.engine.isModelDownloaded()) {
                        modelReady = app.engine.initialize()
                    }
                }
            }
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Text translation state
    var textState by remember { mutableStateOf(TextTranslateState()) }

    // File translation state
    var fileState by remember { mutableStateOf(FileTranslateState()) }

    // Web translation state
    var webState by remember { mutableStateOf(WebTranslateState()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("混元翻译") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Translate, contentDescription = null) },
                    label = { Text("文本") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("文件") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Language, contentDescription = null) },
                    label = { Text("网页") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
            }
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (selectedTab) {
                0 -> TextTranslateScreen(
                    state = textState,
                    onStateChange = { textState = it },
                    modelReady = modelReady,
                    translate = { text, src, tgt ->
                        app.engine.translate(text, src, tgt)
                    }
                )
                1 -> FileTranslateScreen(
                    state = fileState,
                    onStateChange = { fileState = it },
                    modelReady = modelReady,
                    context = "com.hytranslator",
                    translate = { text, src, tgt ->
                        app.engine.translate(text, src, tgt)
                    }
                )
                2 -> WebTranslateScreen(
                    state = webState,
                    onStateChange = { webState = it },
                    modelReady = modelReady,
                    translate = { text, src, tgt ->
                        app.engine.translate(text, src, tgt)
                    }
                )
            }
        }
    }
}

@Composable
fun DownloadScreen(state: DownloadState, onStartDownload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CloudDownload, contentDescription = null,
            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("混元翻译", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("首次使用需下载离线翻译模型 (~440MB)",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        when (state) {
            is DownloadState.Idle -> Button(onClick = onStartDownload) { Text("下载模型") }
            is DownloadState.Checking -> { CircularProgressIndicator(); Text("检查中...") }
            is DownloadState.Downloading -> {
                LinearProgressIndicator(progress = { state.progress / 100f },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp))
                Text((state.progress).toString() + "%")
            }
            is DownloadState.Complete -> Button(onClick = onStartDownload) { Text("加载模型") }
            is DownloadState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onStartDownload) { Text("重试") }
            }
        }
    }
}