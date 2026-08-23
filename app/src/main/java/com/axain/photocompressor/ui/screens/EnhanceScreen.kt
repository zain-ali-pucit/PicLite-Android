package com.axain.photocompressor.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axain.photocompressor.domain.CompressionResult
import com.axain.photocompressor.domain.ImageEngine
import com.axain.photocompressor.domain.LocalHistoryStore
import com.axain.photocompressor.domain.OutputFormat
import com.axain.photocompressor.domain.ShareUtils
import com.axain.photocompressor.domain.SourceImage
import com.axain.photocompressor.ui.components.FormatRow
import com.axain.photocompressor.ui.components.GradientButton
import com.axain.photocompressor.ui.components.ResultCard
import com.axain.photocompressor.ui.components.ResultsSummary
import com.axain.photocompressor.ui.components.ScreenScaffold
import com.axain.photocompressor.ui.components.SectionLabel
import com.axain.photocompressor.ui.components.SelectPhotosCard
import com.axain.photocompressor.ui.components.SelectedStrip
import com.axain.photocompressor.ui.theme.Violet
import com.axain.photocompressor.ui.theme.rememberBrandGradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EnhanceUiState(
    val sources: List<SourceImage> = emptyList(),
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f,
    val sharpness: Float = 0.2f,
    val format: OutputFormat = OutputFormat.JPEG,
    val results: List<CompressionResult> = emptyList(),
    val preview: Bitmap? = null,
    val isLoadingSources: Boolean = false,
    val isProcessing: Boolean = false,
    val progressLabel: String = ""
) {
    val hasSources get() = sources.isNotEmpty()
    val hasResults get() = results.isNotEmpty()
    val canRun get() = hasSources && !isProcessing
}

class EnhanceViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(EnhanceUiState())
    val state: StateFlow<EnhanceUiState> = _state.asStateFlow()
    private var previewBase: Bitmap? = null

    fun onPickResult(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.update { it.copy(isLoadingSources = true) }
        viewModelScope.launch {
            val loaded = uris.mapNotNull { ImageEngine.readSource(getApplication(), it) }
            _state.update { it.copy(sources = loaded, results = emptyList(), isLoadingSources = false) }
            buildPreviewBase()
        }
    }

    fun adopt(sources: List<SourceImage>) {
        _state.update { it.copy(sources = sources, results = emptyList()) }
        viewModelScope.launch { buildPreviewBase() }
    }

    private suspend fun buildPreviewBase() {
        val src = _state.value.sources.firstOrNull() ?: return
        previewBase = withContext(Dispatchers.IO) {
            ImageEngine.decodeBitmap(getApplication(), src.uri)?.let { downscale(it, 720) }
        }
        refreshPreview()
    }

    private fun downscale(bmp: Bitmap, maxDim: Int): Bitmap {
        val f = maxOf(bmp.width, bmp.height)
        if (f <= maxDim) return bmp
        val scale = maxDim.toFloat() / f
        return Bitmap.createScaledBitmap(bmp, (bmp.width * scale).toInt(), (bmp.height * scale).toInt(), true)
    }

    fun setBrightness(v: Float) { _state.update { it.copy(brightness = v, results = emptyList()) }; refreshPreview() }
    fun setContrast(v: Float) { _state.update { it.copy(contrast = v, results = emptyList()) }; refreshPreview() }
    fun setSaturation(v: Float) { _state.update { it.copy(saturation = v, results = emptyList()) }; refreshPreview() }
    fun setSharpness(v: Float) { _state.update { it.copy(sharpness = v, results = emptyList()) }; refreshPreview() }
    fun reset() {
        _state.update { it.copy(brightness = 0f, contrast = 1f, saturation = 1f, sharpness = 0.2f, results = emptyList()) }
        refreshPreview()
    }
    fun selectFormat(f: OutputFormat) = _state.update { it.copy(format = f, results = emptyList()) }
    fun removeSource(s: SourceImage) {
        _state.update { it.copy(sources = it.sources - s, results = emptyList()) }
        if (_state.value.sources.isEmpty()) { previewBase = null; _state.update { it.copy(preview = null) } }
    }

    private fun refreshPreview() {
        val base = previewBase ?: return
        val s = _state.value
        viewModelScope.launch {
            val out = withContext(Dispatchers.IO) {
                ImageEngine.applyAdjustments(base, s.brightness, s.contrast, s.saturation, s.sharpness)
            }
            _state.update { it.copy(preview = out) }
        }
    }

    fun run() {
        val s = _state.value
        if (!s.canRun) return
        _state.update { it.copy(isProcessing = true, results = emptyList(), progressLabel = "Preparing…") }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val out = mutableListOf<CompressionResult>()
            s.sources.forEachIndexed { i, src ->
                _state.update { it.copy(progressLabel = "Enhancing ${i + 1} of ${s.sources.size}") }
                ImageEngine.enhance(ctx, src, s.brightness, s.contrast, s.saturation, s.sharpness, s.format)?.let { out.add(it) }
            }
            _state.update { it.copy(isProcessing = false, results = out, progressLabel = "") }
        }
    }
}

