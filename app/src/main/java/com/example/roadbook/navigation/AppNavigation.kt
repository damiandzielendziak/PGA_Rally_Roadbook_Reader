package com.example.roadbook.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.example.roadbook.model.ScrollDirection
import com.example.roadbook.viewmodel.RoadbookViewModel
import com.example.roadbook.ui.HomeScreen
import com.example.roadbook.ui.MainApplicationScreen
import com.example.roadbook.ui.CustomLottieSplashScreen

@Composable
fun AppNavigation(
    viewModel: RoadbookViewModel,
    navController: NavHostController = rememberNavController(),
    scrollSignal: Pair<ScrollDirection, Long>,
    onGenerateSignal: (ScrollDirection) -> Unit,
    onForceScreenRotation: () -> Unit,
    onExitApp: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        // Deklaracje wyłączające animacje:
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        composable("splash") {
            CustomLottieSplashScreen(
                onAnimationComplete = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToRoadbook = { navController.navigate("roadbook") },
                onNavigateToSettings = { navController.navigate("settings") },
                onExit = { onExitApp() }
            )
        }

        composable("roadbook") {
            MainApplicationScreen(
                viewModel = viewModel,
                scrollSignal = scrollSignal,
                onGenerateSignal = onGenerateSignal,
                onForceScreenRotation = onForceScreenRotation,
                onCancelNavigation = {
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }

        // Dodałem to, żeby kompilator wiedział, gdzie nawigować po kliknięciu "Settings"
        composable("settings") {
            // Tutaj wywołaj swój ekran ustawień, np:
            // SettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}