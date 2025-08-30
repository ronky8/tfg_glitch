package com.pingu.tfg_glitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.Game
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.UserDataStore
import com.pingu.tfg_glitch.ui.screens.*
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.util.Log

class MainActivity : ComponentActivity() {
    private val userDataStore by lazy { UserDataStore(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GranjaGlitchAppTheme(selectedTheme = "TierraGlitch") {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(userDataStore)
                }
            }
        }
    }
}

// Instancia del servicio para la gestión de partidas.
private val gameService = GameService()

// Composable principal que maneja la navegación entre pantallas de alto nivel
@Composable
fun AppNavigation(userDataStore: UserDataStore) {
    val currentScreenState = remember { mutableStateOf("loading") }
    var currentScreen by currentScreenState

    val currentGameIdState = rememberSaveable { mutableStateOf<String?>(null) }
    var currentGameId by currentGameIdState

    val currentPlayerIdState = rememberSaveable { mutableStateOf<String?>(null) }
    var currentPlayerId by currentPlayerIdState

    var lastEventName by rememberSaveable { mutableStateOf<String?>(null) }
    var showEventDialog by rememberSaveable { mutableStateOf(false) }

    // NUEVO: Estado para el pop-up de inicio de turno y el ID de turno del que se mostró
    var showTurnStartDialog by rememberSaveable { mutableStateOf(false) }
    var lastTurnStartPlayerId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastRoundNumber by rememberSaveable { mutableStateOf(-1) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val session = userDataStore.readSession()
        if (session != null) {
            currentGameId = session.first
            currentPlayerId = session.second
            val game = gameService.getGame(currentGameId!!).first()
            if (game != null && !game.hasGameEnded) {
                currentScreen = "game"
            } else {
                userDataStore.clearSession()
                currentGameId = null
                currentPlayerId = null
                currentScreen = "mainMenu"
            }
        } else {
            currentScreen = "mainMenu"
        }
    }

    // Escucha los eventos globales para mostrar un pop-up
    val game by remember(currentGameId) {
        if (currentGameId != null) {
            gameService.getGame(currentGameId!!)
        } else {
            MutableStateFlow<Game?>(null)
        }
    }.collectAsState(initial = null)

    LaunchedEffect(game) {
        val currentGame = game
        // Lógica para mostrar pop-up de evento
        if (currentGame?.lastEvent?.name != null && currentGame.lastEvent?.name != lastEventName) {
            lastEventName = currentGame.lastEvent!!.name
            showEventDialog = true
        }
        // Lógica para mostrar pop-up de inicio de turno
        if (currentGame != null && currentGame.currentPlayerTurnId == currentPlayerId) {
            if (currentGame.roundNumber != lastRoundNumber) {
                lastRoundNumber = currentGame.roundNumber
                showTurnStartDialog = true
            }
        }
    }

    when (currentScreen) {
        "loading" -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text(text = "Cargando sesión...", modifier = Modifier.padding(top = 16.dp))
            }
        }
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
                Log.d("AppNavigation", "Game created: $gameId, Host Player: $hostPlayerId")
                currentGameId = gameId
                currentPlayerId = hostPlayerId
                currentScreen = "lobby"
                coroutineScope.launch {
                    userDataStore.saveSession(gameId, hostPlayerId)
                }
            },
            onBack = { currentScreen = "multiplayerMenu" }
        )
        "joinGame" -> JoinGameScreen(
            onGameJoined = { gameId, playerId ->
                Log.d("AppNavigation", "Joined game: $gameId, Current Player: $playerId")
                currentGameId = gameId
                currentPlayerId = playerId
                currentScreen = "lobby"
                coroutineScope.launch {
                    userDataStore.saveSession(gameId, playerId)
                }
            },
            onBack = { currentScreen = "multiplayerMenu" }
        )
        "lobby" -> {
            val gameId = currentGameId
            val playerId = currentPlayerId
            if (gameId != null && playerId != null) {
                LobbyScreen(
                    gameId = gameId,
                    currentPlayerId = playerId,
                    onGameStarted = {
                        currentScreen = "game"
                    },
                    onBack = {
                        coroutineScope.launch {
                            userDataStore.clearSession()
                            currentGameId = null
                            currentPlayerId = null
                            currentScreen = "mainMenu"
                        }
                    }
                )
            }
        }
        "game" -> {
            val gameId = currentGameId
            val playerId = currentPlayerId
            if (gameId != null && playerId != null) {
                GameScreen(
                    gameId = gameId,
                    currentPlayerId = playerId,
                    onGameEnded = {
                        Log.d("AppNavigation", "Game has ended. Navigating to FinalScoreScreen.")
                        currentScreen = "finalScore"
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    Log.w("AppNavigation", "gameId or playerId is null, returning to mainMenu.")
                    currentScreenState.value = "mainMenu"
                }
            }
        }
        "oneMobileMode" -> {
            OneMobileScreen(
                onBackToMainMenu = {
                    currentGameIdState.value = null
                    currentPlayerIdState.value = null
                    currentScreenState.value = "mainMenu"
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
                        coroutineScope.launch {
                            userDataStore.clearSession()
                            currentGameId = null
                            currentPlayerId = null
                            currentScreen = "mainMenu"
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    Log.w("AppNavigation", "gameId is null in finalScore, returning to mainMenu.")
                    currentScreenState.value = "mainMenu"
                }
            }
        }
    }

    if (showEventDialog && currentGameId != null) {
        val currentGame = game
        if (currentGame != null) {
            AlertDialog(
                onDismissRequest = { showEventDialog = false },
                title = { Text(text = currentGame.lastEvent?.name ?: "Evento Glitch") },
                text = { Text(text = currentGame.lastEvent?.description ?: "Un evento ha ocurrido.") },
                confirmButton = {
                    TextButton(onClick = { showEventDialog = false }) {
                        Text("Aceptar", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
    // NUEVO: Diálogo de inicio de turno, movido aquí para controlar su estado a nivel global
    if (showTurnStartDialog && game?.currentPlayerTurnId == currentPlayerId) {
        AlertDialog(
            onDismissRequest = { showTurnStartDialog = false },
            title = { Text(text = "¡Es tu turno!") },
            text = {
                Text(text = "Antes de tirar los dados, recuerda: \n\n1. Roba una carta del mazo principal.\n2. Coloca un marcador de crecimiento sobre cada uno de tus cultivos.\n3. Puedes plantar un cultivo normal de tu mano.")
            },
            confirmButton = {
                TextButton(onClick = { showTurnStartDialog = false }) {
                    Text("¡Entendido!")
                }
            }
        )
    }
}

// Composable que contiene las pantallas de juego con la barra de navegación inferior
@Composable
fun GameScreen(
    gameId: String,
    currentPlayerId: String,
    onGameEnded: () -> Unit
) {
    var selectedGameScreen by rememberSaveable { mutableStateOf("actions") }
    val game by gameService.getGame(gameId).collectAsState(initial = null)

    // Observa el estado de la partida para navegar a la pantalla final
    LaunchedEffect(game) {
        val gameData = game
        if (gameData != null) {
            if (gameData.hasGameEnded) {
                Log.d("GameScreen", "Game hasEnded detected. Calling onGameEnded.")
                onGameEnded()
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedGameScreen == "actions",
                    onClick = { selectedGameScreen = "actions" },
                    icon = { Icon(Icons.Filled.Casino, contentDescription = "Acciones") },
                    label = { Text("Acciones") }
                )
                NavigationBarItem(
                    selected = selectedGameScreen == "market",
                    onClick = { selectedGameScreen = "market" },
                    icon = { Icon(Icons.Filled.Storefront, contentDescription = "Mercado") },
                    label = { Text("Mercado") }
                )
                NavigationBarItem(
                    selected = selectedGameScreen == "objectives",
                    onClick = { selectedGameScreen = "objectives" },
                    icon = { Icon(Icons.Filled.WorkspacePremium, contentDescription = "Objetivos") },
                    label = { Text("Objetivos") }
                )
                NavigationBarItem(
                    selected = selectedGameScreen == "players",
                    onClick = { selectedGameScreen = "players" },
                    icon = { Icon(Icons.Filled.Groups, contentDescription = "Jugadores") },
                    label = { Text("Jugadores") }
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(targetState = selectedGameScreen, label = "GameScreenAnimation") { targetScreen ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (targetScreen) {
                    "actions" -> PlayerActionsScreen(gameId = gameId, currentPlayerId = currentPlayerId)
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
