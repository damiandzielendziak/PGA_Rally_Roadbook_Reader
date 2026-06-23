package com.example.roadbook.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip // DODANO: Import do przycinania zawartości
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.roadbook.R
import com.example.roadbook.model.RallyStage
import com.example.roadbook.model.StageCategory
import com.example.roadbook.ui.theme.Montserrat
import com.example.roadbook.ui.theme.RallyBold

@Composable
fun StageDetailsScreen(
    stage: RallyStage,
    onBackClick: () -> Unit,
    onPreviewRoadbookClick: () -> Unit,
    onStartClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val gravelBg = Color(0xFFF4F3F2)
    val deepGraphite = Color(0xFF2B2A29)
    val rallyRed = Color(0xFFD73224)
    val surfaceWhite = Color(0xFFFEFEFE)
    val thumbnailBg = Color(0xFFEAE9E8)

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = gravelBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- 1. NAGŁÓWEK ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(56.dp)
                            .background(deepGraphite.copy(alpha = 0.05f), CircleShape)
                    ) {
                        DetailsArrowBackIcon(modifier = Modifier.size(32.dp), color = deepGraphite)
                    }
                    Spacer(modifier = Modifier.width(16.dp))

                    val upperTitle = stage.title.uppercase()
                    val titleParts = upperTitle.split(" - ")

                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = deepGraphite)) {
                                append(titleParts.getOrNull(0) ?: upperTitle)
                            }
                            if (titleParts.size > 1) {
                                withStyle(style = SpanStyle(color = rallyRed)) {
                                    append(" - ${titleParts[1]}")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        fontFamily = Montserrat,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.2).sp,
                        lineHeight = 44.sp
                    )
                }
                Text(
                    text = "Zweryfikuj parametry trasy przed uruchomieniem odcinka.",
                    fontFamily = Montserrat,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = deepGraphite.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 72.dp)
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // --- 2. PODGLĄD MAPY (NAPRAWIONE ZAOKRĄGLENIA ROGÓW) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(16.dp)) // FIX: Drastyczne i bezwzględne przycięcie dzieci do łuku 16.dp
                    .background(thumbnailBg)
                    .border(1.dp, deepGraphite.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!stage.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = stage.imageUrl,
                        contentDescription = "Panoramiczny podgląd OS z serwera",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.map_placeholder),
                        error = painterResource(id = R.drawable.map_placeholder)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.map_placeholder),
                        contentDescription = "Panoramiczny podgląd OS (Domyślny placeholder)",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 3. SPECYFIKACJA ETAPU ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = surfaceWhite,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)) {
                    Text(
                        text = "SPECYFIKACJA ETAPU",
                        fontFamily = Montserrat,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = rallyRed,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    HorizontalDivider(thickness = 3.dp, color = rallyRed.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 14.dp))

                    val stageTypeValue = if (stage.category == StageCategory.SYSTEM) "Trasa systemowa" else "Trasa pobrana przez użytkownika"
                    val surfaceValue = if (!stage.dominantSurface.isNullOrBlank()) stage.dominantSurface else "Brak danych"

                    MatrixRow(label = "Typ trasy:", value = stageTypeValue)
                    MatrixRow(label = "Dystans sekcji:", value = "${stage.distanceKm.toString().replace(".", ",")} km")
                    MatrixRow(label = "Punkty kontrolne:", value = "${stage.waypointCount} WP")
                    MatrixRow(label = "Nawierzchnia dominująca:", value = surfaceValue)

                    if (!stage.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(22.dp))
                        Text(
                            text = "OPIS ODCINKA:",
                            fontFamily = Montserrat,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = deepGraphite.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = stage.description,
                            fontFamily = Montserrat,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = deepGraphite.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- 4. INFORMACJA OPERACYJNA ---
            Text(
                text = "Kliknięcie przycisku URUCHOM MENU STARTOWE uruchomi ekran startowy odcinka zawierający infomracje o OS jak i stan systemu.",
                fontFamily = Montserrat,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = deepGraphite.copy(alpha = 0.7f),
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = "Przycisk PRZEGLĄDAJ ROADBOOK pozwala na bezpieczne przejrzenie roadbooka bez uruchamiania procesu nawigacji.",
                fontFamily = Montserrat,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = deepGraphite.copy(alpha = 0.7f),
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 26.dp)
            )

            // --- 5. DOLNA KONSOLA STERUJĄCA ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (stage.category == StageCategory.USER) {
                    Button(
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .border(2.5.dp, rallyRed, RoundedCornerShape(50)),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = surfaceWhite)
                    ) {
                        Text(
                            text = "USUŃ TRASĘ",
                            fontFamily = Montserrat,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = rallyRed,
                            letterSpacing = 2.sp
                        )
                    }
                }

                Button(
                    onClick = onPreviewRoadbookClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .border(2.5.dp, deepGraphite, RoundedCornerShape(50)),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = surfaceWhite)
                ) {
                    Text(text = "PRZEGLĄDAJ ROADBOOK", fontFamily = Montserrat, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = deepGraphite, letterSpacing = 2.sp)
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().height(62.dp).offset(y = 4.dp).background(rallyRed, RoundedCornerShape(50)))
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier.fillMaxWidth().height(62.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = deepGraphite)
                    ) {
                        Text(text = "URUCHOM MENU STARTOWE", fontFamily = Montserrat, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = surfaceWhite, letterSpacing = 2.sp)
                    }
                }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmation = false
                            onDeleteClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = rallyRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("TAK, USUŃ", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = surfaceWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("ANULUJ", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = deepGraphite)
                    }
                },
                title = {
                    Text(
                        text = "POTWIERDŹ USUNIĘCIE",
                        fontFamily = RallyBold,
                        fontSize = 24.sp,
                        color = deepGraphite,
                        letterSpacing = 1.sp
                    )
                },
                text = {
                    Text(
                        text = "Czy na pewno chcesz usunąć tę trasę z pamięci urządzenia? Tej operacji nie da się cofnąć.",
                        fontFamily = Montserrat,
                        fontSize = 16.sp,
                        color = deepGraphite,
                        lineHeight = 22.sp
                    )
                },
                shape = RoundedCornerShape(12.dp),
                containerColor = surfaceWhite
            )
        }
    }
}

@Composable
private fun MatrixRow(label: String, value: String) {
    val deepGraphite = Color(0xFF2B2A29)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontFamily = Montserrat, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = deepGraphite.copy(alpha = 0.5f))
        androidx.compose.foundation.Canvas(modifier = Modifier.weight(1f).height(1.dp).padding(horizontal = 14.dp)) {
            drawLine(
                color = deepGraphite.copy(alpha = 0.08f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }
        Text(text = value, fontFamily = Montserrat, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = deepGraphite)
    }
}

@Composable
private fun DetailsArrowBackIcon(modifier: Modifier = Modifier, color: Color) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val strokeWidth = 3.5.dp.toPx()
        val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawLine(color = color, start = Offset(19f * scaleX, 12f * scaleY), end = Offset(5f * scaleX, 12f * scaleY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(12f * scaleX, 19f * scaleY)
            lineTo(5f * scaleX, 12f * scaleY)
            lineTo(12f * scaleX, 5f * scaleY)
        }
        drawPath(path = path, color = color, style = strokeStyle)
    }
}