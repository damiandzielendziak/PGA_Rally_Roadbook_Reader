package com.example.roadbook.model

enum class StageCategory {
    SYSTEM, // Pobierane i aktualizowane z serwera
    USER    // Importowane ręcznie przez użytkownika z pliku .gpx
}

enum class StageStatus {
    UP_TO_DATE,       // Aktualny, gotowy do jazdy
    UPDATE_AVAILABLE, // Wykryto nowszą wersję na serwerze
    NEW,              // Całkiem nowy etap na serwerze, jeszcze nie pobrany
    LOCAL             // Etap zaimportowany ręcznie (zawsze lokalny)
}

data class RallyStage(
    val id: String,
    val title: String,
    val distanceKm: Double,
    val waypointCount: Int,
    val category: StageCategory,
    val status: StageStatus,
    val version: String,
    val description: String = "Puszcza Zielonka - Trasa rajdowa",
    val dominantSurface: String? = null, // Zachowane dla pełnej kompatybilności z widokiem kafelków dwukolumnowych

    // --- INTEGRACJA Z NOWĄ SPECYFIKACJĄ SERWERA ---
    val imageUrl: String? = null,        // Link do miniatury pobierany z pliku JSON
    val password: String? = null,        // Hasło blokady dla tras użytkownika (np. "9999")
    val expiresAt: Long? = null          // Timestamp określający ważność odcinka w milisekundach
)