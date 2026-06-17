package com.example.roadbook.model

import android.location.Location
import com.example.roadbook.model.RallyWaypoint
import kotlinx.coroutines.*

class GpsSimulator(
    private val waypoints: List<RallyWaypoint>,
    private val onLocationTick: (Location) -> Unit
) {
    private var simulatorJob: Job? = null
    private var currentTargetIndex = 0

    private var currentLat = waypoints.getOrNull(0)?.latitude ?: 0.0
    private var currentLon = waypoints.getOrNull(0)?.longitude ?: 0.0

    fun startSimulation(scope: CoroutineScope) {
        if (simulatorJob?.isActive == true || waypoints.isEmpty()) return

        simulatorJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val target = waypoints[currentTargetIndex]

                val startLoc = Location("sim").apply { latitude = currentLat; longitude = currentLon }
                val endLoc = Location("target").apply { latitude = target.latitude; longitude = target.longitude }
                val distanceToTarget = startLoc.distanceTo(endLoc)

                // SYMULATOR: Przełącza punkt docelowy jazdy dokładnie w momencie przekroczenia bramki z pliku GPX
                if (distanceToTarget < target.validationRadiusMeters) {
                    if (currentTargetIndex < waypoints.lastIndex) {
                        currentTargetIndex++
                    } else {
                        currentTargetIndex = 0
                    }
                    continue
                }

                val bearing = startLoc.bearingTo(endLoc)
                val fraction = 20f / distanceToTarget

                currentLat += (target.latitude - currentLat) * fraction
                currentLon += (target.longitude - currentLon) * fraction

                val mockedLocation = Location("gps_simulator").apply {
                    latitude = currentLat
                    longitude = currentLon
                    speed = 20f
                    this.bearing = if (bearing < 0) bearing + 360f else bearing
                    time = System.currentTimeMillis()
                }

                withContext(Dispatchers.Main) {
                    onLocationTick(mockedLocation)
                }

                delay(1000)
            }
        }
    }

    fun stopSimulation() {
        simulatorJob?.cancel()
    }
}