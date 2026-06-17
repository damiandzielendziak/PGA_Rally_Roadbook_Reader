package com.example.roadbook.infrastructure

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.BatteryManager
import android.os.SystemClock
import com.google.android.gms.location.*
import kotlin.math.roundToInt

class GpsTracker(
    private val context: Context,
    private val onLocationReceived: (Location) -> Unit,
    private val onSignalQualityChanged: (String) -> Unit,
    private val onBatteryLevelChanged: (String) -> Unit
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var locationCallback: LocationCallback? = null

    // Pobieranie stanu baterii za pomocą sticky broadcast, dokładnie tak jak w Twoim kodzie
    fun updateBatteryStatus() {
        try {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            val percentage = if (level >= 0 && scale > 0) {
                ((level / scale.toFloat()) * 100).toInt()
            } else {
                100
            }
            onBatteryLevelChanged("$percentage%")
        } catch (e: Exception) {
            onBatteryLevelChanged("Nieznany")
        }
    }

    // Uruchomienie sprzętowego sensora GPS z Twoimi agresywnymi interwałami (500ms / 200ms)
    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (locationCallback != null) return // Zabezpieczenie przed podwójną rejestracją listenera

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 500L
        ).apply {
            setMinUpdateIntervalMillis(200L)
            setMaxUpdateDelayMillis(500L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Iterujemy po lokalizacjach, filtrując je Twoim algorytmem rajdowym
                for (location in locationResult.locations) {
                    if (isLocationValid(location)) {

                        // Dynamiczne przeliczanie jakości sygnału na tekst dla okna Boot Loadera
                        if (location.hasAccuracy()) {
                            val accuracy = location.accuracy
                            val quality = when {
                                accuracy <= 4f -> "Doskonała (±${accuracy.roundToInt()}m)"
                                accuracy <= 10f -> "Dobra (±${accuracy.roundToInt()}m)"
                                else -> "Graniczna (±${accuracy.roundToInt()}m)"
                            }
                            onSignalQualityChanged(quality)
                        } else {
                            onSignalQualityChanged("Zafiksowany (Brak metryki)")
                        }

                        // Przesyłamy bezpieczną, przefiltrowaną lokalizację do silnika
                        onLocationReceived(location)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            context.mainLooper
        )
    }

    // Wyłączenie sensora i zwolnienie zasobów urządzenia
    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        locationCallback = null
        onSignalQualityChanged("Moduł GPS nieaktywny")
    }

    // TWÓJ AUTORSKI FILTR RAJDOWY - Zaimplementowany 1:1 dla odcięcia zakłóceń i "pływania" GPS
    private fun isLocationValid(location: Location): Boolean {
        // 1. PUNKTOWA ZMIANA: Dokładność słabsza niż 15 metrów jest natychmiast odrzucana (Brama Szumu)
        if (location.accuracy > 15.0f) return false

        // 2. Filtrujemy dane zcache'owane (stale data). Max 2 sekundy różnicy od czasu rzeczywistego
        val locationAgeNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        val locationAgeMillis = locationAgeNanos / 1_000_000L
        if (locationAgeMillis > 2000L) return false

        return true
    }
}