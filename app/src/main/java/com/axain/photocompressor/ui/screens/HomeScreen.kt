package com.axain.photocompressor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import com.axain.photocompressor.navigation.Routes
import com.axain.photocompressor.ui.components.FeatureCard
import com.axain.photocompressor.ui.theme.rememberBrandGradients

private data class Feature(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val gradientPicker: (com.axain.photocompressor.ui.theme.BrandGradients) -> Brush
)

@Composable
fun HomeScreen(dark: Boolean, onOpen: (String) -> Unit) {
    val g = rememberBrandGradients(dark)
    val statusPad = WindowInsets.statusBars.asPaddingValues()

    val features = listOf(
        Feature("Compress Photos", "Shrink to an exact target size", Icons.Rounded.Compress, Routes.COMPRESS) { it.accentA },
        Feature("Resize Photos", "Set new dimensions with ease", Icons.Rounded.Tune, Routes.RESIZE) { it.accentC },
        Feature("Convert Format", "JPEG · PNG · WebP", Icons.Rounded.SwapHoriz, Routes.CONVERT) { it.accentB },
        Feature("Batch Processing", "Many photos, one tap", Icons.Rounded.PhotoLibrary, Routes.BATCH) { it.accentD },
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(g.heroSoft)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp,
                top = statusPad.calculateTopPadding() + 12.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeroHeader(g.hero) }
            item {
                Text(
                    "What would you like to do?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }
            items(features.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { f ->
                        FeatureCard(
                            title = f.title,
                            subtitle = f.subtitle,
                            icon = f.icon,
                            gradient = f.gradientPicker(g),
                            modifier = Modifier.weight(1f),
                            onClick = { onOpen(f.route) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { TipCard(g.hero) }
        }
    }
}

@Composable
private fun HeroHeader(hero: Brush) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(hero)
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Lumina",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Beautiful photos,\nfeather-light files.",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Compress, resize and convert your images without losing the moment.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun TipCard(hero: Brush) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(hero),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                "Smart target sizing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Pick 100 KB and Lumina finds the best quality that fits.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
