package com.example.roadbook.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import com.example.roadbook.ui.theme.RallyBold

@Composable
fun InstrumentPanel(
    modifier: Modifier = Modifier,
    totalDistanceMeters: Double,
    tripDistanceMeters: Double,
    dtwBearing: Float,
    capHeading: Float,
    dtwDistance: Float,
    // ŻELAZNA POPRAWKA: Usunięto "= false". Brak domyślnej wartości wymusi bezpieczną kompilację w MainActivity
    isGpsActive: Boolean,
    // DODANE: Flaga określająca, czy wjechaliśmy w strefę otwarcia waypointu
    isWaypointOpen: Boolean = false,
    onLeftZoneStateChange: (Boolean) -> Unit,
    onRightZoneStateChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(3.dp, Color.Black)
    ) {
        // COLUMN A (LEFT): Odometer / Tripmaster counters
        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            onLeftZoneStateChange(true)
                            waitForUpOrCancellation()
                            onLeftZoneStateChange(false)
                        }
                    })
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            onRightZoneStateChange(true)
                            waitForUpOrCancellation()
                            onRightZoneStateChange(false)
                        }
                    })
                }
                Text(
                    text = "%.2f".format(totalDistanceMeters / 1000.0),
                    color = Color.Black, fontSize = 160.sp, fontFamily = RallyBold,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 30.dp)
                )
            }

            HorizontalDivider(thickness = 3.dp, color = Color.Black)

            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "%.2f".format(tripDistanceMeters / 1000.0),
                    color = Color.Black, fontSize = 120.sp, fontFamily = RallyBold,
                    textAlign = TextAlign.Center, modifier = Modifier.padding(top = 24.dp)
                )
            }
        }

        VerticalDivider(thickness = 3.dp, color = Color.Black)

        // COLUMN B (RIGHT): Compass CAP and DTW Target Arrow
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color.White)) {

            // Inner left side of Column B (Bearing deviation and compass dial)
            Column(
                modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight().align(Alignment.TopStart),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom
            ) {
                // WARUNEK FIA: Elementy nawigacyjne do celu (Azymut i Tarcza kompasu) ukryte, dopóki strefa nie jest otwarta
                if (isWaypointOpen) {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(3f).padding(end = 8.dp).offset(y = 30.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Text(text = "${abs(dtwBearing.roundToInt())}°", color = Color(0xFF0022CC), fontSize = 65.sp, fontFamily = RallyBold)
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(5f), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize(0.90f)) {
                            val center = this.center
                            val radius = size.minDimension / 2
                            drawCircle(
                                color = Color.Black,
                                radius = radius,
                                center = center,
                                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 6f), 0f))
                            )
                            rotate(degrees = dtwBearing, pivot = center) {
                                val totalHeight = radius * 1.35f
                                val topY = center.y - totalHeight / 2
                                val headBottomY = topY + radius * 0.5f
                                val bottomY = totalHeight + topY
                                val arrowPath = Path().apply {
                                    moveTo(center.x, topY)
                                    lineTo(center.x + radius * 0.4f, headBottomY)
                                    lineTo(center.x + radius * 0.16f, headBottomY)
                                    lineTo(center.x + radius * 0.16f, bottomY)
                                    lineTo(center.x - radius * 0.16f, bottomY)
                                    lineTo(center.x - radius * 0.16f, headBottomY)
                                    lineTo(center.x - radius * 0.4f, headBottomY)
                                    close()
                                }
                                drawPath(path = arrowPath, color = Color(0xFF0022CC))
                                drawPath(path = arrowPath, color = Color.Black, style = Stroke(width = 3f))
                            }
                        }
                    }
                }
            }

            // Inner right side of Column B (CAP box and DTW distance value)
            Column(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight().align(Alignment.TopEnd)) {
                // WARUNEK KONTROLI: Ukrywamy stopnie i gasimy tło, gdy GPS/Symulator nie przesyła danych
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(if (isGpsActive) Color(0xFFFFEB3B) else Color(0xFFE0E0E0))
                        .border(3.dp, Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGpsActive) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.Center) {
                            Text(
                                text = "%d".format(capHeading.roundToInt()),
                                color = Color.Black, fontSize = 100.sp, fontFamily = RallyBold,
                                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp)
                            )
                            Text(text = "°", color = Color.Black, fontSize = 60.sp, fontFamily = RallyBold, modifier = Modifier.padding(top = 20.dp))
                        }
                    } else {
                        Text(
                            text = "---",
                            color = Color.Black, fontSize = 100.sp, fontFamily = RallyBold,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White).padding(start = 12.dp, top = 20.dp)) {
                    // WARUNEK FIA: Dystans w linii prostej (DTW) ukryty, dopóki strefa nie jest otwarta
                    if (isWaypointOpen) {
                        Text(text = "DTW", color = Color(0xFF0022CC), fontSize = 55.sp, fontFamily = RallyBold)
                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "${dtwDistance.roundToInt()}",
                            color = Color(0xFF0022CC), fontSize = 100.sp, fontFamily = RallyBold
                        )
                    }
                }
            }
        }
    }
}