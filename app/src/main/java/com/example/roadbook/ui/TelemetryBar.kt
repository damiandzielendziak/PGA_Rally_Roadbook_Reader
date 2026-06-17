package com.example.roadbook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

import com.example.roadbook.ui.theme.RallyBold

@Composable
fun TelemetryBar(
    modifier: Modifier = Modifier,
    currentSpeed: Float,
    // Parametr licznika zaliczonych waypointów
    validatedWaypointsCount: Int
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Speed Display (Lewa strona) - Obniżone o 4.dp dla idealnej symetrii w pionie
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.offset(y = 8.dp)
        ) {
            Text(
                text = "${currentSpeed.roundToInt()}",
                color = Color.White,
                fontSize = 55.sp,
                fontFamily = RallyBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "km/h",
                color = Color.Gray,
                fontSize = 16.sp,
                fontFamily = RallyBold,
                modifier = Modifier.padding(bottom = 10.dp) // Zachowane ciasne dopasowanie jednostki
            )
        }

        // WP Counter Display (Prawa strona) - Obniżone o 4.dp dla idealnej symetrii w pionie
        Text(
            text = "WP: $validatedWaypointsCount",
            color = Color(0xFFFFEB3B), // Żółty rajdowy zachowany dla szybkiej weryfikacji
            fontSize = 42.sp,
            fontFamily = RallyBold,
            modifier = Modifier.offset(y = 6.dp)
        )
    }
}