@Composable
fun EnhanceScreen(dark: Boolean, onBack: () -> Unit) {
    val vm: EnhanceViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val g = rememberBrandGradients(dark)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = LocalHistoryStore.current

    LaunchedEffect(Unit) {
        if (state.sources.isEmpty() && store.handoff.isNotEmpty()) {
            vm.adopt(store.handoff); store.handoff = emptyList()
        }
    }
    LaunchedEffect(state.results) { if (state.results.isNotEmpty()) store.record(state.results) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(com.axain.photocompressor.billing.ProManager.photoLimit())
    ) { uris -> vm.onPickResult(uris) }
    fun launchPicker() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    ScreenScaffold("Enhance", "Brighten, punch up and sharpen", g.heroSoft, onBack) { topInset ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SelectPhotosCard(state.sources.size, state.isLoadingSources, ::launchPicker) }
            if (state.hasSources) {
                item { SelectedStrip(state.sources, vm::removeSource, ::launchPicker) }
                state.preview?.let { bmp ->
                    item {
                        Image(
                            bitmap = bmp.asImageBitmap(), contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        SectionLabel("Adjust", Modifier.padding(start = 2.dp))
                        Spacer(Modifier.weight(1f))
                        Text("Reset", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Violet,
                            modifier = Modifier.clickable { vm.reset() })
                    }
                }
                item { AdjustSlider("Brightness", state.brightness, -0.3f..0.3f, vm::setBrightness) }
                item { AdjustSlider("Contrast", state.contrast, 0.7f..1.3f, vm::setContrast) }
                item { AdjustSlider("Saturation", state.saturation, 0f..2f, vm::setSaturation) }
                item { AdjustSlider("Sharpness", state.sharpness, 0f..1f, vm::setSharpness) }
                item { SectionLabel("Output format", Modifier.padding(top = 4.dp, start = 2.dp)) }
                item { FormatRow(state.format, g.accentB, vm::selectFormat) }
                item {
                    GradientButton(
                        text = if (state.isProcessing) state.progressLabel.ifBlank { "Working…" }
                        else "Enhance ${state.sources.size} photo${if (state.sources.size > 1) "s" else ""}",
                        gradient = g.accentD, enabled = state.canRun, leadingIcon = Icons.Rounded.AutoFixHigh,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp), onClick = vm::run
                    )
                }
            }
            if (state.hasResults) {
                item {
                    ResultsSummary(state.results, "Enhanced",
                        onSaveAll = {
                            scope.launch {
                                var ok = 0
                                state.results.forEach {
                                    if (ImageEngine.saveToGallery(context, it.savedCacheFileName, it.format) != null) ok++
                                }
                                Toast.makeText(context, "Saved $ok to gallery", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShareAll = { ShareUtils.shareImages(context, state.results) })
                }
                items(state.results) { r ->
                    ResultCard(r,
                        onSave = {
                            scope.launch {
                                val uri = ImageEngine.saveToGallery(context, r.savedCacheFileName, r.format)
                                Toast.makeText(context, if (uri != null) "Saved to gallery" else "Couldn't save", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShare = { ShareUtils.shareImages(context, listOf(r)) },
                        onPreview = { ShareUtils.viewImage(context, r) })
                }
            }
        }
    }
}

@Composable
private fun AdjustSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
