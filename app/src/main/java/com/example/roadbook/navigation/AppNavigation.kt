package com.example.roadbook.navigation

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

    NavHost(navController = navController, startDestination = "home") {

        // --- 1. EKRAN GŁÓWNY (HOME) ---
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToRoadbook = {
                    navController.navigate("stage_selection")
                },
                // USUNIĘTO onNavigateToSettings - HomeScreen obsługuje to teraz sam!
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