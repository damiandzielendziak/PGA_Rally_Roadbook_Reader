package com.example.roadbook.model

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.location.Location
import kotlin.math.abs
import kotlin.math.roundToInt

enum class ScrollDirection { UP, DOWN, NONE }

enum class OpenRallyWpType {
    STANDARD,
    WPM, WPC, WPS, WPN, WPP, WPV, WPE,
    DZ, FZ,
    DSS, ASS,
    DN, FN,
    DT, FT
}

data class RallyWaypoint(
    val name: String,
    val distanceMeters: Float,
    val tulipBitmap: Bitmap?,
    val notesBitmap: Bitmap?,
    val latitude: Double,
    val longitude: Double,
    val validationRadiusMeters: Float,
    // =========================================================
    // DODANE: Promień otwarcia strefy waypointu (zgodny z FIA)
    // =========================================================
    val openingRadiusMeters: Float = 0f,
    val danger: Int,
    val speed: Int,
    val wpType: OpenRallyWpType,
    val gpxCap: Int,
    // POPRAWKA: Dodano = 0, aby stare konstruktory w aplikacji nie wywalały błędów kompilacji
    val waypointIndex: Int = 0
)

fun decodeBase64ToBitmap(base64String: String): Bitmap? {
    return try {
        val cleanString = base64String
            .replace("data:image/png;base64,", "")
            .replace("data:image/jpeg;base64,", "")
            .trim()
        val decodedBytes = Base64.decode(cleanString, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun calculateAzimuthBetweenPoints(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val loc1 = Location("1").apply { latitude = lat1; longitude = lon1 }
    val loc2 = Location("2").apply { latitude = lat2; longitude = lon2 }
    val bearing = loc1.bearingTo(loc2)
    return if (bearing < 0) (bearing + 360).roundToInt() else bearing.roundToInt()
}

fun convertToDmsFormat(coordinate: Double, isLatitude: Boolean): String {
    val direction = if (isLatitude) {
        if (coordinate >= 0) "N" else "S"
    } else {
        if (coordinate >= 0) "E" else "W"
    }
    val absCoordinate = abs(coordinate)
    val degrees = absCoordinate.toInt()
    val unclippedMinutes = (absCoordinate - degrees) * 60
    val minutes = unclippedMinutes.toInt()
    val seconds = (unclippedMinutes - minutes) * 60
    return "%s%d° %d' %.3f\"".format(direction, degrees, minutes, seconds)
}