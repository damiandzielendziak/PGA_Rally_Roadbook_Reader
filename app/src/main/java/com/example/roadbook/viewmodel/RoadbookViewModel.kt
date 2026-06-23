package com.example.roadbook.viewmodel

import android.content.Context
import android.location.Location
import android.net.Uri
import android.view.InputDevice
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

import com.example.roadbook.data.local.FileStorageManager
import com.example.roadbook.data.repository.StageRepository
import com.example.roadbook.domain.GpsSimulatorEngine
import com.example.roadbook.domain.RallyNavigationEngine
import com.example.roadbook.model.RallyWaypoint
import com.example.roadbook.model.parseGpxFile
import com.example.roadbook.model.ParsedGpxResult
import com.example.roadbook.model.RallyStage
import com.example.roadbook.model.StageCategory
import com.example.roadbook.model.StageStatus

sealed class ImportResult {
    object Idle : ImportResult()
    data class Success(val message: String) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

class RoadbookViewModel : ViewModel() {

    // --- TELEMETRY STATE ---
    var totalDistanceMeters = mutableStateOf(0.0)
    var tripDistanceMeters = mutableStateOf(0.0)
    var currentSpeed = mutableStateOf(0f)
    var capHeading = mutableStateOf(0f)
    var validatedWaypointsCount = mutableStateOf(0)

    // --- NAVIGATION STATE ---
    var waypointList = mutableStateOf<List<RallyWaypoint>>(emptyList())
    var activeWaypointIndex = mutableStateOf(0)
    var dtwDistance = mutableStateOf(0f)
    var dtwBearing = mutableStateOf(0f)
    var activeRallyWaypoint = mutableStateOf<RallyWaypoint?>(null)

    // --- INTERFACE & SIMULATION STATE ---
    var showSettings = mutableStateOf(false)
    var leftZonePressed = mutableStateOf(false)
    var rightZonePressed = mutableStateOf(false)
    var tapsEnabled = mutableStateOf(true)
    var isGpsActive = mutableStateOf(false)
    var isSimulationMode = mutableStateOf(false)
    var isAutoScrollEnabled = mutableStateOf(false)
    var isLandscapeOrientation = mutableStateOf(false)
    var isPreviewMode = mutableStateOf(false)

    // --- STAGE SELECTION STATE ---
    var availableStages = mutableStateOf<List<RallyStage>>(emptyList())
    var isSyncingFromServer = mutableStateOf(false)
    var syncStatusMessage = mutableStateOf("")
    var selectedStageId = mutableStateOf<String?>(null)

    var activeStageMetadata = mutableStateOf<RallyStage?>(null)
    var cloudImportResult = mutableStateOf<ImportResult>(ImportResult.Idle)

    var bluetoothDeviceName = mutableStateOf("Nie wykryto")
    var controllerProfile = mutableStateOf("Użyj dowolnego klawisza aby wykryć")
    var isNavigationStarted = mutableStateOf(false)
    var showStartupDialog = mutableStateOf(true)
    var batteryLevel = mutableStateOf("---")
    var gpsSignalQuality = mutableStateOf("Szukanie satelitów...")
    var uiScale = mutableStateOf(1.0f)

    private val navigationEngine = RallyNavigationEngine()
    private var locationEngine: GpsSimulatorEngine? = null
    private var adjustmentJob: Job? = null
    private var appContext: Context? = null

    var pendingOverwriteResult = mutableStateOf<ParsedGpxResult?>(null)

    private val stagePasswords = mutableMapOf<String, String>()
    private val stageExpirations = mutableMapOf<String, Long>()
    private val unlockedStagesByPilot = mutableSetOf<String>()

    init {
        showStartupDialog.value = true
        isNavigationStarted.value = false
    }

    fun isStageExpired(stageId: String): Boolean {
        val expirationTimestamp = stageExpirations[stageId] ?: return false
        if (expirationTimestamp == 0L) return false
        return System.currentTimeMillis() > expirationTimestamp
    }

    fun requiresPassword(stageId: String): Boolean {
        val password = stagePasswords[stageId]
        return !password.isNullOrBlank() && !unlockedStagesByPilot.contains(stageId)
    }

    fun unlockStageWithPassword(stageId: String, input: String): Boolean {
        val correctPassword = stagePasswords[stageId] ?: return true
        if (correctPassword == input) {
            unlockedStagesByPilot.add(stageId)
            val context = appContext ?: return true
            FileStorageManager(context).saveUserStages(
                userStages = availableStages.value.filter { it.category == StageCategory.USER },
                passwords = stagePasswords,
                expirations = stageExpirations,
                unlocked = unlockedStagesByPilot
            )
            return true
        }
        return false
    }

