package com.example.roadbook.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingNavigationMenu(
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit,
    onPauseClick: () -> Unit,
    onRotationClick: () -> Unit,
    isPaused: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pionowe sub-menu rozwijane w górę (zbalansowane do 100.dp)
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Item 3: Obrót ekranu
                FloatingActionButton(
                    onClick = {
                        onRotationClick()
                        isExpanded = false
                    },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(100.dp).border(4.dp, Color.Black, CircleShape)
                ) {
                    Text("↻", fontSize = 54.sp, fontWeight = FontWeight.Bold)
                }

                // Item 2: Menu Pauzy (Zatrzymuje GPS i wywołuje Boot Loader z flagą WZNÓW)
                FloatingActionButton(
                    onClick = {
                        onPauseClick()
                        isExpanded = false
                    },
                    containerColor = if (isPaused) Color(0xFFD32F2F) else Color.White,
                    contentColor = if (isPaused) Color.White else Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(100.dp).border(4.dp, if (isPaused) Color.Red else Color.Black, CircleShape)
                ) {
                    Row(
                        modifier = Modifier.size(34.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val barColor = if (isPaused) Color.White else Color.Black
                        Box(modifier = Modifier.fillMaxHeight().weight(1f).background(barColor))
                        Box(modifier = Modifier.fillMaxHeight().weight(1f).background(barColor))
                    }
                }

                // Item 1: Ustawienia Roadbooka
                FloatingActionButton(
                    onClick = {
                        onSettingsClick()
                        isExpanded = false
                    },
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier.size(100.dp).border(4.dp, Color.Black, CircleShape)
                ) {
                    Text("⚙", fontSize = 54.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // POPRAWKA: Przycisk hamburgera wyłącznie steruje stanem otwarcia (isExpanded) i nie dotyka Boot Loadera
        FloatingActionButton(
            onClick = { isExpanded = !isExpanded },
            containerColor = Color.Black,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(100.dp).border(4.dp, Color.White, CircleShape)
        ) {
            Text("☰", fontSize = 54.sp, color = Color.White)
        }
    }
}