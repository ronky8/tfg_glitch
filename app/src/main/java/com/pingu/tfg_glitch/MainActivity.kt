package com.pingu.tfg_glitch

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.pingu.tfg_glitch.data.FirestoreService
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.ui.screens.MarketScreen
import com.pingu.tfg_glitch.ui.screens.PlayerManagementScreen
import com.pingu.tfg_glitch.ui.screens.PlayerActionsScreen
import com.pingu.tfg_glitch.ui.screens.MainMenuScreen
import com.pingu.tfg_glitch.ui.screens.MultiplayerMenuScreen
import com.pingu.tfg_glitch.ui.screens.CreateGameScreen
import com.pingu.tfg_glitch.ui.screens.JoinGameScreen
import com.pingu.tfg_glitch.ui.screens.OneMobileScreen
import com.pingu.tfg_glitch.ui.screens.RulesScreen
import com.pingu.tfg_glitch.ui.screens.FinalScoreScreen
import com.pingu.tfg_glitch.ui.screens.ObjectivesScreen
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.GlitchRed
import com.pingu.tfg_glitch.ui.theme.DarkCard
import com.pingu.tfg_glitch.ui.theme.AccentPurple
import com.pingu.tfg_glitch.ui.theme.TextLight
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import android.util.Log
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GranjaGlitchAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

// Instancias de los servicios
private val gameService = GameService()
private val firestoreService = FirestoreService() // CORRECCIÓN: Instancia añadida

object SessionManager {
    private const val PREFS_NAME = "GranjaGlitchPrefs"
    private const val KEY_GAME_ID = "currentGameId"
    private const val KEY_PLAYER_ID = "currentPlayerId"

    fun saveSession(context: Context, gameId: String, playerId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_GAME_ID, gameId).putString(KEY_PLAYER_ID, playerId).apply()
        Log.d("SessionManager", "Session saved: gameId=$gameId, playerId=$playerId")
    }

    fun getSession(context: Context): Pair<String?, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gameId = prefs.getString(KEY_GAME_ID, null)
        val playerId = prefs.getString(KEY_PLAYER_ID, null)
        Log.d("SessionManager", "Session retrieved: gameId=$gameId, playerId=$playerId")
        return Pair(gameId, playerId)
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_GAME_ID).remove(KEY_PLAYER_ID).apply()
        Log.d("SessionManager", "Session cleared.")
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val (savedGameId, savedPlayerId) = remember { SessionManager.getSession(context) }

    var currentScreen by rememberSaveable { mutableStateOf(if (savedGameId != null && savedPlayerId != null) "game" else "mainMenu") }
    var currentGameId by rememberSaveable { mutableStateOf(savedGameId) }
    var currentPlayerId by rememberSaveable { mutableStateOf(savedPlayerId) }

    when (currentScreen) {
        "mainMenu" -> MainMenuScreen(
            onStartGame = { currentScreen = "multiplayerMenu" },
            onViewRules = { currentScreen = "rules" },
            onOneMobileMode = { currentScreen = "oneMobileMode" }
        )
        "multiplayerMenu" -> MultiplayerMenuScreen(
            onBack = { currentScreen = "mainMenu" },
            onStartMultiplayerGame = { currentScreen = "createGame" },
            onJoinMultiplayerGame = { currentScreen = "joinGame" }
        )
        "createGame" -> CreateGameScreen(
            onGameCreated = { gameId, hostPlayerId ->
                SessionManager.saveSession(context, gameId, hostPlayerId)
                currentGameId = gameId
                currentPlayerId = hostPlayerId
                currentScreen = "game"
            }
        )
        "joinGame" -> JoinGameScreen(
            onGameJoined = { gameId, playerId ->
                SessionManager.saveSession(context, gameId, playerId)
                currentGameId = gameId
                currentPlayerId = playerId
                currentScreen = "game"
            }
        )
        "game" -> {
            val gameId = currentGameId
            val playerId = currentPlayerId
            if (gameId != null && playerId != null) {
                GameScreen(
                    gameId = gameId,
                    currentPlayerId = playerId,
                    onGameEnded = { currentScreen = "finalScore" }
                )
            } else {
                LaunchedEffect(Unit) {
                    SessionManager.clearSession(context)
                    currentScreen = "mainMenu"
                }
            }
        }
        "oneMobileMode" -> {
            OneMobileScreen(
                onBackToMainMenu = {
                    currentGameId = null
                    currentPlayerId = null
                    currentScreen = "mainMenu"
                }
            )
        }
        "rules" -> {
            RulesScreen(onBack = { currentScreen = "mainMenu" })
        }
        "finalScore" -> {
            val gameId = currentGameId
            if (gameId != null) {
                FinalScoreScreen(
                    gameId = gameId,
                    onBackToMainMenu = {
                        SessionManager.clearSession(context)
                        currentGameId = null
                        currentPlayerId = null
                        currentScreen = "mainMenu"
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    SessionManager.clearSession(context)
                    currentScreen = "mainMenu"
                }
            }
        }
    }
}

