package com.axain.photocompressor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.axain.photocompressor.domain.HistoryEntry
import com.axain.photocompressor.domain.LocalHistoryStore
import com.axain.photocompressor.domain.formatBytes
import com.axain.photocompressor.ui.theme.Amber
import com.axain.photocompressor.ui.theme.rememberBrandGradients
import java.util.Calendar

@Composable
private fun TabContainer(
    dark: Boolean,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    val g = rememberBrandGradients(dark)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(Modifier.fillMaxSize().background(g.heroSoft)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = statusTop + 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            content()
        }
    }
}

@Composable
private fun EmptyHint(icon: ImageVector, text: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(40.dp))
        Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun LibraryScreen(dark: Boolean) {
    val store = LocalHistoryStore.current
    TabContainer(dark, "Library", "Everything you've optimized") {
        if (store.entries.isEmpty()) {
            EmptyHint(Icons.Rounded.PhotoLibrary, "Optimized photos will show up here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                store.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { e -> LibraryThumb(e, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryThumb(entry: HistoryEntry, modifier: Modifier) {
    val store = LocalHistoryStore.current
    Box(
        modifier
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { store.detail = entry }
    ) {
        AsyncImage(model = store.fileFor(entry), contentDescription = entry.name,
            modifier = Modifier.fillMaxSize())
        Box(
            Modifier.align(Alignment.BottomStart).padding(8.dp)
                .clip(RoundedCornerShape(9.dp)).background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 7.dp, vertical = 4.dp)
        ) {
            Text("${formatBytes(entry.originalBytes)} → ${formatBytes(entry.outputBytes)}",
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
fun HistoryScreen(dark: Boolean) {
    val store = LocalHistoryStore.current
    TabContainer(dark, "History", "Your recent results") {
        if (store.entries.isEmpty()) {
            EmptyHint(Icons.Rounded.Schedule, "No history yet. Optimize a photo to begin.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                groupByDay(store.entries).forEach { (title, entries) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(start = 2.dp))
                        entries.forEach { HistoryRow(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(dark: Boolean) {
    val store = LocalHistoryStore.current
    TabContainer(dark, "Favorites", "Results you starred") {
        val favs = store.favorites
        if (favs.isEmpty()) {
            EmptyHint(Icons.Rounded.StarBorder, "Star a result in History to keep it here.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                favs.forEach { HistoryRow(it) }
            }
        }
    }
}

@Composable
fun HistoryRow(entry: HistoryEntry) {
    val store = LocalHistoryStore.current
    val fav = store.isFavorite(entry)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable { store.detail = entry }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AsyncImage(
            model = store.fileFor(entry), contentDescription = entry.name,
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(entry.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface)
            Text("${formatBytes(entry.originalBytes)} → ${formatBytes(entry.outputBytes)}",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("-${(entry.savedRatio * 100).toInt()}% · ${entry.format.label}",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
        }
        Icon(
            if (fav) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = "Favorite",
            tint = if (fav) Amber else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp).clickable { store.toggleFavorite(entry) }
        )
    }
}

private fun groupByDay(entries: List<HistoryEntry>): List<Pair<String, List<HistoryEntry>>> {
    val cal = Calendar.getInstance()
    fun startOfDay(ms: Long): Long {
        cal.timeInMillis = ms
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    val todayStart = startOfDay(System.currentTimeMillis())
    val dayMs = 24L * 60 * 60 * 1000
    return entries.groupBy { startOfDay(it.date) }
        .toSortedMap(compareByDescending { it })
        .map { (day, list) ->
            val title = when (day) {
                todayStart -> "Today"
                todayStart - dayMs -> "Yesterday"
                else -> java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(day)
            }
            title to list.sortedByDescending { it.date }
        }
}
