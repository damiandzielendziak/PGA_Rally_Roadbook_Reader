package com.example.roadbook.data.local

import android.content.Context
import com.example.roadbook.model.RallyStage
import com.example.roadbook.model.StageCategory
import com.example.roadbook.model.StageStatus
import java.io.File
import java.io.InputStream

class FileStorageManager(private val context: Context) {

    private val userPrefs = context.getSharedPreferences("RoadbookUserStages", Context.MODE_PRIVATE)
    private val versionPrefs = context.getSharedPreferences("RoadbookSystemVersions", Context.MODE_PRIVATE)
    private val systemCachePrefs = context.getSharedPreferences("RoadbookSystemStagesCache", Context.MODE_PRIVATE)

    fun saveGpxFile(stageId: String, inputStream: InputStream) {
        context.openFileOutput("$stageId.gpx", Context.MODE_PRIVATE).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
    }

    fun deleteGpxFile(stageId: String) {
        try {
            val file = File(context.filesDir, "$stageId.gpx")
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun gpxFileExists(stageId: String): Boolean {
        return File(context.filesDir, "$stageId.gpx").exists()
    }

    fun getLocalVersion(stageId: String): String {
        return versionPrefs.getString("VER_$stageId", "") ?: ""
    }

    fun saveLocalVersion(stageId: String, version: String) {
        versionPrefs.edit().putString("VER_$stageId", version).apply()
    }

    fun saveSystemStagesCache(systemStages: List<RallyStage>) {
        val stringSet = systemStages.map {
            "${it.id}|${it.title}|${it.distanceKm}|${it.waypointCount}|${it.version}|${it.description ?: ""}|${it.dominantSurface ?: ""}"
        }.toSet()
        systemCachePrefs.edit().putStringSet("SAVED_SYSTEM_STAGES", stringSet).apply()
    }

    fun loadSystemStagesCache(): List<RallyStage> {
        val stringSet = systemCachePrefs.getStringSet("SAVED_SYSTEM_STAGES", emptySet()) ?: emptySet()
        return stringSet.mapNotNull { str ->
            val parts = str.split("|")
            if (parts.size >= 5) {
                val id = parts[0]
                val desc = if (parts.size >= 6) parts[5] else ""
                val surface = if (parts.size >= 7) parts[6] else ""
                if (gpxFileExists(id)) {
                    RallyStage(
                        id = id,
                        title = parts[1],
                        distanceKm = parts[2].toDoubleOrNull() ?: 0.0,
                        waypointCount = parts[3].toIntOrNull() ?: 0,
                        category = StageCategory.SYSTEM,
                        status = StageStatus.UP_TO_DATE,
                        version = parts[4],
                        description = desc,
                        dominantSurface = surface
                    )
                } else null
            } else null
        }
    }

    fun saveUserStages(userStages: List<RallyStage>, passwords: Map<String, String>, expirations: Map<String, Long>, unlocked: Set<String>) {
        val stringSet = userStages.map {
            "${it.id}|${it.title}|${it.distanceKm}|${it.waypointCount}|${it.version}|${it.description ?: ""}|${it.dominantSurface ?: ""}"
        }.toSet()

        val editor = userPrefs.edit()
        editor.putStringSet("SAVED_USER_STAGES", stringSet)

        userStages.forEach { stage ->
            passwords[stage.id]?.let { editor.putString("PASS_${stage.id}", it) }
            expirations[stage.id]?.let { editor.putLong("EXP_${stage.id}", it) }
            if (unlocked.contains(stage.id)) {
                editor.putBoolean("UNLOCKED_${stage.id}", true)
            }
        }
        editor.apply()
    }

    fun loadUserStages(passwords: MutableMap<String, String>, expirations: MutableMap<String, Long>, unlocked: MutableSet<String>): List<RallyStage> {
        val stringSet = userPrefs.getStringSet("SAVED_USER_STAGES", emptySet()) ?: emptySet()
        return stringSet.mapNotNull { str ->
            val parts = str.split("|")
            if (parts.size >= 5) {
                val id = parts[0]
                val desc = if (parts.size >= 6) parts[5] else ""
                val surface = if (parts.size >= 7) parts[6] else ""

                userPrefs.getString("PASS_$id", "")?.let { if(it.isNotBlank()) passwords[id] = it }
                userPrefs.getLong("EXP_$id", 0L).let { if(it > 0L) expirations[id] = it }
                if (userPrefs.getBoolean("UNLOCKED_$id", false)) unlocked.add(id)

                RallyStage(
                    id = id,
                    title = parts[1],
                    distanceKm = parts[2].toDoubleOrNull() ?: 0.0,
                    waypointCount = parts[3].toIntOrNull() ?: 0,
                    category = StageCategory.USER,
                    status = StageStatus.LOCAL,
                    version = parts[4],
                    description = desc,
                    dominantSurface = surface
                )
            } else null
        }
    }
}