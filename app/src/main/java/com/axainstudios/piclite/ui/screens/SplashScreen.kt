package com.axainstudios.piclite.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.axainstudios.piclite.R
import com.axainstudios.piclite.ui.theme.rememberBrandGradients
import kotlinx.coroutines.delay
import androidx.compose.material3.Text

/** An elegant launch animation: the logo springs in with a soft glow, then the wordmark rises. */
@Composable
fun SplashScreen(dark: Boolean, onFinished: () -> Unit) {
    val g = rememberBrandGradients(dark)
    var start by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (start) 1f else 0.68f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow), label = "scale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f, animationSpec = tween(600), label = "logoAlpha"
    )
    val wordAlpha by animateFloatAsState(
        targetValue = if (start) 1f else 0f, animationSpec = tween(500, delayMillis = 350), label = "wordAlpha"
    )
    val wordOffset by animateFloatAsState(
        targetValue = if (start) 0f else 14f, animationSpec = tween(500, delayMillis = 350), label = "wordOffset"
    )
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.72f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow"
    )

    LaunchedEffect(Unit) { start = true; delay(1800); onFinished() }

    Box(Modifier.fillMaxSize().background(g.hero), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(300.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = logoAlpha }
                .blur(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_fg),
                contentDescription = "PicLite",
                modifier = Modifier
                    .size(112.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; alpha = logoAlpha }
                    .clip(RoundedCornerShape(30.dp))
            )
            Text(
                "PicLite", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.graphicsLayer { alpha = wordAlpha; translationY = wordOffset * density }
            )
        }
    }
}
