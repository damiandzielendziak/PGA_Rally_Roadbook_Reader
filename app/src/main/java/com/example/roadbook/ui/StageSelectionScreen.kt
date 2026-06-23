package com.example.roadbook.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.roadbook.R
import com.example.roadbook.model.RallyStage
import com.example.roadbook.model.StageCategory
import com.example.roadbook.ui.theme.Montserrat
import com.example.roadbook.ui.theme.RallyBold
import com.example.roadbook.viewmodel.RoadbookViewModel
import com.example.roadbook.viewmodel.ImportResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

private val SelectionArrowBackIcon: ImageVector
    get() = ImageVector.Builder(
        name = "SelectionArrowBack",
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
    val coroutineScope = rememberCoroutineScope()

    val gravelBg = Color(0xFFF4F3F2)
    val deepGraphite = Color(0xFF2B2A29)
    val rallyRed = Color(0xFFD73224)
    val surfaceWhite = Color(0xFFFEFEFE)

    var selectedTabIndex by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var infoDialogMessage by remember { mutableStateOf("") }

    var stageToDeleteFromList by remember { mutableStateOf<RallyStage?>(null) }
    var showDeleteConfirmFromList by remember { mutableStateOf(false) }

    // Stan wyciągnięty do ViewModelu (Odporność na niszczenie widoku)
    val selectedStageId = viewModel.selectedStageId.value
    val selectedStageForPreview = remember(selectedStageId, viewModel.availableStages.value) {
        viewModel.availableStages.value.find { it.id == selectedStageId }
    }

    // PRIORYTETOWY POWRÓT: Synchronizacja odpala się poprawnie przy wejściu na ten ekran
    LaunchedEffect(Unit) {
        viewModel.checkForStageUpdates(context)
    }

    LaunchedEffect(viewModel.cloudImportResult.value) {
        when (val result = viewModel.cloudImportResult.value) {
            is ImportResult.Success -> {
                infoDialogMessage = result.message
                showSuccessDialog = true
                viewModel.cloudImportResult.value = ImportResult.Idle
            }
            is ImportResult.Error -> {
                infoDialogMessage = result.message
                showSuccessDialog = true
                viewModel.cloudImportResult.value = ImportResult.Idle
            }
            ImportResult.Idle -> {}
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

    val allStages = viewModel.availableStages.value
    val filteredStages = allStages.filter {
        val matchesTab = if (selectedTabIndex == 0) it.category == StageCategory.SYSTEM else it.category == StageCategory.USER
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true)
        matchesTab && matchesSearch
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. SŁAJD LISTY GŁÓWNEJ TRAS
        AnimatedVisibility(
            visible = selectedStageForPreview == null,
            enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(400)),
            exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(400))
        ) {
            Scaffold(
                containerColor = gravelBg
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 12.dp)
                    ) {
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
                                    Icon(
                                        imageVector = SelectionArrowBackIcon,
                                        contentDescription = "Wróć",
                                        tint = deepGraphite,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(color = deepGraphite)) { append("BAZA TRAS") }
                                        withStyle(style = SpanStyle(color = rallyRed)) { append(" - PGA RALLY") }
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
                                text = "Wybierz odcinek drogowy lub etap specjalny, aby załadować go do komputera nawigacyjnego.",
                                fontFamily = Montserrat,
                                fontSize = 18.sp,
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
                                            Text("Wyszukaj trasę po nazwie...", fontFamily = Montserrat, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = deepGraphite.copy(alpha = 0.4f))
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
                            if (viewModel.isSyncingFromServer.value && filteredStages.isEmpty()) {
                                CustomRallyLoader(color = rallyRed)
                            } else {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = PaddingValues(bottom = 100.dp)
                                    ) {
                                        items(filteredStages) { stage ->
                                            var showContextMenu by remember { mutableStateOf(false) }

                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                RouteCard(
                                                    stage = stage,
                                                    isExpired = viewModel.isStageExpired(stage.id),
                                                    onClick = { viewModel.selectedStageId.value = stage.id },
                                                    onLongClick = {
                                                        if (stage.category == StageCategory.USER) {
                                                            showContextMenu = true
                                                        }
                                                    }
                                                )

                                                DropdownMenu(
                                                    expanded = showContextMenu,
                                                    onDismissRequest = { showContextMenu = false },
                                                    modifier = Modifier.background(surfaceWhite)
                                                ) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                "USUŃ TRASĘ z bazy",
                                                                fontFamily = Montserrat,
                                                                fontWeight = FontWeight.Bold,
                                                                color = rallyRed
                                                            )
                                                        },
                                                        onClick = {
                                                            showContextMenu = false
                                                            stageToDeleteFromList = stage
                                                            showDeleteConfirmFromList = true
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (viewModel.isSyncingFromServer.value) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CustomRallyLoader(color = rallyRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 28.dp)
                            .fillMaxWidth(0.65f)
                            .height(72.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = rallyRed),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
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

        // 2. SŁAJD EKRANU SZCZEGÓŁÓW ODCINKA
        AnimatedVisibility(
            visible = selectedStageForPreview != null,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400)),
            modifier = Modifier.fillMaxSize()
        ) {
            if (selectedStageForPreview != null) {
                StageDetailsScreen(
                    stage = selectedStageForPreview,
                    onBackClick = { viewModel.selectedStageId.value = null },
                    onPreviewRoadbookClick = {
                        val finalStage = selectedStageForPreview
                        viewModel.selectedStageId.value = finalStage.id
                        viewModel.activeStageMetadata.value = finalStage
                        viewModel.loadGpxDataForStage(context, finalStage.id)

                        viewModel.enterPreviewMode()
                        onStageSelected(finalStage)
                    },
                    onStartClick = {
                        val finalStage = selectedStageForPreview
                        viewModel.selectedStageId.value = finalStage.id
                        viewModel.activeStageMetadata.value = finalStage
                        viewModel.loadGpxDataForStage(context, finalStage.id)

                        viewModel.isPreviewMode.value = false
                        viewModel.showStartupDialog.value = true
                        onStageSelected(finalStage)
                    },
                    onDeleteClick = {
                        viewModel.deleteUserStage(selectedStageForPreview.id)
                        viewModel.selectedStageId.value = null
                    }
                )
            }
        }
    }
}

