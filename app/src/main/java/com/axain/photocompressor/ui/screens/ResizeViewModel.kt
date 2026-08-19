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

enum class DimensionPreset(val label: String, val longEdge: Int?) {
    UHD("2160 px", 2160),
    FHD("1080 px", 1080),
    HD("720 px", 720),
    SMALL("480 px", 480),
    CUSTOM("Custom", null)
}

data class ResizeUiState(
    val sources: List<SourceImage> = emptyList(),
    val preset: DimensionPreset = DimensionPreset.FHD,
    val customLongEdge: String = "",
    val format: OutputFormat = OutputFormat.JPEG,
    val results: List<CompressionResult> = emptyList(),
    val isLoadingSources: Boolean = false,
    val isProcessing: Boolean = false,
    val progressLabel: String = ""
) {
    val hasSources get() = sources.isNotEmpty()
    val hasResults get() = results.isNotEmpty()
    val longEdge: Int?
        get() = when (preset) {
            DimensionPreset.CUSTOM -> customLongEdge.toIntOrNull()?.takeIf { it in 16..20000 }
            else -> preset.longEdge
        }
    val canRun get() = hasSources && !isProcessing && longEdge != null
}

class ResizeViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(ResizeUiState())
    val state: StateFlow<ResizeUiState> = _state.asStateFlow()

    fun onPickResult(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.update { it.copy(isLoadingSources = true) }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val loaded = uris.mapNotNull { ImageEngine.readSource(ctx, it) }
            _state.update { it.copy(sources = loaded, results = emptyList(), isLoadingSources = false) }
        }
    }

    fun selectPreset(p: DimensionPreset) = _state.update { it.copy(preset = p, results = emptyList()) }
    fun setCustom(v: String) = _state.update {
        it.copy(customLongEdge = v.filter { c -> c.isDigit() }.take(5), results = emptyList())
    }
    fun selectFormat(f: OutputFormat) = _state.update { it.copy(format = f, results = emptyList()) }
    fun removeSource(s: SourceImage) = _state.update { it.copy(sources = it.sources - s, results = emptyList()) }

    fun run() {
        val s = _state.value
        val edge = s.longEdge ?: return
        if (!s.canRun) return
        _state.update { it.copy(isProcessing = true, results = emptyList(), progressLabel = "Preparing…") }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val out = mutableListOf<CompressionResult>()
            s.sources.forEachIndexed { i, src ->
                _state.update { it.copy(progressLabel = "Resizing ${i + 1} of ${s.sources.size}") }
                ImageEngine.resize(ctx, src, edge, edge, s.format)?.let { out.add(it) }
            }
            _state.update { it.copy(isProcessing = false, results = out, progressLabel = "") }
        }
    }
}
