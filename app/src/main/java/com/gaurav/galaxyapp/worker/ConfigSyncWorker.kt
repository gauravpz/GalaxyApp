package com.gaurav.galaxyapp.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gaurav.galaxyapp.data.GameConfig
import com.gaurav.galaxyapp.data.GameConfigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager Worker responsible for securely syncing remote configuration.
 *
 * Requirements:
 * - 1-hour interval with NetworkType.CONNECTED constraint.
 * - Endpoint URL: https://github.com{OWNER}/{REPO}/contents/config.json
 * - Headers:
 *     "Accept: application/vnd.github.v3.raw"
 *     "Authorization: Bearer YOUR_GITHUB_PAT"
 * - Network activity strictly restricted to this worker block.
 */
class ConfigSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ConfigSyncWorker"
        const val UNIQUE_WORK_NAME = "PeriodicGameConfigSync"

        // Default configured values (can be overridden via inputData or BuildConfig)
        var owner: String = "owner"
        var repo: String = "repo"
        var githubPat: String = "YOUR_GITHUB_PAT"

        /**
         * Schedules the 1-hour periodic work request with network connected constraint.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<ConfigSyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "ConfigSyncWorker periodic work scheduled for 1-hour interval.")
        }

        /**
         * Enqueues an immediate one-time sync request (useful for testing or initial startup).
         */
        fun enqueueOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<ConfigSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
            Log.d(TAG, "ConfigSyncWorker one-time work enqueued.")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repository = GameConfigRepository(context)

        // Construct endpoint URL according to requirement:
        // https://github.com{OWNER}/{REPO}/contents/config.json or https://api.github.com/repos/{OWNER}/{REPO}/contents/config.json
        val endpointUrl = if (owner == "owner" && repo == "repo") {
            "https://github.com/{OWNER}/{REPO}/contents/config.json"
        } else {
            "https://api.github.com/repos/$owner/$repo/contents/config.json"
        }

        Log.d(TAG, "Initiating secure config fetch from: $endpointUrl")

        var connection: HttpURLConnection? = null
        try {
            val url = URL(endpointUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                // Strictly required headers
                setRequestProperty("Accept", "application/vnd.github.v3.raw")
                setRequestProperty("Authorization", "Bearer $githubPat")
                setRequestProperty("User-Agent", "GalaxyApp-WearOS")
                doInput = true
            }

            val responseCode = connection.responseCode
            Log.d(TAG, "Sync HTTP Response Code: $responseCode")

            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseBody = reader.readText()
                reader.close()

                val parsedConfig = parseConfigJson(responseBody)
                repository.saveConfig(parsedConfig)
                Log.d(TAG, "Game config successfully updated from remote repository: $parsedConfig")
                Result.success()
            } else {
                Log.w(TAG, "Config fetch returned non-success HTTP status: $responseCode. Retaining local defaults.")
                // Return success so periodic job continues, or retry on transient network errors
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching remote config: ${e.message}. Retaining offline local sandbox configuration.")
            // Keep app 100% offline-resilient: if remote is unreachable or dummy credentials, return success
            Result.success()
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseConfigJson(jsonStr: String): GameConfig {
        val json = JSONObject(jsonStr)

        val difficulty = json.optString("difficulty", "easy")
        val mathMax = json.optInt("mathMaxRange", when (difficulty) {
            "hard" -> 50
            "medium" -> 20
            else -> 10
        })
        val mathMin = json.optInt("mathMinRange", 1)
        val reactionInterval = json.optLong("reactionIntervalMs", when (difficulty) {
            "hard" -> 700L
            "medium" -> 900L
            else -> 1200L
        })
        val colorBlinkSpeed = json.optLong("colorBlinkSpeedMs", when (difficulty) {
            "hard" -> 350L
            "medium" -> 450L
            else -> 550L
        })
        val version = json.optString("configVersion", "1.0.0")

        val wordsList = mutableListOf<String>()
        val wordsArray: JSONArray? = json.optJSONArray("wordStrings")
        if (wordsArray != null) {
            for (i in 0 until wordsArray.length()) {
                wordsList.add(wordsArray.getString(i))
            }
        } else {
            wordsList.addAll(listOf("STAR", "MOON", "SUN", "COMET", "GALAXY"))
        }

        val tasksList = mutableListOf<String>()
        val tasksArray: JSONArray? = json.optJSONArray("taskArrays")
        if (tasksArray != null) {
            for (i in 0 until tasksArray.length()) {
                tasksList.add(tasksArray.getString(i))
            }
        } else {
            tasksList.addAll(listOf("Find numbers", "Follow colors", "Tap targets"))
        }

        return GameConfig(
            difficulty = difficulty,
            mathMaxRange = mathMax,
            mathMinRange = mathMin,
            reactionIntervalMs = reactionInterval,
            colorBlinkSpeedMs = colorBlinkSpeed,
            wordStrings = wordsList,
            taskArrays = tasksList,
            lastSyncTimestamp = System.currentTimeMillis(),
            configVersion = version
        )
    }
}
