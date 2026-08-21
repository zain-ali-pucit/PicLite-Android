package com.axain.photocompressor.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.axain.photocompressor.domain.CompressionResult
import com.axain.photocompressor.domain.ImageEngine
import com.axain.photocompressor.domain.LocalHistoryStore
import com.axain.photocompressor.domain.OutputFormat
import com.axain.photocompressor.domain.ShareUtils
import com.axain.photocompressor.domain.SourceImage
import com.axain.photocompressor.ui.components.FormatRow
import com.axain.photocompressor.ui.components.GradientButton
import com.axain.photocompressor.ui.components.ResultCard
import com.axain.photocompressor.ui.components.ScreenScaffold
import com.axain.photocompressor.ui.components.SectionLabel
import com.axain.photocompressor.ui.components.SelectPhotosCard
import com.axain.photocompressor.ui.components.SelectableChip
import com.axain.photocompressor.ui.theme.Violet
import com.axain.photocompressor.ui.theme.rememberBrandGradients
import kotlinx.coroutines.launch

private data class Aspect(val label: String, val ratio: Float?)

private val aspects = listOf(
    Aspect("Free", null), Aspect("1:1", 1f), Aspect("4:3", 4f / 3f),
    Aspect("3:4", 3f / 4f), Aspect("16:9", 16f / 9f), Aspect("9:16", 9f / 16f)
)

