package com.axainstudios.piclite.ui.screens

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axainstudios.piclite.domain.CompressionResult
import com.axainstudios.piclite.domain.ImageEngine
import com.axainstudios.piclite.domain.LocalHistoryStore
import com.axainstudios.piclite.domain.OutputFormat
import com.axainstudios.piclite.domain.ShareUtils
import com.axainstudios.piclite.domain.SourceImage
import com.axainstudios.piclite.ui.components.FormatRow
import com.axainstudios.piclite.ui.components.GradientButton
import com.axainstudios.piclite.ui.components.ResultCard
import com.axainstudios.piclite.ui.components.ResultsSummary
import com.axainstudios.piclite.ui.components.ScreenScaffold
import com.axainstudios.piclite.ui.components.SectionLabel
import com.axainstudios.piclite.ui.components.SelectPhotosCard
import com.axainstudios.piclite.ui.components.SelectedStrip
import com.axainstudios.piclite.ui.theme.Violet
import com.axainstudios.piclite.ui.theme.rememberBrandGradients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QualityUiState(
    val sources: List<SourceImage> = emptyList(),
    val quality: Int = 80,
    val format: OutputFormat = OutputFormat.JPEG,
    val results: List<CompressionResult> = emptyList(),
    val isLoadingSources: Boolean = false,
    val isProcessing: Boolean = false,
    val progressLabel: String = ""
) {
    val hasSources get() = sources.isNotEmpty()
    val hasResults get() = results.isNotEmpty()
    val canRun get() = hasSources && !isProcessing
}

class QualityViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(QualityUiState())
    val state: StateFlow<QualityUiState> = _state.asStateFlow()

    fun onPickResult(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.update { it.copy(isLoadingSources = true) }
        viewModelScope.launch {
            val loaded = uris.mapNotNull { ImageEngine.readSource(getApplication(), it) }
            _state.update { it.copy(sources = loaded, results = emptyList(), isLoadingSources = false) }
        }
    }

    fun adopt(sources: List<SourceImage>) = _state.update { it.copy(sources = sources, results = emptyList()) }
    fun setQuality(q: Int) = _state.update { it.copy(quality = q, results = emptyList()) }
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
                _state.update { it.copy(progressLabel = "Encoding ${i + 1} of ${s.sources.size}") }
                ImageEngine.requality(ctx, src, s.format, s.quality)?.let { out.add(it) }
            }
            _state.update { it.copy(isProcessing = false, results = out, progressLabel = "") }
        }
    }
}

@Composable
fun QualityScreen(dark: Boolean, onBack: () -> Unit) {
    val vm: QualityViewModel = viewModel()
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
        ActivityResultContracts.PickMultipleVisualMedia(com.axainstudios.piclite.billing.ProManager.photoLimit())
    ) { uris -> vm.onPickResult(uris) }
    fun launchPicker() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    ScreenScaffold("Quality", "Dial in the perfect trade-off", g.heroSoft, onBack) { topInset ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SelectPhotosCard(state.sources.size, state.isLoadingSources, ::launchPicker) }
            if (state.hasSources) {
                item { SelectedStrip(state.sources, vm::removeSource, ::launchPicker) }
                item { SectionLabel("Quality", Modifier.padding(top = 4.dp, start = 2.dp)) }
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Text(qualityLabel(state.quality), fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                            Text("${state.quality}%", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Violet)
                        }
                        Slider(
                            value = state.quality.toFloat(),
                            onValueChange = { vm.setQuality(it.toInt()) },
                            valueRange = 5f..100f
                        )
                    }
                }
                item { SectionLabel("Output format", Modifier.padding(top = 4.dp, start = 2.dp)) }
                item { FormatRow(state.format, g.accentB, vm::selectFormat) }
                item {
                    GradientButton(
                        text = if (state.isProcessing) state.progressLabel.ifBlank { "Working…" }
                        else "Apply to ${state.sources.size} photo${if (state.sources.size > 1) "s" else ""}",
                        gradient = g.accentB, enabled = state.canRun, leadingIcon = Icons.Rounded.Speed,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp), onClick = vm::run
                    )
                }
            }
            if (state.hasResults) {
                item {
                    ResultsSummary(state.results, "Encoded",
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

private fun qualityLabel(q: Int): String = when {
    q < 35 -> "Smallest file"
    q < 70 -> "Balanced"
    q < 90 -> "High quality"
    else -> "Maximum quality"
}
