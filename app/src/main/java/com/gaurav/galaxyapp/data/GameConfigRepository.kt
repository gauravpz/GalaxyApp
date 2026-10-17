package com.gaurav.galaxyapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "game_config_prefs")

class GameConfigRepository(private val context: Context) {

    private object PreferencesKeys {
        val DIFFICULTY = stringPreferencesKey("game_difficulty")
        val MATH_MAX = intPreferencesKey("math_max_range")
        val MATH_MIN = intPreferencesKey("math_min_range")
        val REACTION_INTERVAL = longPreferencesKey("reaction_interval_ms")
        val COLOR_BLINK_SPEED = longPreferencesKey("color_blink_speed_ms")
        val WORD_STRINGS = stringPreferencesKey("word_strings_json")
        val TASK_ARRAYS = stringPreferencesKey("task_arrays_json")
        val LAST_SYNC = longPreferencesKey("last_sync_timestamp")
        val CONFIG_VERSION = stringPreferencesKey("config_version")
        val MONITORING_JSON = stringPreferencesKey("monitoring_json")
        val QUESTIONS_JSON = stringPreferencesKey("questions_json")
    }

    val configFlow: Flow<GameConfig> = context.dataStore.data.map { preferences ->
        val difficulty = preferences[PreferencesKeys.DIFFICULTY] ?: "medium"
        val mathMax = preferences[PreferencesKeys.MATH_MAX] ?: 20
        val mathMin = preferences[PreferencesKeys.MATH_MIN] ?: 1
        val reactionInterval = preferences[PreferencesKeys.REACTION_INTERVAL] ?: 1000L
        val blinkSpeed = preferences[PreferencesKeys.COLOR_BLINK_SPEED] ?: 450L
        val lastSync = preferences[PreferencesKeys.LAST_SYNC] ?: 0L
        val version = preferences[PreferencesKeys.CONFIG_VERSION] ?: "1.1.0"

        val wordsJson = preferences[PreferencesKeys.WORD_STRINGS]
        val words = if (!wordsJson.isNullOrEmpty()) {
            parseJsonStringList(wordsJson)
        } else {
            listOf("STAR", "MOON", "SUN", "COMET", "GALAXY", "PLANET")
        }

        val tasksJson = preferences[PreferencesKeys.TASK_ARRAYS]
        val tasks = if (!tasksJson.isNullOrEmpty()) {
            parseJsonStringList(tasksJson)
        } else {
            listOf("Find numbers", "Follow colors", "Tap targets", "Math practice")
        }

        val monitoringJson = preferences[PreferencesKeys.MONITORING_JSON]
        val monitoring = parseMonitoringJson(monitoringJson)

        val questionsJson = preferences[PreferencesKeys.QUESTIONS_JSON]
        val questions = parseQuestionsJson(questionsJson)

        GameConfig(
            difficulty = difficulty,
            mathMaxRange = mathMax,
            mathMinRange = mathMin,
            reactionIntervalMs = reactionInterval,
            colorBlinkSpeedMs = blinkSpeed,
            wordStrings = words,
            taskArrays = tasks,
            lastSyncTimestamp = lastSync,
            configVersion = version,
            monitoring = monitoring,
            questions = questions
        )
    }

