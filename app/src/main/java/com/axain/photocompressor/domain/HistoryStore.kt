package com.axain.photocompressor.domain

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A persisted record of one produced image. Survives process death. */
data class HistoryEntry(
    val id: String,
    val name: String,
    val originalBytes: Long,
    val outputBytes: Long,
    val width: Int,
    val height: Int,
    val format: OutputFormat,
    val file: String,
    val date: Long,
    val favorite: Boolean = false
) {
    val savedRatio: Float
        get() = if (originalBytes <= 0) 0f
        else (1f - outputBytes.toFloat() / originalBytes.toFloat()).coerceIn(0f, 1f)
}

/** Session + persistent store: history feed, favorites, the "+" hand-off, and the detail overlay. */
class HistoryStore(private val context: Context) {

    val entries = mutableStateListOf<HistoryEntry>()

    /** Photos picked via "+" (camera or library), handed to whichever tool the user chooses. */
    var handoff by mutableStateOf<List<SourceImage>>(emptyList())
    /** A history entry shown in the centered detail card, if any. */
    var detail by mutableStateOf<HistoryEntry?>(null)

    private val dir = File(context.filesDir, "history").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")

    init { load() }

    val favorites: List<HistoryEntry> get() = entries.filter { it.favorite }

    fun fileFor(entry: HistoryEntry): File = File(dir, entry.file)

    fun shareUri(entry: HistoryEntry): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fileFor(entry))

    fun record(results: List<CompressionResult>) {
        results.forEach { r ->
            if (entries.any { it.id == r.savedCacheFileName }) return@forEach
            val srcFile = File(File(context.cacheDir, "shared"), r.savedCacheFileName)
            val dest = File(dir, r.savedCacheFileName)
            if (!dest.exists() && srcFile.exists()) runCatching { srcFile.copyTo(dest) }
            entries.add(
                0,
                HistoryEntry(
                    id = r.savedCacheFileName,
                    name = r.source.displayName,
                    originalBytes = r.source.originalBytes,
                    outputBytes = r.outputBytes,
                    width = r.outputWidth,
                    height = r.outputHeight,
                    format = r.format,
                    file = r.savedCacheFileName,
                    date = System.currentTimeMillis()
                )
            )
        }
        while (entries.size > 200) entries.removeAt(entries.lastIndex)
        save()
    }

    fun toggleFavorite(entry: HistoryEntry) {
        val idx = entries.indexOfFirst { it.id == entry.id }
        if (idx >= 0) {
            entries[idx] = entries[idx].copy(favorite = !entries[idx].favorite)
            save()
        }
    }

    fun isFavorite(entry: HistoryEntry): Boolean =
        entries.firstOrNull { it.id == entry.id }?.favorite ?: false

    private fun save() {
        runCatching {
            val arr = JSONArray()
            entries.forEach { e ->
                arr.put(JSONObject().apply {
                    put("id", e.id); put("name", e.name)
                    put("orig", e.originalBytes); put("out", e.outputBytes)
                    put("w", e.width); put("h", e.height)
                    put("fmt", e.format.name); put("file", e.file)
                    put("date", e.date); put("fav", e.favorite)
                })
            }
            indexFile.writeText(arr.toString())
        }
    }

    private fun load() {
        if (!indexFile.exists()) return
        runCatching {
            val arr = JSONArray(indexFile.readText())
            val list = ArrayList<HistoryEntry>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (!File(dir, o.getString("file")).exists()) continue
                list.add(
                    HistoryEntry(
                        id = o.getString("id"), name = o.getString("name"),
                        originalBytes = o.getLong("orig"), outputBytes = o.getLong("out"),
                        width = o.getInt("w"), height = o.getInt("h"),
                        format = OutputFormat.valueOf(o.getString("fmt")),
                        file = o.getString("file"), date = o.getLong("date"),
                        favorite = o.optBoolean("fav", false)
                    )
                )
            }
            entries.clear(); entries.addAll(list)
        }
    }
}

val LocalHistoryStore = staticCompositionLocalOf<HistoryStore> {
    error("HistoryStore not provided")
}
