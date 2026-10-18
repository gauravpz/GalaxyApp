package com.gaurav.galaxyapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gaurav.galaxyapp.games.ColorMemoGame
import com.gaurav.galaxyapp.games.DailyQuizGame
import com.gaurav.galaxyapp.games.MathDashGame
import com.gaurav.galaxyapp.games.ReactionTapperGame
import com.gaurav.galaxyapp.ui.ActiveScreen
import com.gaurav.galaxyapp.ui.GameHubScreen
import com.gaurav.galaxyapp.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fix 4: Reduce screen brightness to 50% — OLED display is the #1 battery consumer.
        // Half-brightness is fully readable indoors and on a child's small watch face.
        window.attributes = window.attributes.apply {
            screenBrightness = 0.5f
        }

        setContent {
            WearGameApp(viewModel = viewModel)
        }
    }
}

@Composable
fun WearGameApp(viewModel: GameViewModel) {
    val config by viewModel.configState.collectAsState()
    val mathScore by viewModel.mathScore.collectAsState()
    val colorScore by viewModel.colorMemoHighScore.collectAsState()
    val reactionScore by viewModel.reactionHighScore.collectAsState()
    val quizScore by viewModel.quizHighScore.collectAsState()
    val todayPlaySeconds by viewModel.todayPlaySeconds.collectAsState()

    var currentScreen by remember { mutableStateOf(ActiveScreen.HUB) }

    // Fix 1: Battery-efficient playtime tracking.
    // OLD: while(true) { delay(5000); writeDataStore() } — 360 DataStore writes per 30-min session.
    // NEW: Record session start time in-memory; flush ONE write to DataStore when the user exits
    //      the game screen. DisposableEffect's onDispose fires on every screen transition.
    val sessionStartMs = remember { mutableStateOf(0L) }
    DisposableEffect(currentScreen) {
        if (currentScreen != ActiveScreen.HUB && config.monitoring.enabled) {
            sessionStartMs.value = System.currentTimeMillis()
        }
        onDispose {
            // Flush accumulated play-seconds once on screen exit — from 360 writes → 1 write
            if (sessionStartMs.value > 0L && config.monitoring.enabled) {
                val elapsedSeconds = (System.currentTimeMillis() - sessionStartMs.value) / 1000L
                if (elapsedSeconds > 0L) {
                    viewModel.recordPlaytime(elapsedSeconds)
                }
                sessionStartMs.value = 0L
            }
        }
    }

    when (currentScreen) {
        ActiveScreen.HUB -> {
            GameHubScreen(
                config = config,
                playtimeSeconds = todayPlaySeconds,
                quizHighScore = quizScore,
                mathHighScore = mathScore,
                colorMemoHighScore = colorScore,
                reactionHighScore = reactionScore,
                onNavigate = { screen -> currentScreen = screen },
                onToggleDifficulty = {
                    val nextDiff = when (config.difficulty.lowercase()) {
                        "easy" -> "medium"
                        "medium" -> "hard"
                        else -> "easy"
                    }
                    viewModel.updateDifficulty(nextDiff)
                },
                onSyncConfig = {
                    viewModel.syncNow()
                }
            )
        }

        ActiveScreen.QUIZ -> {
            DailyQuizGame(
                config = config,
                highScore = quizScore,
                onScoreUpdated = { newScore -> viewModel.updateQuizHighScore(newScore) },
                onBack = { currentScreen = ActiveScreen.HUB }
            )
        }

        ActiveScreen.MATH_DASH -> {
            MathDashGame(
                config = config,
                highScore = mathScore,
                onScoreUpdated = { newScore -> viewModel.updateMathScore(newScore) },
                onBack = { currentScreen = ActiveScreen.HUB }
            )
        }

        ActiveScreen.COLOR_MEMO -> {
            ColorMemoGame(
                config = config,
                highScore = colorScore,
                onScoreUpdated = { newScore -> viewModel.updateColorMemoHighScore(newScore) },
                onBack = { currentScreen = ActiveScreen.HUB }
            )
        }

        ActiveScreen.REACTION_TAPPER -> {
            ReactionTapperGame(
                config = config,
                highScore = reactionScore,
                onScoreUpdated = { newScore -> viewModel.updateReactionHighScore(newScore) },
                onBack = { currentScreen = ActiveScreen.HUB }
            )
        }
    }
}
