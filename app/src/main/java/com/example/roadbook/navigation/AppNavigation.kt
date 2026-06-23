package com.example.roadbook.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.roadbook.model.ScrollDirection
import com.example.roadbook.ui.HomeScreen
import com.example.roadbook.ui.MainApplicationScreen
import com.example.roadbook.ui.StageSelectionScreen
import com.example.roadbook.viewmodel.RoadbookViewModel

@Composable
fun AppNavigation(
    viewModel: RoadbookViewModel,
    scrollSignal: Pair<ScrollDirection, Long>,
    onGenerateSignal: (ScrollDirection) -> Unit,
    onForceScreenRotation: () -> Unit,
    onExitApp: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home",
        // --- GLOBALNE ANIMACJE: Domyślny ruch z prawej do lewej dla całej aplikacji ---
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400))
        }
    ) {

        // --- 1. EKRAN GŁÓWNY (HOME) ---
        composable(
            route = "home",
            // NADPISANIE: Ekran główny pojawia się płynnie (Fade), ignorując globalny slajd
            enterTransition = {
                fadeIn(animationSpec = tween(800))
            }
        ) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToRoadbook = {
                    navController.navigate("stage_selection")
                },
                onExit = onExitApp
            )
        }

        // --- 2. EKRAN WYBORU ETAPÓW (STAGE SELECTION) ---
        composable("stage_selection") {
            StageSelectionScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                },
                onStageSelected = { selectedStage ->
                    viewModel.selectedStageId.value = selectedStage.id
                    navController.navigate("roadbook")
                }
            )
        }

        // --- 3. EKRAN NAWIGACJI (ROADBOOK) ---
        composable("roadbook") {
            MainApplicationScreen(
                viewModel = viewModel,
                scrollSignal = scrollSignal,
                onGenerateSignal = onGenerateSignal,
                onForceScreenRotation = onForceScreenRotation,
                onCancelNavigation = {
                    navController.popBackStack("stage_selection", inclusive = false)
                }
            )
        }
    }
}