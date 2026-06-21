package com.example.roadbook.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbook.model.RallyStage
import com.example.roadbook.model.StageCategory
import com.example.roadbook.ui.theme.Montserrat
import com.example.roadbook.ui.theme.RallyBold

@Composable
fun StageDetailsScreen( // Przywrócona oryginalna nazwa - naprawia błąd Unresolved Reference
    stage: RallyStage,
    onBackClick: () -> Unit,
    onPreviewRoadbookClick: () -> Unit,
    onStartClick: () -> Unit
) {
    val gravelBg = Color(0xFFF4F3F2)
    val deepGraphite = Color(0xFF2B2A29)
    val rallyRed = Color(0xFFD73224)
    val surfaceWhite = Color(0xFFFEFEFE)
    val thumbnailBg = Color(0xFFEAE9E8)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = gravelBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 32.dp), // Poprawka 4: Przyciski mieszczą się na ekranie
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- POPRAWKA 1: NAGŁÓWEK (Spójna typografia i rajdowa ikona) ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
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
                        fontFamily = RallyBold,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
                Text(
                    text = "Zweryfikuj parametry topograficzne przed odpaleniem odcinka.",
                    fontFamily = Montserrat,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = deepGraphite.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 72.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- POPRAWKA 2: PANORAMICZNY PODGLĄD MAPY (Zredukowana wysokość do 260.dp) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp) // Proporcje panoramiczne zgodne z makietą
                    .background(thumbnailBg, RoundedCornerShape(32.dp))
                    .border(1.dp, deepGraphite.copy(alpha = 0.08f), RoundedCornerShape(32.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val gridSize = 40.dp.toPx()
                    var x = 0f
                    while (x < canvasWidth) {
                        drawLine(color = deepGraphite.copy(alpha = 0.03f), start = Offset(x, 0f), end = Offset(x, canvasHeight), strokeWidth = 1.dp.toPx())
                        x += gridSize
                    }
                    var y = 0f
                    while (y < canvasHeight) {
                        drawLine(color = deepGraphite.copy(alpha = 0.03f), start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1.dp.toPx())
                        y += gridSize
                    }

                    val path = Path().apply {
                        moveTo(canvasWidth * 0.1f, canvasHeight * 0.7f)
                        cubicTo(
                            canvasWidth * 0.25f, canvasHeight * 0.2f,
                            canvasWidth * 0.45f, canvasHeight * 0.9f,
                            canvasWidth * 0.6f, canvasHeight * 0.3f
                        )
                        quadraticBezierTo(
                            canvasWidth * 0.75f, canvasHeight * 0.45f,
                            canvasWidth * 0.9f, canvasHeight * 0.6f
                        )
                    }

                    drawPath(path = path, color = deepGraphite, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawCircle(color = rallyRed, radius = 9.dp.toPx(), center = Offset(canvasWidth * 0.1f, canvasHeight * 0.7f))
                    drawCircle(color = deepGraphite, radius = 7.dp.toPx(), center = Offset(canvasWidth * 0.28f, canvasHeight * 0.42f))
                    drawCircle(color = deepGraphite, radius = 7.dp.toPx(), center = Offset(canvasWidth * 0.46f, canvasHeight * 0.68f))
                    drawCircle(color = deepGraphite, radius = 7.dp.toPx(), center = Offset(canvasWidth * 0.6f, canvasHeight * 0.3f))
                    drawCircle(color = deepGraphite, radius = 7.dp.toPx(), center = Offset(canvasWidth * 0.72f, canvasHeight * 0.42f))
                    drawCircle(color = rallyRed, radius = 9.dp.toPx(), center = Offset(canvasWidth * 0.9f, canvasHeight * 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- POPRAWKA 3: SPECYFIKACJA ETAPU (Wizualnie zgodna z makietą HTML) ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = surfaceWhite,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)) {
                    Text(
                        text = "SPECYFIKACJA ETAPU",
                        fontFamily = Montserrat,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = rallyRed,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    HorizontalDivider(thickness = 2.dp, color = rallyRed.copy(alpha = 0.08f), modifier = Modifier.padding(bottom = 4.dp))

                    MatrixRow(label = "Wczytany etap:", value = if(stage.category == StageCategory.SYSTEM) "SS01 (Odcinek Specjalny)" else "Odcinek Treningowy")
                    MatrixRow(label = "Dystans sekcji:", value = "${stage.distanceKm.toString().replace(".", ",")} km")
                    MatrixRow(label = "Punkty kontrolne:", value = "${stage.waypointCount} WP")
                    MatrixRow(label = "Nawierzchnia dominująca:", value = "Szuter / Szlak leśny")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- INFORMACJA OPERACYJNA ---
            Text(
                text = "Kliknięcie przycisku START uruchomi rejestrację OS i aktywuje pełny ekran nawigatora. Przycisk powyżej pozwala na bezpieczne przestudiowanie notatek i legenda roadbooka bez naliczania czasu przejazdu.",
                fontFamily = Montserrat,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = deepGraphite.copy(alpha = 0.5f),
                lineHeight = 22.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // --- POPRAWKA 4: DOLNA KONSOLA STERUJĄCA (Zoptymalizowana wysokość do 62.dp) ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                        Text(text = "URUCHOM START", fontFamily = Montserrat, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = surfaceWhite, letterSpacing = 2.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixRow(label: String, value: String) {
    val deepGraphite = Color(0xFF2B2A29)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontFamily = Montserrat, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = deepGraphite.copy(alpha = 0.5f))
        Canvas(modifier = Modifier.weight(1f).height(1.dp).padding(horizontal = 12.dp)) {
            drawLine(
                color = deepGraphite.copy(alpha = 0.08f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }
        Text(text = value, fontFamily = Montserrat, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = deepGraphite)
    }
}

@Composable
private fun DetailsArrowBackIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val strokeWidth = 3.5.dp.toPx()
        val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawLine(color = color, start = Offset(19f * scaleX, 12f * scaleY), end = Offset(5f * scaleX, 12f * scaleY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        val path = Path().apply {
            moveTo(12f * scaleX, 19f * scaleY)
            lineTo(5f * scaleX, 12f * scaleY)
            lineTo(12f * scaleX, 5f * scaleY)
        }
        drawPath(path = path, color = color, style = strokeStyle)
    }
}