package com.gaurav.galaxyapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages child daily playtime and session tracking.
 * Automatically resets daily counter when a new calendar day starts.
 */
class PlaytimeManager(private val context: Context) {

    private object Keys {
        val TODAY_PLAY_SECONDS = longPreferencesKey("today_play_seconds")
        val LAST_PLAY_DATE = stringPreferencesKey("last_play_date")
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun getTodayDateString(): String = dateFormat.format(Date())

    val todayPlaySecondsFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        val savedDate = prefs[Keys.LAST_PLAY_DATE] ?: ""
        val today = getTodayDateString()
        if (savedDate == today) {
            prefs[Keys.TODAY_PLAY_SECONDS] ?: 0L
        } else {
            0L // Automatically resets on new day
        }
    }

    suspend fun addPlayTime(seconds: Long) {
        val today = getTodayDateString()
        context.dataStore.edit { prefs ->
            val savedDate = prefs[Keys.LAST_PLAY_DATE] ?: ""
            val currentSeconds = if (savedDate == today) {
                prefs[Keys.TODAY_PLAY_SECONDS] ?: 0L
            } else {
                0L
            }
            prefs[Keys.LAST_PLAY_DATE] = today
            prefs[Keys.TODAY_PLAY_SECONDS] = currentSeconds + seconds
        }
    }
}