    private fun initLocationEngine(context: Context) {
        if (locationEngine == null) {
            locationEngine = GpsSimulatorEngine(
                context = context.applicationContext,
                onLocationProcessed = { location, speedKmh, distanceMoved, bearing ->
                    if (!isPreviewMode.value && !showStartupDialog.value) {
                        currentSpeed.value = speedKmh
                        if (distanceMoved > 0.0) {
                            capHeading.value = bearing
                            totalDistanceMeters.value += distanceMoved
                            tripDistanceMeters.value += distanceMoved
                        }
                        updateNavigationRow(location.latitude, location.longitude)
                    }
                },
                onSignalQualityChanged = { gpsSignalQuality.value = it },
                onBatteryLevelChanged = { batteryLevel.value = it }
            )
        }
    }

    private fun updateNavigationRow(lat: Double, lon: Double) {
        val startWp = waypointList.value.getOrNull(0)
        if (validatedWaypointsCount.value == 0 && startWp != null) {
            activeWaypointIndex.value = 0
            activeRallyWaypoint.value = startWp
            if (totalDistanceMeters.value < 15.0) validatedWaypointsCount.value = 1
            return
        }

        val result = navigationEngine.calculateNavigation(
            currentLat = lat,
            currentLon = lon,
            capHeading = capHeading.value,
            totalDistanceMeters = totalDistanceMeters.value,
            waypointList = waypointList.value,
            currentValidatedCount = validatedWaypointsCount.value
        )
        activeWaypointIndex.value = result.activeWaypointIndex
        validatedWaypointsCount.value = result.validatedWaypointsCount
        activeRallyWaypoint.value = result.activeRallyWaypoint
        dtwDistance.value = result.dtwDistance
        dtwBearing.value = result.dtwBearing
    }

    fun checkForStageUpdates(context: Context) {
        if (appContext == null) appContext = context.applicationContext
        initLocationEngine(context)
        val storageManager = FileStorageManager(context)
        val repository = StageRepository(context, storageManager)

        viewModelScope.launch {
            val cachedUser = storageManager.loadUserStages(stagePasswords, stageExpirations, unlockedStagesByPilot)
            val cachedSystem = storageManager.loadSystemStagesCache()
            availableStages.value = cachedSystem + cachedUser

            isSyncingFromServer.value = true
            syncStatusMessage.value = "Synchronizacja etapów..."

            try {
                val syncedResults = repository.syncStages(cachedSystem, cachedUser)
                availableStages.value = syncedResults
                syncStatusMessage.value = "Baza zsynchronizowana (${syncedResults.size} tras)"
            } catch (e: Exception) {
                val readableError = "Błąd połączenia: ${e.localizedMessage ?: e.javaClass.simpleName}"
                syncStatusMessage.value = readableError
            } finally {
                isSyncingFromServer.value = false
            }
        }
    }

