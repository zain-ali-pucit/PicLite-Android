package com.axainstudios.piclite.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axainstudios.piclite.billing.ProManager
import com.axainstudios.piclite.ui.components.GradientButton
import com.axainstudios.piclite.ui.theme.Violet
import com.axainstudios.piclite.ui.theme.rememberBrandGradients

private data class Feature(val icon: ImageVector, val text: String)

private val features = listOf(
    Feature(Icons.Rounded.AllInclusive, "Unlimited photos per batch"),
    Feature(Icons.Rounded.HideImage, "No ads, ever"),
    Feature(Icons.Rounded.Tune, "Custom compression"),
    Feature(Icons.Rounded.Layers, "Batch processing"),
    Feature(Icons.Rounded.AutoAwesome, "Original quality preservation"),
    Feature(Icons.Rounded.SwapHoriz, "HEIC ↔ JPG / PNG"),
)

private fun Context.findActivity(): Activity? {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    return null
}

@Composable
fun PaywallScreen(dark: Boolean, onClose: () -> Unit) {
    val g = rememberBrandGradients(dark)
    val context = LocalContext.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isPro = ProManager.isPro

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Hero header
            Box(Modifier.fillMaxWidth().background(g.hero).padding(top = statusTop + 20.dp, bottom = 28.dp)) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.WorkspacePremium, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    Text("PicLite Pro", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Everything unlocked, one-time.", fontSize = 14.sp, color = Color.White.copy(alpha = 0.9f))
                }
                Box(
                    Modifier.align(Alignment.TopEnd).padding(top = statusTop, end = 12.dp)
                        .size(30.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White,
                        modifier = Modifier.size(16.dp))
                }
            }

            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                features.forEach { f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(f.icon, contentDescription = null, tint = Violet, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Text(f.text, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.Check, contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Column(
                Modifier.padding(horizontal = 24.dp).padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPro) {
                    Text("You're Pro. Thank you! 💜", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary)
                    GradientButton("Done", g.hero, Modifier.fillMaxWidth(), onClick = onClose)
                } else {
                    GradientButton(
                        text = "Unlock Pro — ${ProManager.priceText}",
                        gradient = g.hero, modifier = Modifier.fillMaxWidth(),
                        leadingIcon = Icons.Rounded.WorkspacePremium,
                        onClick = {
                            val activity = context.findActivity()
                            if (ProManager.productDetails == null || activity == null) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Purchases are unavailable right now. Make sure you’re signed in to Google Play and try again.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            } else {
                                ProManager.purchase(activity)
                            }
                        }
                    )
                    Text("Restore Purchase", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            ProManager.restore()
                            android.widget.Toast.makeText(context, "Restoring…", android.widget.Toast.LENGTH_SHORT).show()
                        })
                }
                Text("One-time purchase. No subscription.", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fun open(url: String) = runCatching {
                        context.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    Text("Terms of Use", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { open("https://piclite.axainstudios.com/terms.html") })
                    Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Privacy Policy", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { open("https://piclite.axainstudios.com/privacy.html") })
                }
            }
        }
    }
}
