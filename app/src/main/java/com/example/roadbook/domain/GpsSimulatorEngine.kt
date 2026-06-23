package com.example.roadbook.domain

import android.content.Context
import android.location.Location
import com.example.roadbook.infrastructure.GpsTracker
import com.example.roadbook.model.GpsSimulator
import com.example.roadbook.model.RallyWaypoint
import kotlinx.coroutines.CoroutineScope
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.atan2

class GpsSimulatorEngine(
    private val context: Context,
    private val onLocationProcessed: (Location, Float, Double, Float) -> Unit, // location, speedKmh, distanceMovedMeters, bearing
    private val onSignalQualityChanged: (String) -> Unit,
    private val onBatteryLevelChanged: (String) -> Unit
) {
    private var gpsTracker: GpsTracker? = null
    private var gpsSimulator: GpsSimulator? = null
    private var lastKnownLocation: Location? = null

    init {
        gpsTracker = GpsTracker(
            context = context,
            onLocationReceived = { loc -> handleIncomingLocation(loc, false) },
            onSignalQualityChanged = onSignalQualityChanged,
            onBatteryLevelChanged = onBatteryLevelChanged
        )
    }

    fun startLocationUpdates() {
        gpsTracker?.startLocationUpdates()
    }

    fun stopLocationUpdates() {
        gpsTracker?.stopLocationUpdates()
        lastKnownLocation = null
    }

    fun updateBatteryStatus() {
        gpsTracker?.updateBatteryStatus()
    }

    fun toggleSimulation(enable: Boolean, waypoints: List<RallyWaypoint>, scope: CoroutineScope) {
        if (enable) {
            stopLocationUpdates()
            gpsSimulator = GpsSimulator(waypoints) { simulatedLocation ->
                handleIncomingLocation(simulatedLocation, true)
            }
            gpsSimulator?.startSimulation(scope)
        } else {
            gpsSimulator?.stopSimulation()
            gpsSimulator = null
            lastKnownLocation = null
        }
    }

    private fun handleIncomingLocation(location: Location, isSimulation: Boolean) {
        val isMovingSpeed = location.speed >= 0.5f || isSimulation
        val speedKmh = if (isMovingSpeed) (location.speed * 3.6f) else 0f
        var distanceMoved = 0.0
        var bearing = location.bearing

        lastKnownLocation?.let { last ->
            distanceMoved = last.distanceTo(location).toDouble()
            val isTeleportation = distanceMoved > 300.0 && !isSimulation
            val isMovingDistance = distanceMoved > 1.0 || isSimulation

            if (isMovingSpeed && isMovingDistance && !isTeleportation) {
                if (!location.hasBearing() && (last.latitude != location.latitude || last.longitude != location.longitude)) {
                    bearing = calculateAzimuth(last, location)
                }
            } else {
                distanceMoved = 0.0
            }
            lastKnownLocation = location
        } ?: run {
            lastKnownLocation = location
        }

        onLocationProcessed(location, speedKmh, distanceMoved, bearing)
    }

    private fun calculateAzimuth(last: Location, current: Location): Float {
        val dLon = Math.toRadians(current.longitude - last.longitude)
        val rLat1 = Math.toRadians(last.latitude)
        val rLat2 = Math.toRadians(current.latitude)
        val y = sin(dLon) * cos(rLat2)
        val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
        val azimuth = Math.toDegrees(atan2(y, x))
        return ((azimuth + 360) % 360).toFloat()
    }
}