@Composable
fun CropScreen(dark: Boolean, onBack: () -> Unit) {
    val g = rememberBrandGradients(dark)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = LocalHistoryStore.current

    var source by remember { mutableStateOf<SourceImage?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<CompressionResult?>(null) }
    var format by remember { mutableStateOf(OutputFormat.JPEG) }
    var isProcessing by remember { mutableStateOf(false) }
    var cropN by remember { mutableStateOf(Rect(0.08f, 0.08f, 0.92f, 0.92f)) }
    var ratioR by remember { mutableStateOf<Float?>(null) }

    // Consume handoff (first photo)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (source == null && store.handoff.isNotEmpty()) {
            source = store.handoff.first(); store.handoff = emptyList()
            cropN = Rect(0.08f, 0.08f, 0.92f, 0.92f); ratioR = null; result = null
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scope.launch {
            isLoading = true
            source = ImageEngine.readSource(context, uri)
            cropN = Rect(0.08f, 0.08f, 0.92f, 0.92f); ratioR = null; result = null
            isLoading = false
        }
    }
    fun launchPicker() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    val src = source
    val imgAspect = if (src != null && src.height > 0) src.width.toFloat() / src.height else 1f
    val ratioN = ratioR?.let { it / imgAspect }

    fun applyRatio() {
        val rN = ratioN ?: return
        val nw: Float; val nh: Float
        if (rN >= 1f) { nw = 0.9f; nh = 0.9f / rN } else { nh = 0.9f; nw = 0.9f * rN }
        val x = (1f - nw) / 2f; val y = (1f - nh) / 2f
        cropN = Rect(x, y, x + nw, y + nh)
    }

    ScreenScaffold("Crop", "Trim to the perfect frame", g.heroSoft, onBack) { topInset ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SelectPhotosCard(if (src == null) 0 else 1, isLoading, ::launchPicker) }
            if (src != null) {
                item {
                    CropCanvas(src, src.width, src.height, cropN, { cropN = it }, ratioN)
                }
                item { SectionLabel("Aspect ratio", Modifier.padding(top = 4.dp, start = 2.dp)) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        aspects.forEach { a ->
                            SelectableChip(a.label, ratioR == a.ratio, g.accentC, Modifier.width(76.dp)) {
                                ratioR = a.ratio; result = null; applyRatio()
                            }
                        }
                    }
                }
                item { SectionLabel("Output format", Modifier.padding(top = 4.dp, start = 2.dp)) }
                item { FormatRow(format, g.accentB) { format = it; result = null } }
                item {
                    GradientButton(
                        text = if (isProcessing) "Working…" else "Crop photo",
                        gradient = g.accentC, enabled = !isProcessing, leadingIcon = Icons.Rounded.Crop,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        isProcessing = true
                        val w = src.width; val h = src.height
                        val rect = android.graphics.Rect(
                            (cropN.left * w).toInt(), (cropN.top * h).toInt(),
                            (cropN.right * w).toInt(), (cropN.bottom * h).toInt()
                        )
                        scope.launch {
                            val r = ImageEngine.crop(context, src, rect, format)
                            result = r; isProcessing = false
                            if (r != null) store.record(listOf(r))
                        }
                    }
                }
            }
            result?.let { r ->
                item {
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

@Composable
private fun CropCanvas(
    source: SourceImage, imgW: Int, imgH: Int,
    cropN: Rect, setCropN: (Rect) -> Unit, ratioN: Float?
) {
    val density = LocalDensity.current
    val minS = 0.12f
    BoxWithConstraints(
        Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        val a = imgW.toFloat() / imgH
        val dw: Float; val dh: Float
        if (cw / ch > a) { dh = ch; dw = ch * a } else { dw = cw; dh = cw / a }
        val dx = (cw - dw) / 2f; val dy = (ch - dh) / 2f
        val c = Rect(dx + cropN.left * dw, dy + cropN.top * dh, dx + cropN.right * dw, dy + cropN.bottom * dh)

        AsyncImage(model = source.uri, contentDescription = source.displayName,
            contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())

        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val dim = Color.Black.copy(alpha = 0.4f)
            drawRect(dim, size = Size(size.width, c.top))
            drawRect(dim, topLeft = Offset(0f, c.bottom), size = Size(size.width, size.height - c.bottom))
            drawRect(dim, topLeft = Offset(0f, c.top), size = Size(c.left, c.height))
            drawRect(dim, topLeft = Offset(c.right, c.top), size = Size(size.width - c.right, c.height))
            drawRect(Color.White, topLeft = Offset(c.left, c.top), size = Size(c.width, c.height),
                style = Stroke(width = 2.dp.toPx()))
        }

        val cn by rememberUpdatedState(cropN)

        // Move
        androidx.compose.foundation.layout.Box(
            Modifier
                .offset { IntOffset(c.left.toInt(), c.top.toInt()) }
                .size(with(density) { c.width.toDp() }, with(density) { c.height.toDp() })
                .pointerInput(dw, dh) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val cur = cn
                        val nx = (cur.left + drag.x / dw).coerceIn(0f, 1f - cur.width)
                        val ny = (cur.top + drag.y / dh).coerceIn(0f, 1f - cur.height)
                        setCropN(Rect(nx, ny, nx + cur.width, ny + cur.height))
                    }
                }
        )

        Handle(c.left, c.top, density) { drag ->
            val cur = cn
            var nl = (cur.left + drag.x / dw).coerceIn(0f, cur.right - minS)
            var nt = (cur.top + drag.y / dh).coerceIn(0f, cur.bottom - minS)
            val r = cur.right; val b = cur.bottom
            if (ratioN != null) {
                val w = r - nl; var h = w / ratioN; nt = b - h
                if (nt < 0f) { nt = 0f; h = b; nl = r - h * ratioN }
            }
            setCropN(Rect(nl, nt, r, b))
        }
        Handle(c.right, c.bottom, density) { drag ->
            val cur = cn
            var nr = (cur.right + drag.x / dw).coerceIn(cur.left + minS, 1f)
            var nb = (cur.bottom + drag.y / dh).coerceIn(cur.top + minS, 1f)
            val l = cur.left; val t = cur.top
            if (ratioN != null) {
                val w = nr - l; var h = w / ratioN; nb = t + h
                if (nb > 1f) { nb = 1f; h = 1f - t; nr = l + h * ratioN }
            }
            setCropN(Rect(l, t, nr, nb))
        }
    }
}

@Composable
private fun Handle(px: Float, py: Float, density: androidx.compose.ui.unit.Density, onDrag: (Offset) -> Unit) {
    val half = with(density) { 13.dp.toPx() }
    val drag by rememberUpdatedState(onDrag)
    androidx.compose.foundation.layout.Box(
        Modifier
            .offset { IntOffset((px - half).toInt(), (py - half).toInt()) }
            .size(26.dp).clip(CircleShape).background(Color.White)
            .border(3.dp, Violet, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, d -> change.consume(); drag(d) }
            }
    )
}
