package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameType
import com.example.model.Player
import com.example.sound.GameAudioFeedback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    player1: Player,
    player2: Player,
    onUpdatePlayers: (Player, Player) -> Unit,
    onSelectGame: (GameType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditPlayersDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ігри на двох",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            GameAudioFeedback.playClick()
                            showRulesDialog = true
                        },
                        modifier = Modifier.testTag("rules_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Правила"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Players Bar Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            GameAudioFeedback.playClick()
                            showEditPlayersDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Player 1
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(player1.avatar, fontSize = 22.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Гравець 1", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(player1.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Text("⚔️", fontSize = 20.sp)

                        // Player 2
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Гравець 2", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(player2.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(player2.avatar, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Оберіть гру на одному телефоні:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            // The 4 Games Cards
            items(GameType.entries) { game ->
                GameCard(
                    game = game,
                    onClick = {
                        GameAudioFeedback.playClick()
                        onSelectGame(game)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showEditPlayersDialog) {
        EditPlayersDialog(
            initialPlayer1 = player1,
            initialPlayer2 = player2,
            onDismiss = { showEditPlayersDialog = false },
            onSave = { p1, p2 ->
                onUpdatePlayers(p1, p2)
                showEditPlayersDialog = false
            }
        )
    }

    if (showRulesDialog) {
        RulesDialog(onDismiss = { showRulesDialog = false })
    }
}

@Composable
private fun GameCard(
    game: GameType,
    onClick: () -> Unit
) {
    val (primaryAccent, containerColor) = when (game) {
        GameType.HIGHER_LOWER -> Color(0xFFEF4444) to Color(0xFFFEE2E2)
        GameType.GUESS_MIRACLE -> Color(0xFF8B5CF6) to Color(0xFFEDE9FE)
        GameType.GOOD_MEMORY -> Color(0xFFF59E0B) to Color(0xFFFEF3C7)
        GameType.BOOM_BOOM -> Color(0xFFEA580C) to Color(0xFFFFEDD5)
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("game_card_${game.name}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = containerColor,
                modifier = Modifier.size(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(game.iconEmoji, fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = game.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = game.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = primaryAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = game.shortDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = CircleShape,
                color = containerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Грати в ${game.title}",
                        tint = primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditPlayersDialog(
    initialPlayer1: Player,
    initialPlayer2: Player,
    onDismiss: () -> Unit,
    onSave: (Player, Player) -> Unit
) {
    var name1 by remember { mutableStateOf(initialPlayer1.name) }
    var avatar1 by remember { mutableStateOf(initialPlayer1.avatar) }

    var name2 by remember { mutableStateOf(initialPlayer2.name) }
    var avatar2 by remember { mutableStateOf(initialPlayer2.avatar) }

    val avatars = listOf("🦁", "🐯", "🐼", "🦊", "🚀", "⚡", "🎮", "👑")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Імена та аватари гравців", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Player 1
                Text("Гравець 1:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                OutlinedTextField(
                    value = name1,
                    onValueChange = { name1 = it },
                    label = { Text("Ім'я першого друга") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(avatars) { av ->
                        Surface(
                            onClick = { avatar1 = av },
                            shape = CircleShape,
                            color = if (avatar1 == av) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(av, fontSize = 20.sp) }
                        }
                    }
                }

                Divider()

                // Player 2
                Text("Гравець 2:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                OutlinedTextField(
                    value = name2,
                    onValueChange = { name2 = it },
                    label = { Text("Ім'я другого друга") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(avatars) { av ->
                        Surface(
                            onClick = { avatar2 = av },
                            shape = CircleShape,
                            color = if (avatar2 == av) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) { Text(av, fontSize = 20.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initialPlayer1.copy(name = name1.ifBlank { "Гравець 1" }, avatar = avatar1),
                        initialPlayer2.copy(name = name2.ifBlank { "Гравець 2" }, avatar = avatar2)
                    )
                }
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

@Composable
private fun RulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Правила 4 ігор на двох 📖", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(
                        text = "1. «Більше чи Менше» 🎯",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Один друг таємно загадує число (наприклад, 55). Інший відгадує, а система каже «Більше» чи «Менше». При точній відповіді лунає «Гра пройдена!»",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    Text(
                        text = "2. «Вгадай чудо» 🦄",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Один друг обирає людину чи тварину (наприклад, Поліцейський) та дає йому ім'я (наприклад, Степан). Інший має відгадати, хто це!",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    Text(
                        text = "3. «Гарна памʼять» ⚡",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Натисни кнопку, і на 0.5 секунди з'являться 3 випадкові цифри! Після цього потрібно написати саме ці цифри у правильному порядку.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                item {
                    Text(
                        text = "4. «Ой-ой бум бум» 💣",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Один друг мінує бомбу: обирає дії зі списку (Торкнутися цифри 5, Перерізати провод, Натискати багато раз на бомбу, Викинути бомбу). Сапер має 1 хвилину. За будь-яку помилку штраф -10 секунд! Якщо час вийде, бомба вибухає і другові кажуть: «Ну ти майстер!»",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Зрозуміло!")
            }
        }
    )
}
