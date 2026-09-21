package com.example.games.goodmemory

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.sound.GameAudioFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class MemoryState {
    READY,
    COUNTDOWN,
    FLASHING,
    INPUT,
    RESULT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodMemoryGameScreen(
    player1: Player,
    player2: Player,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activePlayerIndex by remember { mutableIntStateOf(0) }
    var score1 by remember { mutableIntStateOf(0) }
    var score2 by remember { mutableIntStateOf(0) }
    var roundNum by remember { mutableIntStateOf(1) }

    val currentPlayer = if (activePlayerIndex == 0) player1 else player2

    var state by remember { mutableStateOf(MemoryState.READY) }
    var secretDigits by remember { mutableStateOf(listOf(0, 0, 0)) }
    var enteredDigits by remember { mutableStateOf(listOf<Int>()) }
    var isCorrect by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableIntStateOf(3) }

    fun startFlashSequence() {
        coroutineScope.launch {
            // Generate 3 random digits (0-9)
            val d1 = Random.nextInt(0, 10)
            val d2 = Random.nextInt(0, 10)
            val d3 = Random.nextInt(0, 10)
            secretDigits = listOf(d1, d2, d3)
            enteredDigits = emptyList()

            // Countdown
            state = MemoryState.COUNTDOWN
            countdownValue = 3
            GameAudioFeedback.playTick()
            delay(500)
            countdownValue = 2
            GameAudioFeedback.playTick()
            delay(500)
            countdownValue = 1
            GameAudioFeedback.playTick()
            delay(500)

            // Flash digits for EXACTLY 500 ms (0.5 seconds) as requested by the user
            state = MemoryState.FLASHING
            GameAudioFeedback.playFlash()
            GameAudioFeedback.vibrate(context, 40)
            delay(500) // Exactly 0.5s!

            // Transition to input
            state = MemoryState.INPUT
        }
    }

    fun submitGuess() {
        if (enteredDigits.size == 3) {
            val win = enteredDigits == secretDigits
            isCorrect = win
            if (win) {
                if (activePlayerIndex == 0) score1++ else score2++
                GameAudioFeedback.playSuccess()
                GameAudioFeedback.vibratePattern(context, longArrayOf(0, 60, 40, 100))
            } else {
                GameAudioFeedback.playPenalty()
                GameAudioFeedback.vibrate(context, 100)
            }
            state = MemoryState.RESULT
        }
    }

    fun nextTurn() {
        activePlayerIndex = if (activePlayerIndex == 0) 1 else 0
        roundNum++
        enteredDigits = emptyList()
        state = MemoryState.READY
    }

    fun resetGame() {
        score1 = 0
        score2 = 0
        roundNum = 1
        activePlayerIndex = 0
        enteredDigits = emptyList()
        state = MemoryState.READY
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Гарна памʼять",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "0.5 сек на 3 цифри • Раунд $roundNum",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            GameAudioFeedback.playClick()
                            onBackToMenu()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад до меню"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        GameAudioFeedback.playClick()
                        resetGame()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Скинути рахунок")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Duel Scoreboard
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player 1
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (activePlayerIndex == 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent
                            )
                            .padding(8.dp)
                    ) {
                        Text("${player1.avatar} ${player1.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$score1", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }

                    Text("VS", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.outline)

                    // Player 2
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (activePlayerIndex == 1) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent
                            )
                            .padding(8.dp)
                    ) {
                        Text("${player2.avatar} ${player2.name}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("$score2", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main interaction area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    MemoryState.READY -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(currentPlayer.avatar, fontSize = 32.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Черга: ${currentPlayer.name}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Після натискання кнопки на 0.5 секунди\nбудуть показані 3 цифри. Запам'ятай їх!",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(28.dp))
                            Button(
                                onClick = {
                                    GameAudioFeedback.playClick()
                                    startFlashSequence()
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(56.dp)
                                    .testTag("start_memory_flash_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Почати (0.5 сек)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    MemoryState.COUNTDOWN -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Увага...", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "$countdownValue",
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    MemoryState.FLASHING -> {
                        // Flash 3 digits prominently
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            secretDigits.forEach { digit ->
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(88.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = "$digit",
                                            fontSize = 52.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    MemoryState.INPUT -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${currentPlayer.name}, які 3 цифри були?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // 3 slots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0..2) {
                                    val digit = enteredDigits.getOrNull(i)
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (digit != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier
                                            .size(72.dp)
                                            .border(
                                                width = 2.dp,
                                                color = if (enteredDigits.size == i) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = digit?.toString() ?: "?",
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (digit != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // On-screen Keypad (0-9)
                            Keypad(
                                onDigitClick = { d ->
                                    if (enteredDigits.size < 3) {
                                        enteredDigits = enteredDigits + d
                                        GameAudioFeedback.playClick()
                                    }
                                },
                                onBackspace = {
                                    if (enteredDigits.isNotEmpty()) {
                                        enteredDigits = enteredDigits.dropLast(1)
                                        GameAudioFeedback.playClick()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { submitGuess() },
                                enabled = enteredDigits.size == 3,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(52.dp)
                                    .testTag("submit_memory_button")
                            ) {
                                Text("Перевірити відповідь", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    MemoryState.RESULT -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(if (isCorrect) "🎉" else "❌", fontSize = 64.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isCorrect) "Гарна памʼять! Точно!" else "Ой, не так!",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ),
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Було показано:", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = secretDigits.joinToString("   "),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Ти ввів:", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        text = enteredDigits.joinToString("   "),
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    GameAudioFeedback.playClick()
                                    nextTurn()
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(54.dp)
                                    .testTag("next_player_memory_button")
                            ) {
                                val nextPlayer = if (activePlayerIndex == 0) player2 else player1
                                Text("Наступний хід: ${nextPlayer.name}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Keypad(
    onDigitClick: (Int) -> Unit,
    onBackspace: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val rows = listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { d ->
                    KeypadButton(text = "$d", onClick = { onDigitClick(d) })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Empty placeholder to balance
            Spacer(modifier = Modifier.size(68.dp, 48.dp))
            KeypadButton(text = "0", onClick = { onDigitClick(0) })
            Surface(
                onClick = onBackspace,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(68.dp, 48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Backspace, contentDescription = "Видалити", modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.size(68.dp, 48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
