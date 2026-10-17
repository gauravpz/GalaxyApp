package com.gaurav.galaxyapp.data

/**
 * Data model for game configuration fetched from remote config or local defaults.
 */
data class GameConfig(
    val difficulty: String = "easy",
    val mathMaxRange: Int = 10,
    val mathMinRange: Int = 1,
    val reactionIntervalMs: Long = 1200L,
    val colorBlinkSpeedMs: Long = 500L,
    val wordStrings: List<String> = listOf("STAR", "MOON", "SUN", "COMET", "GALAXY"),
    val taskArrays: List<String> = listOf("Find numbers", "Follow colors", "Tap targets"),
    val lastSyncTimestamp: Long = 0L,
    val configVersion: String = "1.0.0"
)