@Composable
fun GameScreen(
    gameId: String,
    currentPlayerId: String,
    onGameEnded: () -> Unit
) {
    var selectedGameScreen by remember { mutableStateOf("actions") }
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val currentPlayer by firestoreService.getPlayer(currentPlayerId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isGameLoading by remember { mutableStateOf(true) }

    var turnPopupShownForTurnId by remember { mutableStateOf<String?>(null) }


    val isHost = remember(game, currentPlayerId) {
        game?.hostPlayerId == currentPlayerId
    }

    LaunchedEffect(game, currentPlayer) { // Escuchar cambios en currentPlayer también
        val gameData = game
        if (gameData != null) {
            if (isGameLoading) {
                isGameLoading = false
            }
            if (gameData.hasGameEnded) {
                onGameEnded()
            }
            // CORRECCIÓN SALTO DE MERCADO: Lógica para saltar el mercado si no hay inventario
            if (gameData.roundPhase == "MARKET_PHASE" &&
                currentPlayer?.inventario.isNullOrEmpty() && // CORRECCIÓN: Acceso a la propiedad
                !gameData.playersFinishedMarket.contains(currentPlayerId)) {

                coroutineScope.launch {
                    gameService.playerFinishedMarket(gameId, currentPlayerId)
                    snackbarHostState.showSnackbar(
                        message = "No tienes nada que vender. Turno de mercado saltado.",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
        else if (game == null && !isGameLoading) {
            onGameEnded()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedGameScreen == "actions",
                    onClick = { selectedGameScreen = "actions" },
                    icon = { Icon(Icons.Filled.Casino, contentDescription = "Acciones") },
                    label = { Text("Acciones") }
                )
                NavigationBarItem(
                    selected = selectedGameScreen == "market",
                    onClick = { selectedGameScreen = "market" },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Mercado") },
                    label = { Text("Mercado") }
                )
                NavigationBarItem(
                    selected = selectedGameScreen == "objectives",
                    onClick = { selectedGameScreen = "objectives" },
                    icon = { Icon(Icons.Filled.Star, contentDescription = "Objetivos") },
                    label = { Text("Objetivos") }
                )
                NavigationBarItem(
                    selected = selectedGameScreen == "players",
                    onClick = { selectedGameScreen = "players" },
                    icon = { Icon(Icons.Filled.Build, contentDescription = "Jugadores") },
                    label = { Text("Jugadores") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (isGameLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp), color = AccentPurple)
                    Text(text = "Cargando partida...", color = TextLight, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                when (selectedGameScreen) {
                    "actions" -> PlayerActionsScreen(
                        gameId = gameId,
                        currentPlayerId = currentPlayerId,
                        hasShownTurnPopup = turnPopupShownForTurnId == game?.currentPlayerTurnId,
                        onTurnPopupShown = { turnPopupShownForTurnId = game?.currentPlayerTurnId }
                    )
                    "market" -> MarketScreen(gameId = gameId, currentPlayerId = currentPlayerId)
                    "objectives" -> ObjectivesScreen(gameId = gameId, currentPlayerId = currentPlayerId)
                    "players" -> PlayerManagementScreen(
                        gameId = gameId,
                        currentPlayerId = currentPlayerId,
                        onGameEnded = onGameEnded
                    )
                }
            }
        }
    }
}
