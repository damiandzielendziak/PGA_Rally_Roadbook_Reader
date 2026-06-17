package com.example.roadbook.domain

import android.location.Location
import com.example.roadbook.model.RallyWaypoint
import com.example.roadbook.model.calculateAzimuthBetweenPoints

class RallyNavigationEngine {

    // Paczka danych wynikowych przesyłana zwrotnie do ViewModelu
    data class NavigationResult(
        val activeWaypointIndex: Int,
        val validatedWaypointsCount: Int,
        val activeRallyWaypoint: RallyWaypoint?,
        val dtwDistance: Float,
        val dtwBearing: Float
    )

    fun calculateNavigation(
        currentLat: Double,
        currentLon: Double,
        capHeading: Float,
        totalDistanceMeters: Double,
        waypointList: List<RallyWaypoint>,
        currentValidatedCount: Int
    ): NavigationResult {
        if (waypointList.isEmpty()) {
            return NavigationResult(0, currentValidatedCount, null, 0f, 0f)
        }

        // 1. Logika look-ahead oparta na liniowym przebiegu Odo
        var detectedIndex = waypointList.lastIndex
        for (i in waypointList.indices) {
            if (waypointList[i].distanceMeters > totalDistanceMeters) {
                detectedIndex = i
                break
            }
        }

        // 2. Szukanie najbliższego nadchodzącego punktu kontrolnego
        var rallyTarget: RallyWaypoint? = null
        for (i in detectedIndex..waypointList.lastIndex) {
            if (waypointList[i].waypointIndex > 0) {
                rallyTarget = waypointList[i]
                break
            }
        }

        var updatedValidatedCount = currentValidatedCount
        var dtwDistance = 0f
        var dtwBearing = 0f

        // 3. Obliczenia geograficzne GPS względem namierzonego Waypointu
        if (rallyTarget != null) {
            val loc1 = Location("current_pos").apply { latitude = currentLat; longitude = currentLon }
            val locRally = Location("rally_target_pos").apply { latitude = rallyTarget.latitude; longitude = rallyTarget.longitude }
            dtwDistance = loc1.distanceTo(locRally)

            // Walidacja strefy zaliczenia punktu
            if (dtwDistance <= rallyTarget.validationRadiusMeters) {
                if (rallyTarget.waypointIndex > updatedValidatedCount) {
                    updatedValidatedCount = rallyTarget.waypointIndex
                }
            }

            // Obliczanie kąta strzałki kompasu (DTW Bearing)
            val targetBearing = calculateAzimuthBetweenPoints(currentLat, currentLon, rallyTarget.latitude, rallyTarget.longitude).toFloat()
            dtwBearing = targetBearing - capHeading
            if (dtwBearing < 0) dtwBearing += 360f
        }

        return NavigationResult(
            activeWaypointIndex = detectedIndex,
            validatedWaypointsCount = updatedValidatedCount,
            activeRallyWaypoint = rallyTarget,
            dtwDistance = dtwDistance,
            dtwBearing = dtwBearing
        )
    }
}