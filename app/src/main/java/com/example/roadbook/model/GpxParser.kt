package com.example.roadbook.model

import android.content.Context
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

fun parseGpxFile(context: Context): List<RallyWaypoint> {
    val waypointList = mutableListOf<RallyWaypoint>()
    try {
        val inputStream = context.assets.open("roadbook.gpx")
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
        var openingRadius = 0f // Domyślnie 0f - punkt ślepy/ukryty (np. WPC)
        var danger = 0
        var speed = 0
        var gpxCap = 0
        var wpType = OpenRallyWpType.STANDARD

        // Dynamiczny licznik rzeczywistych Waypointów rajdowych
        var waypointCounter = 0

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName) {
                    "wpt" -> {
                        lat = parser.getAttributeValue(null, "lat")?.toDouble() ?: 0.0
                        lon = parser.getAttributeValue(null, "lon")?.toDouble() ?: 0.0
                        name = ""
                        distanceKm = 0f
                        tulipBase64 = ""
                        notesBase64 = ""
                        validationRadius = 90f
                        openingRadius = 0f // Reset do zera dla czystej kratki roadbooka
                        danger = 0
                        speed = 0
                        gpxCap = 0
                        wpType = OpenRallyWpType.STANDARD
                    }
                    "name" -> name = parser.nextText()
                    "openrally:distance" -> distanceKm = parser.nextText().toFloatOrNull() ?: 0f
                    "openrally:tulip" -> tulipBase64 = parser.nextText()
                    "openrally:notes" -> notesBase64 = parser.nextText()
                    "openrally:danger" -> danger = parser.nextText().toIntOrNull() ?: 0
                    "openrally:speed" -> speed = parser.nextText().toIntOrNull() ?: 0
                    "openrally:cap" -> gpxCap = parser.nextText().toIntOrNull() ?: 0

                    "openrally:wpm" -> {
                        wpType = OpenRallyWpType.WPM
                        validationRadius = 90f // Standard FIA dla Masked
                        openingRadius = 800f  // Standard FIA dla Masked (Otwarcie z 800m)
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:wpc" -> {
                        wpType = OpenRallyWpType.WPC
                        validationRadius = 300f // Standard FIA dla Control
                        openingRadius = 0f     // PUNKT ŚLEPY - NIGDY SIĘ NIE OTWIERA
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:wps" -> {
                        wpType = OpenRallyWpType.WPS
                        validationRadius = 90f  // Standard FIA dla Safety
                        openingRadius = 1000f // Standard FIA dla Safety (Otwarcie z 1000m)
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
                        openingRadius = Float.MAX_VALUE // FIA: Punkt widoczny od samego początku roli aktywnej
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:wpe" -> {
                        wpType = OpenRallyWpType.WPE
                        validationRadius = 90f
                        openingRadius = Float.MAX_VALUE // Eclipse/Entry również traktujemy jako jawny od startu
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
                        openingRadius = Float.MAX_VALUE // Start odcinka jest zawsze widoczny
                        parser.getAttributeValue(null, "clear")?.toFloatOrNull()?.let { validationRadius = it }
                        parser.getAttributeValue(null, "open")?.toFloatOrNull()?.let { openingRadius = it }
                    }
                    "openrally:ass" -> {
                        wpType = OpenRallyWpType.ASS
                        validationRadius = 90f
                        openingRadius = Float.MAX_VALUE // Meta odcinka jest zawsze widoczna
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
    return waypointList
}