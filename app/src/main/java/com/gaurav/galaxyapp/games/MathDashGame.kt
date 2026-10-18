package com.gaurav.galaxyapp.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.gaurav.galaxyapp.data.GameConfig
import kotlin.random.Random

data class MathQuestion(
    val equationText: String,
    val correctAnswer: Int,
    val choices: List<Int>
)

private fun generateQuestion(config: GameConfig): MathQuestion {
    val maxVal = config.mathMaxRange.coerceAtLeast(10)
    val minVal = config.mathMinRange.coerceAtLeast(1)
    val opType = when (config.difficulty.lowercase()) {
        "hard" -> Random.nextInt(3) // 0: +, 1: -, 2: *
        "medium" -> if (Random.nextBoolean()) 0 else 1
        else -> 0 // easy: mostly addition and small subtraction
    }

    val (equation, answer) = when (opType) {
        2 -> {
            // Multiplication
            val a = Random.nextInt(2, 7)
            val b = Random.nextInt(2, 6)
            "$a × $b" to (a * b)
        }
        1 -> {
            // Subtraction (positive results)
            val a = Random.nextInt(minVal + 2, maxVal + 1)
            val b = Random.nextInt(minVal, a)
            "$a - $b" to (a - b)
        }
        else -> {
            // Addition
            val a = Random.nextInt(minVal, maxVal)
            val b = Random.nextInt(minVal, maxVal)
            "$a + $b" to (a + b)
        }
    }

    // Generate 2 unique plausible distractors
    val distractors = mutableSetOf<Int>()
    val offsets = listOf(-2, -1, 1, 2, 3, -3).shuffled()
    for (offset in offsets) {
        val cand = answer + offset
        if (cand >= 0 && cand != answer) {
            distractors.add(cand)
            if (distractors.size == 2) break
        }
    }
    while (distractors.size < 2) {
        val cand = Random.nextInt(0, answer + 5)
        if (cand != answer) distractors.add(cand)
    }

    val allChoices = (distractors.toList() + answer).shuffled()
    return MathQuestion(
        equationText = equation,
        correctAnswer = answer,
        choices = allChoices
    )
}

/**
 * Game 1: "Math Dash"
 * - Displays basic random equations based on fetched remote config difficulty.
 * - Shows 3 simple interactive circle buttons with numbers.
 * - Clicking correct gains 10 points and instantly rolls a new random equation.
 * - Wrapped in an outer Box with 16.dp padding to ensure Galaxy Watch circular UI integrity.
 */
@Composable
fun MathDashGame(
    config: GameConfig,
    highScore: Int,
    onScoreUpdated: (Int) -> Unit,
    onBack: () -> Unit
) {
    var score by remember { mutableIntStateOf(0) }
    var currentQuestion by remember { mutableStateOf(generateQuestion(config)) }
    var feedbackColor by remember { mutableStateOf<Color?>(null) }

    val bgColor by animateColorAsState(
        targetValue = feedbackColor ?: Color(0xFF0F141C),
        animationSpec = tween(durationMillis = 250),
        label = "math_feedback"
    )

    // Reset feedback color after brief animation
    LaunchedEffect(feedbackColor) {
        if (feedbackColor != null) {
            kotlinx.coroutines.delay(300)
            feedbackColor = null
        }
    }

    // Outer Box with strict 16.dp padding for Galaxy Watch 8 round display isolation
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Back icon button at top
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() }
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = " EXIT",
                    color = Color(0xFFAAAAAA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Main game content column
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Difficulty tag and score display
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.difficulty.uppercase(),
                    color = Color(0xFF4DD0E1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " • ",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Text(
                    text = "SCORE: $score",
                    color = Color(0xFFFFD54F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Equation Card
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E2838))
                    .padding(horizontal = 20.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${currentQuestion.equationText} = ?",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3 Interactive Circle Choice Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentQuestion.choices.forEach { choice ->
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2962FF))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (choice == currentQuestion.correctAnswer) {
                                    score += 10
                                    onScoreUpdated(score)
                                    feedbackColor = Color(0xFF1B5E20) // Success green flash
                                    currentQuestion = generateQuestion(config)
                                } else {
                                    feedbackColor = Color(0xFFB71C1C) // Failure red flash
                                    if (score >= 5) score -= 5
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$choice",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
