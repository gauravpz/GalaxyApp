package com.gaurav.galaxyapp.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import com.gaurav.galaxyapp.data.GameConfig
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Game 3: "Fast Reaction Tapper" (Whack-a-Mole style)
 * - Single target indicator jumping to random X/Y within safe padded circle bounding area.
 * - Kotlin 'while(isPlaying)' coroutine loop triggering position change every X ms.
 * - Player gains 10 points when tapping the target before it teleports away.
 * - Outer Box 16.dp padding for Galaxy Watch 8 round display isolation.
 */
@Composable
fun ReactionTapperGame(
    config: GameConfig,
    highScore: Int,
    onScoreUpdated: (Int) -> Unit,
    onBack: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var score by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var timeLeftSeconds by remember { mutableIntStateOf(30) }

    // Coordinates relative to center, strictly bounded inside circular area
    var targetOffsetX by remember { mutableStateOf(0.dp) }
    var targetOffsetY by remember { mutableStateOf(0.dp) }
    var teleportTrigger by remember { mutableIntStateOf(0) }

    val scaleAnim = remember { Animatable(1f) }

    fun generateNewCoordinate() {
        // Safe radius inside circular watch face: 50.dp max displacement from center
        val maxRadiusDp = 50.0
        val angle = Random.nextDouble(0.0, 2.0 * Math.PI)
        val distance = Random.nextDouble(10.0, maxRadiusDp)
        targetOffsetX = (distance * cos(angle)).toInt().dp
        targetOffsetY = (distance * sin(angle)).toInt().dp
    }

    // 30-Second countdown loop
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            timeLeftSeconds = 30
            while (timeLeftSeconds > 0 && isPlaying) {
                delay(1000L)
                timeLeftSeconds--
            }
            isPlaying = false
            onScoreUpdated(score)
        }
    }

    // Kotlin while(isPlaying) flow loop triggering target jump every X milliseconds
    LaunchedEffect(isPlaying, teleportTrigger) {
        if (isPlaying) {
            generateNewCoordinate()
            // Reset pop scale animation
            scaleAnim.snapTo(0.3f)
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )

            // Wait for interval from config (e.g. 1200ms easy, 900ms med, 700ms hard)
            delay(config.reactionIntervalMs.coerceAtLeast(500L))
            // Teleport away if not tapped
            misses++
            teleportTrigger++
        }
    }

    fun handleTargetTap() {
        if (!isPlaying) return
        score += 10
        hits++
        onScoreUpdated(score)
        // Instant teleport upon hit
        teleportTrigger++
    }

    // Outer Box with 16.dp padding ensuring round watch integrity
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Top status and back button
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

        // Circular gameplay area & target indicator
        if (isPlaying) {
            // Target Indicator (jumps inside localized safe padded circle bounding area)
            Box(
                modifier = Modifier
                    .offset(x = targetOffsetX, y = targetOffsetY)
                    .size(48.dp)
                    .scale(scaleAnim.value)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFF1744), Color(0xFFFF5252), Color(0xFFB71C1C))
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { handleTargetTap() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "★",
                    color = Color.Yellow,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )
            }
        } else {
            // Game Over / Summary
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "TIME'S UP!",
                    color = Color(0xFFFF5252),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Score: $score",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Hits: $hits • Misses: $misses",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2962FF))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            score = 0
                            hits = 0
                            misses = 0
                            isPlaying = true
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Play Again",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = " PLAY AGAIN",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Bottom HUD: Time & Current Score
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "⏱ ${timeLeftSeconds}s",
                    color = if (timeLeftSeconds <= 5) Color(0xFFFF5252) else Color(0xFF80D8FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "PTS: $score",
                    color = Color(0xFFFFD54F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
