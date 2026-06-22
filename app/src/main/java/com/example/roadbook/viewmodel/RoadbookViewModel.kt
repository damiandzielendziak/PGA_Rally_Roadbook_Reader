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
import org.json.JSONArray
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2

import com.example.roadbook.model.RallyWaypoint
import com.example.roadbook.model.parseGpxFile
import com.example.roadbook.model.ParsedGpxResult
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
    var syncStatusMessage = mutableStateOf("")
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

    private var appContext: Context? = null

    var pendingOverwriteResult = mutableStateOf<ParsedGpxResult?>(null)

    // --- BANKI PAMIĘCI DLA PARAMETRÓW BEZPIECZEŃSTWA ---
    private val stagePasswords = mutableMapOf<String, String>()
    private val stageExpirations = mutableMapOf<String, Long>()
    private val unlockedStagesByPilot = mutableSetOf<String>()

    init {
        showStartupDialog.value = true
        isNavigationStarted.value = false
    }

    // --- METODY WALIDACJI BEZPIECZEŃSTWA DLA INTERFEJSU (UI) ---

    fun isStageExpired(stageId: String): Boolean {
        val expirationTimestamp = stageExpirations[stageId] ?: return false
        if (expirationTimestamp == 0L) return false
        return System.currentTimeMillis() > expirationTimestamp
    }

    fun requiresPassword(stageId: String): Boolean {
        val password = stagePasswords[stageId]
        if (password.isNullOrBlank()) return false
        return !unlockedStagesByPilot.contains(stageId)
    }

    fun unlockStageWithPassword(stageId: String, input: String): Boolean {
        val correctPassword = stagePasswords[stageId] ?: return true
        if (correctPassword == input) {
            unlockedStagesByPilot.add(stageId)
            saveUserStages()
            return true
        }
        return false
    }

    private fun saveUserStages() {
        val context = appContext ?: return
        val sharedPrefs = context.getSharedPreferences("RoadbookUserStages", Context.MODE_PRIVATE)
        val userStages = availableStages.value.filter { it.category == StageCategory.USER }

        val stringSet = userStages.map {
            "${it.id}|${it.title}|${it.distanceKm}|${it.waypointCount}|${it.version}"
        }.toSet()

        val editor = sharedPrefs.edit()
        editor.putStringSet("SAVED_USER_STAGES", stringSet)

        userStages.forEach { stage ->
            stagePasswords[stage.id]?.let { editor.putString("PASS_${stage.id}", it) }
            stageExpirations[stage.id]?.let { editor.putLong("EXP_${stage.id}", it) }
            if (unlockedStagesByPilot.contains(stage.id)) {
                editor.putBoolean("UNLOCKED_${stage.id}", true)
            }
        }
        editor.apply()
    }

    private fun loadUserStages(context: Context): List<RallyStage> {
        val sharedPrefs = context.getSharedPreferences("RoadbookUserStages", Context.MODE_PRIVATE)
        val stringSet = sharedPrefs.getStringSet("SAVED_USER_STAGES", emptySet()) ?: emptySet()

        return stringSet.mapNotNull { str ->
            val parts = str.split("|")
            if (parts.size >= 5) {
                val id = parts[0]

                sharedPrefs.getString("PASS_$id", "")?.let { if(it.isNotBlank()) stagePasswords[id] = it }
                sharedPrefs.getLong("EXP_$id", 0L).let { if(it > 0L) stageExpirations[id] = it }
                if (sharedPrefs.getBoolean("UNLOCKED_$id", false)) unlockedStagesByPilot.add(id)

                RallyStage(
                    id = id,
                    title = parts[1],
                    distanceKm = parts[2].toDoubleOrNull() ?: 0.0,
                    waypointCount = parts[3].toIntOrNull() ?: 0,
                    category = StageCategory.USER,
                    status = StageStatus.LOCAL,
                    version = parts[4]
                )
            } else null
        }
    }

    private fun saveSystemStagesCache(systemStages: List<RallyStage>) {
        val context = appContext ?: return
        val sharedPrefs = context.getSharedPreferences("RoadbookSystemStagesCache", Context.MODE_PRIVATE)
        val stringSet = systemStages.map {
            "${it.id}|${it.title}|${it.distanceKm}|${it.waypointCount}|${it.version}"
        }.toSet()
        sharedPrefs.edit().putStringSet("SAVED_SYSTEM_STAGES", stringSet).apply()
    }

    private fun loadSystemStagesCache(context: Context): List<RallyStage> {
        val sharedPrefs = context.getSharedPreferences("RoadbookSystemStagesCache", Context.MODE_PRIVATE)
        val stringSet = sharedPrefs.getStringSet("SAVED_SYSTEM_STAGES", emptySet()) ?: emptySet()

        return stringSet.mapNotNull { str ->
            val parts = str.split("|")
            if (parts.size >= 5) {
                val id = parts[0]
                if (File(context.filesDir, "$id.gpx").exists()) {
                    RallyStage(
                        id = id,
                        title = parts[1],
                        distanceKm = parts[2].toDoubleOrNull() ?: 0.0,
                        waypointCount = parts[3].toIntOrNull() ?: 0,
                        category = StageCategory.SYSTEM,
                        status = StageStatus.UP_TO_DATE,
                        version = parts[4]
                    )
                } else null
            } else null
        }
    }

    private suspend fun downloadGpxFileSilently(context: Context, urlStr: String, stageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                var currentUrl = urlStr.replace("[\\s\\p{Z}\\u00A0\\u200B]".toRegex(), "")
                var redirectCount = 0
                var success = false

                while (redirectCount < 5 && !success) {
                    val url = URL(currentUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.instanceFollowRedirects = false
                    connection.useCaches = false
                    connection.defaultUseCaches = false

                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15")
                    connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    connection.setRequestProperty("Accept-Language", "pl-PL,pl;q=0.9")
                    connection.setRequestProperty("Cache-Control", "no-cache")

                    val responseCode = connection.responseCode

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.use { inputStream ->
                            context.openFileOutput("$stageId.gpx", Context.MODE_PRIVATE).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        success = true
                    } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == 307 || responseCode == 308) {

                        val location = connection.getHeaderField("Location")
                        if (!location.isNullOrBlank()) {
                            currentUrl = if (location.startsWith("http")) location else {
                                val base = URL(currentUrl)
                                URL(base.protocol, base.host, base.port, location).toString()
                            }
                            redirectCount++
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
                success
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun fetchSecureText(urlStr: String): String {
        return withContext(Dispatchers.IO) {
            var currentUrl = urlStr.replace("[\\s\\p{Z}\\u00A0\\u200B]".toRegex(), "")
            var redirectCount = 0
            var success = false
            var result = ""

            while (redirectCount < 5 && !success) {
                val url = URL(currentUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                connection.defaultUseCaches = false

                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15")
                connection.setRequestProperty("Accept", "application/json, text/plain, */*")

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    result = connection.inputStream.bufferedReader().use { it.readText() }
                    success = true
                } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else {
                            val base = URL(currentUrl)
                            URL(base.protocol, base.host, base.port, location).toString()
                        }
                        redirectCount++
                    } else {
                        break
                    }
                } else {
                    break
                }
            }

            if (!success) throw java.io.IOException("Fetch failed")
            result
        }
    }

    // --- STAGE SELECTION LOGIC ---

    fun checkForStageUpdates(context: Context) {
        if (appContext == null) appContext = context.applicationContext

        viewModelScope.launch {
            val cachedUserStages = loadUserStages(context)
            val cachedSystemStages = loadSystemStagesCache(context)
            availableStages.value = cachedSystemStages + cachedUserStages

            isSyncingFromServer.value = true
            val sharedPrefs = context.getSharedPreferences("RoadbookSystemVersions", Context.MODE_PRIVATE)

            try {
                val jsonText = fetchSecureText("https://pga-rally.com/rally/stages.json")
                val rootJson = JSONObject(jsonText)
                val systemArray = rootJson.getJSONArray("system_stages")

                val freshSystemStages = mutableListOf<RallyStage>()
                for (i in 0 until systemArray.length()) {
                    val item = systemArray.getJSONObject(i)
                    val id = item.getString("id").trim()
                    val title = item.getString("title")
                    val serverVersion = item.getString("version").trim()
                    val fileUrl = item.getString("file_url").trim()

                    val localVersion = sharedPrefs.getString("VER_$id", "")
                    val fileExists = File(context.filesDir, "$id.gpx").exists()

                    try {
                        if (localVersion != serverVersion || !fileExists) {
                            val downloadSuccess = downloadGpxFileSilently(context, fileUrl, id)
                            if (downloadSuccess) {
                                sharedPrefs.edit().putString("VER_$id", serverVersion).apply()
                            }
                        }

                        if (File(context.filesDir, "$id.gpx").exists()) {
                            freshSystemStages.add(
                                RallyStage(
                                    id = id,
                                    title = title,
                                    distanceKm = item.getDouble("distance_km"),
                                    waypointCount = item.getInt("waypoint_count"),
                                    category = StageCategory.SYSTEM,
                                    status = StageStatus.UP_TO_DATE,
                                    version = serverVersion
                                )
                            )
                        }
                    } catch (fileException: Exception) {
                        fileException.printStackTrace()
                        cachedSystemStages.firstOrNull { it.id == id }?.let { freshSystemStages.add(it) }
                    }
                }

                saveSystemStagesCache(freshSystemStages)
                availableStages.value = freshSystemStages + cachedUserStages

            } catch (e: Exception) {
                e.printStackTrace()
            } finally { // POPRAWKA: Zmiana z 'final' na poprawny w Kotlinie 'finally'
                isSyncingFromServer.value = false
            }
        }
    }

    fun downloadOrUpdateStage(stageId: String) { }

    fun importUserGpxFile(context: Context, fileUri: Uri) {
        if (appContext == null) appContext = context.applicationContext
        try {
            context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
                val result = parseGpxFile(inputStream)
                val isDuplicate = availableStages.value.any { it.title.equals(result.title, ignoreCase = true) }

                if (isDuplicate) {
                    pendingOverwriteResult.value = result
                } else {
                    executeActualImport(result)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resolveOverwrite(proceed: Boolean) {
        val result = pendingOverwriteResult.value
        pendingOverwriteResult.value = null

        if (proceed && result != null) {
            val cleanList = availableStages.value.filter { !it.title.equals(result.title, ignoreCase = true) }
            val newId = "user_${System.currentTimeMillis()}"
            val newStage = RallyStage(
                id = newId,
                title = result.title,
                distanceKm = result.distanceKm,
                waypointCount = result.waypointCount,
                category = StageCategory.USER,
                status = StageStatus.LOCAL,
                version = "v1.0"
            )
            availableStages.value = cleanList + newStage
            saveUserStages()
        }
    }

    private fun executeActualImport(result: ParsedGpxResult) {
        val newStage = RallyStage(
            id = "user_${System.currentTimeMillis()}",
            title = result.title,
            distanceKm = result.distanceKm,
            waypointCount = result.waypointCount,
            category = StageCategory.USER,
            status = StageStatus.LOCAL,
            version = "v1.0"
        )
        availableStages.value = availableStages.value + newStage
        saveUserStages()
    }

    fun importRoadbookFromCloud(context: Context, code: String) {
        if (appContext == null) appContext = context.applicationContext
        if (isSyncingFromServer.value) return

        viewModelScope.launch {
            isSyncingFromServer.value = true
            val searchCode = code.uppercase().trim()

            try {
                val jsonText = fetchSecureText("https://pga-rally.com/rally/stages.json")
                val rootJson = JSONObject(jsonText)
                val userStagesJson = rootJson.getJSONObject("user_stages")

                if (!userStagesJson.has(searchCode)) {
                    isSyncingFromServer.value = false
                    return@launch
                }

                val stageData = userStagesJson.getJSONObject(searchCode)
                val title = stageData.getString("title")
                val distance = stageData.getDouble("distance_km")
                val waypoints = stageData.getInt("waypoint_count")
                val version = stageData.getString("version").trim()
                val fileUrl = stageData.getString("file_url").trim()

                val password = stageData.optString("password", "")
                val expiresAt = stageData.optLong("expires_at", 0L)

                val isDuplicate = availableStages.value.any { it.title.equals(title, ignoreCase = true) }

                if (isDuplicate) {
                    pendingOverwriteResult.value = ParsedGpxResult(
                        title = title,
                        distanceKm = distance,
                        waypointCount = waypoints,
                        waypoints = emptyList()
                    )
                } else {
                    val newId = "cloud_${System.currentTimeMillis()}"
                    val downloadSuccess = downloadGpxFileSilently(context, fileUrl, newId)

                    if (downloadSuccess) {
                        if (password.isNotBlank()) stagePasswords[newId] = password
                        if (expiresAt > 0L) stageExpirations[newId] = expiresAt

                        val cloudStage = RallyStage(
                            id = newId,
                            title = title,
                            distanceKm = distance,
                            waypointCount = waypoints,
                            category = StageCategory.USER,
                            status = StageStatus.LOCAL,
                            version = version
                        )

                        val existingUserStages = availableStages.value.filter { it.category == StageCategory.USER }
                        val currentSystemStages = availableStages.value.filter { it.category == StageCategory.SYSTEM }

                        availableStages.value = currentSystemStages + existingUserStages + cloudStage
                        saveUserStages()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isSyncingFromServer.value = false
            }
        }
    }

    fun deleteUserStage(stageId: String) {
        availableStages.value = availableStages.value.filter { it.id != stageId }
        stagePasswords.remove(stageId)
        stageExpirations.remove(stageId)
        unlockedStagesByPilot.remove(stageId)

        try {
            val file = File(appContext?.filesDir, "$stageId.gpx")
            if (file.exists()) file.delete()
        } catch (e: Exception) { e.printStackTrace() }

        saveUserStages()
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

    fun loadGpxDataForStage(context: Context, stageId: String) {
        if (appContext == null) appContext = context.applicationContext
        viewModelScope.launch {
            try {
                val downloadedFile = File(context.filesDir, "$stageId.gpx")
                val inputStream = if (downloadedFile.exists()) {
                    context.openFileInput("$stageId.gpx")
                } else {
                    context.assets.open("roadbook.gpx")
                }

                inputStream.use { stream ->
                    val result = parseGpxFile(stream)
                    waypointList.value = result.waypoints
                    if (result.waypoints.isNotEmpty()) {
                        activeWaypointIndex.value = 0
                        validatedWaypointsCount.value = 0
                        activeRallyWaypoint.value = result.waypoints.firstOrNull { it.waypointIndex > 0 }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadGpxData(context: Context) {
        loadGpxDataForStage(context, "sys_01")
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