package com.example.games.guessmiracle

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.sound.GameAudioFeedback

enum class MiracleStep {
    SETUP,
    HANDOVER,
    GUESSING,
    REVEALED
}

enum class MiracleCategory(val displayName: String, val emoji: String) {
    PERSON("Людина / Професія", "👤"),
    ANIMAL("Тварина", "🐾")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuessMiracleGameScreen(
    player1: Player,
    player2: Player,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var setterIndex by remember { mutableIntStateOf(0) }
    val currentSetter = if (setterIndex == 0) player1 else player2
    val currentGuesser = if (setterIndex == 0) player2 else player1

    var step by remember { mutableStateOf(MiracleStep.SETUP) }
    var selectedCategory by remember { mutableStateOf(MiracleCategory.PERSON) }
    var targetWord by remember { mutableStateOf("Поліцейський") }
    var givenNameOrClue by remember { mutableStateOf("") }

    var guessInput by remember { mutableStateOf("") }
    val guessHistory = remember { mutableStateListOf<String>() }
    var feedbackMsg by remember { mutableStateOf("") }

    val personPresets = listOf("Поліцейський", "Лікар", "Пожежник", "Вчитель", "Космонавт", "Кухар", "Детектив", "Футболіст", "Пілот", "Художник")
    val animalPresets = listOf("Кіт", "Собака", "Лев", "Ведмідь", "Слон", "Дельфін", "Орел", "Панда", "Тигр", "Вовк", "Хом'як")

    fun startNewRound(newSetterIndex: Int) {
        setterIndex = newSetterIndex
        targetWord = if (selectedCategory == MiracleCategory.PERSON) "Поліцейський" else "Кіт"
        givenNameOrClue = ""
        guessInput = ""
        guessHistory.clear()
        feedbackMsg = ""
        step = MiracleStep.SETUP
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Вгадай чудо",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "${currentSetter.name} загадує ➔ ${currentGuesser.name}",
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
                        startNewRound(setterIndex)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Почати знову")
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
                MiracleStep.SETUP -> {
                    MiracleSetupPhase(
                        setter = currentSetter,
                        selectedCategory = selectedCategory,
                        onSelectCategory = {
                            selectedCategory = it
                            targetWord = if (it == MiracleCategory.PERSON) "Поліцейський" else "Кіт"
                            GameAudioFeedback.playClick()
                        },
                        targetWord = targetWord,
                        onTargetWordChange = { targetWord = it },
                        presets = if (selectedCategory == MiracleCategory.PERSON) personPresets else animalPresets,
                        givenNameOrClue = givenNameOrClue,
                        onGivenNameChange = { givenNameOrClue = it },
                        onSubmit = {
                            if (targetWord.isNotBlank() && givenNameOrClue.isNotBlank()) {
                                GameAudioFeedback.playSuccess()
                                GameAudioFeedback.vibrate(context, 40)
                                step = MiracleStep.HANDOVER
                            }
                        }
                    )
                }

                MiracleStep.HANDOVER -> {
                    MiracleHandoverPhase(
                        guesser = currentGuesser,
                        category = selectedCategory,
                        givenName = givenNameOrClue,
                        onReady = {
                            GameAudioFeedback.playClick()
                            step = MiracleStep.GUESSING
                        }
                    )
                }

                MiracleStep.GUESSING -> {
                    MiracleGuessingPhase(
                        guesser = currentGuesser,
                        category = selectedCategory,
                        givenName = givenNameOrClue,
                        guessInput = guessInput,
                        onInputChange = { guessInput = it },
                        history = guessHistory,
                        feedbackMsg = feedbackMsg,
                        onSubmitGuess = {
                            val cleanGuess = guessInput.trim()
                            if (cleanGuess.isNotBlank()) {
                                val cleanTarget = targetWord.trim()
                                guessInput = ""
                                if (cleanGuess.equals(cleanTarget, ignoreCase = true) ||
                                    cleanTarget.lowercase().contains(cleanGuess.lowercase()) ||
                                    cleanGuess.lowercase().contains(cleanTarget.lowercase())
                                ) {
                                    GameAudioFeedback.playSuccess()
                                    GameAudioFeedback.vibratePattern(context, longArrayOf(0, 80, 50, 120))
                                    step = MiracleStep.REVEALED
                                } else {
                                    feedbackMsg = "«$cleanGuess» — не те! Спробуй ще!"
                                    guessHistory.add(0, cleanGuess)
                                    GameAudioFeedback.playTone(350.0, 80)
                                    GameAudioFeedback.vibrate(context, 40)
                                }
                            }
                        },
                        onVerbalWin = {
                            GameAudioFeedback.playSuccess()
                            GameAudioFeedback.vibratePattern(context, longArrayOf(0, 80, 50, 120))
                            step = MiracleStep.REVEALED
                        }
                    )
                }

                MiracleStep.REVEALED -> {
                    MiracleRevealedPhase(
                        guesser = currentGuesser,
                        setter = currentSetter,
                        category = selectedCategory,
                        targetWord = targetWord,
                        givenName = givenNameOrClue,
                        totalAttempts = guessHistory.size + 1,
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
private fun MiracleSetupPhase(
    setter: Player,
    selectedCategory: MiracleCategory,
    onSelectCategory: (MiracleCategory) -> Unit,
    targetWord: String,
    onTargetWordChange: (String) -> Unit,
    presets: List<String>,
    givenNameOrClue: String,
    onGivenNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(setter.avatar, fontSize = 32.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${setter.name}, загадай чудо!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Вибери людину чи тварину і дай їй ім'я, а друг має відгадати хто це!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        item {
            // Category selector tabs
            Text(
                text = "1. Обери категорію:",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiracleCategory.entries.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectCategory(cat) },
                        label = { Text("${cat.emoji} ${cat.displayName}") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Text(
                text = "2. Хто це саме (таємно):",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Presets
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presets) { preset ->
                    SuggestionChip(
                        onClick = { onTargetWordChange(preset) },
                        label = { Text(preset) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (targetWord == preset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = targetWord,
                onValueChange = onTargetWordChange,
                label = { Text("Кого загадуєш?") },
                placeholder = { Text("наприклад, Поліцейський") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("target_word_input")
            )
        }

        item {
            Text(
                text = "3. Придумай ім'я або підказку для друга:",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Це ім'я побачить твій друг. Наприклад: Степан, Барсик, Капітан...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = givenNameOrClue,
                onValueChange = onGivenNameChange,
                label = { Text("Ім'я людини чи тварини") },
                placeholder = { Text("наприклад, Степан") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("given_name_input")
            )
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSubmit,
                enabled = targetWord.isNotBlank() && givenNameOrClue.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("confirm_miracle_button")
            ) {
                Text("Загадати та передати другу!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MiracleHandoverPhase(
    guesser: Player,
    category: MiracleCategory,
    givenName: String,
    onReady: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(category.emoji, fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Передай телефон ${guesser.name}!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Тобі дали ім'я:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "«$givenName»",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Категорія: ${category.displayName}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onReady,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("ready_miracle_guesser_button")
        ) {
            Text("${guesser.name}, я готовий вгадувати!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MiracleGuessingPhase(
    guesser: Player,
    category: MiracleCategory,
    givenName: String,
    guessInput: String,
    onInputChange: (String) -> Unit,
    history: List<String>,
    feedbackMsg: String,
    onSubmitGuess: () -> Unit,
    onVerbalWin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Clue banner
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category.emoji, fontSize = 36.sp)
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Категорія: ${category.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ім'я: «$givenName»",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Feedback
        AnimatedVisibility(visible = feedbackMsg.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = feedbackMsg,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(12.dp)
                )
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
                placeholder = { Text("Хто це? (наприклад, Поліцейський)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSubmitGuess() }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("miracle_guess_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSubmitGuess,
                enabled = guessInput.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("submit_miracle_guess_button")
            ) {
                Text("Вгадати")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick verbal button
        OutlinedCard(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onVerbalWin() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🗣️", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Друг назвав правильну відповідь вголос?", fontSize = 13.sp)
                }
                Text("Вгадав! ✅", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Попередні спроби (${history.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Подумай, хто це може бути за ім'ям «$givenName»!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { guess ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(guess, fontWeight = FontWeight.Medium)
                            Text("❌ Не те", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiracleRevealedPhase(
    guesser: Player,
    setter: Player,
    category: MiracleCategory,
    targetWord: String,
    givenName: String,
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
        Text("✨", fontSize = 68.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Чудо відгадано!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(category.emoji, fontSize = 42.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = targetWord,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ім'я від ${setter.name}: «$givenName»",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${guesser.name} чудово впорався! Спроб: $totalAttempts",
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
                .testTag("swap_miracle_roles_button")
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
        ) {
            Text("Грати ще раз")
        }
    }
}
