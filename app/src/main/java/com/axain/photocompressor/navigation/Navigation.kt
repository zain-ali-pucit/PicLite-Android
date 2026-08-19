package com.axain.photocompressor.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.axain.photocompressor.ui.screens.BatchScreen
import com.axain.photocompressor.ui.screens.CompressScreen
import com.axain.photocompressor.ui.screens.ConvertScreen
import com.axain.photocompressor.ui.screens.HomeScreen
import com.axain.photocompressor.ui.screens.ResizeScreen

object Routes {
    const val HOME = "home"
    const val COMPRESS = "compress"
    const val RESIZE = "resize"
    const val CONVERT = "convert"
    const val BATCH = "batch"
}

@Composable
fun AppNavHost(dark: Boolean) {
    val nav = rememberNavController()
    val enter = { slideInHorizontally(tween(320)) { it / 6 } + fadeIn(tween(320)) }
    val exit = { slideOutHorizontally(tween(280)) { -it / 8 } + fadeOut(tween(200)) }
    val popEnter = { slideInHorizontally(tween(320)) { -it / 6 } + fadeIn(tween(320)) }
    val popExit = { slideOutHorizontally(tween(280)) { it / 8 } + fadeOut(tween(200)) }

    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        enterTransition = { enter() },
        exitTransition = { exit() },
        popEnterTransition = { popEnter() },
        popExitTransition = { popExit() }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                dark = dark,
                onOpen = { route -> nav.navigate(route) }
            )
        }
        composable(Routes.COMPRESS) {
            CompressScreen(dark = dark, onBack = { nav.popBackStack() })
        }
        composable(Routes.RESIZE) {
            ResizeScreen(dark = dark, onBack = { nav.popBackStack() })
        }
        composable(Routes.CONVERT) {
            ConvertScreen(dark = dark, onBack = { nav.popBackStack() })
        }
        composable(Routes.BATCH) {
            BatchScreen(dark = dark, onBack = { nav.popBackStack() })
        }
    }
}
