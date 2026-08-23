package com.axain.photocompressor.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import com.axain.photocompressor.R
import com.axain.photocompressor.ui.components.ScreenScaffold
import com.axain.photocompressor.ui.theme.Amber
import com.axain.photocompressor.ui.theme.Mint
import com.axain.photocompressor.ui.theme.Orchid
import com.axain.photocompressor.ui.theme.Rose
import com.axain.photocompressor.ui.theme.Sky
import com.axain.photocompressor.ui.theme.Violet
import com.axain.photocompressor.ui.theme.rememberBrandGradients

/** External links + contact. Replace the placeholder URLs with your real ones. */
private object AppLinks {
    const val PRIVACY = "https://piclite.axainstudios.com/privacy.html"
    const val TERMS = "https://piclite.axainstudios.com/terms.html"
    const val PLAY_STORE = "https://play.google.com/store/apps/details?id=com.axain.photocompressor"
    const val SUPPORT = "mailto:axainstudios@gmail.com"
    const val WHATSAPP = "https://wa.me/923225582024"
}

@Composable
fun SettingsScreen(dark: Boolean, onBack: () -> Unit, onUpgrade: () -> Unit = {}) {
    val g = rememberBrandGradients(dark)
    val context = LocalContext.current

    val version = remember {
        runCatching {
            val pi = context.packageManager.getPackageInfo(context.packageName, 0)
            "Version ${pi.versionName} (${PackageInfoCompat.getLongVersionCode(pi)})"
        }.getOrDefault("Version 1.0")
    }

    fun open(url: String) = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    fun shareApp() {
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Make your photos lighter with PicLite: ${AppLinks.PLAY_STORE}")
        }
        context.startActivity(Intent.createChooser(i, "Share PicLite").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    ScreenScaffold("Settings", "PicLite", g.heroSoft, onBack) { topInset ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = topInset, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header() }
            item {
                SettingsGroup {
                    if (com.axain.photocompressor.billing.ProManager.isPro) {
                        SettingsRow(Icons.Rounded.WorkspacePremium, Amber, "PicLite Pro · Active") {}
                    } else {
                        SettingsRow(Icons.Rounded.WorkspacePremium, Amber, "Upgrade to Pro") { onUpgrade() }
                    }
                    Divider()
                    SettingsRow(Icons.Rounded.Star, Violet, "Rate PicLite") { open(AppLinks.PLAY_STORE) }
                    Divider()
                    SettingsRow(Icons.Rounded.Share, Sky, "Share PicLite") { shareApp() }
                }
            }
            item {
                SettingsGroup {
                    SettingsRow(Icons.Rounded.PrivacyTip, Mint, "Privacy Policy") { open(AppLinks.PRIVACY) }
                    Divider()
                    SettingsRow(Icons.Rounded.Description, Orchid, "Terms of Use") { open(AppLinks.TERMS) }
                    Divider()
                    SettingsRow(Icons.Rounded.Email, Rose, "Contact Support") { open(AppLinks.SUPPORT) }
                    Divider()
                    SettingsRow(Icons.Rounded.Chat, Mint, "WhatsApp") { open(AppLinks.WHATSAPP) }
                }
            }
            item {
                Text(version, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.mipmap.ic_launcher), contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)))
        Spacer(Modifier.width(14.dp))
        Column {
            Text("PicLite", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Text("Make your photos lighter", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
    ) { content() }
}

@Composable
private fun SettingsRow(icon: ImageVector, tint: Color, title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 60.dp)
        .background(MaterialTheme.colorScheme.outline))
}