@Composable
fun RouteCard(
    stage: RallyStage,
    isExpired: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val surfaceWhite = Color(0xFFFEFEFE)
    val deepGraphite = Color(0xFF2B2A29)
    val rallyRed = Color(0xFFD73224)
    val activeGreen = Color(0xFF2E7D32)
    val thumbnailBg = Color(0xFFEAE9E8)

    val formattedDistance = String.format("%.1f", stage.distanceKm).replace('.', ',')

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(thumbnailBg)
            ) {
                if (!stage.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = stage.imageUrl,
                        contentDescription = "Podgląd mapy odcinka z serwera",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.map_placeholder),
                        error = painterResource(id = R.drawable.map_placeholder)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.map_placeholder),
                        contentDescription = "Domyślny podgląd mapy odcinka",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
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

                if (!stage.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stage.description,
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = deepGraphite.copy(alpha = 0.6f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                if (!stage.dominantSurface.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .background(deepGraphite.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "NAWIERZCHNIA: ${stage.dominantSurface.uppercase()}",
                            fontFamily = Montserrat,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = deepGraphite.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$formattedDistance km",
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = deepGraphite
                    )
                    Text(
                        text = "  •  ",
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = rallyRed
                    )
                    Text(
                        text = "${stage.waypointCount} WP",
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = deepGraphite
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = deepGraphite.copy(alpha = 0.06f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wersja: ${stage.version}",
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = deepGraphite.copy(alpha = 0.5f)
                    )

                    Text(
                        text = if (isExpired) "STATUS: WYGASŁA" else "STATUS: AKTYWNA",
                        fontFamily = Montserrat,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isExpired) rallyRed else activeGreen
                    )
                }
            }
        }
    }
}