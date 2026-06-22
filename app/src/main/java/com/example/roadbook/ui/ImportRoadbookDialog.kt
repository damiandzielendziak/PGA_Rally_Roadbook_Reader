package com.example.roadbook.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.roadbook.ui.theme.Montserrat
import com.example.roadbook.ui.theme.RallyBold

@Composable
fun ImportRoadbookDialog(
    onDismissRequest: () -> Unit,
    onFileBrowseClick: () -> Unit,
    onDownloadWithCodeClick: (String) -> Unit
) {
    val deepGraphite = Color(0xFF2B2A29)
    val rallyRed = Color(0xFFD73224)
    val surfaceWhite = Color(0xFFFEFEFE)
    val controlBg = Color(0xFFF1F0EF)

    var roadbookCode by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .width(580.dp) // Proporcjonalna szerokość dla ekranu 1080px
                .wrapContentHeight()
                .padding(20.dp),
            shape = RoundedCornerShape(12.dp), // Spójne, mniejsze zaokrąglenie makiety
            color = surfaceWhite,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                // --- STREFA NAGŁÓWKA POPUPU ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = deepGraphite)) { append("IMPORTUJ NOWY ") }
                            withStyle(style = SpanStyle(color = rallyRed)) { append("ROADBOOK") }
                        },
                        fontFamily = RallyBold,
                        fontSize = 32.sp,
                        letterSpacing = 2.sp
                    )

                    // Wektorowy przycisk zamknięcia "X" (1:1 z SVG w HTML)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onDismissRequest() },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            val strokeWidth = 3.dp.toPx()
                            drawLine(
                                color = deepGraphite,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, size.height),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = deepGraphite,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, 0f),
                                strokeWidth = strokeWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Linia oddzielająca nagłówek
                HorizontalDivider(
                    thickness = 2.dp,
                    color = deepGraphite.copy(alpha = 0.08f),
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // --- STACK OPCJI IMPORTU ---
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // OPCJA 1: Przeglądaj pliki z dysku
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(controlBg, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Wskaż plik mapy z pamięci urządzenia (.gpx, .json, .pga):",
                            fontFamily = Montserrat,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = deepGraphite
                        )
                        Text(
                            text = "Aplikacja obsługuje pliki roadbooka w formacie Open Rally. Wczytaj przygotowany wcześniej plik.",
                            fontFamily = Montserrat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = deepGraphite.copy(alpha = 0.6f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onFileBrowseClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .border(2.dp, deepGraphite, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = surfaceWhite)
                        ) {
                            Text(
                                text = "PRZEGLĄDAJ PLIK",
                                fontFamily = Montserrat,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = deepGraphite,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    // SEPARATOR "lub"
                    Text(
                        text = "lub",
                        fontFamily = RallyBold,
                        fontSize = 24.sp,
                        color = deepGraphite.copy(alpha = 0.3f),
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // OPCJA 2: Pobieranie zdalne przez kod trasy
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(controlBg, RoundedCornerShape(12.dp))
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Wpisz unikalny kod roadbooka rajdowego, aby pobrać go z chmury:",
                            fontFamily = Montserrat,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = deepGraphite
                        )
                        Text(
                            text = "W oknie poniżej wpisz kod roadbooka pozyskany od organizatora. Zostanie on automatycznie pobrany.",
                            fontFamily = Montserrat,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = deepGraphite.copy(alpha = 0.6f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Układ wpisywania kodu (Input + Przycisk w jednym wierszu)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp)
                                    .background(surfaceWhite, RoundedCornerShape(12.dp))
                                    .border(
                                        width = 2.dp,
                                        color = if (roadbookCode.isNotBlank()) rallyRed.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                BasicTextField(
                                    value = roadbookCode,
                                    onValueChange = { roadbookCode = it },
                                    textStyle = TextStyle(
                                        fontFamily = Montserrat,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = deepGraphite
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (roadbookCode.isEmpty()) {
                                            Text(
                                                text = "np. PGA-735",
                                                fontFamily = Montserrat,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp,
                                                color = deepGraphite.copy(alpha = 0.3f)
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                            }

                            Button(
                                onClick = { if (roadbookCode.isNotBlank()) onDownloadWithCodeClick(roadbookCode) },
                                enabled = roadbookCode.isNotBlank(),
                                modifier = Modifier
                                    .height(54.dp)
                                    .widthIn(min = 120.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = deepGraphite,
                                    disabledContainerColor = deepGraphite.copy(alpha = 0.15f)
                                )
                            ) {
                                Text(
                                    text = "POBIERZ",
                                    fontFamily = Montserrat,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (roadbookCode.isNotBlank()) surfaceWhite else deepGraphite.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                // --- DOPISEK INFORMACYJNY NA SAMYM DOLE (Z LINIĄ PRZERYWANĄ) ---
                Spacer(modifier = Modifier.height(28.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                    drawLine(
                        color = deepGraphite.copy(alpha = 0.06f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
                Text(
                    text = "Zaimportowany roadbook dostępny będzie z zakładki Trasy użytkownika.",
                    fontFamily = Montserrat,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = deepGraphite.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    lineHeight = 20.sp
                )
            }
        }
    }
}