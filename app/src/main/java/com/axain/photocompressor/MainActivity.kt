package com.axain.photocompressor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.axain.photocompressor.ads.InterstitialAdManager
import com.axain.photocompressor.domain.HistoryStore
import com.axain.photocompressor.domain.LocalHistoryStore
import com.axain.photocompressor.navigation.AppNavHost
import com.axain.photocompressor.ui.theme.PhotoCompressorTheme
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize the Google Mobile Ads SDK, then preload the first interstitial.
        MobileAds.initialize(this) {
            InterstitialAdManager.preload(this)
        }
        setContent {
            val dark = isSystemInDarkTheme()
            val appContext = LocalContext.current.applicationContext
            val store = remember { HistoryStore(appContext) }
            PhotoCompressorTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalHistoryStore provides store) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNavHost(dark = dark)
                    }
                }
            }
        }
    }
}
