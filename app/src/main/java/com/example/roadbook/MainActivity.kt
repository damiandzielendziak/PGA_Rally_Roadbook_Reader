package com.example.roadbook

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Importy modeli i logiki
import com.example.roadbook.model.ScrollDirection
import com.example.roadbook.viewmodel.RoadbookViewModel
import com.example.roadbook.ui.ControllerManager
import com.example.roadbook.navigation.AppNavigation

class MainActivity : ComponentActivity() {

    private val viewModel: RoadbookViewModel by viewModels()
    // Stan przewijania zostaje tutaj, bo MainActivity odbiera zdarzenia sprzętowe z klawiatury/kontrolera
    private var scrollSignal by mutableStateOf(Pair(ScrollDirection.NONE, 0L))
    private lateinit var controllerManager: ControllerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Prawidłowa inicjalizacja Splash Screena
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Ekran zawsze włączony
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Tryb pełnoekranowy (Edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        viewModel.initializeSettings(this)

        // Inicjalizacja kontrolera sprzętowego
        controllerManager = ControllerManager(
            viewModel = viewModel,
            onScrollRequest = { direction -> scrollSignal = Pair(direction, System.currentTimeMillis()) },
            onActionHub = { executeControllerActionHub() }
        )

        // Ładowanie trasy w tle
        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.loadGpxData(this@MainActivity)
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {

                    val context = LocalContext.current
                    val activity = context as? ComponentActivity

                    // Reagowanie na zmiany orientacji ekranu z ViewModelu
                    LaunchedEffect(viewModel.isLandscapeOrientation.value) {
                        activity?.requestedOrientation = if (viewModel.isLandscapeOrientation.value) {
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                        } else {
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                    }

                    // Bezpieczna obsługa uprawnień GPS (Android 12+)
                    val permissionLauncher = rememberLauncherForActivityResult<Array<String>, Map<String, Boolean>>(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { results: Map<String, Boolean> ->
                        val isFineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                        val isCoarseGranted = results[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

                        if (isFineGranted || isCoarseGranted) {
                            if (!viewModel.isPreviewMode.value) {
                                viewModel.startLocationUpdates(context)
                            }
                        } else {
                            viewModel.gpsSignalQuality.value = "Brak uprawnień GPS"
                        }
                    }

                    // Pierwsze akcje przy starcie UI
                    LaunchedEffect(Unit) {
                        viewModel.updateBatteryStatus(context)
                        viewModel.checkConnectedControllers()
                        // Wywołanie pytania o OBA uprawnienia
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }

                    // Skalowanie interfejsu (Zoom UI)
                    val currentDensity = LocalDensity.current
                    val scaleFactor = viewModel.uiScale.value * 0.85f

                    CompositionLocalProvider(
                        LocalDensity provides Density(
                            density = currentDensity.density * scaleFactor,
                            fontScale = 1.0f * scaleFactor
                        )
                    ) {
                        // Nawigacja
                        AppNavigation(
                            viewModel = viewModel,
                            scrollSignal = scrollSignal,
                            onGenerateSignal = { direction ->
                                scrollSignal = Pair(direction, System.currentTimeMillis())
                            },
                            onForceScreenRotation = {
                                viewModel.isLandscapeOrientation.value = !viewModel.isLandscapeOrientation.value
                            },
                            onExitApp = { finish() }
                        )
                    }
                }
            }
        }
    }

    // Przechwytywanie fizycznych przycisków
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return window.superDispatchKeyEvent(event)

        if (controllerManager.handleKeyEvent(event)) {
            return true
        }
        return window.superDispatchKeyEvent(event)
    }

    // Logika podłączona do przycisku akcji kontrolera
    fun executeControllerActionHub() {
        val context = this
        when {
            viewModel.showStartupDialog.value -> {
                if (viewModel.isNavigationStarted.value) {
                    viewModel.resumeNavigation()
                } else {
                    viewModel.confirmStart()
                }
                viewModel.startLocationUpdates(context)
            }
            viewModel.isPreviewMode.value -> {
                viewModel.isPreviewMode.value = false
                viewModel.showStartupDialog.value = true
                viewModel.startLocationUpdates(context)
            }
            else -> {
                viewModel.stopLocationUpdates()
                viewModel.showStartupDialog.value = true
            }
        }
    }
}