    suspend fun saveConfig(config: GameConfig) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DIFFICULTY] = config.difficulty
            prefs[PreferencesKeys.MATH_MAX] = config.mathMaxRange
            prefs[PreferencesKeys.MATH_MIN] = config.mathMinRange
            prefs[PreferencesKeys.REACTION_INTERVAL] = config.reactionIntervalMs
            prefs[PreferencesKeys.COLOR_BLINK_SPEED] = config.colorBlinkSpeedMs
            prefs[PreferencesKeys.WORD_STRINGS] = JSONArray(config.wordStrings).toString()
            prefs[PreferencesKeys.TASK_ARRAYS] = JSONArray(config.taskArrays).toString()
            prefs[PreferencesKeys.LAST_SYNC] = config.lastSyncTimestamp
            prefs[PreferencesKeys.CONFIG_VERSION] = config.configVersion
            prefs[PreferencesKeys.MONITORING_JSON] = serializeMonitoring(config.monitoring)
            prefs[PreferencesKeys.QUESTIONS_JSON] = serializeQuestions(config.questions)
        }
    }

    private fun parseJsonStringList(jsonString: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (_: Exception) {
            return jsonString.split(",").map { it.trim() }
        }
        return list
    }

    private fun parseMonitoringJson(jsonStr: String?): MonitoringConfig {
        if (jsonStr.isNullOrEmpty()) return MonitoringConfig()
        return try {
            val json = JSONObject(jsonStr)
            val subjects = mutableListOf<String>()
            val subArray = json.optJSONArray("trackSubjects")
            if (subArray != null) {
                for (i in 0 until subArray.length()) subjects.add(subArray.getString(i))
            } else {
                subjects.addAll(listOf("math", "english", "hindi", "marathi"))
            }

            MonitoringConfig(
                enabled = json.optBoolean("enabled", true),
                maxDailyPlayTimeMinutes = json.optInt("maxDailyPlayTimeMinutes", 30),
                breakReminderMinutes = json.optInt("breakReminderMinutes", 15),
                trackSessionDuration = json.optBoolean("trackSessionDuration", true),
                trackSubjects = subjects,
                syncPlayStats = json.optBoolean("syncPlayStats", true),
                quietHoursEnabled = json.optBoolean("quietHoursEnabled", true),
                quietHoursStart = json.optInt("quietHoursStart", 21),
                quietHoursEnd = json.optInt("quietHoursEnd", 7)
            )
        } catch (_: Exception) {
            MonitoringConfig()
        }
    }

    private fun serializeMonitoring(monitoring: MonitoringConfig): String {
        val json = JSONObject()
        json.put("enabled", monitoring.enabled)
        json.put("maxDailyPlayTimeMinutes", monitoring.maxDailyPlayTimeMinutes)
        json.put("breakReminderMinutes", monitoring.breakReminderMinutes)
        json.put("trackSessionDuration", monitoring.trackSessionDuration)
        json.put("trackSubjects", JSONArray(monitoring.trackSubjects))
        json.put("syncPlayStats", monitoring.syncPlayStats)
        json.put("quietHoursEnabled", monitoring.quietHoursEnabled)
        json.put("quietHoursStart", monitoring.quietHoursStart)
        json.put("quietHoursEnd", monitoring.quietHoursEnd)
        return json.toString()
    }

    private fun parseQuestionsJson(jsonStr: String?): Map<String, List<QuizQuestion>> {
        if (jsonStr.isNullOrEmpty()) return emptyMap()
        val result = mutableMapOf<String, List<QuizQuestion>>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val subject = keys.next()
                val qArray = json.optJSONArray(subject) ?: continue
                val list = mutableListOf<QuizQuestion>()
                for (i in 0 until qArray.length()) {
                    val qObj = qArray.getJSONObject(i)
                    val opts = mutableListOf<String>()
                    val optArr = qObj.optJSONArray("options")
                    if (optArr != null) {
                        for (j in 0 until optArr.length()) {
                            opts.add(optArr.getString(j))
                        }
                    }
                    list.add(
                        QuizQuestion(
                            id = qObj.optString("id", "$subject-$i"),
                            question = qObj.optString("question"),
                            options = opts,
                            answer = qObj.optString("answer"),
                            explanation = qObj.optString("explanation")
                        )
                    )
                }
                result[subject] = list
            }
        } catch (_: Exception) {}
        return result
    }

    private fun serializeQuestions(questions: Map<String, List<QuizQuestion>>): String {
        val root = JSONObject()
        for ((subject, list) in questions) {
            val arr = JSONArray()
            for (q in list) {
                val obj = JSONObject()
                obj.put("id", q.id)
                obj.put("question", q.question)
                obj.put("options", JSONArray(q.options))
                obj.put("answer", q.answer)
                obj.put("explanation", q.explanation)
                arr.put(obj)
            }
            root.put(subject, arr)
        }
        return root.toString()
    }
}
