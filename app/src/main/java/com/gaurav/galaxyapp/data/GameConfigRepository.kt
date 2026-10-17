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
    }

    val configFlow: Flow<GameConfig> = context.dataStore.data.map { preferences ->
        val difficulty = preferences[PreferencesKeys.DIFFICULTY] ?: "easy"
        val mathMax = preferences[PreferencesKeys.MATH_MAX] ?: 10
        val mathMin = preferences[PreferencesKeys.MATH_MIN] ?: 1
        val reactionInterval = preferences[PreferencesKeys.REACTION_INTERVAL] ?: 1200L
        val blinkSpeed = preferences[PreferencesKeys.COLOR_BLINK_SPEED] ?: 500L
        val lastSync = preferences[PreferencesKeys.LAST_SYNC] ?: 0L
        val version = preferences[PreferencesKeys.CONFIG_VERSION] ?: "1.0.0"

        val wordsJson = preferences[PreferencesKeys.WORD_STRINGS]
        val words = if (!wordsJson.isNullOrEmpty()) {
            parseJsonStringList(wordsJson)
        } else {
            listOf("STAR", "MOON", "SUN", "COMET", "GALAXY")
        }

        val tasksJson = preferences[PreferencesKeys.TASK_ARRAYS]
        val tasks = if (!tasksJson.isNullOrEmpty()) {
            parseJsonStringList(tasksJson)
        } else {
            listOf("Find numbers", "Follow colors", "Tap targets")
        }

        GameConfig(
            difficulty = difficulty,
            mathMaxRange = mathMax,
            mathMinRange = mathMin,
            reactionIntervalMs = reactionInterval,
            colorBlinkSpeedMs = blinkSpeed,
            wordStrings = words,
            taskArrays = tasks,
            lastSyncTimestamp = lastSync,
            configVersion = version
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
            // fallback if string is comma separated
            return jsonString.split(",").map { it.trim() }
        }
        return list
    }
}
