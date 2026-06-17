package com.example.roadbook.ui

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// POPRAWIONY IMPORT: Skierowany na główny pakiet zgodnie z RoadbookRow i MainActivity
import com.example.roadbook.ui.theme.RallyBold

@Composable
fun MockNavigationPanel(
    isMockEnabled: Boolean,
    onToggleMock: (Boolean) -> Unit,
    mockLat: String,
    onLatChange: (String) -> Unit,
    mockLon: String,
    onLonChange: (String) -> Unit,
    mockCap: String,
    onCapChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEEEEEE))
            .border(2.dp, Color.Black)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GPS SYMULATOR (DEBUG)",
                fontFamily = RallyBold,
                fontSize = 18.sp,
                color = if (isMockEnabled) Color(0xFFFF5722) else Color.Gray
            )
            Switch(
                checked = isMockEnabled,
                onCheckedChange = onToggleMock,
                colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFFFF5722))
            )
        }

        if (isMockEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = mockLat,
                    onValueChange = onLatChange,
                    label = { Text("Latitude (N/S)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = mockLon,
                    onValueChange = onLonChange,
                    label = { Text("Longitude (E/W)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = mockCap,
                    onValueChange = onCapChange,
                    label = { Text("CAP (°)") },
                    modifier = Modifier.weight(0.8f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            Text(
                text = "Zmień dane, by natychmiast zobaczyć reakcję strzałki",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
    }
}