package com.axain.photocompressor.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.axain.photocompressor.domain.ImageEngine
import com.axain.photocompressor.domain.LocalHistoryStore
import com.axain.photocompressor.ui.components.ScreenScaffold
import com.axain.photocompressor.ui.components.SelectedStrip
import com.axain.photocompressor.ui.theme.rememberBrandGradients
import kotlinx.coroutines.launch

@Composable
fun ChooseToolScreen(dark: Boolean, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val store = LocalHistoryStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val g = rememberBrandGradients(dark)
    var sources by remember { mutableStateOf(store.handoff) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(20)
    ) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            sources = uris.mapNotNull { ImageEngine.readSource(context, it) }
        }
    }

    ScreenScaffold(
        title = "Choose a tool",
        subtitle = "${sources.size} photo${if (sources.size == 1) "" else "s"} selected",
        background = g.heroSoft,
        onBack = onBack
    ) { topInset ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SelectedStrip(
                    sources = sources,
                    onRemove = { s -> sources = sources.filterNot { it.uri == s.uri } },
                    onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                )
            }
            items(allTools.chunked(3)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { tool ->
                        ToolTile(tool, Modifier.weight(1f)) {
                            if (sources.isNotEmpty()) {
                                store.handoff = sources
                                onOpen(tool.route)
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
