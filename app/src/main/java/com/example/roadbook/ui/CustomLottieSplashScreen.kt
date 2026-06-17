package com.example.roadbook.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.*
import com.example.roadbook.R

@Composable
fun CustomLottieSplashScreen(onAnimationComplete: () -> Unit) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.galka_pga_anim))

    val progress by animateLottieCompositionAsState(
        composition = composition,
        isPlaying = true,
        restartOnPlay = false
    )

    // Poprawka: używamy progowej wartości (np. 0.95f),
    // aby mieć pewność, że callback zadziała nawet przy błędach zaokrągleń
    LaunchedEffect(progress) {
        if (progress >= 0.95f) {
            onAnimationComplete()
        }
    }

    LottieAnimation(
        composition = composition,
        progress = { progress },
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )

}