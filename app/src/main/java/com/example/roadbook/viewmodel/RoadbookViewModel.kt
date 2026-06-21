package com.example.roadbook.viewmodel

import android.content.Context
import android.location.Location
import android.view.InputDevice
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

import com.example.roadbook.model.RallyWaypoint
import com.example.roadbook.model.parseGpxFile
import com.example.roadbook.model.GpsSimulator
import com.example.roadbook.domain.RallyNavigationEngine
import com.example.roadbook.infrastructure.GpsTracker
import com.example.roadbook.model.RallyStage
import com.example.roadbook.model.StageCategory
import com.example.roadbook.model.StageStatus

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
    var syncStatusMessage = mutableStateOf("Ostatnia synchronizacja: Nie sprawdzano")
    var selectedStageId = mutableStateOf<String?>(null)

    // Detekcja sprzętu BLE
    var bluetoothDeviceName = mutableStateOf("Nie wykryto")
    var controllerProfile = mutableStateOf("Użyj dowolnego klawisza aby wykryć")

    var isNavigationStarted = mutableStateOf(false)

    var showStartupDialog = mutableStateOf(true)
    var batteryLevel = mutableStateOf("---")
    var gpsSignalQuality = mutableStateOf("Szukanie satelitów...")

    var uiScale = mutableStateOf(1.0f)

    private val navigationEngine = RallyNavigationEngine()
    private var gpsTracker: GpsTracker? = null

    private var adjustmentJob: Job? = null
    private var gpsSimulator: GpsSimulator? = null
    private var lastKnownLocation: Location? = null

    init {
        showStartupDialog.value = true
        isNavigationStarted.value = false
    }

    // --- STAGE SELECTION LOGIC ---
    fun checkForStageUpdates(context: Context) {
        if (isSyncingFromServer.value) return

        viewModelScope.launch {
            isSyncingFromServer.value = true
            syncStatusMessage.value = "Sprawdzanie aktualizacji z serwera..."
            availableStages.value = emptyList() // Czyszczenie listy, aby wymusić stan ładowania i pokazać loader

            delay(1500L) // Kontrolowane opóźnienie sieciowe (1.5 sekundy)

            availableStages.value = listOf(
                RallyStage("sys_01", "Puszcza Zielonka - SS01", 39.5, 43, StageCategory.SYSTEM, StageStatus.UP_TO_DATE, "v1.4"),
                RallyStage("sys_02", "Biedrusko Poligon - SS02", 54.2, 68, StageCategory.SYSTEM, StageStatus.UPDATE_AVAILABLE, "v2.1"),
                RallyStage("sys_03", "Drawsko Pomorskie - OS3", 120.8, 142, StageCategory.SYSTEM, StageStatus.NEW, "v1.0"),
                RallyStage("user_01", "Moja Trasa Treningowa", 15.4, 12, StageCategory.USER, StageStatus.LOCAL, "v1.0")
            )

            isSyncingFromServer.value = false
            syncStatusMessage.value = "Zsynchronizowano pomyślnie"
        }
    }

    fun downloadOrUpdateStage(stageId: String) {
        availableStages.value = availableStages.value.map { stage ->
            if (stage.id == stageId) {
                stage.copy(status = StageStatus.UP_TO_DATE)
            } else stage
        }
    }

    fun importUserGpxFile(context: Context, fileName: String) {
        val newStage = RallyStage(
            id = "user_${System.currentTimeMillis()}",
            title = fileName.removeSuffix(".gpx"),
            distanceKm = 24.8,
            waypointCount = 28,
            category = StageCategory.USER,
            status = StageStatus.LOCAL,
            version = "v1.0"
        )
        availableStages.value = availableStages.value + newStage
    }

    // --- HARDWARE & SYSTEM STATUS ---
    fun checkConnectedControllers() {
        val deviceIds = InputDevice.getDeviceIds()
        var found = false
        for (id in deviceIds) {
            val device = InputDevice.getDevice(id)
            if (device != null && (
                        device.name.contains("PGA", ignoreCase = true) ||
                                device.name.contains("Rally", ignoreCase = true) ||
                                device.name.contains("Controller", ignoreCase = true)
                        )) {
                bluetoothDeviceName.value = device.name
                found = true
                break
            }
        }
        if (!found) {
            bluetoothDeviceName.value = "Nie wykryto"
        }
    }

    // --- NAVIGATION FLOW CONTROL ---
    fun confirmStart() {
        showStartupDialog.value = false
        isPreviewMode.value = false
        isNavigationStarted.value = true

        totalDistanceMeters.value = 0.0
        tripDistanceMeters.value = 0.0
        validatedWaypointsCount.value = 0
        activeWaypointIndex.value = 0
        lastKnownLocation = null
        currentSpeed.value = 0f
    }

    fun resumeNavigation() {
        showStartupDialog.value = false
        isPreviewMode.value = false
        lastKnownLocation = null
        currentSpeed.value = 0f
    }

    fun enterPreviewMode() {
        showStartupDialog.value = false
        isPreviewMode.value = true
        stopLocationUpdates()
        currentSpeed.value = 0f
    }

    // --- SETTINGS MANAGEMENT ---
    fun loadSettings(context: Context) {
        val sharedPreferences = context.getSharedPreferences("RoadbookSettings", Context.MODE_PRIVATE)
        uiScale.value = sharedPreferences.getFloat("UI_SCALE", 1.0f)
        isLandscapeOrientation.value = sharedPreferences.getBoolean("IS_LANDSCAPE", false)
        tapsEnabled.value = sharedPreferences.getBoolean("TAPS_ENABLED", true)
        isAutoScrollEnabled.value = sharedPreferences.getBoolean("AUTO_SCROLL", false)
        isSimulationMode.value = sharedPreferences.getBoolean("SIMULATION_MODE", false)
    }

    fun saveSettings(context: Context) {
        val sharedPreferences = context.getSharedPreferences("RoadbookSettings", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putFloat("UI_SCALE", uiScale.value)
            putBoolean("IS_LANDSCAPE", isLandscapeOrientation.value)
            putBoolean("TAPS_ENABLED", tapsEnabled.value)
            putBoolean("AUTO_SCROLL", isAutoScrollEnabled.value)
            putBoolean("SIMULATION_MODE", isSimulationMode.value)
            apply()
        }
        showSettings.value = false
    }

    // --- DATA LOADING ---
    fun loadGpxData(context: Context) {
        viewModelScope.launch {
            val parsedList = parseGpxFile(context)
            waypointList.value = parsedList
            if (parsedList.isNotEmpty()) {
                activeWaypointIndex.value = 0
                validatedWaypointsCount.value = 0
                activeRallyWaypoint.value = parsedList.firstOrNull { it.waypointIndex > 0 }
            }
        }
    }

    // --- GPS & LOCATION LOGIC ---
    private fun initGpsTracker(context: Context) {
        gpsTracker = GpsTracker(
            context = context,
            onLocationReceived = { location -> processIncomingLocation(location) },
            onSignalQualityChanged = { quality -> gpsSignalQuality.value = quality },
            onBatteryLevelChanged = { level -> batteryLevel.value = level }
        )
    }

    fun updateBatteryStatus(context: Context) {
        if (gpsTracker == null) initGpsTracker(context)
        gpsTracker?.updateBatteryStatus()
    }

    fun toggleSimulation(enable: Boolean) {
        if (isPreviewMode.value) return
        isSimulationMode.value = enable
        if (enable) {
            stopLocationUpdates()
            gpsSimulator = GpsSimulator(waypointList.value) { simulatedLocation ->
                processIncomingLocation(simulatedLocation)
            }
            gpsSimulator?.startSimulation(viewModelScope)
            isGpsActive.value = true
        } else {
            gpsSimulator?.stopSimulation()
            gpsSimulator = null
            isGpsActive.value = false
            currentSpeed.value = 0f
            lastKnownLocation = null
        }
    }

    private fun processIncomingLocation(location: Location) {
        if (isPreviewMode.value) return

        if (showStartupDialog.value) {
            currentSpeed.value = 0f
            return
        }

        val isMovingSpeed = location.speed >= 0.5f || isSimulationMode.value
        currentSpeed.value = if (isMovingSpeed) (location.speed * 3.6f) else 0f

        lastKnownLocation?.let { last ->
            val distanceMoved = last.distanceTo(location).toDouble()

            val isTeleportation = distanceMoved > 300.0 && !isSimulationMode.value
            val isMovingDistance = distanceMoved > 1.0 || isSimulationMode.value

            if (isMovingSpeed && isMovingDistance && !isTeleportation) {
                if (location.hasBearing()) {
                    capHeading.value = location.bearing
                } else if (last.latitude != location.latitude || last.longitude != location.longitude) {
                    val dLon = Math.toRadians(location.longitude - last.longitude)
                    val rLat1 = Math.toRadians(last.latitude)
                    val rLat2 = Math.toRadians(location.latitude)
                    val y = sin(dLon) * cos(rLat2)
                    val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
                    val azimuth = Math.toDegrees(atan2(y, x))
                    capHeading.value = ((azimuth + 360) % 360).toFloat()
                }

                totalDistanceMeters.value += distanceMoved
                tripDistanceMeters.value += distanceMoved
            }
            lastKnownLocation = location
        } ?: run {
            if (location.hasBearing() && isMovingSpeed) {
                capHeading.value = location.bearing
            }
            lastKnownLocation = location
        }

        val startWp = waypointList.value.getOrNull(0)
        if (validatedWaypointsCount.value == 0 && startWp != null) {
            activeWaypointIndex.value = 0
            activeRallyWaypoint.value = startWp

            val targetLoc = Location("gps").apply {
                latitude = startWp.latitude
                longitude = startWp.longitude
            }

            val distanceToStart = location.distanceTo(targetLoc)
            dtwDistance.value = distanceToStart
            dtwBearing.value = (location.bearingTo(targetLoc) + 360f) % 360f

            val validationRadius = 50f
            if (distanceToStart <= validationRadius && totalDistanceMeters.value < 15.0) {
                validatedWaypointsCount.value = 1
            }
            return
        }

        val result = navigationEngine.calculateNavigation(
            currentLat = location.latitude,
            currentLon = location.longitude,
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

    fun startLocationUpdates(context: Context) {
        if (isSimulationMode.value || isPreviewMode.value) return
        isGpsActive.value = true

        if (gpsTracker == null) initGpsTracker(context)
        gpsTracker?.startLocationUpdates()
    }

    fun stopLocationUpdates() {
        if (!isSimulationMode.value && !isPreviewMode.value) isGpsActive.value = false
        gpsTracker?.stopLocationUpdates()
        lastKnownLocation = null
    }

    // --- MANUAL CONTROLS ---
    fun startIncrementLoop() {
        if (adjustmentJob?.isActive == true) return
        adjustmentJob = viewModelScope.launch {
            var counter = 0
            while (isActive && leftZonePressed.value) {
                counter++
                val progressiveStep = when {
                    counter <= 5 -> 10.0
                    counter <= 12 -> 100.0
                    counter <= 20 -> 500.0
                    else -> 1000.0
                }

                if (validatedWaypointsCount.value > 0) {
                    totalDistanceMeters.value += progressiveStep
                    recalculateRoadbookRowOnly()
                }
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
                val progressiveStep = when {
                    counter <= 5 -> 10.0
                    counter <= 12 -> 100.0
                    counter <= 20 -> 500.0
                    else -> 1000.0
                }

                if (validatedWaypointsCount.value > 0) {
                    if (totalDistanceMeters.value - progressiveStep >= 0) {
                        totalDistanceMeters.value -= progressiveStep
                    } else {
                        totalDistanceMeters.value = 0.0
                    }
                    recalculateRoadbookRowOnly()
                }
                delay(150)
            }
        }
    }

    fun recalculateRoadbookRowOnlyPublic() {
        recalculateRoadbookRowOnly()
    }

    private fun recalculateRoadbookRowOnly() {
        if (validatedWaypointsCount.value == 0) {
            activeWaypointIndex.value = 0
            return
        }

        val list = waypointList.value
        val odo = totalDistanceMeters.value
        var detectedIndex = list.lastIndex
        for (i in list.indices) {
            if (list[i].distanceMeters > odo) {
                detectedIndex = i
                break
            }
        }
        activeWaypointIndex.value = detectedIndex
    }

    fun stopSmoothAdjustment() { adjustmentJob?.cancel() }

    override fun onCleared() {
        super.onCleared()
        gpsSimulator?.stopSimulation()
        stopSmoothAdjustment()
        stopLocationUpdates()
    }
}