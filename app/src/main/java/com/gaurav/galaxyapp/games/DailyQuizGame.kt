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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import com.gaurav.galaxyapp.data.GameConfig
import com.gaurav.galaxyapp.data.QuizQuestion

@Composable
fun DailyQuizGame(
    config: GameConfig,
    highScore: Int,
    onScoreUpdated: (Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedSubject by remember { mutableStateOf<String?>(null) }

    if (selectedSubject == null) {
        SubjectSelectionScreen(
            config = config,
            onSelectSubject = { selectedSubject = it },
            onBack = onBack
        )
    } else {
        SubjectQuizScreen(
            subject = selectedSubject!!,
            config = config,
            highScore = highScore,
            onScoreUpdated = onScoreUpdated,
            onBack = { selectedSubject = null }
        )
    }
}

@Composable
private fun SubjectSelectionScreen(
    config: GameConfig,
    onSelectSubject: (String) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val availableSubjects = listOf(
        "math" to "🔢 Math",
        "english" to "🔤 English",
        "hindi" to "🇮🇳 Hindi (हिंदी)",
        "marathi" to "🚩 Marathi (मराठी)",
        "all" to "🎲 All Subjects"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F1D))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onBack() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFAAAAAA),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = " EXIT",
                        color = Color(0xFFAAAAAA),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                Text(
                    text = "3RD STD QUIZ",
                    color = Color(0xFFFFD54F),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }

            items(availableSubjects.size) { index ->
                val (key, label) = availableSubjects[index]
                val qCount = if (key == "all") {
                    config.questions.values.sumOf { it.size }
                } else {
                    config.questions[key]?.size ?: 0
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectSubject(key) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (qCount > 0) "$qCount Qs" else "10 Qs",
                            color = Color(0xFF38BDF8),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectQuizScreen(
    subject: String,
    config: GameConfig,
    highScore: Int,
    onScoreUpdated: (Int) -> Unit,
    onBack: () -> Unit
) {
    val questionsList = remember(subject, config) {
        val list = if (subject == "all") {
            config.questions.values.flatten().shuffled()
        } else {
            config.questions[subject] ?: emptyList()
        }
        if (list.isNotEmpty()) list else getDefaultFallbackQuestions(subject)
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var feedbackColor by remember { mutableStateOf<Color?>(null) }
    var showExplanation by remember { mutableStateOf(false) }

    val currentQ = questionsList.getOrNull(currentIndex % questionsList.size) ?: getDefaultFallbackQuestions(subject).first()

    val bgColor by animateColorAsState(
        targetValue = feedbackColor ?: Color(0xFF0F141C),
        animationSpec = tween(durationMillis = 200),
        label = "quiz_feedback"
    )

    LaunchedEffect(feedbackColor) {
        if (feedbackColor != null) {
            kotlinx.coroutines.delay(1100)
            feedbackColor = null
            showExplanation = false
            currentIndex++
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Back button at top
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onBack() }
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = " BACK",
                    color = Color(0xFFAAAAAA),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quiz Column
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Score and Question number
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${subject.uppercase()} • ${(currentIndex % questionsList.size) + 1}/${questionsList.size}",
                    color = Color(0xFF4DD0E1),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = " • ", color = Color.Gray, fontSize = 11.sp)
                Text(
                    text = "PTS: $score",
                    color = Color(0xFFFFD54F),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Question Card
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E2838))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentQ.question,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }

            if (showExplanation && currentQ.explanation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentQ.explanation,
                    color = Color(0xFFA7F3D0),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Choices
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                currentQ.options.take(3).forEach { option ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2563EB))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (feedbackColor == null) {
                                    val isCorrect = option.trim().equals(currentQ.answer.trim(), ignoreCase = true)
                                    if (isCorrect) {
                                        score += 10
                                        onScoreUpdated(score)
                                        feedbackColor = Color(0xFF1B5E20)
                                    } else {
                                        feedbackColor = Color(0xFFB71C1C)
                                        if (score >= 5) score -= 5
                                    }
                                    showExplanation = true
                                }
                            }
                            .padding(vertical = 6.dp, horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun getDefaultFallbackQuestions(subject: String): List<QuizQuestion> {
    return when (subject) {
        "english" -> listOf(
            QuizQuestion("e1", "Opposite of 'Huge'?", listOf("Tiny", "Large", "Heavy"), "Tiny", "Tiny means very small"),
            QuizQuestion("e2", "Plural of 'Child'?", listOf("Children", "Childs", "Childrens"), "Children", "Child -> Children"),
            QuizQuestion("e3", "Synonym of 'Glad'?", listOf("Happy", "Sad", "Angry"), "Happy", "Glad means happy")
        )
        "hindi" -> listOf(
            QuizQuestion("h1", "'मित्र' का विलोम शब्द?", listOf("शत्रु", "दोस्त", "सखा"), "शत्रु", "मित्र का उल्टा शत्रु"),
            QuizQuestion("h2", "'जल' का समानार्थी?", listOf("पानी", "आग", "हवा"), "पानी", "जल मतलब पानी"),
            QuizQuestion("h3", "'किताब' का बहुवचन?", listOf("किताबें", "किताबों", "किताबी"), "किताबें", "एक किताब, अनेक किताबें")
        )
        "marathi" -> listOf(
            QuizQuestion("mr1", "'लहान' चा विरुद्धार्थी शब्द?", listOf("मोठा", "उंच", "जाड"), "मोठा", "लहान चा विरुद्ध मोठा"),
            QuizQuestion("mr2", "'पाणी' चा समानार्थी शब्द?", listOf("जल", "हवा", "अग्नी"), "जल", "पाणी म्हणजे जल"),
            QuizQuestion("mr3", "'दिवस' चा विरुद्धार्थी शब्द?", listOf("रात्र", "सकाळ", "दुपार"), "रात्र", "दिवस चा विरुद्ध रात्र")
        )
        else -> listOf(
            QuizQuestion("m1", "7 × 8 = ?", listOf("54", "56", "64"), "56", "7 * 8 = 56"),
            QuizQuestion("m2", "35 + 27 = ?", listOf("52", "62", "72"), "62", "35 + 27 = 62"),
            QuizQuestion("m3", "36 ÷ 4 = ?", listOf("8", "9", "7"), "9", "36 / 4 = 9")
        )
    }
}
