package com.example.roadbook.model

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

// NOWOŚĆ: Kontener na komplet danych po sparsowaniu dowolnego pliku Open Rally
data class ParsedGpxResult(
    val title: String,
    val distanceKm: Double,
    val waypointCount: Int,
    val waypoints: List<RallyWaypoint>
)

// POPRAWKA: Funkcja przyjmuje teraz dowolny InputStream (z chmury, dysku lub assets)
fun parseGpxFile(inputStream: InputStream): ParsedGpxResult {
    val waypointList = mutableListOf<RallyWaypoint>()
    var stageTitle = "Zaimportowana Trasa" // Nazwa domyślna
    var totalDistanceKm = 0.0

    try {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        var eventType = parser.eventType
        var lat = 0.0
        var lon = 0.0
        var name = ""
        var distanceKm = 0f
        var tulipBase64 = ""
        var notesBase64 = ""
        var validationRadius = 90f
        var openingRadius = 0f
        var danger = 0
        var speed = 0
        var gpxCap = 0
        var wpType = OpenRallyWpType.STANDARD

        var waypointCounter = 0
        var isInsideWpt = false // Pomocnicza flaga do odróżnienia nazwy trasy od nazwy punktu

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName) {
                    "wpt" -> {
                        isInsideWpt = true
                        lat = parser.getAttributeValue(null, "lat")?.toDouble() ?: 0.0
                        lon = parser.getAttributeValue(null, "lon")?.toDouble() ?: 0.0
                        name = ""
                        distanceKm = 0f
                        tulipBase64 = ""
                        notesBase64 = ""
                        validationRadius = 90f
                        openingRadius = 0f
                        danger = 0
                        speed = 0
                        gpxCap = 0
                        wpType = OpenRallyWpType.STANDARD
                    }
                    "name" -> {
                        if (!isInsideWpt) {
                            // Nazwa złapana przed wejściem w punkty to globalny tytuł rajdu/odcinka
                            val globalName = parser.nextText()
                            if (globalName.isNotBlank()) stageTitle = globalName
                        } else {
                            name = parser.nextText()
                        }
                    }
                    "openrally:distance" -> {
                        distanceKm = parser.nextText().toFloatOrNull() ?: 0f
                        // Najwyższy odczyt dystansu określa całkowitą długość etapu
                        if (distanceKm > totalDistanceKm) {
                            totalDistanceKm = distanceKm.toDouble()
                        }
                    }
                    "openrally:tulip" -> tulipBase64 = parser.nextText()
                    "openrally:notes" -> notesBase64 = parser.nextText()
                    "openrally:danger" -> danger = parser.nextText().toIntOrNull() ?: 0
                    "openrally:speed" -> speed = parser.nextText().toIntOrNull() ?: 0
                    "openrally:cap" -> gpxCap = parser.nextText().toIntOrNull() ?: 0

                    "openrally:wpm" -> {
                        wpType = OpenRallyWpType.WPM
                        validationRadius = 90f
                        openingRadius = 800f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:wpc" -> {
                        wpType = OpenRallyWpType.WPC
                        validationRadius = 300f
                        openingRadius = 0f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:wps" -> {
                        wpType = OpenRallyWpType.WPS
                        validationRadius = 90f
                        openingRadius = 1000f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:wpn" -> {
                        wpType = OpenRallyWpType.WPN
                        validationRadius = 200f
                        openingRadius = 0f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:wpp" -> {
                        wpType = OpenRallyWpType.WPP
                        validationRadius = 90f
                        openingRadius = 0f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:wpv" -> {
                        wpType = OpenRallyWpType.WPV
                        validationRadius = 90f
                        openingRadius = Float.MAX_VALUE
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:wpe" -> {
                        wpType = OpenRallyWpType.WPE
                        validationRadius = 90f
                        openingRadius = Float.MAX_VALUE
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:dz" -> {
                        wpType = OpenRallyWpType.DZ
                        openingRadius = 0f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:fz" -> {
                        wpType = OpenRallyWpType.FZ
                        openingRadius = 0f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:dss" -> {
                        wpType = OpenRallyWpType.DSS
                        openingRadius = Float.MAX_VALUE
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:ass" -> {
                        wpType = OpenRallyWpType.ASS
                        validationRadius = 90f
                        openingRadius = Float.MAX_VALUE
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:dn" -> {
                        wpType = OpenRallyWpType.DN
                        openingRadius = 0f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:fn" -> {
                        wpType = OpenRallyWpType.FN
                        openingRadius = 0f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:dt" -> {
                        wpType = OpenRallyWpType.DT
                        openingRadius = 0f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:ft" -> {
                        wpType = OpenRallyWpType.FT
                        openingRadius = 0f
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG && tagName == "wpt") {
                isInsideWpt = false
                val isActualWaypoint = wpType == OpenRallyWpType.WPM ||
                        wpType == OpenRallyWpType.WPC ||
                        wpType == OpenRallyWpType.WPS ||
                        OpenRallyWpType.WPN == wpType ||
                        wpType == OpenRallyWpType.WPP ||
                        wpType == OpenRallyWpType.WPV ||
                        wpType == OpenRallyWpType.WPE

                val assignedWpIndex = if (isActualWaypoint) {
                    waypointCounter++
                    waypointCounter
                } else {
                    0
                }

                waypointList.add(
                    RallyWaypoint(
                        name = name,
                        distanceMeters = distanceKm * 1000f,
                        tulipBitmap = decodeBase64ToBitmap(tulipBase64),
                        notesBitmap = decodeBase64ToBitmap(notesBase64),
                        latitude = lat,
                        longitude = lon,
                        validationRadiusMeters = validationRadius,
                        openingRadiusMeters = openingRadius,
                        danger = danger,
                        speed = speed,
                        wpType = wpType,
                        gpxCap = gpxCap,
                        waypointIndex = assignedWpIndex
                    )
                )
            }
            eventType = parser.next()
        }
        inputStream.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Zwracamy spakowaną strukturę z kompletem danych i realnymi wyliczeniami
    return ParsedGpxResult(
        title = stageTitle,
        distanceKm = totalDistanceKm,
        waypointCount = waypointList.size,
        waypoints = waypointList
    )
}