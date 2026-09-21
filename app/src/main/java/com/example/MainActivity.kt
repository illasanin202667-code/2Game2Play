package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.games.boomboom.BoomBoomGameScreen
import com.example.games.goodmemory.GoodMemoryGameScreen
import com.example.games.guessmiracle.GuessMiracleGameScreen
import com.example.games.higherlower.HigherLowerGameScreen
import com.example.model.GameType
import com.example.model.Player
import com.example.ui.MainMenuScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TwoPlayerGameApp()
                }
            }
        }
    }
}

@Composable
fun TwoPlayerGameApp() {
    var player1 by remember { mutableStateOf(Player(id = 1, name = "Гравець 1", avatar = "🦁")) }
    var player2 by remember { mutableStateOf(Player(id = 2, name = "Гравець 2", avatar = "🐯")) }
    var currentGame by remember { mutableStateOf<GameType?>(null) }

    // Intercept back button when inside a mini-game
    BackHandler(enabled = currentGame != null) {
        currentGame = null
    }

    AnimatedContent(
        targetState = currentGame,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "game_transition"
    ) { activeGame ->
        when (activeGame) {
            null -> {
                MainMenuScreen(
                    player1 = player1,
                    player2 = player2,
                    onUpdatePlayers = { p1, p2 ->
                        player1 = p1
                        player2 = p2
                    },
                    onSelectGame = { selected ->
                        currentGame = selected
                    }
                )
            }
            GameType.HIGHER_LOWER -> {
                HigherLowerGameScreen(
                    player1 = player1,
                    player2 = player2,
                    onBackToMenu = { currentGame = null }
                )
            }
            GameType.GUESS_MIRACLE -> {
                GuessMiracleGameScreen(
                    player1 = player1,
                    player2 = player2,
                    onBackToMenu = { currentGame = null }
                )
            }
            GameType.GOOD_MEMORY -> {
                GoodMemoryGameScreen(
                    player1 = player1,
                    player2 = player2,
                    onBackToMenu = { currentGame = null }
                )
            }
            GameType.BOOM_BOOM -> {
                BoomBoomGameScreen(
                    player1 = player1,
                    player2 = player2,
                    onBackToMenu = { currentGame = null }
                )
            }
        }
    }
}
