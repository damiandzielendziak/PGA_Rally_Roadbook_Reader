package com.example.roadbook.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

import com.example.roadbook.MainActivity
import com.example.roadbook.viewmodel.RoadbookViewModel
import com.example.roadbook.ui.theme.Montserrat
import com.example.roadbook.ui.theme.RallyBold

private val BgColor = Color(0xFFF4F3F2)
private val TextPrimary = Color(0xFF2B2A29)
private val AccentRed = Color(0xFFD73224)
private val SurfaceWhite = Color(0xFFFEFEFE)
private val ControlBg = Color(0xFFF1F0EF)

@Composable
fun SettingsScreen(
    viewModel: RoadbookViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current as MainActivity
    val currentScale = viewModel.uiScale.value

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BgColor
    ) {
        // POPRAWKA: Dodano statusBarsPadding wokół głównego kontenera dla równego pasowania z paskiem Androida
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {

            // --- OBSZAR PRZEWIJALNY ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    // UJEDNOLICENIE: Margines ustawiony na sztywno: top = 40.dp (identycznie jak w bazie tras i szczegółach)
                    .padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 12.dp)
            ) {
                HeaderSection(onDismissRequest) // Usunięto przekazywanie skali, aby nagłówek nie skakał podczas testu suwaków

                // --- LISTA USTAWIEŃ ---
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceWhite,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        SettingScaleRow(
                            title = "Layout Scale",
                            description = "Globalne skalowanie wielkości cyfrowych odometrów oraz kadrów legendy roadbooka.",
                            scaleValue = currentScale,
                            onScaleChange = { newScale -> viewModel.uiScale.value = newScale }
                        )

                        CustomDivider()

                        SettingToggleRow(
                            title = "Orientacja pozioma",
                            description = "Zablokuj interfejs w orientacji horyzontalnej i wyłącz automatyczne obracanie czujników kokpitu.",
                            isChecked = viewModel.isLandscapeOrientation.value,
                            onCheckedChange = { viewModel.isLandscapeOrientation.value = it },
                            scale = currentScale
                        )

                        CustomDivider()

                        SettingToggleRow(
                            title = "Tap to Scroll",
                            description = "Zezwalaj na manualne przewijanie kratek roadbooka w przód poprzez tąpnięcie ekranu dłonią.",
                            isChecked = viewModel.tapsEnabled.value,
                            onCheckedChange = { viewModel.tapsEnabled.value = it },
                            scale = currentScale
                        )

                        CustomDivider()

                        SettingToggleRow(
                            title = "Auto-Scroll Roadbook",
                            description = "Automatyczne, płynne przesuwanie notatek nawigacyjnych powiązane z prędkością z GPS pojazdu.",
                            isChecked = viewModel.isAutoScrollEnabled.value,
                            onCheckedChange = { viewModel.isAutoScrollEnabled.value = it },
                            scale = currentScale
                        )

                        CustomDivider()

                        SettingToggleRow(
                            title = "GPS Simulation Mode",
                            description = "Uruchom tryb demonstracyjny poruszający się wirtualnie po śladzie ze stałą prędkością 72 km/h.",
                            isChecked = viewModel.isSimulationMode.value,
                            onCheckedChange = { viewModel.toggleSimulation(it) },
                            scale = currentScale
                        )
                    }
                }
            }

            // --- PANEL DOLNY ---
            BottomActionPanel(
                onSaveClick = {
                    viewModel.saveSettings(context)
                    onDismissRequest()
                },
                scale = currentScale
            )
        }
    }
}

@Composable
private fun HeaderSection(onDismissRequest: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            IconButton(
                onClick = onDismissRequest,
                modifier = Modifier
                    .size(56.dp)
                    .background(TextPrimary.copy(alpha = 0.05f), CircleShape)
            ) {
                Icon(
                    imageVector = CustomArrowBackIcon,
                    contentDescription = "Wróć",
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))

            // UJEDNOLICENIE TYPOGRAFII: Montserrat Bold, 36.sp, ściskanie poziome, bezpieczna wysokość linii
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = TextPrimary)) { append("ROADBOOK") }
                    withStyle(style = SpanStyle(color = AccentRed)) { append(" - SETTINGS") }
                },
                modifier = Modifier.weight(1f), // Zapobiega wypychaniu i gwarantuje stabilne zawijanie linii
                fontFamily = Montserrat,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.2).sp,
                lineHeight = 44.sp
            )
        }

        // UJEDNOLICENIE PODTYTUŁU: Rozmiar 18.sp, brak pływającego mnożnika skali
        Text(
            text = "Dostosuj parametry wyświetlania, skalowania interfejsu oraz konfigurację modułów zewnętrznych.",
            fontFamily = Montserrat,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 72.dp, bottom = 24.dp),
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    scale: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 24.dp)
        ) {
            Text(
                text = title,
                fontFamily = Montserrat,
                fontSize = (18 * scale).sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontFamily = Montserrat,
                fontSize = (14 * scale).sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary.copy(alpha = 0.5f),
                lineHeight = (20 * scale).sp
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SurfaceWhite,
                checkedTrackColor = AccentRed,
                checkedBorderColor = AccentRed,
                uncheckedThumbColor = TextPrimary,
                uncheckedTrackColor = ControlBg,
                uncheckedBorderColor = TextPrimary.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
private fun SettingScaleRow(
    title: String,
    description: String,
    scaleValue: Float,
    onScaleChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 24.dp)
        ) {
            Text(
                text = title,
                fontFamily = Montserrat,
                fontSize = (18 * scaleValue).sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontFamily = Montserrat,
                fontSize = (14 * scaleValue).sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary.copy(alpha = 0.5f),
                lineHeight = (20 * scaleValue).sp
            )
        }

        Row(
            modifier = Modifier
                .background(ControlBg, RoundedCornerShape(40.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(TextPrimary)
                    .clickable {
                        val currentPct = (scaleValue * 100).roundToInt()
                        val newPct = (currentPct - 10).coerceAtLeast(80)
                        onScaleChange(newPct / 100f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("-", color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }

            Text(
                text = "${(scaleValue * 100).roundToInt()}%",
                fontFamily = Montserrat,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (16 * scaleValue).sp,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(64.dp)
            )

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(TextPrimary)
                    .clickable {
                        val currentPct = (scaleValue * 100).roundToInt()
                        val newPct = (currentPct + 10).coerceAtMost(120)
                        onScaleChange(newPct / 100f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = SurfaceWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun BottomActionPanel(onSaveClick: () -> Unit, scale: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset(y = 4.dp)
                .background(AccentRed, RoundedCornerShape(50))
        )

        Button(
            onClick = onSaveClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
        ) {
            Text(
                text = "SAVE SETTINGS",
                fontFamily = Montserrat,
                fontSize = (18 * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                color = SurfaceWhite,
                letterSpacing = (2 * scale).sp
            )
        }
    }
}

@Composable
private fun CustomDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = TextPrimary.copy(alpha = 0.08f)
    )
}

private val CustomArrowBackIcon: ImageVector
    get() = ImageVector.Builder(
        name = "CustomArrowBack",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(20f, 11f)
            horizontalLineTo(7.83f)
            lineTo(13.42f, 5.41f)
            lineTo(12f, 4f)
            lineTo(4f, 12f)
            lineTo(12f, 20f)
            lineTo(13.41f, 18.59f)
            lineTo(7.83f, 13f)
            horizontalLineTo(20f)
            verticalLineTo(11f)
            close()
        }
    }.build()