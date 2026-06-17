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
        viewModelScope.launch {
            showStartupDialog.value = true
            delay(2000L)
            showStartupDialog.value = false
        }
    }

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

    fun initializeSettings(context: Context) {}
    fun saveSettings(context: Context) { showSettings.value = false }

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

                // Przebieg bije nieprzerwanie od zera na dojazdówce i rośnie normalnie
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

        // --- MANUALE ZABEZPIECZENIE PRZEDSTARTOWE (REFEKCJA WORKFLOW ZAWODNIKA) ---
        val startWp = waypointList.value.getOrNull(0)
        if (validatedWaypointsCount.value == 0 && startWp != null) {
            activeWaypointIndex.value = 0
            activeRallyWaypoint.value = startWp

            val targetLoc = Location("gps").apply {
                latitude = startWp.latitude
                longitude = startWp.longitude
            }

            val distanceToStart = location.distanceTo(targetLoc)

            // Strzałka kierunkowa i odliczanie metrów (DTW) działają stabilnie w oparciu o realną pozycję
            dtwDistance.value = distanceToStart
            dtwBearing.value = (location.bearingTo(targetLoc) + 360f) % 360f

            // Przejście w tryb OS następuje TYLKO gdy motocyklista stoi na linii startu (<= 50m)
            // ORAZ manualnie wyzerował swój licznik ODO (wartość mniejsza niż 15 metrów od resetu)
            val validationRadius = 50f
            if (distanceToStart <= validationRadius && totalDistanceMeters.value < 15.0) {
                validatedWaypointsCount.value = 1
            }

            // Dopóki nie wyzerujesz ODO na linii startu, silnik rajdowy nie ma prawa modyfikować danych
            return
        }

        // Pętla silnika rajdowego steruje nawigacją dopiero po zaliczeniu startu i Twoim manualnym resecie
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