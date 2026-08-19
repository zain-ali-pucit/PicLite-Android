package com.axain.photocompressor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.axain.photocompressor.navigation.AppNavHost
import com.axain.photocompressor.ui.theme.PhotoCompressorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            PhotoCompressorTheme(darkTheme = dark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavHost(dark = dark)
                }
            }
        }
    }
}
