package com.example.roadbook.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.roadbook.MainActivity
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

import com.example.roadbook.model.ScrollDirection
import com.example.roadbook.model.RallyWaypoint
import com.example.roadbook.model.calculateAzimuthBetweenPoints
import com.example.roadbook.viewmodel.RoadbookViewModel
import com.example.roadbook.ui.theme.Montserrat
import com.example.roadbook.ui.theme.RallyBold

@SuppressLint("MissingPermission")
@Composable
fun MainApplicationScreen(
    viewModel: RoadbookViewModel,
    scrollSignal: Pair<ScrollDirection, Long>,
    onGenerateSignal: (ScrollDirection) -> Unit,
    onForceScreenRotation: () -> Unit,
    onCancelNavigation: () -> Unit
) {
    val context = LocalContext.current as MainActivity
    val listState = rememberLazyListState()

    val rallyWp = viewModel.activeRallyWaypoint.value
    val isWaypointOpen = if (rallyWp != null && rallyWp.openingRadiusMeters > 0f) {
        viewModel.dtwDistance.value > 0f && viewModel.dtwDistance.value <= rallyWp.openingRadiusMeters
    } else {
        false
    }

    val totalOdo = viewModel.totalDistanceMeters.value
    val currentIndex = viewModel.activeWaypointIndex.value
    val isGpsOrSim = viewModel.isGpsActive.value || viewModel.isSimulationMode.value

    var lastTriggeredIndex50 by remember { mutableStateOf(-1) }
    var lastTriggeredIndex20 by remember { mutableStateOf(-1) }
    var isOverlayVisible by remember { mutableStateOf(false) }

    LaunchedEffect(currentIndex, isGpsOrSim) {
        if (isGpsOrSim) {
            snapshotFlow { viewModel.totalDistanceMeters.value }
                .collect { currentOdo ->
                    val activeWaypoint = viewModel.waypointList.value.getOrNull(currentIndex)
                    if (activeWaypoint != null) {
                        val remainingDistance = activeWaypoint.distanceMeters - totalOdo

                        if (remainingDistance <= 50.0 && remainingDistance > 20.0 && lastTriggeredIndex50 != currentIndex) {
                            lastTriggeredIndex50 = currentIndex
                            isOverlayVisible = true
                            delay(150)
                            isOverlayVisible = false
                        }

                        if (remainingDistance <= 20.0 && remainingDistance > 0.0 && lastTriggeredIndex20 != currentIndex) {
                            lastTriggeredIndex20 = currentIndex
                            isOverlayVisible = true
                            delay(100)
                            isOverlayVisible = false
                            delay(100)
                            isOverlayVisible = true
                            delay(100)
                            isOverlayVisible = false
                        }
                    }
                }
        }
    }

    LaunchedEffect(scrollSignal) {
        val (direction, _) = scrollSignal
        val totalItems = viewModel.waypointList.value.size
        if (totalItems > 0) {
            val currentIndexInternal = listState.firstVisibleItemIndex
            when (direction) {
                ScrollDirection.DOWN -> {
                    val targetIndex = (currentIndexInternal + 1).coerceAtMost(totalItems - 1)
                    listState.animateScrollToItem(targetIndex)
                    viewModel.tripDistanceMeters.value = 0.0
                }
                ScrollDirection.UP -> {
                    val targetIndex = (currentIndexInternal - 1).coerceAtLeast(0)
                    listState.animateScrollToItem(targetIndex)
                }
                ScrollDirection.NONE -> {}
            }
        }
    }

    LaunchedEffect(viewModel.activeWaypointIndex.value) {
        if (viewModel.isAutoScrollEnabled.value && !viewModel.isPreviewMode.value && viewModel.waypointList.value.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.activeWaypointIndex.value)
            viewModel.tripDistanceMeters.value = 0.0
        }
    }

    LaunchedEffect(viewModel.showStartupDialog.value) {
        if (!viewModel.showStartupDialog.value && !viewModel.isPreviewMode.value && viewModel.waypointList.value.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.activeWaypointIndex.value)
        }
    }

    LaunchedEffect(viewModel.leftZonePressed.value) {
        if (viewModel.leftZonePressed.value) viewModel.startIncrementLoop()
    }

    LaunchedEffect(viewModel.rightZonePressed.value) {
        if (viewModel.rightZonePressed.value) viewModel.startDecrementLoop()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            TelemetryBar(
                modifier = Modifier.weight(0.176f),
                currentSpeed = viewModel.currentSpeed.value,
                validatedWaypointsCount = viewModel.validatedWaypointsCount.value
            )

            HorizontalDivider(thickness = 1.dp, color = Color.White)

            InstrumentPanel(
                modifier = Modifier.weight(0.88f),
                totalDistanceMeters = viewModel.totalDistanceMeters.value,
                tripDistanceMeters = viewModel.tripDistanceMeters.value,
                dtwBearing = viewModel.dtwBearing.value,
                capHeading = viewModel.capHeading.value,
                dtwDistance = viewModel.dtwDistance.value,
                isGpsActive = viewModel.isGpsActive.value,
                isWaypointOpen = isWaypointOpen,
                onLeftZoneStateChange = { pressed: Boolean -> viewModel.leftZonePressed.value = pressed },
                onRightZoneStateChange = { pressed: Boolean -> viewModel.rightZonePressed.value = pressed }
            )

            HorizontalDivider(thickness = 4.dp, color = Color.Red)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2.5f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (viewModel.waypointList.value.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize()) {

                        LazyColumn(state = listState, userScrollEnabled = true, modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(viewModel.waypointList.value) { index: Int, waypoint: RallyWaypoint ->
                                val isActive = index == viewModel.activeWaypointIndex.value
                                val previousDistance = if (index > 0) viewModel.waypointList.value[index - 1].distanceMeters else 0f
                                val rowTripDistance = waypoint.distanceMeters - previousDistance

                                val nextAzimuth = if (index < viewModel.waypointList.value.lastIndex) {
                                    calculateAzimuthBetweenPoints(
                                        waypoint.latitude, waypoint.longitude,
                                        viewModel.waypointList.value[index + 1].latitude, viewModel.waypointList.value[index + 1].longitude
                                    )
                                } else 0

                                val isRowFlashing = index == currentIndex && isOverlayVisible

                                RoadbookRow(
                                    waypoint = waypoint,
                                    isActive = isActive,
                                    rowTripDistanceMeters = rowTripDistance,
                                    azimuthToNext = nextAzimuth,
                                    waypointNumber = (index + 1).toString(),
                                    userLocation = null,
                                    isFlashing = isRowFlashing,
                                    onTulipLongClick = {
                                        try {
                                            val geoUriString = "geo:${waypoint.latitude},${waypoint.longitude}?q=${waypoint.latitude},${waypoint.longitude}(Kratka_${index + 1})"
                                            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUriString))
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {}
                                    }
                                )
                            }

                            item {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillParentMaxHeight(0.8f)
                                        .background(Color.Black)
                                )
                            }
                        }

                        if (viewModel.tapsEnabled.value) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = { offset ->
                                                val isTopHalf = offset.y < size.height / 2
                                                onGenerateSignal(if (isTopHalf) ScrollDirection.UP else ScrollDirection.DOWN)
                                            },
                                            onLongPress = { offset ->
                                                val totalWidth = size.width
                                                val isInTulipColumn = offset.x >= totalWidth * 0.30f && offset.x <= totalWidth * 0.65f

                                                if (isInTulipColumn) {
                                                    val clickedItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { itemInfo ->
                                                        offset.y >= itemInfo.offset && offset.y <= (itemInfo.offset + itemInfo.size)
                                                    }
                                                    clickedItem?.let { itemInfo ->
                                                        val index = itemInfo.index
                                                        if (index in viewModel.waypointList.value.indices) {
                                                            val waypoint = viewModel.waypointList.value[index]
                                                            try {
                                                                val geoUriString = "geo:${waypoint.latitude},${waypoint.longitude}?q=${waypoint.latitude},${waypoint.longitude}(Kratka_${index + 1})"
                                                                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUriString))
                                                                context.startActivity(mapIntent)
                                                            } catch (e: Exception) {}
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading OpenRally GPX Structure...", color = Color.White)
                    }
                }
            }
        }

        if (viewModel.isPreviewMode.value) {
            Button(
                onClick = {
                    viewModel.isPreviewMode.value = false
                    viewModel.showStartupDialog.value = true
                    viewModel.startLocationUpdates(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEB3B)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .height(56.dp)
                    .fillMaxWidth(0.5f)
                    .border(3.dp, Color.Black, MaterialTheme.shapes.large),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "ZAKOŃCZ PODGLĄD",
                    fontFamily = RallyBold,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (viewModel.showStartupDialog.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .width(720.dp)
                        .wrapContentHeight(),
                    color = Color(0xFFFEFEFE),
                    shape = RoundedCornerShape(36.dp),
                    shadowElevation = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(48.dp),
                        verticalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "PGA RALLY ",
                                    fontFamily = Montserrat,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF2B2A29)
                                )
                                Text(
                                    text = "ROADBOOK READER",
                                    fontFamily = Montserrat,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFD73224)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Poprawnie wczytano etap. Zapoznaj się ze szczegółami odcinka oraz bieżącym stanem systemu",
                                fontFamily = Montserrat,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2B2A29).copy(alpha = 0.6f),
                                lineHeight = 26.sp
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Color(0xFFF1F0EF),
                                    shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp)
                                )
                                .height(IntrinsicSize.Max)
                        ) {
                            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(Color(0xFFD73224)))

                            Column(modifier = Modifier.padding(24.dp).weight(1f)) {
                                Text(text = "WCZYTANY ETAP:", fontFamily = Montserrat, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2A29).copy(alpha = 0.5f), letterSpacing = 1.sp)
                                Text(text = "Puszcza Zielonka - SS01", fontFamily = Montserrat, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF2B2A29))

                                Spacer(modifier = Modifier.height(18.dp))

                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                                    this.drawLine(
                                        color = Color(0xFF2B2A29).copy(alpha = 0.15f),
                                        start = Offset(0f, 0f),
                                        end = Offset(this.size.width, 0f),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "DYSTANS:", fontFamily = Montserrat, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2A29).copy(alpha = 0.5f), letterSpacing = 1.sp)
                                        Text(text = "39,5 km", fontFamily = Montserrat, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF2B2A29))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "LICZBA WP:", fontFamily = Montserrat, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2A29).copy(alpha = 0.5f), letterSpacing = 1.sp)
                                        Text(text = "${viewModel.waypointList.value.size}", fontFamily = Montserrat, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF2B2A29))
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFF2B2A29).copy(alpha = 0.08f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Stan baterii:", fontFamily = Montserrat, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2A29).copy(alpha = 0.8f))
                                Text(text = viewModel.batteryLevel.value, fontFamily = Montserrat, color = Color(0xFF388E3C), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Sygnał satelitarny:", fontFamily = Montserrat, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2A29).copy(alpha = 0.8f))
                                val isGpsActive = viewModel.isGpsActive.value
                                Text(
                                    text = if (isGpsActive) "Aktywny" else viewModel.gpsSignalQuality.value,
                                    fontFamily = Montserrat,
                                    color = if (isGpsActive) Color(0xFF388E3C) else Color(0xFF1976D2),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "Kontroler bluetooth:", fontFamily = Montserrat, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2A29).copy(alpha = 0.8f))
                                val isConnected = viewModel.bluetoothDeviceName.value != "Nie wykryto"
                                Text(
                                    text = viewModel.bluetoothDeviceName.value,
                                    fontFamily = Montserrat,
                                    color = if (isConnected) Color(0xFF388E3C) else Color(0xFFD73224),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            if (viewModel.bluetoothDeviceName.value != "Nie wykryto") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "Profil kontrolera:", fontFamily = Montserrat, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2B2A29).copy(alpha = 0.8f))
                                    Text(text = viewModel.controllerProfile.value, fontFamily = Montserrat, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                        }

                        Text(
                            text = "Wybór przycisku START uruchomi nawigację po wczytanym odcinku specjalnym.\n\nOpcja PRZEGLĄDAJ ROADBOOK pozwala na zapoznanie się z roadbookiem przed rozpoczęciem nawigacji i nie powoduje rozpoczęcia odcinka.",
                            fontFamily = Montserrat,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2B2A29).copy(alpha = 0.5f),
                            lineHeight = 24.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val isStarted = viewModel.isNavigationStarted.value

                            if (!isStarted) {
                                Button(
                                    onClick = { viewModel.enterPreviewMode() },
                                    shape = RoundedCornerShape(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEFEFE)),
                                    contentPadding = PaddingValues(vertical = 22.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(2.dp, Color(0xFF2B2A29), RoundedCornerShape(50.dp))
                                ) {
                                    Text("PRZEGLĄDAJ ROADBOOK", fontFamily = Montserrat, color = Color(0xFF2B2A29), fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                                }
                            }

                            Button(
                                onClick = onCancelNavigation,
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD73224).copy(alpha = 0.03f)),
                                contentPadding = PaddingValues(vertical = 22.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFFD73224).copy(alpha = 0.2f), RoundedCornerShape(50.dp))
                            ) {
                                Text("WYJDŹ", fontFamily = Montserrat, color = Color(0xFFD73224), fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }

                            Button(
                                onClick = {
                                    if (isStarted) {
                                        viewModel.resumeNavigation()
                                    } else {
                                        viewModel.confirmStart()
                                    }
                                    viewModel.startLocationUpdates(context)
                                },
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2A29)),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isStarted) { "WZNÓW" } else { "START" },
                                        fontFamily = Montserrat,
                                        color = Color(0xFFFEFEFE),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        modifier = Modifier.padding(vertical = 20.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .background(Color(0xFFD73224))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingNavigationMenu(
            modifier = Modifier.align(Alignment.BottomEnd),
            onSettingsClick = { viewModel.showSettings.value = true },
            onPauseClick = { context.executeControllerActionHub() },
            onRotationClick = onForceScreenRotation,
            isPaused = !viewModel.isGpsActive.value
        )
    }

    if (viewModel.showSettings.value) {
        RallySettingsDialog(
            viewModel = viewModel,
            onDismissRequest = { viewModel.showSettings.value = false }
        )
    }
}

