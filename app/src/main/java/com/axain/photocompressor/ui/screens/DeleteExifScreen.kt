package com.axain.photocompressor.ui.screens

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.axain.photocompressor.ui.theme.rememberBrandGradients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeleteExifUiState(
    val sources: List<SourceImage> = emptyList(),
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

class DeleteExifViewModel(app: Application) : AndroidViewModel(app) {
    private val _state = MutableStateFlow(DeleteExifUiState())
    val state: StateFlow<DeleteExifUiState> = _state.asStateFlow()

    fun onPickResult(uris: List<Uri>) {
        if (uris.isEmpty()) return
        _state.update { it.copy(isLoadingSources = true) }
        viewModelScope.launch {
            val loaded = uris.mapNotNull { ImageEngine.readSource(getApplication(), it) }
            _state.update { it.copy(sources = loaded, results = emptyList(), isLoadingSources = false) }
        }
    }

    fun adopt(sources: List<SourceImage>) = _state.update { it.copy(sources = sources, results = emptyList()) }
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
                _state.update { it.copy(progressLabel = "Cleaning ${i + 1} of ${s.sources.size}") }
                ImageEngine.stripMetadata(ctx, src, s.format)?.let { out.add(it) }
            }
            _state.update { it.copy(isProcessing = false, results = out, progressLabel = "") }
        }
    }
}

@Composable
fun DeleteExifScreen(dark: Boolean, onBack: () -> Unit) {
    val vm: DeleteExifViewModel = viewModel()
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
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris -> vm.onPickResult(uris) }
    fun launchPicker() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    ScreenScaffold("Delete EXIF", "Strip location & camera data", g.heroSoft, onBack) { topInset ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SelectPhotosCard(state.sources.size, state.isLoadingSources, ::launchPicker) }
            if (state.hasSources) {
                item { SelectedStrip(state.sources, vm::removeSource, ::launchPicker) }
                item {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(
                            "Removes GPS location, camera model, timestamps and other embedded metadata by re-encoding.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item { SectionLabel("Output format", Modifier.padding(top = 4.dp, start = 2.dp)) }
                item { FormatRow(state.format, g.accentB, vm::selectFormat) }
                item {
                    GradientButton(
                        text = if (state.isProcessing) state.progressLabel.ifBlank { "Working…" }
                        else "Clean ${state.sources.size} photo${if (state.sources.size > 1) "s" else ""}",
                        gradient = g.accentD, enabled = state.canRun, leadingIcon = Icons.Rounded.Delete,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp), onClick = vm::run
                    )
                }
            }
            if (state.hasResults) {
                item {
                    ResultsSummary(state.results, "Cleaned",
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
