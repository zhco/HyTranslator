package com.hytranslator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hytranslator.translator.Language
import kotlinx.coroutines.launch

@Composable
fun TextTranslateScreen(
    state: TextTranslateState,
    onStateChange: (TextTranslateState) -> Unit,
    modelReady: Boolean,
    translate: suspend (String, Language, Language) -> String
) {
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!modelReady) {
            Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                Text("Model not ready", modifier = Modifier.padding(16.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LanguageDropdown("Src", Language.fromCode(state.sourceLang),
                { onStateChange(state.copy(sourceLang = it.code)) }, Modifier.weight(1f))
            Text(" -> ", style = MaterialTheme.typography.headlineSmall)
            LanguageDropdown("Tgt", Language.fromCode(state.targetLang),
                { onStateChange(state.copy(targetLang = it.code)) }, Modifier.weight(1f))
        }
        OutlinedButton(onClick = {
            val t = (state.uiState as? UiState.Success)?.result?.translated ?: state.inputText
            onStateChange(state.copy(sourceLang = state.targetLang, targetLang = state.sourceLang, inputText = t))
        }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Swap") }
        OutlinedTextField(state.inputText, { onStateChange(state.copy(inputText = it)) },
            label = { Text("Input") }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), maxLines = 10)
        Button(onClick = {
            if (state.inputText.isBlank()) return@Button
            scope.launch {
                onStateChange(state.copy(uiState = UiState.Loading))
                try {
                    val res = translate(state.inputText,
                        Language.fromCode(state.sourceLang), Language.fromCode(state.targetLang))
                    onStateChange(state.copy(uiState = UiState.Success(
                        TranslationResult(state.inputText, res, true))))
                } catch (e: Exception) {
                    onStateChange(state.copy(uiState = UiState.Error(e.message ?: "error")))
                }
            }
        }, enabled = state.inputText.isNotBlank() && modelReady, modifier = Modifier.fillMaxWidth()) {
            Text("Translate")
        }
        when (val s = state.uiState) {
            is UiState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            is UiState.Success -> Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(12.dp)) {
                    SelectionContainer { Text(s.result.translated) }
                }
            }
            is UiState.Error -> Card(colors = CardDefaults.cardColors(MaterialTheme.colorScheme.errorContainer)) {
                Text(s.message, Modifier.padding(12.dp))
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(label: String, selected: Language, onSelect: (Language) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = it }, modifier) {
        OutlinedTextField(
            selected.displayName + " (" + selected.code + ")", {}, true, { Text(label) },
            { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, Modifier.menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors())
        ExposedDropdownMenu(expanded, { expanded = false }) {
            Language.entries.forEach { lang ->
                DropdownMenuItem({ Text(lang.displayName + " (" + lang.code + ")") },
                    { onSelect(lang); expanded = false })
            }
        }
    }
}
