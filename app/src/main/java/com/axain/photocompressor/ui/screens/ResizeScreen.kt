package com.axain.photocompressor.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.axain.photocompressor.domain.LocalHistoryStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.axain.photocompressor.domain.ImageEngine
import com.axain.photocompressor.domain.OutputFormat
import com.axain.photocompressor.domain.ShareUtils
import com.axain.photocompressor.ui.components.GradientButton
import com.axain.photocompressor.ui.components.ResultCard
import com.axain.photocompressor.ui.components.ResultsSummary
import com.axain.photocompressor.ui.components.ScreenScaffold
import com.axain.photocompressor.ui.components.SectionLabel
import com.axain.photocompressor.ui.components.SelectPhotosCard
import com.axain.photocompressor.ui.components.SelectableChip
import com.axain.photocompressor.ui.components.SelectedStrip
import com.axain.photocompressor.ui.theme.rememberBrandGradients
import kotlinx.coroutines.launch

@Composable
fun ResizeScreen(dark: Boolean, onBack: () -> Unit) {
    val vm: ResizeViewModel = viewModel()
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
    LaunchedEffect(state.results) {
        if (state.results.isNotEmpty()) store.record(state.results)
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris -> vm.onPickResult(uris) }

    fun launchPicker() = picker.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )

    ScreenScaffold(
        title = "Resize",
        subtitle = "Set new dimensions",
        background = g.heroSoft,
        onBack = onBack
    ) { topInset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SelectPhotosCard(
                    count = state.sources.size,
                    loading = state.isLoadingSources,
                    onClick = ::launchPicker
                )
            }

            if (state.hasSources) {
                item { SelectedStrip(state.sources, onRemove = vm::removeSource, onAdd = ::launchPicker) }

                item { SectionLabel("Longest edge", Modifier.padding(top = 4.dp, start = 2.dp)) }
                item { DimensionGrid(selected = state.preset, gradient = g.accentC, onSelect = vm::selectPreset) }
                item {
                    AnimatedVisibility(
                        visible = state.preset == DimensionPreset.CUSTOM,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = state.customLongEdge,
                            onValueChange = vm::setCustom,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            label = { Text("Longest edge (px)") },
                            placeholder = { Text("e.g. 1600") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                item { SectionLabel("Output format", Modifier.padding(top = 4.dp, start = 2.dp)) }
                item { FormatRow(selected = state.format, onSelect = vm::selectFormat, gradient = g.accentB) }

                item {
                    GradientButton(
                        text = if (state.isProcessing) state.progressLabel.ifBlank { "Working…" }
                        else "Resize ${state.sources.size} photo${if (state.sources.size > 1) "s" else ""}",
                        gradient = g.accentC,
                        enabled = state.canRun,
                        leadingIcon = Icons.Rounded.Tune,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        onClick = vm::run
                    )
                }
            }

            if (state.hasResults) {
                item {
                    ResultsSummary(
                        results = state.results,
                        doneVerb = "Resized",
                        onSaveAll = {
                            scope.launch {
                                var ok = 0
                                state.results.forEach {
                                    if (ImageEngine.saveToGallery(context, it.savedCacheFileName, it.format) != null) ok++
                                }
                                Toast.makeText(context, "Saved $ok to gallery", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onShareAll = { ShareUtils.shareImages(context, state.results) }
                    )
                }
                items(state.results) { r ->
                    ResultCard(
                        result = r,
                        onSave = {
                            scope.launch {
                                val uri = ImageEngine.saveToGallery(context, r.savedCacheFileName, r.format)
                                Toast.makeText(
                                    context,
                                    if (uri != null) "Saved to gallery" else "Couldn't save",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onShare = { ShareUtils.shareImages(context, listOf(r)) },
                        onPreview = { ShareUtils.viewImage(context, r) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DimensionGrid(
    selected: DimensionPreset,
    gradient: Brush,
    onSelect: (DimensionPreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DimensionPreset.entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { p ->
                    SelectableChip(
                        label = p.label,
                        selected = selected == p,
                        gradient = gradient,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(p) }
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun FormatRow(
    selected: OutputFormat,
    gradient: Brush,
    onSelect: (OutputFormat) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutputFormat.entries.forEach { f ->
            SelectableChip(
                label = f.label,
                selected = selected == f,
                gradient = gradient,
                modifier = Modifier.weight(1f),
                onClick = { onSelect(f) }
            )
        }
    }
}
