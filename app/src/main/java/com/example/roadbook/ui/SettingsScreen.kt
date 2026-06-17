package com.example.roadbook.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.roadbook.viewmodel.RoadbookViewModel

@Composable
fun SettingsScreen(viewModel: RoadbookViewModel, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Ustawienia", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Tap to Scroll")
            Switch(
                checked = viewModel.tapsEnabled.value,
                onCheckedChange = { viewModel.tapsEnabled.value = it }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onBack) { Text("Wróć") }
    }
}