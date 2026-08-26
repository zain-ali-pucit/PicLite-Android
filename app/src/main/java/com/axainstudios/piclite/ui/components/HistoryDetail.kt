package com.axainstudios.piclite.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.axainstudios.piclite.domain.HistoryEntry
import com.axainstudios.piclite.domain.ImageEngine
import com.axainstudios.piclite.domain.LocalHistoryStore
import com.axainstudios.piclite.domain.ShareUtils
import com.axainstudios.piclite.domain.formatBytes
import kotlinx.coroutines.launch

@Composable
fun HistoryDetailDialog(entry: HistoryEntry, onDismiss: () -> Unit) {
    val store = LocalHistoryStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var saved by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(30.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            AsyncImage(
                model = store.fileFor(entry),
                contentDescription = entry.name,
                modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip("${entry.width}×${entry.height}")
                Spacer(Modifier.size(8.dp))
                Chip(entry.format.label)
                Spacer(Modifier.size(8.dp))
                Chip("-${(entry.savedRatio * 100).toInt()}%", MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.weight(1f))
                Text("${formatBytes(entry.originalBytes)} → ${formatBytes(entry.outputBytes)}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlineActionButton(
                    text = if (saved) "Saved" else "Save",
                    icon = if (saved) Icons.Rounded.Check else Icons.Rounded.Download,
                    modifier = Modifier.weight(1f)
                ) {
                    scope.launch {
                        val uri = ImageEngine.saveFileToGallery(context, store.fileFor(entry), entry.format)
                        if (uri != null) saved = true
                    }
                }
                OutlineActionButton(
                    text = "Share", icon = Icons.Rounded.Share, modifier = Modifier.weight(1f)
                ) {
                    ShareUtils.shareUris(context, listOf(store.shareUri(entry)), entry.format.mime)
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, color = tint)
    }
}
