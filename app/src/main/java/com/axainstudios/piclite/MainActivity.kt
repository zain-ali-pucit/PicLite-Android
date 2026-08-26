package com.axainstudios.piclite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.axainstudios.piclite.ads.InterstitialAdManager
import com.axainstudios.piclite.domain.HistoryStore
import com.axainstudios.piclite.domain.LocalHistoryStore
import com.axainstudios.piclite.navigation.AppNavHost
import com.axainstudios.piclite.ui.screens.SplashScreen
import com.axainstudios.piclite.ui.theme.PhotoCompressorTheme
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize billing (Pro entitlement) and the Google Mobile Ads SDK.
        com.axainstudios.piclite.billing.ProManager.init(this)
        MobileAds.initialize(this) {
            InterstitialAdManager.preload(this)
        }
        setContent {
            val dark = isSystemInDarkTheme()
            val appContext = LocalContext.current.applicationContext
            val store = remember { HistoryStore(appContext) }
            var showSplash by remember { mutableStateOf(true) }
            PhotoCompressorTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalHistoryStore provides store) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize()) {
                            AppNavHost(dark = dark)
                            AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(450))) {
                                SplashScreen(dark) { showSplash = false }
                            }
                        }
                    }
                }
            }
        }
    }
}
