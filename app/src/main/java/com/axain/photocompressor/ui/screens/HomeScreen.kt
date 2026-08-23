package com.axain.photocompressor.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.axain.photocompressor.R
import com.axain.photocompressor.domain.HistoryEntry
import com.axain.photocompressor.domain.LocalHistoryStore
import com.axain.photocompressor.billing.ProManager
import com.axain.photocompressor.domain.formatBytes
import com.axain.photocompressor.navigation.Routes
import com.axain.photocompressor.ui.theme.Amber
import com.axain.photocompressor.ui.theme.Sky
import com.axain.photocompressor.ui.theme.Violet
import com.axain.photocompressor.ui.theme.rememberBrandGradients

@Composable
fun HomeScreen(
    dark: Boolean,
    onOpen: (String) -> Unit,
    onPlus: () -> Unit,
    onSeeAll: () -> Unit,
    onSettings: () -> Unit
) {
    val g = rememberBrandGradients(dark)
    val store = LocalHistoryStore.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(g.heroSoft)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = statusTop + 8.dp, bottom = 110.dp + navBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Brand()
            HeroCard(dark, onPlus)
            ToolGrid(onOpen)
            if (store.entries.isNotEmpty()) RecentSection(onSeeAll)
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = statusTop + 6.dp, end = 20.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun Brand() {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PLogoMark(52.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Row {
                val big = 34.sp
                Text("PicL", fontSize = big, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Box {
                    Text("ı", fontSize = big, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground)
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 7.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Violet)
                    )
                }
                Text("te", fontSize = big, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Make your photos lighter",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PLogoMark(size: Dp) {
    Image(
        painter = painterResource(R.mipmap.ic_launcher),
        contentDescription = "PicLite",
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.29f))
    )
}

@Composable
private fun HeroCard(dark: Boolean, onPlus: () -> Unit) {
    val g = rememberBrandGradients(dark)
    Box(
        Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(g.hero)
            .clickable(onClick = onPlus),
        contentAlignment = Alignment.Center
    ) {
        Sparkle(15.dp, Modifier.offset(x = (-120).dp, y = (-24).dp))
        Sparkle(10.dp, Modifier.offset(x = (-138).dp, y = 22.dp))
        Sparkle(12.dp, Modifier.offset(x = 124.dp, y = (-6).dp))
        Sparkle(9.dp, Modifier.offset(x = 140.dp, y = 30.dp))

        Box(Modifier.offset(y = (-8).dp), contentAlignment = Alignment.Center) {
            PhotoCardGlyph(60.dp, Modifier.offset(x = (-38).dp, y = 6.dp).rotate(-13f))
            PhotoCardGlyph(60.dp, Modifier.offset(x = 38.dp, y = 6.dp).rotate(13f))
            PhotoCardGlyph(70.dp)
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Add photos", tint = Violet,
                modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun Sparkle(size: Dp, modifier: Modifier) {
    Icon(
        Icons.Rounded.AutoAwesome, contentDescription = null,
        tint = Color.White.copy(alpha = 0.85f),
        modifier = modifier.size(size)
    )
}

@Composable
fun PhotoCardGlyph(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.16f))
            .background(Color.White.copy(alpha = 0.95f))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width; val h = this.size.height
            drawCircle(color = Violet.copy(alpha = 0.85f), radius = w * 0.08f,
                center = Offset(w * 0.34f, h * 0.34f))
            val path = Path().apply {
                moveTo(0f, h)
                lineTo(w * 0.30f, h * 0.45f)
                lineTo(w * 0.50f, h * 0.63f)
                lineTo(w * 0.68f, h * 0.32f)
                lineTo(w, h * 0.62f)
                lineTo(w, h)
                close()
            }
            drawPath(path, Brush.verticalGradient(listOf(Violet, Sky)))
        }
    }
}

val proOnlyRoutes = setOf(Routes.BATCH, Routes.QUALITY)

@Composable
private fun ToolGrid(onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        allTools.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { tool ->
                    val locked = tool.route in proOnlyRoutes && !ProManager.isPro
                    ToolTile(tool, Modifier.weight(1f), locked = locked) {
                        onOpen(if (locked) Routes.PAYWALL else tool.route)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun ToolTile(tool: ToolInfo, modifier: Modifier = Modifier, locked: Boolean = false, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(tool.color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(tool.icon, contentDescription = null, tint = tool.color, modifier = Modifier.size(24.dp))
            }
            if (locked) {
                Box(
                    Modifier.size(18.dp).clip(CircleShape).background(Amber)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Pro", tint = Color.White,
                        modifier = Modifier.size(9.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(tool.title, fontSize = 13.sp, maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RecentSection(onSeeAll: () -> Unit) {
    val store = LocalHistoryStore.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            Text("See All", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Violet,
                modifier = Modifier.clickable(onClick = onSeeAll))
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(store.entries.take(10)) { entry -> RecentThumb(entry) }
        }
    }
}

@Composable
private fun RecentThumb(entry: HistoryEntry) {
    val store = LocalHistoryStore.current
    Box(
        Modifier
            .size(104.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { store.detail = entry }
    ) {
        AsyncImage(
            model = store.fileFor(entry),
            contentDescription = entry.name,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                "${formatBytes(entry.originalBytes)} → ${formatBytes(entry.outputBytes)}",
                fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.White
            )
        }
    }
}
