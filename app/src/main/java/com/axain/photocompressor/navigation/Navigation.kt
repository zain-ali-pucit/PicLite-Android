package com.axain.photocompressor.navigation

import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.axain.photocompressor.ads.InterstitialAdManager
import com.axain.photocompressor.domain.ImageEngine
import com.axain.photocompressor.domain.LocalHistoryStore
import com.axain.photocompressor.ui.components.BottomBar
import com.axain.photocompressor.ui.components.HistoryDetailDialog
import com.axain.photocompressor.ui.screens.BatchScreen
import com.axain.photocompressor.ui.screens.ChooseToolScreen
import com.axain.photocompressor.ui.screens.CompressScreen
import com.axain.photocompressor.ui.screens.ConvertScreen
import com.axain.photocompressor.ui.screens.CropScreen
import com.axain.photocompressor.ui.screens.DeleteExifScreen
import com.axain.photocompressor.ui.screens.EnhanceScreen
import com.axain.photocompressor.ui.screens.FavoritesScreen
import com.axain.photocompressor.ui.screens.HistoryScreen
import com.axain.photocompressor.ui.screens.HomeScreen
import com.axain.photocompressor.ui.screens.PaywallScreen
import com.axain.photocompressor.ui.screens.SettingsScreen
import com.axain.photocompressor.ui.screens.LibraryScreen
import com.axain.photocompressor.ui.screens.QualityScreen
import com.axain.photocompressor.ui.screens.ResizeScreen
import kotlinx.coroutines.launch
import java.io.File

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val FAVORITES = "favorites"
    const val COMPRESS = "compress"
    const val RESIZE = "resize"
    const val CROP = "crop"
    const val CONVERT = "convert"
    const val QUALITY = "quality"
    const val BATCH = "batch"
    const val ENHANCE = "enhance"
    const val DELETE_EXIF = "delete_exif"
    const val CHOOSE_TOOL = "choose_tool"
    const val SETTINGS = "settings"
    const val PAYWALL = "paywall"
}

private val tabRoutes = setOf(Routes.HOME, Routes.LIBRARY, Routes.HISTORY, Routes.FAVORITES)

@Composable
fun AppNavHost(dark: Boolean) {
    val nav = rememberNavController()
    val store = LocalHistoryStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showSourceDialog by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val libraryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(com.axain.photocompressor.billing.ProManager.photoLimit())
    ) { uris ->
        if (uris.isNotEmpty()) scope.launch {
            val sources = uris.mapNotNull { ImageEngine.readSource(context, it) }
            if (sources.isNotEmpty()) { store.handoff = sources; nav.navigate(Routes.CHOOSE_TOOL) }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) scope.launch {
            val src = ImageEngine.readSource(context, uri)
            if (src != null) { store.handoff = listOf(src); nav.navigate(Routes.CHOOSE_TOOL) }
        }
    }
    fun launchLibrary() =
        libraryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun launchCamera() {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }
    val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    // When the user finishes a tool, show an interstitial (frequency-capped) before returning.
    fun popWithAd() {
        val activity = context as? Activity
        if (activity != null) InterstitialAdManager.showThenContinue(activity) { nav.popBackStack() }
        else nav.popBackStack()
    }

    val enter = { slideInHorizontally(tween(320)) { it / 6 } + fadeIn(tween(320)) }
    val exit = { slideOutHorizontally(tween(280)) { -it / 8 } + fadeOut(tween(200)) }
    val popEnter = { slideInHorizontally(tween(320)) { -it / 6 } + fadeIn(tween(320)) }
    val popExit = { slideOutHorizontally(tween(280)) { it / 8 } + fadeOut(tween(200)) }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            enterTransition = { enter() }, exitTransition = { exit() },
            popEnterTransition = { popEnter() }, popExitTransition = { popExit() }
        ) {
            composable(Routes.HOME) {
                HomeScreen(dark,
                    onOpen = { r -> nav.navigate(r) },
                    onPlus = { showSourceDialog = true },
                    onSeeAll = { nav.switchTab(Routes.HISTORY) },
                    onSettings = { nav.navigate(Routes.SETTINGS) })
            }
            composable(Routes.LIBRARY) { LibraryScreen(dark) }
            composable(Routes.HISTORY) { HistoryScreen(dark) }
            composable(Routes.FAVORITES) { FavoritesScreen(dark) }

            composable(Routes.COMPRESS) { CompressScreen(dark, { popWithAd() }) }
            composable(Routes.RESIZE) { ResizeScreen(dark, { popWithAd() }) }
            composable(Routes.CROP) { CropScreen(dark, { popWithAd() }) }
            composable(Routes.CONVERT) { ConvertScreen(dark, { popWithAd() }) }
            composable(Routes.QUALITY) { QualityScreen(dark, { popWithAd() }) }
            composable(Routes.BATCH) { BatchScreen(dark, { popWithAd() }) }
            composable(Routes.ENHANCE) { EnhanceScreen(dark, { popWithAd() }) }
            composable(Routes.DELETE_EXIF) { DeleteExifScreen(dark, { popWithAd() }) }
            composable(Routes.CHOOSE_TOOL) {
                ChooseToolScreen(dark, { nav.popBackStack() }, onOpen = { r -> nav.navigate(r) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(dark, { nav.popBackStack() }, onUpgrade = { nav.navigate(Routes.PAYWALL) })
            }
            composable(Routes.PAYWALL) { PaywallScreen(dark, { nav.popBackStack() }) }
        }

        if (route in tabRoutes) {
            BottomBar(
                dark = dark, current = route,
                onSelect = { nav.switchTab(it) },
                onPlus = { showSourceDialog = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        store.detail?.let { entry ->
            HistoryDetailDialog(entry, onDismiss = { store.detail = null })
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Add photos") },
            text = { Text("Take a new photo or choose from your library.") },
            confirmButton = {
                if (hasCamera) TextButton(onClick = { showSourceDialog = false; launchCamera() }) { Text("Take Photo") }
            },
            dismissButton = {
                TextButton(onClick = { showSourceDialog = false; launchLibrary() }) { Text("Choose from Library") }
            }
        )
    }
}

private fun androidx.navigation.NavController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
