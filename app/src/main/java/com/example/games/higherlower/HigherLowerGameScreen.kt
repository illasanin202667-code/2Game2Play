package com.example.games.higherlower

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.sound.GameAudioFeedback
import kotlin.random.Random

enum class HigherLowerStep {
    SET_NUMBER,
    HANDOVER,
    GUESSING,
    WON
}

data class GuessEntry(
    val guess: Int,
    val result: String, // "Більше" or "Менше" or "Гра пройдена!"
    val isHigher: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HigherLowerGameScreen(
    player1: Player,
    player2: Player,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Which player is currently setting the number (0: player1, 1: player2)
    var setterIndex by remember { mutableIntStateOf(0) }
    val currentSetter = if (setterIndex == 0) player1 else player2
    val currentGuesser = if (setterIndex == 0) player2 else player1

    var step by remember { mutableStateOf(HigherLowerStep.SET_NUMBER) }
    var secretNumberInput by remember { mutableStateOf("") }
    var hideSecretNumber by remember { mutableStateOf(true) }
    var secretNumber by remember { mutableIntStateOf(55) }

    var currentGuessInput by remember { mutableStateOf("") }
    val guessHistory = remember { mutableStateListOf<GuessEntry>() }
    var lastFeedbackMessage by remember { mutableStateOf("") }
    var minRange by remember { mutableIntStateOf(1) }
    var maxRange by remember { mutableIntStateOf(100) }

    fun startNewRound(newSetterIndex: Int) {
        setterIndex = newSetterIndex
        secretNumberInput = ""
        currentGuessInput = ""
        guessHistory.clear()
        lastFeedbackMessage = ""
        minRange = 1
        maxRange = 100
        step = HigherLowerStep.SET_NUMBER
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Більше чи Менше",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "${currentSetter.name} vs ${currentGuesser.name}",
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
                    IconButton(
                        onClick = {
                            GameAudioFeedback.playClick()
                            startNewRound(setterIndex)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Почати спочатку"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
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
        ) {
            when (step) {
                HigherLowerStep.SET_NUMBER -> {
                    SetNumberPhase(
                        setter = currentSetter,
                        secretNumberInput = secretNumberInput,
                        onInputChange = { if (it.length <= 4) secretNumberInput = it.filter { c -> c.isDigit() } },
                        hideSecret = hideSecretNumber,
                        onToggleHide = { hideSecretNumber = !hideSecretNumber },
                        onRandomize = {
                            val r = Random.nextInt(1, 101)
                            secretNumberInput = r.toString()
                            GameAudioFeedback.playClick()
                        },
                        onSubmit = {
                            val num = secretNumberInput.toIntOrNull()
                            if (num != null && num in 1..1000) {
                                secretNumber = num
                                GameAudioFeedback.playSuccess()
                                GameAudioFeedback.vibrate(context, 40)
                                step = HigherLowerStep.HANDOVER
                            }
                        }
                    )
                }

                HigherLowerStep.HANDOVER -> {
                    HandoverPhase(
                        guesser = currentGuesser,
                        onReady = {
                            GameAudioFeedback.playClick()
                            step = HigherLowerStep.GUESSING
                        }
                    )
                }

                HigherLowerStep.GUESSING -> {
                    GuessingPhase(
                        guesser = currentGuesser,
                        guessInput = currentGuessInput,
                        onInputChange = { if (it.length <= 4) currentGuessInput = it.filter { c -> c.isDigit() } },
                        history = guessHistory,
                        feedback = lastFeedbackMessage,
                        minRange = minRange,
                        maxRange = maxRange,
                        onSubmitGuess = {
                            val g = currentGuessInput.toIntOrNull()
                            if (g != null) {
                                currentGuessInput = ""
                                if (g < secretNumber) {
                                    lastFeedbackMessage = "Більше! Спробуй більше число!"
                                    if (g >= minRange) minRange = g + 1
                                    guessHistory.add(0, GuessEntry(g, "Більше", true))
                                    GameAudioFeedback.playTone(700.0, 70)
                                    GameAudioFeedback.vibrate(context, 40)
                                } else if (g > secretNumber) {
                                    lastFeedbackMessage = "Менше! Спробуй менше число!"
                                    if (g <= maxRange) maxRange = g - 1
                                    guessHistory.add(0, GuessEntry(g, "Менше", false))
                                    GameAudioFeedback.playTone(400.0, 70)
                                    GameAudioFeedback.vibrate(context, 40)
                                } else {
                                    // Exactly guessed!
                                    lastFeedbackMessage = "Гра пройдена!"
                                    guessHistory.add(0, GuessEntry(g, "Гра пройдена!", true))
                                    GameAudioFeedback.playSuccess()
                                    GameAudioFeedback.vibratePattern(context, longArrayOf(0, 80, 50, 150))
                                    step = HigherLowerStep.WON
                                }
                            }
                        }
                    )
                }

                HigherLowerStep.WON -> {
                    WonPhase(
                        guesser = currentGuesser,
                        setter = currentSetter,
                        secretNumber = secretNumber,
                        totalAttempts = guessHistory.size,
                        onSwapRoles = {
                            GameAudioFeedback.playClick()
                            startNewRound(if (setterIndex == 0) 1 else 0)
                        },
                        onPlayAgain = {
                            GameAudioFeedback.playClick()
                            startNewRound(setterIndex)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SetNumberPhase(
    setter: Player,
    secretNumberInput: String,
    onInputChange: (String) -> Unit,
    hideSecret: Boolean,
    onToggleHide: () -> Unit,
    onRandomize: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(setter.avatar, fontSize = 36.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${setter.name}, загадай число!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Сховай екран від друга та введи число (від 1 до 100)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = secretNumberInput,
            onValueChange = onInputChange,
            label = { Text("Таємне число") },
            placeholder = { Text("наприклад, 55") },
            singleLine = true,
            visualTransformation = if (hideSecret) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = {
                IconButton(onClick = onToggleHide) {
                    Icon(
                        imageVector = if (hideSecret) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (hideSecret) "Показати число" else "Сховати число"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("secret_number_input"),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onRandomize) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Випадкове число")
            }

            val isValid = (secretNumberInput.toIntOrNull() ?: 0) in 1..1000
            Button(
                onClick = onSubmit,
                enabled = isValid,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("confirm_secret_button")
            ) {
                Text("Загадати!")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}

@Composable
private fun HandoverPhase(
    guesser: Player,
    onReady: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📱", fontSize = 64.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Передай телефон!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Тепер черга ${guesser.name} вгадувати за підказками системи.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onReady,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("ready_guesser_button")
        ) {
            Text("${guesser.name}, я готовий!", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun GuessingPhase(
    guesser: Player,
    guessInput: String,
    onInputChange: (String) -> Unit,
    history: List<GuessEntry>,
    feedback: String,
    minRange: Int,
    maxRange: Int,
    onSubmitGuess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Range & status bar
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(guesser.avatar, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = guesser.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Діапазон: $minRange ... $maxRange",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Спроб: ${history.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Feedback Banner
        AnimatedVisibility(
            visible = feedback.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val isHigher = feedback.contains("Більше", ignoreCase = true)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isHigher) Color(0xFFE0F2FE) else Color(0xFFFEF3C7)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHigher) "⬆️" else "⬇️",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feedback,
                        fontWeight = FontWeight.Bold,
                        color = if (isHigher) Color(0xFF0369A1) else Color(0xFFB45309),
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = guessInput,
                onValueChange = onInputChange,
                placeholder = { Text("Введи число...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                keyboardActions = KeyboardActions(onDone = { onSubmitGuess() }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("guess_number_input")
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onSubmitGuess,
                enabled = guessInput.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("submit_guess_button")
            ) {
                Text("Вгадати")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guesses history
        Text(
            text = "Історія спроб",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(vertical = 4.dp)
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Зроби першу спробу!\nСистема підкаже, більше чи менше загадане число.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(history) { index, entry ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${history.size - index}",
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "${entry.guess}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (entry.isHigher) "⬆️ Треба більше" else "⬇️ Треба менше",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (entry.isHigher) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WonPhase(
    guesser: Player,
    setter: Player,
    secretNumber: Int,
    totalAttempts: Int,
    onSwapRoles: () -> Unit,
    onPlayAgain: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", fontSize = 68.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Гра пройдена!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${guesser.name} відгадав число $secretNumber!",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Знадобилося спроб: $totalAttempts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSwapRoles,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("swap_roles_button")
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Помінятися ролями", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onPlayAgain,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("play_again_button")
        ) {
            Text("Грати ще раз тим же складом")
        }
    }
}
