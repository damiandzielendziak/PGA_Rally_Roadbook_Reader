package com.example.roadbook

import android.location.Location
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class OpenRallyWpType {
    WPM, WPC, DZ, FZ, NT
}

data class RallyWaypoint(
    val id: Int,
    val odometerKm: Double,
    val tulipBase64: String,
    val notes: String,
    val type: OpenRallyWpType,
    val latitude: Double,
    val longitude: Double,
    val dangerLevel: Int
)

data class NavigationStateData(
    val currentSpeedKmh: Int,
    val currentCap: Int,
    val totalOdometerKm: Double,
    val partialOdometerKm: Double,
    val distanceToWaypointMeters: Int,
    val nextWaypointIndex: Int
)

class RallyNavigationEngine {

    private var lastValidLocation: Location? = null
    private var totalOdo: Double = 0.0
    private var partialOdo: Double = 0.0

    // BRAMA SZUMU (Drift Gate): Progi odcięcia szumów statycznych GPS
    private val speedNoiseThresholdMs = 0.5f
    private val distanceNoiseThresholdMeters = 1.0

    fun resetOdometer(toValue: Double = 0.0) {
        totalOdo = toValue
        partialOdo = 0.0
        lastValidLocation = null
    }

    fun adjustOdometer(deltaKm: Double) {
        totalOdo += deltaKm
        if (totalOdo < 0.0) totalOdo = 0.0
        partialOdo += deltaKm
        if (partialOdo < 0.0) partialOdo = 0.0
    }

    fun calculateNavigation(
        currentLocation: Location,
        waypoints: List<RallyWaypoint>
    ): NavigationStateData {

        val previousLocation = lastValidLocation

        // STAN UZBROJENIA SILNIKA: Pierwszy pewny i świeży fix staje się punktem bazowym (Kratka Zero).
        // Kotwiczymy pozycję, zwracając zerowy przyrost, zapobiegając "skokowi startowemu" (Initial Jump).
        if (previousLocation == null) {
            lastValidLocation = currentLocation
            return NavigationStateData(
                currentSpeedKmh = 0,
                currentCap = if (currentLocation.hasBearing()) currentLocation.bearing.toInt().coerceIn(0, 359) else 0,
                totalOdometerKm = totalOdo,
                partialOdometerKm = partialOdo,
                distanceToWaypointMeters = calculateDistanceToFirstVisibleWp(currentLocation, waypoints),
                nextWaypointIndex = findNextWpIndex(totalOdo, waypoints)
            )
        }

        val distanceMovedMeters = previousLocation.distanceTo(currentLocation).toDouble()

        // BRAMA SZUMU (Drift Gate): Jeśli prędkość raportowana sprzętowo < 0.5 m/s, ignorujemy ruch na stojaku
        val isMoving = currentLocation.speed >= speedNoiseThresholdMs && distanceMovedMeters > distanceNoiseThresholdMeters

        if (isMoving) {
            val deltaKm = distanceMovedMeters / 1000.0
            totalOdo += deltaKm
            partialOdo += deltaKm
            lastValidLocation = currentLocation
        }

        // Wyliczanie prędkości (Twarde zero, jeśli Drift Gate wykrył pływanie pozycji)
        val speedKmh = if (isMoving) {
            (currentLocation.speed * 3.6).toInt()
        } else {
            0
        }

        // Wyliczanie kierunku CAP (Zapobiega losowemu obracaniu się roadbooka, gdy motocykl stoi)
        val cap = if (isMoving && currentLocation.hasBearing()) {
            currentLocation.bearing.toInt().coerceIn(0, 359)
        } else if (isMoving) {
            calculateBearing(previousLocation.latitude, previousLocation.longitude, currentLocation.latitude, currentLocation.longitude)
        } else {
            if (previousLocation.hasBearing()) previousLocation.bearing.toInt().coerceIn(0, 359) else 0
        }

        val nextWpIndex = findNextWpIndex(totalOdo, waypoints)
        val distanceToWp = if (nextWpIndex < waypoints.size) {
            val wpLoc = Location("WP").apply {
                latitude = waypoints[nextWpIndex].latitude
                longitude = waypoints[nextWpIndex].longitude
            }
            currentLocation.distanceTo(wpLoc).toInt()
        } else {
            0
        }

        return NavigationStateData(
            currentSpeedKmh = speedKmh,
            currentCap = cap,
            totalOdometerKm = totalOdo,
            partialOdometerKm = partialOdo,
            distanceToWaypointMeters = distanceToWp,
            nextWaypointIndex = nextWpIndex
        )
    }

    private fun findNextWpIndex(currentOdo: Double, waypoints: List<RallyWaypoint>): Int {
        for (i in waypoints.indices) {
            if (waypoints[i].odometerKm > currentOdo) {
                return i
            }
        }
        return waypoints.size
    }

    private fun calculateDistanceToFirstVisibleWp(currentLocation: Location, waypoints: List<RallyWaypoint>): Int {
        val idx = findNextWpIndex(totalOdo, waypoints)
        if (idx < waypoints.size) {
            val wpLoc = Location("WP").apply {
                latitude = waypoints[idx].latitude
                longitude = waypoints[idx].longitude
            }
            return currentLocation.distanceTo(wpLoc).toInt()
        }
        return 0
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val y = sin(dLon) * cos(rLat2)
        val x = cos(rLat1) * sin(rLat2) - sin(rLat1) * cos(rLat2) * cos(dLon)
        val azimuth = Math.toDegrees(atan2(y, x))
        return ((azimuth + 360) % 360).toInt()
    }
}