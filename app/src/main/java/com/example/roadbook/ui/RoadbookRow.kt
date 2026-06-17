package com.example.roadbook.ui

import android.location.Location
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Zachowane oryginalne importy domeny i zasobów projektu
import com.example.roadbook.ui.theme.RallyBold
import com.example.roadbook.model.RallyWaypoint
import com.example.roadbook.model.OpenRallyWpType
import com.example.roadbook.model.convertToDmsFormat

@Composable
fun RoadbookRow(
    waypoint: RallyWaypoint,
    isActive: Boolean,
    rowTripDistanceMeters: Float,
    azimuthToNext: Int,
    waypointNumber: String,
    userLocation: Location? = null,
    isFlashing: Boolean = false, // Flaga aktywująca czystą nakładkę ostrzegawczą
    onTulipLongClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val dynamicRowHeight = maxWidth * 0.35f

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dynamicRowHeight)
                .background(Color.White)
                .border(2.dp, Color.Black)
        ) {
            // =================================================================
            // --- KOLUMNA 1: SEKCJA DYSTANSU (30% SZEROKOŚCI) ---
            // =================================================================
            val isGreenWarning = waypoint.distanceMeters > 0f && rowTripDistanceMeters < 300f

            val distanceBoxBg = if (isGreenWarning) {
                Color(0xFF8AFB8A)
            } else {
                when (waypoint.wpType) {
                    OpenRallyWpType.WPV, OpenRallyWpType.WPM -> Color(0xFFE0F7FA)
                    OpenRallyWpType.WPC -> Color(0xFFFFF9C4)
                    OpenRallyWpType.WPS -> Color(0xFFFFF3E0)
                    OpenRallyWpType.WPN -> Color(0xFFECEFF1)
                    OpenRallyWpType.WPP -> Color(0xFFFCE4EC)
                    OpenRallyWpType.WPE -> Color(0xFFE0F2F1)
                    OpenRallyWpType.DZ, OpenRallyWpType.FZ -> Color(0xFFFFFDE7)
                    OpenRallyWpType.DN, OpenRallyWpType.FN -> Color(0xFFE1F5FE)
                    OpenRallyWpType.DT, OpenRallyWpType.FT -> Color(0xFFF5F5F5)
                    OpenRallyWpType.DSS -> Color(0xFFE8F5E9)
                    OpenRallyWpType.ASS -> Color(0xFFFFEBEE)
                    else -> Color.White
                }
            }

            Box(
                modifier = Modifier
                    .weight(0.30f)
                    .fillMaxHeight()
                    .background(distanceBoxBg)
            ) {
                Text(
                    text = "%.2f".format(waypoint.distanceMeters / 1000f),
                    color = Color.Black,
                    fontSize = 108.sp,
                    fontFamily = RallyBold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 14.dp)
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .border(2.dp, Color.Black)
                                .background(distanceBoxBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "%.2f".format(rowTripDistanceMeters / 1000f),
                                color = Color.Black,
                                fontSize = 40.sp,
                                fontFamily = RallyBold,
                                modifier = Modifier.offset(y = 6.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .background(Color.Black)
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = waypointNumber,
                                color = Color.White,
                                fontSize = 36.sp,
                                fontFamily = RallyBold,
                                modifier = Modifier.offset(y = 6.dp)
                            )
                        }
                    }

                    val badgeConfig = when (waypoint.wpType) {
                        OpenRallyWpType.WPV -> Pair("V", Color(0xFF4DD0E1))
                        OpenRallyWpType.WPM -> Pair("M", Color(0xFF4DD0E1))
                        OpenRallyWpType.WPC -> Pair("C", Color(0xFFFFEB3B))
                        OpenRallyWpType.WPS -> Pair("S", Color(0xFFFFB74D))
                        OpenRallyWpType.WPN -> Pair("N", Color(0xFF78909C))
                        OpenRallyWpType.WPP -> Pair("P", Color(0xFFF06292))
                        OpenRallyWpType.WPE -> Pair("E", Color(0xFF4DB6AC))
                        OpenRallyWpType.DZ -> Pair("DZ", Color(0xFFFFCC80))
                        OpenRallyWpType.FZ -> Pair("FZ", Color(0xFF81C784))
                        OpenRallyWpType.DSS -> Pair("DSS", Color(0xFF4CAF50))
                        OpenRallyWpType.ASS -> Pair("ASS", Color(0xFFE53935))
                        OpenRallyWpType.DN -> Pair("DN", Color(0xFF29B6F6))
                        OpenRallyWpType.FN -> Pair("FN", Color(0xFF90CAF9))
                        OpenRallyWpType.DT -> Pair("DT", Color(0xFFB0BEC5))
                        OpenRallyWpType.FT -> Pair("FT", Color(0xFFCFD8DC))
                        else -> null
                    }

                    badgeConfig?.let { (textToShow, circleColor) ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(x = 15.dp, y = (-6).dp)
                                .size(84.dp)
                                .border(6.dp, Color.Black, CircleShape)
                                .background(circleColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val finalFontSize = if (textToShow == "V") 52.sp else (if (textToShow.length > 1) 26.sp else 52.sp)
                            val textTextColor = if (waypoint.wpType == OpenRallyWpType.ASS || waypoint.wpType == OpenRallyWpType.DSS) Color.White else Color.Black

                            Text(
                                text = textToShow,
                                color = textTextColor,
                                fontSize = finalFontSize,
                                fontFamily = RallyBold,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(top = if (textToShow == "V") 16.dp else (if (textToShow.length > 1) 10.dp else 16.dp))
                            )
                        }
                    }
                }
            }

            VerticalDivider(thickness = 2.dp, color = Color.Black)

            // =================================================================
            // --- KOLUMNA 2: TULIP DIAGRAM / MANEWR (35% SZEROKOŚCI) ---
            // =================================================================
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                waypoint.tulipBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Tulip Diagram",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        filterQuality = FilterQuality.None
                    )
                } ?: Text(text = "NO TULIP", color = Color.LightGray, fontSize = 14.sp)
            }

            VerticalDivider(thickness = 2.dp, color = Color.Black)

            // =================================================================
            // --- KOLUMNA 3: NOTATKI / SYMBOLE DODATKOWE (35% SZEROKOŚCI) ---
            // =================================================================
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(Color.White)
            ) {
                waypoint.notesBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Notes",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        filterQuality = FilterQuality.None
                    )
                } ?: Text(
                    text = waypoint.name,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontFamily = RallyBold,
                    modifier = Modifier.padding(8.dp)
                )

                // DOLNY PASEK: Wyłącznie wymagane elementy tekstowe (CAP oraz współrzędne DMS)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val capToDisplay = if (waypoint.gpxCap > 0) waypoint.gpxCap else azimuthToNext

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .offset(x = (-2).dp)
                            .width(72.dp)
                            .border(2.dp, Color.Black)
                            .background(Color(0xFFFEEA3B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (capToDisplay > 0) "$capToDisplay" else "---",
                            color = Color.Black,
                            fontSize = 40.sp,
                            fontFamily = RallyBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.offset(y = 4.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .border(2.dp, Color.Black)
                            .background(Color.White)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy((-2).dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text(
                                text = convertToDmsFormat(waypoint.latitude, true),
                                color = Color.Black,
                                fontSize = 20.sp,
                                fontFamily = RallyBold,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = convertToDmsFormat(waypoint.longitude, false),
                                color = Color.Black,
                                fontSize = 20.sp,
                                fontFamily = RallyBold,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                // Wskaźnik indeksu waypointu w prawym dolnym rogu kolumny
                if (waypoint.waypointIndex > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-5).dp, y = (-66).dp)
                            .size(58.dp)
                            .border(3.dp, Color.Black, CircleShape)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${waypoint.waypointIndex}",
                            color = Color.Black,
                            fontSize = 32.sp,
                            fontFamily = RallyBold,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }
                }
            }
        }

        // =================================================================
        // --- SAMA CZERWONA NAKŁADKA (MNIEJSZA PRZEZROCZYSTOŚĆ: ALPHA = 0.65) ---
        // =================================================================
        if (isFlashing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dynamicRowHeight)
                    .background(Color.Red.copy(alpha = 0.65f)) // Zmniejszona przezroczystość dla lepszej widoczności
            )
        }
    }
}