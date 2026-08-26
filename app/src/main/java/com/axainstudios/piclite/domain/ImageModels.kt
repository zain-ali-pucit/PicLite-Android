package com.axainstudios.piclite.domain

import android.net.Uri

/** Preset target sizes offered on the compression screen. */
enum class TargetPreset(val label: String, val bytes: Long?) {
    KB_100("100 KB", 100L * 1024),
    KB_500("500 KB", 500L * 1024),
    MB_1("1 MB", 1024L * 1024),
    MB_2("2 MB", 2L * 1024 * 1024),
    CUSTOM("Custom", null)
}

/** Output format for compression / conversion. */
enum class OutputFormat(val label: String, val extension: String, val mime: String) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp")
}

data class SourceImage(
    val uri: Uri,
    val displayName: String,
    val originalBytes: Long,
    val width: Int,
    val height: Int
)

data class CompressionResult(
    val source: SourceImage,
    val outputBytes: Long,
    val outputWidth: Int,
    val outputHeight: Int,
    val format: OutputFormat,
    val savedCacheFileName: String
) {
    val savedRatio: Float
        get() = if (source.originalBytes <= 0) 0f
        else (1f - outputBytes.toFloat() / source.originalBytes.toFloat()).coerceIn(0f, 1f)
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.2f MB", mb)
    return String.format("%.2f GB", mb / 1024.0)
}