    fun importUserGpxFile(context: Context, fileUri: Uri) {
        val storageManager = FileStorageManager(context)
        try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                val result = parseGpxFile(inputStream)
                val newId = "user_${System.currentTimeMillis()}"

                context.contentResolver.openInputStream(fileUri)?.use { freshStream ->
                    storageManager.saveGpxFile(newId, freshStream)
                }

                val newStage = RallyStage(
                    id = newId, title = result.title, distanceKm = result.distanceKm,
                    waypointCount = result.waypointCount, category = StageCategory.USER,
                    status = StageStatus.LOCAL, version = "v1.0",
                    description = "Ręcznie zaimportowany odcinek treningowy.", dominantSurface = "Szuter / Ziemia"
                )

                if (availableStages.value.any { it.title.equals(result.title, ignoreCase = true) }) {
                    pendingOverwriteResult.value = result
                } else {
                    availableStages.value = availableStages.value + newStage
                    storageManager.saveUserStages(availableStages.value.filter { it.category == StageCategory.USER }, stagePasswords, stageExpirations, unlockedStagesByPilot)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resolveOverwrite(proceed: Boolean) {
        val result = pendingOverwriteResult.value
        pendingOverwriteResult.value = null
        val context = appContext ?: return
        val storageManager = FileStorageManager(context)

        if (proceed && result != null) {
            val cleanList = availableStages.value.filter { !it.title.equals(result.title, ignoreCase = true) }
            val newId = "user_${System.currentTimeMillis()}"
            val newStage = RallyStage(newId, result.title, result.distanceKm, result.waypointCount, StageCategory.USER, StageStatus.LOCAL, "v1.0", "", "")
            availableStages.value = cleanList + newStage
            storageManager.saveUserStages(availableStages.value.filter { it.category == StageCategory.USER }, stagePasswords, stageExpirations, unlockedStagesByPilot)
        }
    }

    fun importRoadbookFromCloud(context: Context, code: String) {
        if (isSyncingFromServer.value) return
        val storageManager = FileStorageManager(context)
        val repository = StageRepository(context, storageManager)
        val cleanCode = code.uppercase().trim()

        viewModelScope.launch {
            isSyncingFromServer.value = true
            try {
                val stageData = repository.fetchCloudStage(cleanCode)
                if (stageData != null) {
                    val title = stageData.getString("title")
                    if (!availableStages.value.any { it.title.equals(title, ignoreCase = true) }) {
                        val newId = "cloud_${System.currentTimeMillis()}"
                        val remoteGpxUrl = stageData.getString("file_url").trim()

                        if (repository.downloadCloudGpx(remoteGpxUrl, newId)) {
                            stageData.optString("password", "").let { if(it.isNotBlank()) stagePasswords[newId] = it }
                            stageData.optLong("expires_at", 0L).let { if(it > 0L) stageExpirations[newId] = it }

                            val cloudStage = RallyStage(
                                id = newId, title = title, distanceKm = stageData.getDouble("distance_km"),
                                waypointCount = stageData.getInt("waypoint_count"), category = StageCategory.USER,
                                status = StageStatus.LOCAL, version = stageData.getString("version").trim(),
                                description = stageData.optString("description", "Trasa pobrana z chmury."),
                                dominantSurface = stageData.optString("dominant_surface", "Szuter")
                            )
                            availableStages.value = availableStages.value + cloudStage
                            storageManager.saveUserStages(availableStages.value.filter { it.category == StageCategory.USER }, stagePasswords, stageExpirations, unlockedStagesByPilot)
                            cloudImportResult.value = ImportResult.Success("Pomyślnie zaimportowano roadbook: $title")
                        } else {
                            cloudImportResult.value = ImportResult.Error("Błąd serwera: Nie pobrano pliku GPX.")
                        }
                    } else {
                        cloudImportResult.value = ImportResult.Error("Operacja anulowana: Trasa już istnieje.")
                    }
                } else {
                    cloudImportResult.value = ImportResult.Error("Błąd pobierania: Nieprawidłowy kod.")
                }
            } catch (e: Exception) {
                cloudImportResult.value = ImportResult.Error("Awaria chmury: ${e.localizedMessage}")
            } finally { // FIX: military -> finally
                isSyncingFromServer.value = false
            }
        }
    }

    fun deleteUserStage(stageId: String) {
        val context = appContext ?: return
        val storageManager = FileStorageManager(context)
        availableStages.value = availableStages.value.filter { it.id != stageId }
        stagePasswords.remove(stageId)
        stageExpirations.remove(stageId)
        unlockedStagesByPilot.remove(stageId)
        storageManager.deleteGpxFile(stageId)
        storageManager.saveUserStages(availableStages.value.filter { it.category == StageCategory.USER }, stagePasswords, stageExpirations, unlockedStagesByPilot)
    }

    fun checkConnectedControllers() {
        val deviceIds = InputDevice.getDeviceIds()
        var foundName = "Nie wykryto"
        for (id in deviceIds) {
            val device = InputDevice.getDevice(id)
            if (device != null) {
                val name = device.name
                if (name.contains("PGA", ignoreCase = true) || name.contains("Rally", ignoreCase = true) || name.contains("Controller", ignoreCase = true)) {
                    foundName = name
                    break
                }
            }
        }
        bluetoothDeviceName.value = foundName
    }

    fun confirmStart() {
        showStartupDialog.value = false
        isPreviewMode.value = false
        isNavigationStarted.value = true
        totalDistanceMeters.value = 0.0
        tripDistanceMeters.value = 0.0
        validatedWaypointsCount.value = 0
        activeWaypointIndex.value = 0
        currentSpeed.value = 0f
    }

    fun resumeNavigation() {
        showStartupDialog.value = false
        isPreviewMode.value = false
        currentSpeed.value = 0f
    }

    fun enterPreviewMode() {
        showStartupDialog.value = false
        isPreviewMode.value = true
        stopLocationUpdates()
        currentSpeed.value = 0f
    }

    fun loadSettings(context: Context) {
        val sharedPreferences = context.getSharedPreferences("RoadbookSettings", Context.MODE_PRIVATE)
        uiScale.value = sharedPreferences.getFloat("UI_SCALE", 1.0f)
        isLandscapeOrientation.value = sharedPreferences.getBoolean("IS_LANDSCAPE", false)
        tapsEnabled.value = sharedPreferences.getBoolean("TAPS_ENABLED", true)
        isAutoScrollEnabled.value = sharedPreferences.getBoolean("AUTO_SCROLL", false)
        isSimulationMode.value = sharedPreferences.getBoolean("SIMULATION_MODE", false)
    }

    fun saveSettings(context: Context) {
        context.getSharedPreferences("RoadbookSettings", Context.MODE_PRIVATE).edit().apply {
            putFloat("UI_SCALE", uiScale.value)
            putBoolean("IS_LANDSCAPE", isLandscapeOrientation.value)
            putBoolean("TAPS_ENABLED", tapsEnabled.value)
            putBoolean("AUTO_SCROLL", isAutoScrollEnabled.value)
            putBoolean("SIMULATION_MODE", isSimulationMode.value)
            apply()
        }
        showSettings.value = false
    }

    fun loadGpxDataForStage(context: Context, stageId: String) {
        if (appContext == null) appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val downloadedFile = File(context.filesDir, "$stageId.gpx")
                    val inputStream = if (downloadedFile.exists()) {
                        context.openFileInput("$stageId.gpx")
                    } else {
                        try { context.assets.open("$stageId.gpx") } catch (e: Exception) { null }
                    }
                    inputStream?.use { stream -> parseGpxFile(stream) }
                }

                if (result != null) {
                    waypointList.value = result.waypoints
                    if (result.waypoints.isNotEmpty()) {
                        activeWaypointIndex.value = 0
                        validatedWaypointsCount.value = 0
                        activeRallyWaypoint.value = result.waypoints.firstOrNull { it.waypointIndex > 0 }
                    }
                } else {
                    throw FileNotFoundException("Brak pliku GPX")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                waypointList.value = emptyList()
                activeWaypointIndex.value = 0
                validatedWaypointsCount.value = 0
                activeRallyWaypoint.value = null
            }
        }
    }

