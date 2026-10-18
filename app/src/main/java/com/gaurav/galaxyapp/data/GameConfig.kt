package com.gaurav.galaxyapp.data

import androidx.compose.runtime.Stable

/**
 * Single question model for quiz questions (Math, English, Hindi, Marathi).
 */
@Stable
data class QuizQuestion(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val answer: String = "",
    val explanation: String = ""
)

/**
 * Monitoring and playtime control configuration.
 */
@Stable
data class MonitoringConfig(
    val enabled: Boolean = true,
    val maxDailyPlayTimeMinutes: Int = 30,
    val breakReminderMinutes: Int = 15,
    val trackSessionDuration: Boolean = true,
    val trackSubjects: List<String> = listOf("math", "english", "hindi", "marathi"),
    val syncPlayStats: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: Int = 21,
    val quietHoursEnd: Int = 7
)

/**
 * Data model for game configuration fetched from remote config or local defaults.
 */
@Stable
data class GameConfig(
    val difficulty: String = "medium",
    val mathMaxRange: Int = 20,
    val mathMinRange: Int = 1,
    val reactionIntervalMs: Long = 1000L,
    val colorBlinkSpeedMs: Long = 450L,
    val wordStrings: List<String> = listOf("STAR", "MOON", "SUN", "COMET", "GALAXY", "PLANET"),
    val taskArrays: List<String> = listOf("Find numbers", "Follow colors", "Tap targets", "Math practice"),
    val lastSyncTimestamp: Long = 0L,
    val configVersion: String = "1.1.0",
    val monitoring: MonitoringConfig = MonitoringConfig(),
    val questions: Map<String, List<QuizQuestion>> = emptyMap()
)
