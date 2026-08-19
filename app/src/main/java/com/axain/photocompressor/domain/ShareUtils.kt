package com.axain.photocompressor.domain

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtils {

    fun shareImages(context: Context, results: List<CompressionResult>) {
        if (results.isEmpty()) return
        val uris = ArrayList(results.map { ImageEngine.shareableUri(context, it.savedCacheFileName) })
        val mime = results.map { it.format.mime }.distinct().singleOrNull() ?: "image/*"
        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uris.first())
                type = mime
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                type = mime
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun viewImage(context: Context, result: CompressionResult) {
        val uri: Uri = ImageEngine.shareableUri(context, result.savedCacheFileName)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, result.format.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
