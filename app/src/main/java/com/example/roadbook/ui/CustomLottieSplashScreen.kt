package com.example.roadbook.ui

import androidx.compose.runtime.*
import com.airbnb.lottie.compose.*
import com.example.roadbook.R

@Composable
fun CustomLottieSplashScreen(onAnimationComplete: () -> Unit) {
    // Ładowanie animacji z Twojego pliku w res/raw
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.galka_pga_anim))
    val progress by animateLottieCompositionAsState(
        composition,
        isPlaying = true,
        restartOnPlay = false
    )

    // Gdy animacja osiągnie 100% (1f), przejdź dalej
    LaunchedEffect(progress) {
        if (progress == 1f) {
            onAnimationComplete()
        }
    }

    LottieAnimation(
        composition = composition,
        progress = { progress }
    )
}