@Composable
fun RallySettingsDialog(
    viewModel: RoadbookViewModel,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current

    val rallyRed = Color(0xFFD73224) // Kolor AccentRed z SettingsScreen
    val darkGray = Color(0xFF2B2A29) // Kolor TextPrimary z SettingsScreen
    val lightBackground = Color(0xFFF4F3F2) // Kolor BgColor z SettingsScreen
    val surfaceWhite = Color(0xFFFEFEFE)

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = lightBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ROADBOOK ",
                        fontFamily = Montserrat,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = darkGray
                    )
                    Text(
                        text = "SETTINGS",
                        fontFamily = Montserrat,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = rallyRed
                    )
                }

                HorizontalDivider(color = darkGray.copy(alpha = 0.08f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Layout Scale", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = darkGray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = darkGray,
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                val currentPct = (viewModel.uiScale.value * 100).roundToInt()
                                val newPct = (currentPct - 10).coerceAtLeast(80)
                                viewModel.uiScale.value = newPct / 100f
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("-", fontFamily = Montserrat, color = surfaceWhite, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "${(viewModel.uiScale.value * 100).roundToInt()}%",
                            fontFamily = Montserrat,
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontWeight = FontWeight.Bold,
                            color = darkGray
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = darkGray,
                            modifier = Modifier.size(32.dp),
                            onClick = {
                                val currentPct = (viewModel.uiScale.value * 100).roundToInt()
                                val newPct = (currentPct + 10).coerceAtMost(120)
                                viewModel.uiScale.value = newPct / 100f
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+", fontFamily = Montserrat, color = surfaceWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = darkGray.copy(alpha = 0.08f), thickness = 1.dp)

                val switchColors = SwitchDefaults.colors(
                    checkedThumbColor = surfaceWhite,
                    checkedTrackColor = rallyRed,
                    uncheckedThumbColor = darkGray,
                    uncheckedTrackColor = Color(0xFFF1F0EF),
                    checkedBorderColor = rallyRed,
                    uncheckedBorderColor = darkGray.copy(alpha = 0.12f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Orientacja pozioma", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = darkGray)
                        Text("Zablokuj interfejs w poziomie", fontFamily = Montserrat, fontSize = 12.sp, color = darkGray.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = viewModel.isLandscapeOrientation.value,
                        onCheckedChange = { viewModel.isLandscapeOrientation.value = it },
                        colors = switchColors
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tap to Scroll", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = darkGray)
                    Switch(
                        checked = viewModel.tapsEnabled.value,
                        onCheckedChange = { viewModel.tapsEnabled.value = it },
                        colors = switchColors
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Scroll Roadbook", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = darkGray)
                    Switch(
                        checked = viewModel.isAutoScrollEnabled.value,
                        onCheckedChange = { viewModel.isAutoScrollEnabled.value = it },
                        colors = switchColors
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("GPS Simulation", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = darkGray)
                        Text("Drives route at 72 km/h", fontFamily = Montserrat, fontSize = 12.sp, color = darkGray.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = viewModel.isSimulationMode.value,
                        onCheckedChange = { viewModel.toggleSimulation(it) },
                        colors = switchColors
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // === DOKŁADNIE PRZENIESIONY STYL BUTTONU Z SETTINGSSCREEN ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // WARSTWA 1: Czerwone "podkreślenie"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .offset(y = 4.dp)
                            .background(rallyRed, RoundedCornerShape(50))
                    )

                    // WARSTWA 2: Główny, czarny przycisk
                    Button(
                        onClick = {
                            viewModel.saveSettings(context)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = darkGray)
                    ) {
                        Text(
                            text = "SAVE SETTINGS",
                            fontFamily = Montserrat,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = surfaceWhite,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}