    fun loadGpxData(context: Context) {
        val currentStageId = selectedStageId.value ?: "sys_01"
        if (activeStageMetadata.value == null) {
            activeStageMetadata.value = availableStages.value.find { it.id == currentStageId }
        }
        loadGpxDataForStage(context, currentStageId)
    }

    fun updateBatteryStatus(context: Context) { initLocationEngine(context); locationEngine?.updateBatteryStatus() }

    fun toggleSimulation(enable: Boolean) {
        if (isPreviewMode.value) return
        isSimulationMode.value = enable
        isGpsActive.value = enable
        val context = appContext ?: return
        initLocationEngine(context)
        locationEngine?.toggleSimulation(enable, waypointList.value, viewModelScope)
        if (!enable) currentSpeed.value = 0f
    }

    fun startLocationUpdates(context: Context) {
        if (isSimulationMode.value || isPreviewMode.value) return
        isGpsActive.value = true
        initLocationEngine(context)
        locationEngine?.startLocationUpdates()
    }

    fun stopLocationUpdates() {
        if (!isSimulationMode.value && !isPreviewMode.value) isGpsActive.value = false
        locationEngine?.stopLocationUpdates()
        currentSpeed.value = 0f
    }

    fun startIncrementLoop() {
        if (adjustmentJob?.isActive == true) return
        adjustmentJob = viewModelScope.launch {
            var counter = 0
            while (isActive && leftZonePressed.value) {
                counter++
                val step = when { counter <= 5 -> 10.0; counter <= 12 -> 100.0; counter <= 20 -> 500.0; else -> 1000.0 }
                if (validatedWaypointsCount.value > 0) { totalDistanceMeters.value += step; recalculateRoadbookRowOnly() }
                delay(150)
            }
        }
    }

    fun startDecrementLoop() {
        if (adjustmentJob?.isActive == true) return
        adjustmentJob = viewModelScope.launch {
            var counter = 0
            while (isActive && rightZonePressed.value) {
                counter++
                val step = when { counter <= 5 -> 10.0; counter <= 12 -> 100.0; counter <= 20 -> 500.0; else -> 1000.0 }
                if (validatedWaypointsCount.value > 0) {
                    totalDistanceMeters.value = (totalDistanceMeters.value - step).coerceAtLeast(0.0)
                    recalculateRoadbookRowOnly()
                }
                delay(150)
            }
        }
    }

    fun recalculateRoadbookRowOnlyPublic() { recalculateRoadbookRowOnly() }

    private fun recalculateRoadbookRowOnly() {
        if (validatedWaypointsCount.value == 0) { activeWaypointIndex.value = 0; return }
        val list = waypointList.value
        val odo = totalDistanceMeters.value
        var detectedIndex = list.lastIndex
        for (i in list.indices) { if (list[i].distanceMeters > odo) { detectedIndex = i; break } }
        activeWaypointIndex.value = detectedIndex
    }

    fun stopSmoothAdjustment() { adjustmentJob?.cancel() }

    override fun onCleared() {
        super.onCleared()
        stopSmoothAdjustment()
        stopLocationUpdates()
    }
}