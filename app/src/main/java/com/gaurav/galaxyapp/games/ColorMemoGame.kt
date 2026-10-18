package com.gaurav.galaxyapp.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import kotlinx.coroutines.delay
import kotlin.random.Random

// Quadrant Color Identifiers
private const val COLOR_TOP_RED = 0
private const val COLOR_RIGHT_BLUE = 1
private const val COLOR_BOTTOM_YELLOW = 2
private const val COLOR_LEFT_GREEN = 3

/**
 * Game 2: "Color Memo" (Simon Says Clone)
 * - 4-quadrant layout matching a circular screen (Top, Bottom, Left, Right).
 * - Safe Kotlin coroutine delay loop to blink a sequence of highlights.
 * - Tracks child tap order against standard ArrayList / index tracking.
 * - Circular UI integrity with outer Box 16.dp padding.
 */
@Composable
fun ColorMemoGame(
    config: GameConfig,
    highScore: Int,
    onScoreUpdated: (Int) -> Unit,
    onBack: () -> Unit
) {
    // Sequence tracking array
    val sequence = remember { mutableStateListOf<Int>() }
    var userTapIndex by remember { mutableIntStateOf(0) }
    var activeHighlight by remember { mutableStateOf<Int?>(null) }
    var isShowingSequence by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var roundNumber by remember { mutableIntStateOf(1) }
    var currentScore by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("Watch!") }

    // Start or advance round
    fun startNextRound() {
        userTapIndex = 0
        sequence.add(Random.nextInt(4))
        isShowingSequence = true
        statusText = "Watch!"
    }

    // Initialize game on first compose
    LaunchedEffect(Unit) {
        sequence.clear()
        startNextRound()
    }

    // Coroutine delay loop to blink sequence highlights
    LaunchedEffect(isShowingSequence) {
        if (isShowingSequence && !isGameOver) {
            statusText = "Watch!"
            delay(500)
            for (colorId in sequence) {
                activeHighlight = colorId
                delay(config.colorBlinkSpeedMs.coerceAtLeast(300L))
                activeHighlight = null
                delay(160)
            }
            statusText = "Your Turn!"
            isShowingSequence = false
        }
    }

    fun handleQuadrantClick(colorId: Int) {
        if (isShowingSequence || isGameOver) return

        // Flash tapped color briefly
        activeHighlight = colorId

        if (colorId == sequence[userTapIndex]) {
            // Correct tap in sequence
            userTapIndex++
            if (userTapIndex == sequence.size) {
                // Completed the round sequence!
                currentScore += 10 * sequence.size
                onScoreUpdated(currentScore)
                roundNumber++
                statusText = "Great Job!"
                startNextRound()
            }
        } else {
            // Mistake made
            isGameOver = true
            statusText = "Game Over!"
        }
    }

    LaunchedEffect(activeHighlight) {
        if (activeHighlight != null && !isShowingSequence) {
            delay(200)
            activeHighlight = null
        }
    }

    // Outer Box with 16.dp padding to respect Galaxy Watch circular boundaries
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0F14))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Exit button at top
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
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Exit",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = " EXIT",
                    color = Color(0xFFAAAAAA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Circular 4-Quadrant Layout
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            // TOP: Red (Quadrant 0)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .size(width = 60.dp, height = 36.dp)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomEnd = 8.dp, bottomStart = 8.dp))
                    .background(
                        if (activeHighlight == COLOR_TOP_RED) Color(0xFFFF5252) else Color(0xFF881B1B)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { handleQuadrantClick(COLOR_TOP_RED) }
            )

            // BOTTOM: Yellow (Quadrant 2)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 60.dp, height = 36.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 30.dp, bottomStart = 30.dp))
                    .background(
                        if (activeHighlight == COLOR_BOTTOM_YELLOW) Color(0xFFFFEB3B) else Color(0xFF8B8000)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { handleQuadrantClick(COLOR_BOTTOM_YELLOW) }
            )

            // LEFT: Green (Quadrant 3)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(width = 36.dp, height = 60.dp)
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 8.dp, bottomEnd = 8.dp, bottomStart = 30.dp))
                    .background(
                        if (activeHighlight == COLOR_LEFT_GREEN) Color(0xFF69F0AE) else Color(0xFF1B5E20)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { handleQuadrantClick(COLOR_LEFT_GREEN) }
            )

            // RIGHT: Blue (Quadrant 1)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(width = 36.dp, height = 60.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 30.dp, bottomEnd = 30.dp, bottomStart = 8.dp))
                    .background(
                        if (activeHighlight == COLOR_RIGHT_BLUE) Color(0xFF40C4FF) else Color(0xFF0D47A1)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { handleQuadrantClick(COLOR_RIGHT_BLUE) }
            )

            // Center Circular Info Hub
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2230)),
                contentAlignment = Alignment.Center
            ) {
                if (isGameOver) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isGameOver = false
                                currentScore = 0
                                roundNumber = 1
                                sequence.clear()
                                startNextRound()
                            }
                    ) {
                        Text(
                            text = "Score: $currentScore",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RETRY",
                            color = Color(0xFFFF5252),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "RND $roundNumber",
                            color = Color(0xFFFFD54F),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = statusText,
                            color = if (isShowingSequence) Color(0xFF80D8FF) else Color(0xFFB9F6CA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
