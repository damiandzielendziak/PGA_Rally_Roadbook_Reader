package com.example.roadbook.data.repository

import android.content.Context
import com.example.roadbook.data.local.FileStorageManager
import com.example.roadbook.model.RallyStage
import com.example.roadbook.model.StageCategory
import com.example.roadbook.model.StageStatus
import com.example.roadbook.utils.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import java.io.IOException

class StageRepository(
    private val context: Context,
    private val storageManager: FileStorageManager
) {

    private suspend fun downloadGpxFileSilently(urlStr: String, stageId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                var currentUrl = urlStr.replace("[\\s\\p{Z}\\u00A0\\u200B]".toRegex(), "")
                var redirectCount = 0
                var success = false

                while (redirectCount < 5 && !success) {
                    val url = URL(currentUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000
                    connection.instanceFollowRedirects = false
                    connection.useCaches = false
                    connection.defaultUseCaches = false

                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15")
                    connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    connection.setRequestProperty("Accept-Language", "pl-PL,pl;q=0.9")
                    connection.setRequestProperty("Cache-Control", "no-cache")
                    connection.setRequestProperty("X-PGA-Client-Token", SecurityUtils.getSecretToken())

                    val responseCode = connection.responseCode

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        connection.inputStream.use { inputStream ->
                            storageManager.saveGpxFile(stageId, inputStream)
                        }
                        success = true
                    } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                        responseCode == 307 || responseCode == 308) {

                        val location = connection.getHeaderField("Location")
                        if (!location.isNullOrBlank()) {
                            currentUrl = if (location.startsWith("http")) location else {
                                val base = URL(currentUrl)
                                URL(base.protocol, base.host, base.port, location).toString()
                            }
                            redirectCount++
                        } else {
                            break
                        }
                    } else {
                        break
                    }
                }
                success
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun fetchSecureText(urlStr: String): String {
        return withContext(Dispatchers.IO) {
            var currentUrl = urlStr.replace("[\\s\\p{Z}\\u00A0\\u200B]".toRegex(), "")
            var redirectCount = 0
            var success = false
            var result = ""

            while (redirectCount < 5 && !success) {
                val url = URL(currentUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.instanceFollowRedirects = false
                connection.useCaches = false
                connection.defaultUseCaches = false

                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15")
                connection.setRequestProperty("Accept", "application/json, text/plain, */*")
                connection.setRequestProperty("Cache-Control", "no-cache")
                connection.setRequestProperty("X-PGA-Client-Token", SecurityUtils.getSecretToken())

                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    result = connection.inputStream.bufferedReader().use { it.readText() }
                    success = true
                } else if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrBlank()) {
                        currentUrl = if (location.startsWith("http")) location else {
                            val base = URL(currentUrl)
                            URL(base.protocol, base.host, base.port, location).toString()
                        }
                        redirectCount++
                    } else {
                        break
                    }
                } else {
                    break
                }
            }

            if (!success) throw IOException("Fetch failed with status from server")
            result
        }
    }

    suspend fun syncStages(
        cachedSystemStages: List<RallyStage>,
        cachedUserStages: List<RallyStage>
    ): List<RallyStage> {
        try {
            val jsonText = fetchSecureText("https://pga-rally.com/rally/stages.json")
            val rootJson = JSONObject(jsonText)
            val systemArray = rootJson.getJSONArray("system_stages")

            val freshSystemStages = mutableListOf<RallyStage>()
            for (i in 0 until systemArray.length()) {
                val item = systemArray.getJSONObject(i)
                val id = item.getString("id").trim()
                val title = item.getString("title")
                val serverVersion = item.getString("version").trim()
                val fileUrl = item.getString("file_url").trim()
                val desc = item.optString("description", "")
                val surface = item.optString("dominant_surface", "")
                val imageUrl = item.optString("image_url", null) // Pobieranie nowego pola z JSON

                val localVersion = storageManager.getLocalVersion(id)
                val fileExists = storageManager.gpxFileExists(id)

                try {
                    if (localVersion != serverVersion || !fileExists) {
                        val downloadSuccess = downloadGpxFileSilently(fileUrl, id)
                        if (downloadSuccess) {
                            storageManager.saveLocalVersion(id, serverVersion)
                        }
                    }

                    if (storageManager.gpxFileExists(id)) {
                        freshSystemStages.add(
                            RallyStage(
                                id = id,
                                title = title,
                                distanceKm = item.getDouble("distance_km"),
                                waypointCount = item.getInt("waypoint_count"),
                                category = StageCategory.SYSTEM,
                                status = StageStatus.UP_TO_DATE,
                                version = serverVersion,
                                description = desc,
                                dominantSurface = surface,
                                imageUrl = imageUrl // Przekazanie adresu URL do obiektu
                            )
                        )
                    }
                } catch (fileException: Exception) {
                    fileException.printStackTrace()
                    cachedSystemStages.firstOrNull { it.id == id }?.let { freshSystemStages.add(it) }
                }
            }

            storageManager.saveSystemStagesCache(freshSystemStages)
            return freshSystemStages + cachedUserStages

        } catch (e: Exception) {
            e.printStackTrace()
            return cachedSystemStages + cachedUserStages
        }
    }

    suspend fun fetchCloudStage(code: String): JSONObject? {
        return try {
            val jsonText = fetchSecureText("https://pga-rally.com/rally/stages.json")
            val rootJson = JSONObject(jsonText)
            val userStagesJson = rootJson.getJSONObject("user_stages")
            if (userStagesJson.has(code)) userStagesJson.getJSONObject(code) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadCloudGpx(fileUrl: String, newId: String): Boolean {
        return downloadGpxFileSilently(fileUrl, newId)
    }
}