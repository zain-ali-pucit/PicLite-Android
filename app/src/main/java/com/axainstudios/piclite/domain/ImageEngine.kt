package com.axainstudios.piclite.domain

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * All bitmap-heavy work lives here. Every public function is suspend + runs on Dispatchers.IO.
 */
object ImageEngine {

    /** Read metadata (name, size, dimensions) for a picked image. */
    suspend fun readSource(context: Context, uri: Uri): SourceImage? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            var name = "image"
            var size = 0L
            resolver.query(uri, null, null, null, null)?.use { c ->
                val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (c.moveToFirst()) {
                    if (nameIdx >= 0) name = c.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = c.getLong(sizeIdx)
                }
            }
            if (size <= 0L) {
                resolver.openFileDescriptor(uri, "r")?.use { size = it.statSize }
            }
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            SourceImage(
                uri = uri,
                displayName = name,
                originalBytes = size,
                width = opts.outWidth.coerceAtLeast(0),
                height = opts.outHeight.coerceAtLeast(0)
            )
        }.getOrNull()
    }

    /**
     * Compress [source] to at most [targetBytes] (or best-effort quality when null),
     * writing the output to app cache. Returns the result with real output size.
     */
    suspend fun compress(
        context: Context,
        source: SourceImage,
        targetBytes: Long?,
        format: OutputFormat,
        qualityFloor: Int = 30
    ): CompressionResult? = withContext(Dispatchers.IO) {
        runCatching {
            var bitmap = decodeOriented(context, source.uri) ?: return@runCatching null
            val compressFormat = format.toCompressFormat()

            var bytes: ByteArray = if (targetBytes == null) {
                encode(bitmap, compressFormat, if (format == OutputFormat.PNG) 100 else 85)
            } else {
                encodeToTarget(bitmap, compressFormat, targetBytes, format, qualityFloor).also {
                    // If still over target and format supports quality, progressively downscale.
                    var result = it
                    var guard = 0
                    while (result.size > targetBytes && guard < 6 &&
                        (bitmap.width > 320 && bitmap.height > 320)
                    ) {
                        val scaled = Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * 0.8f).toInt().coerceAtLeast(1),
                            (bitmap.height * 0.8f).toInt().coerceAtLeast(1),
                            true
                        )
                        if (scaled != bitmap) bitmap.recycle()
                        bitmap = scaled
                        result = encodeToTarget(bitmap, compressFormat, targetBytes, format, qualityFloor)
                        guard++
                    }
                    return@runCatching writeAndBuild(context, source, bitmap, result, format)
                }
            }
            writeAndBuild(context, source, bitmap, bytes, format)
        }.getOrNull()
    }

    private fun writeAndBuild(
        context: Context,
        source: SourceImage,
        bitmap: Bitmap,
        bytes: ByteArray,
        format: OutputFormat
    ): CompressionResult {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val base = source.displayName.substringBeforeLast('.').ifBlank { "image" }
        val fileName = "${base}_piclite_${System.currentTimeMillis()}.${format.extension}"
        FileOutputStream(File(dir, fileName)).use { it.write(bytes) }
        val out = CompressionResult(
            source = source,
            outputBytes = bytes.size.toLong(),
            outputWidth = bitmap.width,
            outputHeight = bitmap.height,
            format = format,
            savedCacheFileName = fileName
        )
        bitmap.recycle()
        return out
    }

    /** Binary-search JPEG/WebP quality to approach the target size from below. */
    private fun encodeToTarget(
        bitmap: Bitmap,
        compressFormat: Bitmap.CompressFormat,
        targetBytes: Long,
        format: OutputFormat,
        qualityFloor: Int
    ): ByteArray {
        if (format == OutputFormat.PNG) {
            // PNG is lossless; quality has no effect. Return as-is.
            return encode(bitmap, compressFormat, 100)
        }
        var low = qualityFloor
        var high = 100
        var best = encode(bitmap, compressFormat, low)
        // If even the floor quality is under target, climb up for better fidelity.
        if (best.size <= targetBytes) {
            while (low <= high) {
                val mid = (low + high) / 2
                val candidate = encode(bitmap, compressFormat, mid)
                if (candidate.size <= targetBytes) {
                    best = candidate
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
        }
        return best
    }

    private fun encode(bitmap: Bitmap, format: Bitmap.CompressFormat, quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(format, quality.coerceIn(0, 100), stream)
        return stream.toByteArray()
    }

    /** Decode a bitmap, honoring EXIF orientation and guarding against OOM. */
    private fun decodeOriented(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val maxDim = 4096
        var sample = 1
        var w = bounds.outWidth
        var h = bounds.outHeight
        while (w / sample > maxDim || h / sample > maxDim) sample *= 2

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: return null

        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it) }
                ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { if (it != bitmap) bitmap.recycle() }
    }

    /** Resize a source to an exact bounding box (kept aspect ratio) and encode. */
    suspend fun resize(
        context: Context,
        source: SourceImage,
        maxWidth: Int,
        maxHeight: Int,
        format: OutputFormat,
        quality: Int = 90
    ): CompressionResult? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeOriented(context, source.uri) ?: return@runCatching null
            val ratio = minOf(
                maxWidth.toFloat() / bitmap.width,
                maxHeight.toFloat() / bitmap.height
            ).coerceAtMost(1f)
            val target = if (ratio < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * ratio).toInt().coerceAtLeast(1),
                    (bitmap.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else bitmap
            if (target != bitmap) bitmap.recycle()
            val bytes = encode(target, format.toCompressFormat(), quality)
            writeAndBuild(context, source, target, bytes, format)
        }.getOrNull()
    }

    /** Crop [source] to [rect] (pixel coordinates) and encode. */
    suspend fun crop(
        context: Context,
        source: SourceImage,
        rect: Rect,
        format: OutputFormat,
        quality: Int = 92
    ): CompressionResult? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeOriented(context, source.uri) ?: return@runCatching null
            val x = rect.left.coerceIn(0, bitmap.width - 1)
            val y = rect.top.coerceIn(0, bitmap.height - 1)
            val w = rect.width().coerceIn(1, bitmap.width - x)
            val h = rect.height().coerceIn(1, bitmap.height - y)
            val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
            if (cropped != bitmap) bitmap.recycle()
            val bytes = encode(cropped, format.toCompressFormat(), quality)
            writeAndBuild(context, source, cropped, bytes, format)
        }.getOrNull()
    }

    /** Re-encode [source] at an explicit [quality] (the "Quality" tool). */
    suspend fun requality(
        context: Context,
        source: SourceImage,
        format: OutputFormat,
        quality: Int
    ): CompressionResult? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeOriented(context, source.uri) ?: return@runCatching null
            val bytes = encode(bitmap, format.toCompressFormat(), quality)
            writeAndBuild(context, source, bitmap, bytes, format)
        }.getOrNull()
    }

    /** Re-encode [source] to drop EXIF/metadata (the "Delete EXIF" tool). */
    suspend fun stripMetadata(
        context: Context,
        source: SourceImage,
        format: OutputFormat
    ): CompressionResult? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeOriented(context, source.uri) ?: return@runCatching null
            val bytes = encode(bitmap, format.toCompressFormat(), if (format == OutputFormat.PNG) 100 else 95)
            writeAndBuild(context, source, bitmap, bytes, format)
        }.getOrNull()
    }

    /** Apply color/sharpen adjustments and encode (the "Enhance" tool). */
    suspend fun enhance(
        context: Context,
        source: SourceImage,
        brightness: Float, contrast: Float, saturation: Float, sharpness: Float,
        format: OutputFormat,
        quality: Int = 92
    ): CompressionResult? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeOriented(context, source.uri) ?: return@runCatching null
            val out = applyAdjustments(bitmap, brightness, contrast, saturation, sharpness)
            if (out != bitmap) bitmap.recycle()
            val bytes = encode(out, format.toCompressFormat(), quality)
            writeAndBuild(context, source, out, bytes, format)
        }.getOrNull()
    }

    /** Apply brightness/contrast/saturation (ColorMatrix) then optional sharpen. */
    fun applyAdjustments(
        src: Bitmap,
        brightness: Float, contrast: Float, saturation: Float, sharpness: Float
    ): Bitmap {
        val cm = ColorMatrix().apply { setSaturation(saturation) }
        val c = contrast
        val t = (1f - c) * 128f + brightness * 255f
        val contrastMatrix = ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c, 0f, t,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(contrastMatrix)
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return if (sharpness > 0f) sharpen(result, sharpness) else result
    }

    /** Simple unsharp 3x3 convolution. */
    private fun sharpen(src: Bitmap, amount: Float): Bitmap {
        val w = src.width; val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        val out = IntArray(w * h)
        val k = amount.coerceIn(0f, 1f)
        val center = 1f + 4f * k
        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) { out[i] = pixels[i]; continue }
                var r = 0f; var g = 0f; var b = 0f
                fun add(px: Int, wt: Float) {
                    r += ((px shr 16) and 0xFF) * wt
                    g += ((px shr 8) and 0xFF) * wt
                    b += (px and 0xFF) * wt
                }
                add(pixels[i], center)
                add(pixels[i - 1], -k); add(pixels[i + 1], -k)
                add(pixels[i - w], -k); add(pixels[i + w], -k)
                val a = pixels[i] and 0xFF000000.toInt()
                out[i] = a or (r.toInt().coerceIn(0, 255) shl 16) or
                    (g.toInt().coerceIn(0, 255) shl 8) or b.toInt().coerceIn(0, 255)
            }
        }
        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        src.recycle()
        return result
    }

    /** Decode a bitmap directly (oriented), for previews. */
    suspend fun decodeBitmap(context: Context, uri: Uri): Bitmap? =
        withContext(Dispatchers.IO) { decodeOriented(context, uri) }

    /** Copy a cached output into the device gallery (MediaStore). Returns the public Uri. */
    suspend fun saveToGallery(context: Context, cacheFileName: String, format: OutputFormat): Uri? =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(File(context.cacheDir, "shared"), cacheFileName)
                if (!file.exists()) return@runCatching null
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, cacheFileName)
                    put(MediaStore.Images.Media.MIME_TYPE, format.mime)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PicLite")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                val item = resolver.insert(collection, values) ?: return@runCatching null
                resolver.openOutputStream(item)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(item, values, null, null)
                }
                item
            }.getOrNull()
        }

    /** Copy an arbitrary output [file] into the device gallery. */
    suspend fun saveFileToGallery(context: Context, file: File, format: OutputFormat): Uri? =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!file.exists()) return@runCatching null
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                    put(MediaStore.Images.Media.MIME_TYPE, format.mime)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PicLite")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val item = resolver.insert(collection, values) ?: return@runCatching null
                resolver.openOutputStream(item)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(item, values, null, null)
                }
                item
            }.getOrNull()
        }

    /** Build a FileProvider content Uri suitable for sharing. */
    fun shareableUri(context: Context, cacheFileName: String): Uri {
        val file = File(File(context.cacheDir, "shared"), cacheFileName)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun OutputFormat.toCompressFormat(): Bitmap.CompressFormat = when (this) {
        OutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
        OutputFormat.PNG -> Bitmap.CompressFormat.PNG
        OutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
    }
}
