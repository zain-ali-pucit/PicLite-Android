package com.axain.photocompressor.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.axain.photocompressor.domain.CompressionResult
import com.axain.photocompressor.domain.ImageEngine
import com.axain.photocompressor.domain.OutputFormat
import com.axain.photocompressor.domain.SourceImage
import com.axain.photocompressor.domain.TargetPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CompressUiState(
    val sources: List<SourceImage> = emptyList(),
    val preset: TargetPreset = TargetPreset.KB_500,
    val customKb: String = "",
    val format: OutputFormat = OutputFormat.JPEG,
    val results: List<CompressionResult> = emptyList(),
    val isLoadingSources: Boolean = false,
    val isProcessing: Boolean = false,
    val progressLabel: String = ""
) {
    val hasSources get() = sources.isNotEmpty()
    val hasResults get() = results.isNotEmpty()

    val targetBytes: Long?
        get() = when (preset) {
            TargetPreset.CUSTOM -> customKb.toLongOrNull()?.takeIf { it > 0 }?.times(1024)
            else -> preset.bytes
        }

    val canCompress: Boolean
        get() = hasSources && !isProcessing &&
            (preset != TargetPreset.CUSTOM || (customKb.toLongOrNull() ?: 0) > 0)
}

class CompressViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(CompressUiState())
    val state: StateFlow<CompressUiState> = _state.asStateFlow()

    fun onPickResult(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.update { it.copy(isLoadingSources = true) }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val loaded = uris.mapNotNull { ImageEngine.readSource(ctx, it) }
            _state.update {
                it.copy(
                    sources = loaded,
                    results = emptyList(),
                    isLoadingSources = false
                )
            }
        }
    }

    fun selectPreset(preset: TargetPreset) = _state.update { it.copy(preset = preset, results = emptyList()) }
    fun setCustomKb(value: String) =
        _state.update { it.copy(customKb = value.filter { c -> c.isDigit() }.take(7), results = emptyList()) }
    fun selectFormat(format: OutputFormat) = _state.update { it.copy(format = format, results = emptyList()) }

    fun removeSource(source: SourceImage) =
        _state.update { it.copy(sources = it.sources - source, results = emptyList()) }

    fun clearAll() = _state.update { CompressUiState() }

    fun compress() {
        val s = _state.value
        if (!s.canCompress) return
        _state.update { it.copy(isProcessing = true, results = emptyList(), progressLabel = "Preparing…") }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val out = mutableListOf<CompressionResult>()
            s.sources.forEachIndexed { index, src ->
                _state.update { it.copy(progressLabel = "Compressing ${index + 1} of ${s.sources.size}") }
                ImageEngine.compress(
                    context = ctx,
                    source = src,
                    targetBytes = s.targetBytes,
                    format = s.format
                )?.let { out.add(it) }
            }
            _state.update { it.copy(isProcessing = false, results = out, progressLabel = "") }
        }
    }
}
