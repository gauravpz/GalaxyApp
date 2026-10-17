package com.gaurav.galaxyapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gaurav.galaxyapp.data.GameConfig
import com.gaurav.galaxyapp.data.GameConfigRepository
import com.gaurav.galaxyapp.worker.ConfigSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameConfigRepository(application)

    // Central config StateFlow collected by all games
    val configState: StateFlow<GameConfig> = repository.configFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GameConfig()
        )

    // High scores tracked in local memory state
    private val _mathScore = MutableStateFlow(0)
    val mathScore: StateFlow<Int> = _mathScore.asStateFlow()

    private val _colorMemoHighScore = MutableStateFlow(0)
    val colorMemoHighScore: StateFlow<Int> = _colorMemoHighScore.asStateFlow()

    private val _reactionHighScore = MutableStateFlow(0)
    val reactionHighScore: StateFlow<Int> = _reactionHighScore.asStateFlow()

    fun updateMathScore(newScore: Int) {
        if (newScore > _mathScore.value) {
            _mathScore.value = newScore
        }
    }

    fun updateColorMemoHighScore(rounds: Int) {
        if (rounds > _colorMemoHighScore.value) {
            _colorMemoHighScore.value = rounds
        }
    }

    fun updateReactionHighScore(score: Int) {
        if (score > _reactionHighScore.value) {
            _reactionHighScore.value = score
        }
    }

    fun syncNow() {
        ConfigSyncWorker.enqueueOneTimeSync(getApplication())
    }

    fun updateDifficulty(difficulty: String) {
        viewModelScope.launch {
            val current = configState.value
            val newConfig = when (difficulty.lowercase()) {
                "hard" -> current.copy(
                    difficulty = "hard",
                    mathMaxRange = 30,
                    reactionIntervalMs = 700L,
                    colorBlinkSpeedMs = 350L
                )
                "medium" -> current.copy(
                    difficulty = "medium",
                    mathMaxRange = 20,
                    reactionIntervalMs = 900L,
                    colorBlinkSpeedMs = 450L
                )
                else -> current.copy(
                    difficulty = "easy",
                    mathMaxRange = 10,
                    reactionIntervalMs = 1200L,
                    colorBlinkSpeedMs = 550L
                )
            }
            repository.saveConfig(newConfig)
        }
    }
}
