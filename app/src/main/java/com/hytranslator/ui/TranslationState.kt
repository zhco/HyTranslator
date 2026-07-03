package com.hytranslator.ui

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TranslationResult(
    val original: String,
    val translated: String,
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data object ModelInitFailed : UiState
    data class Success(val result: TranslationResult) : UiState
    data class Error(val message: String) : UiState
}

data class TextTranslateState(
    val sourceLang: String = "zh",
    val targetLang: String = "en",
    val inputText: String = "",
    val uiState: UiState = UiState.Idle
)

data class FileTranslateState(
    val sourceLang: String = "zh",
    val targetLang: String = "en",
    val selectedFileUri: Uri? = null,
    val selectedFileName: String = "",
    val uiState: UiState = UiState.Idle,
    val extractedText: String = "",
    val translatedText: String = ""
)

data class WebTranslateState(
    val sourceLang: String = "zh",
    val targetLang: String = "en",
    val selectedFolderUri: Uri? = null,
    val selectedFolderName: String = "",
    val fileCount: Int = 0,
    val uiState: UiState = UiState.Idle,
    val progress: Int = 0,
    val outputFiles: List<String> = emptyList()
)
