package com.example.games.boomboom

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BombAction
import com.example.model.Player
import com.example.sound.GameAudioFeedback
import kotlinx.coroutines.delay

enum class BoomStep {
    SET_BOMB,
    HANDOVER,
    DEFUSING,
    DEFUSED_WIN,
    EXPLODED_LOSE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoomBoomGameScreen(
    player1: Player,
    player2: Player,
    onBackToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var setterIndex by remember { mutableIntStateOf(0) }
    val bombMaster = if (setterIndex == 0) player1 else player2
    val bombDefuser = if (setterIndex == 0) player2 else player1

    var step by remember { mutableStateOf(BoomStep.SET_BOMB) }

    // Selected actions chosen by the master
    val selectedActions = remember { mutableStateListOf<BombAction>() }

    // Actions completed by defuser
    val completedActions = remember { mutableStateListOf<BombAction>() }

    // Defuser progress state
    var timeLeftSeconds by remember { mutableIntStateOf(60) }
    var penaltyTriggered by remember { mutableStateOf(false) }
    var penaltyMessage by remember { mutableStateOf("") }

    // Defusal sub-states
    var wireCut by remember { mutableStateOf(false) }
    var bombTapsCount by remember { mutableIntStateOf(0) }
    var isBombThrown by remember { mutableStateOf(false) }
    var fiveTouched by remember { mutableStateOf(false) }

    fun startNewRound(newSetterIndex: Int) {
        setterIndex = newSetterIndex
        selectedActions.clear()
        // Default select 2 actions for convenience
        selectedActions.add(BombAction.TOUCH_FIVE)
        selectedActions.add(BombAction.CUT_WIRE)
        completedActions.clear()
        timeLeftSeconds = 60
        penaltyTriggered = false
        penaltyMessage = ""
        wireCut = false
        bombTapsCount = 0
        isBombThrown = false
        fiveTouched = false
        step = BoomStep.SET_BOMB
    }

    // Initialize default selection on first launch
    LaunchedEffect(Unit) {
        if (selectedActions.isEmpty()) {
            selectedActions.add(BombAction.TOUCH_FIVE)
            selectedActions.add(BombAction.CUT_WIRE)
        }
    }

    // Timer countdown effect in DEFUSING phase
    LaunchedEffect(step, timeLeftSeconds) {
        if (step == BoomStep.DEFUSING && timeLeftSeconds > 0) {
            delay(1000)
            if (step == BoomStep.DEFUSING) {
                timeLeftSeconds -= 1
                if (timeLeftSeconds <= 15) {
                    GameAudioFeedback.playTone(850.0, 30)
                } else {
                    GameAudioFeedback.playTick()
                }

                if (timeLeftSeconds <= 0) {
                    // Exploded!
                    GameAudioFeedback.playExplosion()
                    GameAudioFeedback.vibratePattern(context, longArrayOf(0, 200, 100, 400))
                    step = BoomStep.EXPLODED_LOSE
                }
            }
        }
    }

    fun applyPenalty(reason: String) {
        GameAudioFeedback.playPenalty()
        GameAudioFeedback.vibratePattern(context, longArrayOf(0, 100, 50, 100))
        penaltyMessage = "ПОМИЛКА: -10 СЕКУНД!\n$reason"
        penaltyTriggered = true
        timeLeftSeconds = (timeLeftSeconds - 10).coerceAtLeast(0)
        if (timeLeftSeconds <= 0) {
            GameAudioFeedback.playExplosion()
            step = BoomStep.EXPLODED_LOSE
        }
    }

    fun checkWinCondition() {
        if (completedActions.containsAll(selectedActions)) {
            GameAudioFeedback.playSuccess()
            GameAudioFeedback.vibratePattern(context, longArrayOf(0, 100, 50, 150))
            step = BoomStep.DEFUSED_WIN
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ой-ой бум бум",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Майстер: ${bombMaster.name} • Сапер: ${bombDefuser.name}",
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
                        Icon(Icons.Default.Refresh, contentDescription = "Перезапустити")
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
                    if (step == BoomStep.DEFUSING && timeLeftSeconds <= 15) {
                        Brush.verticalGradient(
                            listOf(Color(0xFF3F0B0B), Color(0xFF190505))
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        )
                    }
                )
        ) {
            when (step) {
                BoomStep.SET_BOMB -> {
                    SetBombPhase(
                        master = bombMaster,
                        defuser = bombDefuser,
                        selectedActions = selectedActions,
                        onToggleAction = { action ->
                            GameAudioFeedback.playClick()
                            if (selectedActions.contains(action)) {
                                if (selectedActions.size > 1) {
                                    selectedActions.remove(action)
                                }
                            } else {
                                selectedActions.add(action)
                            }
                        },
                        onArmBomb = {
                            if (selectedActions.isNotEmpty()) {
                                GameAudioFeedback.playSuccess()
                                GameAudioFeedback.vibrate(context, 40)
                                step = BoomStep.HANDOVER
                            }
                        }
                    )
                }

                BoomStep.HANDOVER -> {
                    HandoverDefuserPhase(
                        defuser = bombDefuser,
                        onStart = {
                            GameAudioFeedback.playClick()
                            timeLeftSeconds = 60
                            step = BoomStep.DEFUSING
                        }
                    )
                }

                BoomStep.DEFUSING -> {
                    DefusingPhase(
                        timeLeftSeconds = timeLeftSeconds,
                        defuser = bombDefuser,
                        completedCount = completedActions.size,
                        totalRequiredCount = selectedActions.size,
                        wireCut = wireCut,
                        fiveTouched = fiveTouched,
                        bombTapsCount = bombTapsCount,
                        isBombThrown = isBombThrown,
                        penaltyTriggered = penaltyTriggered,
                        penaltyMessage = penaltyMessage,
                        onDismissPenalty = { penaltyTriggered = false },
                        onTouchDigit = { digit ->
                            if (digit == 5) {
                                if (!fiveTouched) {
                                    fiveTouched = true
                                    if (selectedActions.contains(BombAction.TOUCH_FIVE)) {
                                        GameAudioFeedback.playSuccess()
                                        completedActions.add(BombAction.TOUCH_FIVE)
                                        checkWinCondition()
                                    } else {
                                        applyPenalty("Не треба було торкатися цифри 5!")
                                    }
                                }
                            } else {
                                applyPenalty("Не та цифра! Треба було бути обережнішим!")
                            }
                        },
                        onCutWire = {
                            if (!wireCut) {
                                wireCut = true
                                if (selectedActions.contains(BombAction.CUT_WIRE)) {
                                    GameAudioFeedback.playSuccess()
                                    completedActions.add(BombAction.CUT_WIRE)
                                    checkWinCondition()
                                } else {
                                    applyPenalty("Не треба було перерізати провід!")
                                }
                            }
                        },
                        onTapBomb = {
                            if (!completedActions.contains(BombAction.TAP_MANY_TIMES)) {
                                bombTapsCount++
                                GameAudioFeedback.playTone(500.0 + bombTapsCount * 30, 25)
                                if (bombTapsCount >= 10) {
                                    if (selectedActions.contains(BombAction.TAP_MANY_TIMES)) {
                                        GameAudioFeedback.playSuccess()
                                        completedActions.add(BombAction.TAP_MANY_TIMES)
                                        checkWinCondition()
                                    } else {
                                        applyPenalty("Не треба було стукати по бомбі!")
                                    }
                                }
                            }
                        },
                        onThrowBomb = {
                            if (!isBombThrown) {
                                isBombThrown = true
                                if (selectedActions.contains(BombAction.THROW_BOMB)) {
                                    GameAudioFeedback.playSuccess()
                                    completedActions.add(BombAction.THROW_BOMB)
                                    checkWinCondition()
                                } else {
                                    applyPenalty("Рано викидати бомбу!")
                                }
                            }
                        }
                    )
                }

                BoomStep.DEFUSED_WIN -> {
                    DefusedWinPhase(
                        defuser = bombDefuser,
                        master = bombMaster,
                        timeLeft = timeLeftSeconds,
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

                BoomStep.EXPLODED_LOSE -> {
                    ExplodedLosePhase(
                        master = bombMaster,
                        defuser = bombDefuser,
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
private fun SetBombPhase(
    master: Player,
    defuser: Player,
    selectedActions: List<BombAction>,
    onToggleAction: (BombAction) -> Unit,
    onArmBomb: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.size(68.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("💣", fontSize = 36.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${master.name}, замінуй бомбу!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Обери правильні дії, щоб поломити бомбу. ${defuser.name} матиме 1 хвилину. За будь-яку помилку: -10 секунд!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        item {
            Text(
                text = "Список дій для знешкодження:",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(BombAction.entries.size) { i ->
            val action = BombAction.entries[i]
            val isChecked = selectedActions.contains(action)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleAction(action) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(action.iconEmoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = action.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = action.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggleAction(action) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onArmBomb,
                enabled = selectedActions.isNotEmpty(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("arm_bomb_button")
            ) {
                Text("Активувати бомбу (60 сек)!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HandoverDefuserPhase(
    defuser: Player,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Передай телефон саперу!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${defuser.name}, бомба вже цокає! У тебе є рівно 1 хвилина, щоб знешкодити її. Якщо зробиш щось не так — втрачаєш 10 секунд!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onStart,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_defusing_button")
        ) {
            Text("${defuser.name}, почати знешкодження!", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DefusingPhase(
    timeLeftSeconds: Int,
    defuser: Player,
    completedCount: Int,
    totalRequiredCount: Int,
    wireCut: Boolean,
    fiveTouched: Boolean,
    bombTapsCount: Int,
    isBombThrown: Boolean,
    penaltyTriggered: Boolean,
    penaltyMessage: String,
    onDismissPenalty: () -> Unit,
    onTouchDigit: (Int) -> Unit,
    onCutWire: () -> Unit,
    onTapBomb: () -> Unit,
    onThrowBomb: () -> Unit
) {
    // Pulse animation for low time
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (timeLeftSeconds <= 15) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Digital Timer Header
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (timeLeftSeconds <= 15) Color(0xFF5A1010) else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulseScale)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val minutes = timeLeftSeconds / 60
                    val seconds = timeLeftSeconds % 60
                    val timeStr = String.format("%02d:%02d", minutes, seconds)

                    Text(
                        text = timeStr,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (timeLeftSeconds <= 15) Color(0xFFFF5252) else MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Сапер: ${defuser.name} • Виконано дій: $completedCount з $totalRequiredCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LinearProgressIndicator(
                        progress = { (timeLeftSeconds / 60f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = if (timeLeftSeconds <= 15) Color(0xFFFF5252) else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        // Penalty Banner
        item {
            AnimatedVisibility(visible = penaltyTriggered) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF3333)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismissPenalty() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💥", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = penaltyMessage,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Item 1: The Single Wire
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Провід живлення (єдиний)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (wireCut) "ПЕРЕРІЗАНО ✂️" else "ЦІЛИЙ ⚡",
                            fontWeight = FontWeight.SemiBold,
                            color = if (wireCut) MaterialTheme.colorScheme.primary else Color(0xFFEAB308)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Wire visual
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (wireCut) {
                                    Modifier.background(Color(0xFF4B5563))
                                } else {
                                    Modifier.background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFEF4444), Color(0xFFF59E0B))
                                        )
                                    )
                                }
                            )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onCutWire,
                        enabled = !wireCut,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cut_wire_button")
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (wireCut) "Провід вже перерізано" else "Перерізати провід")
                    }
                }
            }
        }

        // Item 2: Number Pad (Touch Digit 5)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Цифрова панель бомби",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = if (fiveTouched) "Кнопку '5' вже активовано" else "Обережно торкайся потрібних цифр",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1-9 Grid
                    val gridRows = listOf(
                        listOf(1, 2, 3),
                        listOf(4, 5, 6),
                        listOf(7, 8, 9)
                    )

                    gridRows.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            row.forEach { digit ->
                                val isFive = digit == 5
                                Surface(
                                    onClick = { onTouchDigit(digit) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isFive && fiveTouched -> MaterialTheme.colorScheme.primaryContainer
                                        isFive -> Color(0xFFFBBF24)
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    modifier = Modifier.size(52.dp, 44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "$digit",
                                            fontWeight = if (isFive) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (isFive && fiveTouched) MaterialTheme.colorScheme.onPrimaryContainer else Color.Unspecified
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Item 3: Tap Many Times on the Bomb
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Швидкі натискання на бомбу",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Натиснуто: $bombTapsCount / 10", fontWeight = FontWeight.Bold)
                        LinearProgressIndicator(
                            progress = { (bombTapsCount / 10f).coerceIn(0f, 1f) },
                            modifier = Modifier.width(160.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onTapBomb,
                        enabled = bombTapsCount < 10,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("tap_bomb_button")
                    ) {
                        Text("💣 Натискати на бомбу (${10 - bombTapsCount.coerceAtMost(10)} лишилось)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Item 4: Throw away the bomb
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Аварійний люк безпеки",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isBombThrown) "Бомбу викинуто у захисний люк!" else "Викинь бомбу, якщо це було в інструкції",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onThrowBomb,
                        enabled = !isBombThrown,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("throw_bomb_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isBombThrown) "Бомбу викинуто" else "Викинути бомбу")
                    }
                }
            }
        }
    }
}

@Composable
private fun DefusedWinPhase(
    defuser: Player,
    master: Player,
    timeLeft: Int,
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
        Text("🏆", fontSize = 68.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "БОМБУ ЗНЕШКОДЖЕНО!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "${defuser.name} розгадав усі пастки ${master.name} і знешкодив бомбу!\nЗалишок часу: $timeLeft сек.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSwapRoles,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("swap_boom_roles_button")
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Помінятися ролями (Тепер мінує ${defuser.name})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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

@Composable
private fun ExplodedLosePhase(
    master: Player,
    defuser: Player,
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
        Text("💥", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "ОЙ-ОЙ БУМ БУМ!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("👑", fontSize = 36.sp)
                Spacer(modifier = Modifier.height(6.dp))
                // Exactly as required by user prompt:
                // "а якщо пройде час, я програю і для друга скажуть «Ну ти майстер!»"
                Text(
                    text = "${master.name}: «Ну ти майстер!»",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${defuser.name} не встиг врятуватися від пасток!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSwapRoles,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("swap_boom_roles_button")
        ) {
            Icon(Icons.Default.SwapHoriz, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Помінятися ролями (Черга ${defuser.name})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onPlayAgain,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Спробувати знову")
        }
    }
}
