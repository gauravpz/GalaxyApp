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
import com.gaurav.galaxyapp.data.MonitoringConfig
import com.gaurav.galaxyapp.data.QuizQuestion
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
 * - Endpoint: Synchronizes from GitHub repository (supports raw endpoint and API contents endpoint).
 * - Restores questions for Math, English, Hindi, and Marathi along with playtime monitoring rules.
 * - Network activity strictly restricted to this worker block.
 */
class ConfigSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ConfigSyncWorker"
        const val UNIQUE_WORK_NAME = "PeriodicGameConfigSync"

        var owner: String = "gauravpz"
        var repo: String = "GalaxyApp"
        var branch: String = "main"
        var githubPat: String = ""

        /**
         * Schedules the 6-hour periodic work request with network connected constraint.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<ConfigSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "ConfigSyncWorker periodic work scheduled for 6-hour interval.")
        }

        /**
         * Enqueues an immediate one-time sync request (e.g. manual sync button or when Wi-Fi connects).
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

        // Try primary raw URL first, then fallback to GitHub API endpoint
        val rawUrl = "https://raw.githubusercontent.com/$owner/$repo/$branch/config.json"
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/config.json"

        Log.d(TAG, "Initiating remote config fetch from: $rawUrl")

        var configJsonString = fetchHttp(rawUrl, isRaw = true)
        if (configJsonString == null) {
            Log.d(TAG, "Raw fetch failed, attempting GitHub API: $apiUrl")
            configJsonString = fetchHttp(apiUrl, isRaw = false)
        }

        if (configJsonString != null) {
            try {
                val parsedConfig = parseConfigJson(configJsonString)
                repository.saveConfig(parsedConfig)
                Log.d(TAG, "Game config successfully updated from remote repository: v${parsedConfig.configVersion}")
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing fetched config JSON: ${e.message}")
                Result.success()
            }
        } else {
            Log.w(TAG, "Could not fetch remote config. Retaining offline local sandbox configuration.")
            Result.success()
        }
    }

    private fun fetchHttp(endpointUrl: String, isRaw: Boolean): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(endpointUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "GalaxyApp-WearOS")
                if (!isRaw) {
                    setRequestProperty("Accept", "application/vnd.github.v3.raw")
                }
                if (githubPat.isNotBlank() && !githubPat.startsWith("YOUR_")) {
                    setRequestProperty("Authorization", "Bearer $githubPat")
                }
                doInput = true
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val content = reader.readText()
                reader.close()
                content
            } else {
                Log.w(TAG, "HTTP $responseCode from $endpointUrl")
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "HTTP Exception fetching $endpointUrl: ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseConfigJson(jsonStr: String): GameConfig {
        val json = JSONObject(jsonStr)

        val difficulty = json.optString("difficulty", "medium")
        val mathMax = json.optInt("mathMaxRange", when (difficulty) {
            "hard" -> 50
            "medium" -> 20
            else -> 10
        })
        val mathMin = json.optInt("mathMinRange", 1)
        val reactionInterval = json.optLong("reactionIntervalMs", when (difficulty) {
            "hard" -> 700L
            "medium" -> 1000L
            else -> 1200L
        })
        val colorBlinkSpeed = json.optLong("colorBlinkSpeedMs", when (difficulty) {
            "hard" -> 350L
            "medium" -> 450L
            else -> 550L
        })
        val version = json.optString("configVersion", "1.1.0")

        val wordsList = mutableListOf<String>()
        val wordsArray: JSONArray? = json.optJSONArray("wordStrings")
        if (wordsArray != null) {
            for (i in 0 until wordsArray.length()) {
                wordsList.add(wordsArray.getString(i))
            }
        } else {
            wordsList.addAll(listOf("STAR", "MOON", "SUN", "COMET", "GALAXY", "PLANET"))
        }

        val tasksList = mutableListOf<String>()
        val tasksArray: JSONArray? = json.optJSONArray("taskArrays")
        if (tasksArray != null) {
            for (i in 0 until tasksArray.length()) {
                tasksList.add(tasksArray.getString(i))
            }
        } else {
            tasksList.addAll(listOf("Find numbers", "Follow colors", "Tap targets", "Math practice"))
        }

        // Parse monitoring config
        val monitoringObj = json.optJSONObject("monitoring")
        val monitoring = if (monitoringObj != null) {
            val subjects = mutableListOf<String>()
            val subArr = monitoringObj.optJSONArray("trackSubjects")
            if (subArr != null) {
                for (i in 0 until subArr.length()) subjects.add(subArr.getString(i))
            } else {
                subjects.addAll(listOf("math", "english", "hindi", "marathi"))
            }

            val quietObj = monitoringObj.optJSONObject("quietHours")
            val quietEnabled = quietObj?.optBoolean("enabled", true) ?: true
            val quietStart = quietObj?.optInt("startHour", 21) ?: 21
            val quietEnd = quietObj?.optInt("endHour", 7) ?: 7

            MonitoringConfig(
                enabled = monitoringObj.optBoolean("enabled", true),
                maxDailyPlayTimeMinutes = monitoringObj.optInt("maxDailyPlayTimeMinutes", 30),
                breakReminderMinutes = monitoringObj.optInt("breakReminderMinutes", 15),
                trackSessionDuration = monitoringObj.optBoolean("trackSessionDuration", true),
                trackSubjects = subjects,
                syncPlayStats = monitoringObj.optBoolean("syncPlayStats", true),
                quietHoursEnabled = quietEnabled,
                quietHoursStart = quietStart,
                quietHoursEnd = quietEnd
            )
        } else {
            MonitoringConfig()
        }

        // Parse questions map
        val questionsObj = json.optJSONObject("questions")
        val questionsMap = mutableMapOf<String, List<QuizQuestion>>()
        if (questionsObj != null) {
            val keys = questionsObj.keys()
            while (keys.hasNext()) {
                val subject = keys.next()
                val qArray = questionsObj.optJSONArray(subject) ?: continue
                val qList = mutableListOf<QuizQuestion>()
                for (i in 0 until qArray.length()) {
                    val qObj = qArray.getJSONObject(i)
                    val opts = mutableListOf<String>()
                    val optArr = qObj.optJSONArray("options")
                    if (optArr != null) {
                        for (j in 0 until optArr.length()) {
                            opts.add(optArr.getString(j))
                        }
                    }
                    qList.add(
                        QuizQuestion(
                            id = qObj.optString("id", "$subject-$i"),
                            question = qObj.optString("question"),
                            options = opts,
                            answer = qObj.optString("answer"),
                            explanation = qObj.optString("explanation")
                        )
                    )
                }
                questionsMap[subject] = qList
            }
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
            configVersion = version,
            monitoring = monitoring,
            questions = questionsMap
        )
    }
}
