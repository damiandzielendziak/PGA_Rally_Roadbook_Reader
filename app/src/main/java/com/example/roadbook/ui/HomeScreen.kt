package com.example.roadbook.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbook.R
import com.example.roadbook.ui.theme.Montserrat
import com.example.roadbook.viewmodel.RoadbookViewModel
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    viewModel: RoadbookViewModel,
    onNavigateToRoadbook: () -> Unit,
    onNavigateToSettings: () -> Unit, // Teraz to wywołuje ekran ustawień
    onExit: () -> Unit
) {
    val gravelBg = Color(0xFFF4F3F2)
    val deepGraphite = Color(0xFF2B2A29)
    val rallyRed = Color(0xFFD73224)

    var scalePulse by remember { mutableStateOf(0.96f) }
    var opacityPulse by remember { mutableStateOf(0.8f) }

    LaunchedEffect(Unit) {
        val steps = 150
        var currentStep = 0
        var goingUp = true
        while (true) {
            delay(16)
            if (goingUp) {
                currentStep++
                if (currentStep >= steps) goingUp = false
            } else {
                currentStep--
                if (currentStep <= 0) goingUp = true
            }
            val t = currentStep.toFloat() / steps
            val smoothT = t * t * (3f - 2f * t)
            scalePulse = 0.96f + (1.04f - 0.96f) * smoothT
            opacityPulse = 0.8f + (1.0f - 0.8f) * smoothT
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gravelBg)
            .padding(vertical = 60.dp, horizontal = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(435.dp).offset(y = (-20).dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(870.dp).scale(scalePulse).background(
                    brush = Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = opacityPulse * 0.95f),
                        0.2f to rallyRed.copy(alpha = 0.04f * opacityPulse),
                        0.6f to gravelBg.copy(alpha = 0f)
                    )
                ))
                Image(painter = painterResource(id = R.drawable.ic_pga_logo), contentDescription = null, modifier = Modifier.fillMaxSize())
                Text("ROADBOOK READER", fontFamily = Montserrat, fontSize = 35.sp, color = rallyRed, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.BottomCenter).offset(y = 30.dp))
            }

            Spacer(modifier = Modifier.height(45.dp))

            Column(modifier = Modifier.widthIn(max = 550.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = onNavigateToRoadbook, shape = RoundedCornerShape(50.dp), colors = ButtonDefaults.buttonColors(containerColor = deepGraphite), contentPadding = PaddingValues(24.dp), modifier = Modifier.fillMaxWidth().border(4.dp, rallyRed, RoundedCornerShape(50.dp))) {
                    Text("START ROADBOOK", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp, letterSpacing = 3.sp)
                }

                OutlinedButton(onClick = onNavigateToSettings, shape = RoundedCornerShape(50.dp), border = androidx.compose.foundation.BorderStroke(2.dp, deepGraphite), contentPadding = PaddingValues(24.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("USTAWIENIA", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = deepGraphite, fontSize = 18.sp, letterSpacing = 3.sp)
                }

                Button(onClick = onExit, shape = RoundedCornerShape(30.dp), colors = ButtonDefaults.buttonColors(containerColor = rallyRed.copy(alpha = 0.04f)), border = androidx.compose.foundation.BorderStroke(1.dp, rallyRed.copy(alpha = 0.18f)), contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("WYJDŹ Z APLIKACJI", fontFamily = Montserrat, fontWeight = FontWeight.SemiBold, color = rallyRed, fontSize = 14.sp, letterSpacing = 4.sp)
                }
            }
        }

        Row(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(top = 40.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Satelity: ${viewModel.gpsSignalQuality.value}", color = deepGraphite.copy(alpha = 0.5f), fontFamily = Montserrat, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            Text(" • ", color = rallyRed.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
            Text("Zasilanie: ${viewModel.batteryLevel.value}", color = deepGraphite.copy(alpha = 0.5f), fontFamily = Montserrat, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
        }
    }
}