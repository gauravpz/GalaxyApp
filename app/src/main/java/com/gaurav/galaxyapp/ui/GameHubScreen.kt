package com.gaurav.galaxyapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import com.gaurav.galaxyapp.data.GameConfig

enum class ActiveScreen {
    HUB,
    QUIZ,
    MATH_DASH,
    COLOR_MEMO,
    REACTION_TAPPER
}

@Composable
fun GameHubScreen(
    config: GameConfig,
    playtimeSeconds: Long,
    quizHighScore: Int,
    mathHighScore: Int,
    colorMemoHighScore: Int,
    reactionHighScore: Int,
    onNavigate: (ActiveScreen) -> Unit,
    onToggleDifficulty: () -> Unit,
    onSyncConfig: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val playedMinutes = playtimeSeconds / 60
    val maxMinutes = config.monitoring.maxDailyPlayTimeMinutes
    val isLimitReached = config.monitoring.enabled && playedMinutes >= maxMinutes

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 3.dp)
                ) {
                    Text(
                        text = "★ GALAXY APP ★",
                        color = Color(0xFFFFD54F),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))

                    // Difficulty & Playtime Pill Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onToggleDifficulty() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = config.difficulty.uppercase(),
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (config.monitoring.enabled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isLimitReached) Color(0xFF7F1D1D) else Color(0xFF14532D))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "⏱ $playedMinutes/${maxMinutes}m",
                                    color = if (isLimitReached) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Primary Card: 3rd Std Quiz (Math, English, Hindi, Marathi)
            item {
                GameLauncherCard(
                    title = "★ 3rd Std Quiz",
                    subtitle = "Math • Eng • Hindi • Marathi",
                    gradient = listOf(Color(0xFF4C1D95), Color(0xFF7C3AED)),
                    iconColor = Color(0xFFA78BFA),
                    onClick = { onNavigate(ActiveScreen.QUIZ) }
                )
            }

            // Game 1: Math Dash Card
            item {
                GameLauncherCard(
                    title = "1. Math Dash",
                    subtitle = "Fast equations • High: $mathHighScore",
                    gradient = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB)),
                    iconColor = Color(0xFF60A5FA),
                    onClick = { onNavigate(ActiveScreen.MATH_DASH) }
                )
            }

            // Game 2: Color Memo Card
            item {
                GameLauncherCard(
                    title = "2. Color Memo",
                    subtitle = "Simon Says • High: $colorMemoHighScore",
                    gradient = listOf(Color(0xFF065F46), Color(0xFF059669)),
                    iconColor = Color(0xFF34D399),
                    onClick = { onNavigate(ActiveScreen.COLOR_MEMO) }
                )
            }

            // Game 3: Fast Reaction Tapper Card
            item {
                GameLauncherCard(
                    title = "3. Fast Tapper",
                    subtitle = "Whack-a-Mole • High: $reactionHighScore",
                    gradient = listOf(Color(0xFF7F1D1D), Color(0xFFDC2626)),
                    iconColor = Color(0xFFF87171),
                    onClick = { onNavigate(ActiveScreen.REACTION_TAPPER) }
                )
            }

            // Sync Remote Config Footer Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2430))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSyncConfig() }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = Color(0xFFA0AEC0),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Sync Remote Config",
                            color = Color(0xFFA0AEC0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameLauncherCard(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
