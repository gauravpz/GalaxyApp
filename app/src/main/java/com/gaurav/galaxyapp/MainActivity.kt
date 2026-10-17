package com.gaurav.galaxyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.gaurav.galaxyapp.games.ColorMemoGame
import com.gaurav.galaxyapp.games.MathDashGame
import com.gaurav.galaxyapp.games.ReactionTapperGame
import com.gaurav.galaxyapp.ui.ActiveScreen
import com.gaurav.galaxyapp.ui.GameHubScreen
import com.gaurav.galaxyapp.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    var currentScreen by remember { mutableStateOf(ActiveScreen.HUB) }

    when (currentScreen) {
        ActiveScreen.HUB -> {
            GameHubScreen(
                config = config,
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
