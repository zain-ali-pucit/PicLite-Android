package com.axain.photocompressor.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axain.photocompressor.domain.CompressionResult
import com.axain.photocompressor.domain.ImageEngine
import com.axain.photocompressor.domain.OutputFormat
import com.axain.photocompressor.domain.SourceImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ConvertUiState(
    val sources: List<SourceImage> = emptyList(),
    val format: OutputFormat = OutputFormat.PNG,
    val results: List<CompressionResult> = emptyList(),
    val isLoadingSources: Boolean = false,
    val isProcessing: Boolean = false,
    val progressLabel: String = ""
) {
    val hasSources get() = sources.isNotEmpty()
    val hasResults get() = results.isNotEmpty()
    val canRun get() = hasSources && !isProcessing
}

class ConvertViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(ConvertUiState())
    val state: StateFlow<ConvertUiState> = _state.asStateFlow()

    fun onPickResult(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.update { it.copy(isLoadingSources = true) }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val loaded = uris.mapNotNull { ImageEngine.readSource(ctx, it) }
            _state.update { it.copy(sources = loaded, results = emptyList(), isLoadingSources = false) }
        }
    }

    fun selectFormat(f: OutputFormat) = _state.update { it.copy(format = f, results = emptyList()) }
    fun removeSource(s: SourceImage) = _state.update { it.copy(sources = it.sources - s, results = emptyList()) }

    fun run() {
        val s = _state.value
        if (!s.canRun) return
        _state.update { it.copy(isProcessing = true, results = emptyList(), progressLabel = "Preparing…") }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val out = mutableListOf<CompressionResult>()
            s.sources.forEachIndexed { i, src ->
                _state.update { it.copy(progressLabel = "Converting ${i + 1} of ${s.sources.size}") }
                ImageEngine.compress(ctx, src, targetBytes = null, format = s.format)?.let { out.add(it) }
            }
            _state.update { it.copy(isProcessing = false, results = out, progressLabel = "") }
        }
    }
}
