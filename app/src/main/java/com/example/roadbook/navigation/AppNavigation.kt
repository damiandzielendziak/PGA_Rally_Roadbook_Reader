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
import com.example.roadbook.ui.SettingsScreen
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
        // Globalne wyłączenie animacji dla NavHost
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(
            route = "splash",
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            CustomLottieSplashScreen(
                onAnimationComplete = {
                    // Używamy launchSingleTop, aby nie tworzyć stosu ekranów
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "home",
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToRoadbook = { navController.navigate("roadbook") },
                onNavigateToSettings = { navController.navigate("settings") },
                onExit = onExitApp
            )
        }

        composable(
            route = "settings",
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onDismissRequest = { navController.popBackStack() }
            )
        }

        composable(
            route = "roadbook",
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None }
        ) {
            MainApplicationScreen(
                viewModel = viewModel,
                scrollSignal = scrollSignal,
                onGenerateSignal = onGenerateSignal,
                onForceScreenRotation = onForceScreenRotation,
                onCancelNavigation = { navController.popBackStack("home", inclusive = false) }
            )
        }
    }
}