package com.example.roadbook.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roadbook.model.RallyStage
import com.example.roadbook.model.StageCategory
import com.example.roadbook.model.StageStatus
import com.example.roadbook.ui.theme.Montserrat
import com.example.roadbook.ui.theme.RallyBold
import com.example.roadbook.viewmodel.RoadbookViewModel

@Composable
fun CustomRallyLoader(modifier: Modifier = Modifier, color: Color) {
    val transition = rememberInfiniteTransition(label = "spinner_trans")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_rot"
    )

    Canvas(modifier = modifier.size(56.dp)) {
        val thickness = 9.dp.toPx()
        val radius = (size.width - thickness) / 2f

        rotate(rotation) {
            rotate(-90f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        1.0f to color
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = thickness, cap = StrokeCap.Butt),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(thickness / 2f, thickness / 2f)
                )
            }

            drawCircle(
                color = color,
                radius = thickness / 2f,
                center = Offset(size.width / 2f, thickness / 2f)
            )
        }
    }
}

@Composable
fun CustomArrowBackIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val strokeWidth = 3.dp.toPx()
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

@Composable
fun CustomSearchIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val strokeWidth = 3.dp.toPx()
        val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(color = color, radius = 8f * scaleX, center = Offset(11f * scaleX, 11f * scaleY), style = strokeStyle)
        drawLine(color = color, start = Offset(21f * scaleX, 21f * scaleY), end = Offset(16.65f * scaleX, 16.65f * scaleY), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun StageSelectionScreen(
    viewModel: RoadbookViewModel,
    onBackClick: () -> Unit,
    onStageSelected: (RallyStage) -> Unit
) {
    val context = LocalContext.current

    val gravelBg = Color(0xFFF4F3F2)
    val deepGraphite = Color(0xFF2B2A29)
    val rallyRed = Color(0xFFD73224)
    val surfaceWhite = Color(0xFFFEFEFE)

    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedStageForPreview by remember { mutableStateOf<RallyStage?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var infoDialogMessage by remember { mutableStateOf("") }
    var isCloudImportActive by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.isSyncingFromServer.value) {
        if (!viewModel.isSyncingFromServer.value && isCloudImportActive) {
            isCloudImportActive = false
            infoDialogMessage = "Pomyślnie zaimportowano roadbook."
            showSuccessDialog = true
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { realFileUri ->
            viewModel.importUserGpxFile(context, realFileUri)
            infoDialogMessage = "Import pliku zakończył się sukcesem."
            showSuccessDialog = true
        }
    }

    // NATYCHMIASTOWE ODPALENIE: Odświeżenie bazy tras przy każdym wejściu w ekran
    LaunchedEffect(Unit) {
        viewModel.checkForStageUpdates(context)
    }

    val allStages = viewModel.availableStages.value
    val filteredStages = allStages.filter {
        val matchesTab = if (selectedTabIndex == 0) it.category == StageCategory.SYSTEM else it.category == StageCategory.USER
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true)
        matchesTab && matchesSearch
    }

    if (selectedStageForPreview != null) {
        StageDetailsScreen(
            stage = selectedStageForPreview!!,
            onBackClick = { selectedStageForPreview = null },
            onPreviewRoadbookClick = { },
            onStartClick = {
                val finalStage = selectedStageForPreview!!
                selectedStageForPreview = null
                onStageSelected(finalStage)
            },
            onDeleteClick = {
                viewModel.deleteUserStage(selectedStageForPreview!!.id)
                selectedStageForPreview = null
            }
        )
    } else {
        Scaffold(
            containerColor = gravelBg,
            bottomBar = {
                Surface(
                    color = surfaceWhite,
                    shadowElevation = 24.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { showImportDialog = true },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(72.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = rallyRed)
                        ) {
                            Text(
                                text = "IMPORTUJ ROADBOOK",
                                fontFamily = Montserrat,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = 2.sp,
                                color = surfaceWhite
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 32.dp)
                ) {
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
                                CustomArrowBackIcon(modifier = Modifier.size(32.dp), color = deepGraphite)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = deepGraphite)) { append("BAZA TRAS ") }
                                    withStyle(style = SpanStyle(color = rallyRed)) { append("PGA RALLY") }
                                },
                                fontFamily = RallyBold,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }

                        Text(
                            text = "Wybierz odcinek drogowy lub etap specjalny, aby załadować go do komputera nawigacyjnego.",
                            fontFamily = Montserrat,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = deepGraphite.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 72.dp, bottom = 24.dp),
                            lineHeight = 22.sp
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .background(surfaceWhite, RoundedCornerShape(50.dp))
                                .border(3.dp, Color.Transparent, RoundedCornerShape(50.dp))
                                .padding(24.dp, 8.dp, 8.dp, 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = deepGraphite),
                                modifier = Modifier.fillMaxWidth().padding(end = 80.dp),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Wyszukaj trasę po nazwie lub regionie...", fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = deepGraphite.copy(alpha = 0.4f))
                                    }
                                    innerTextField()
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(64.dp)
                                    .background(deepGraphite, CircleShape)
                                    .clickable { },
                                contentAlignment = Alignment.Center
                            ) {
                                CustomSearchIcon(modifier = Modifier.size(24.dp), color = surfaceWhite)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                thickness = 2.dp,
                                color = deepGraphite.copy(alpha = 0.06f)
                            )
                            Row(modifier = Modifier.fillMaxWidth()) {
                                val tabs = listOf("Trasy Systemowe", "Trasy Użytkownika")
                                tabs.forEachIndexed { index, title ->
                                    val isActive = selectedTabIndex == index
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { selectedTabIndex = index },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = title.uppercase(),
                                            fontFamily = Montserrat,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            letterSpacing = 2.sp,
                                            color = deepGraphite.copy(alpha = if (isActive) 1f else 0.4f),
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(rallyRed, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // PRODUKCYJNA CZYSTA SYNCHRONIZACJA: Sam loader, zero komunikatów tekstowych
                        if (viewModel.isSyncingFromServer.value) {
                            CustomRallyLoader(color = rallyRed)
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(filteredStages) { stage ->
                                    RouteCard(
                                        stage = stage,
                                        onClick = { selectedStageForPreview = stage }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showImportDialog) {
                    ImportRoadbookDialog(
                        onDismissRequest = { showImportDialog = false },
                        onFileBrowseClick = {
                            showImportDialog = false
                            filePickerLauncher.launch("*/*")
                        },
                        onDownloadWithCodeClick = { code ->
                            showImportDialog = false
                            isCloudImportActive = true
                            viewModel.importRoadbookFromCloud(context, code)
                        }
                    )
                }

                val duplicateToOverwrite = viewModel.pendingOverwriteResult.value
                if (duplicateToOverwrite != null) {
                    AlertDialog(
                        onDismissRequest = { viewModel.resolveOverwrite(false) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.resolveOverwrite(true)
                                    infoDialogMessage = "Trasa została pomyślnie nadpisana."
                                    showSuccessDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = rallyRed),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("TAK", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = surfaceWhite)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.resolveOverwrite(false) }) {
                                Text("NIE", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = deepGraphite)
                            }
                        },
                        title = {
                            Text(
                                text = "DUPLIKAT TRASY",
                                fontFamily = RallyBold,
                                fontSize = 24.sp,
                                color = deepGraphite,
                                letterSpacing = 1.sp
                            )
                        },
                        text = {
                            Text(
                                text = "Trasa o podanej nazwie \"${duplicateToOverwrite.title}\" już istnieje. Czy nadpisać?",
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

                if (showSuccessDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            selectedTabIndex = 1
                            showSuccessDialog = false
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    selectedTabIndex = 1
                                    showSuccessDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = deepGraphite),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("OK", fontFamily = Montserrat, fontWeight = FontWeight.Bold, color = surfaceWhite)
                            }
                        },
                        title = {
                            Text(
                                text = "ZAKOŃCZONO",
                                fontFamily = RallyBold,
                                fontSize = 24.sp,
                                color = Color(0xFF2E7D32),
                                letterSpacing = 1.sp
                            )
                        },
                        text = {
                            Text(
                                text = infoDialogMessage,
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
    }
}

@Composable
fun RouteCard(
    stage: RallyStage,
    onClick: () -> Unit
) {
    val surfaceWhite = Color(0xFFFEFEFE)
    val deepGraphite = Color(0xFF2B2A29)
    val rallyRed = Color(0xFFD73224)
    val thumbnailBg = Color(0xFFEAE9E8)

    val formattedDistance = String.format("%.1f", stage.distanceKm).replace('.', ',')

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(thumbnailBg)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    val gridSize = 24.dp.toPx()
                    var x = 0f
                    while (x < canvasWidth) {
                        drawLine(color = deepGraphite.copy(alpha = 0.04f), start = Offset(x, 0f), end = Offset(x, canvasHeight), strokeWidth = 2f)
                        x += gridSize
                    }
                    var y = 0f
                    while (y < canvasHeight) {
                        drawLine(color = deepGraphite.copy(alpha = 0.04f), start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 2f)
                        y += gridSize
                    }

                    val path = Path().apply {
                        val startX = canvasWidth * 0.1f
                        val startY = canvasHeight * 0.8f
                        moveTo(startX, startY)
                        val offset1 = if (stage.title.length % 2 == 0) 0.5f else 0.3f
                        val offset2 = if (stage.waypointCount % 2 == 0) 0.3f else 0.7f
                        quadraticBezierTo(canvasWidth * offset1, canvasHeight * offset2, canvasWidth * 0.9f, canvasHeight * 0.2f)
                    }

                    drawPath(path = path, color = deepGraphite, style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round), alpha = 0.85f)
                    drawCircle(color = rallyRed, radius = 18f, center = Offset(canvasWidth * 0.1f, canvasHeight * 0.8f))
                    drawCircle(color = deepGraphite, radius = 12f, center = Offset(canvasWidth * 0.5f, canvasHeight * 0.5f))
                    drawCircle(color = rallyRed, radius = 18f, center = Offset(canvasWidth * 0.9f, canvasHeight * 0.2f))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stage.title,
                    fontFamily = Montserrat,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = deepGraphite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp,
                    modifier = Modifier.height(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "$formattedDistance km", fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = deepGraphite.copy(alpha = 0.7f), maxLines = 1)
                    Text(text = "•", fontFamily = Montserrat, fontWeight = FontWeight.Black, fontSize = 16.sp, color = rallyRed)
                    Text(text = "${stage.waypointCount} WP", fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = deepGraphite.copy(alpha = 0.7f), maxLines = 1)
                }
            }
        }
    }
}