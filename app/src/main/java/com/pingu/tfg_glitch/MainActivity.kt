package com.pingu.tfg_glitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pingu.tfg_glitch.data.Game
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.SoundManager
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
        // Inicializamos el SoundManager una sola vez
        SoundManager.initialize(this)
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

    override fun onDestroy() {
        super.onDestroy()
        // Liberamos los recursos del SoundManager cuando la app se destruye
        SoundManager.release()
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

    var showTurnStartDialog by rememberSaveable { mutableStateOf(false) }
    var lastTurnStartPlayerId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastRoundNumber by rememberSaveable { mutableStateOf(-1) }

    var showKickedDialog by remember { mutableStateOf(false) }
    var showExitOneMobileDialog by remember { mutableStateOf(false) } // Nuevo estado para el diálogo de salida

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- MANEJO DEL BOTÓN DE RETROCESO ---
    BackHandler(enabled = currentScreen != "mainMenu" && currentScreen != "loading") {
        when (currentScreen) {
            "multiplayerMenu", "rules" -> currentScreen = "mainMenu"
            "oneMobileMode" -> showExitOneMobileDialog = true // Mostrar diálogo en lugar de salir
            "createGame", "joinGame" -> currentScreen = "multiplayerMenu"
            "lobby", "finalScore" -> {
                coroutineScope.launch {
                    userDataStore.clearSession()
                    currentGameId = null
                    currentPlayerId = null
                    currentScreen = "mainMenu"
                }
            }
            "game" -> { } // Deshabilitado para no salir de la partida por error
        }
    }
    // --- FIN MANEJO DEL BOTÓN DE RETROCESO ---

    // --- LÓGICA DE AUDIO ---
    var currentMusicType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentScreen) {
        val newMusicType = when (currentScreen) {
            "game", "oneMobileMode", "finalScore" -> "game"
            "loading" -> null // No cambiar la música mientras carga
            else -> "menu"
        }

        if (newMusicType != null && newMusicType != currentMusicType) {
            currentMusicType = newMusicType
            if (newMusicType == "game") {
                SoundManager.playGameMusic(context)
            } else {
                SoundManager.playMenuMusic(context)
            }
        }
    }

    // Gestionar la pausa/reanudación de la música con el ciclo de vida de la app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> SoundManager.pauseMusic()
                Lifecycle.Event.ON_RESUME -> SoundManager.resumeMusic()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // --- FIN LÓGICA DE AUDIO ---

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
        if (currentGame?.isStarted == true && currentGame.lastEvent?.name != null && currentGame.lastEvent?.name != lastEventName) {
            lastEventName = currentGame.lastEvent!!.name
            showEventDialog = true
        }
        // Lógica para mostrar pop-up de inicio de turno
        if (currentGame != null && currentGame.isStarted && currentGame.currentPlayerTurnId == currentPlayerId) {
            if (currentGame.roundNumber != lastRoundNumber) {
                lastRoundNumber = currentGame.roundNumber
                showTurnStartDialog = true
            }
        }
        // Lógica para detectar si el jugador ha sido expulsado
        if (currentGame != null && currentPlayerId != null && currentGame.playerIds.isNotEmpty() && !currentGame.playerIds.contains(currentPlayerId)) {
            showKickedDialog = true
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
                onAttemptExit = {
                    showExitOneMobileDialog = true
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

    // --- DIÁLOGOS GLOBALES ---

    if (showExitOneMobileDialog) {
        AlertDialog(
            onDismissRequest = { showExitOneMobileDialog = false },
            title = { Text("¿Salir de la partida?") },
            text = { Text("Se perderá el progreso. ¿Estás seguro de que quieres volver al menú principal?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (currentGameId != null) {
                                gameService.cleanUpGameData(currentGameId!!)
                            }
                            currentGameId = null
                            currentPlayerId = null
                            showExitOneMobileDialog = false
                            currentScreen = "mainMenu"
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitOneMobileDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showKickedDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Has sido eliminado de la partida") },
            text = { Text("El anfitrión te ha eliminado de la partida. Serás devuelto al menú principal.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            userDataStore.clearSession()
                            currentGameId = null
                            currentPlayerId = null
                            showKickedDialog = false
                            currentScreen = "mainMenu"
                        }
                    }
                ) {
                    Text("Aceptar")
                }
            }
        )
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
                    onClick = {
                        selectedGameScreen = "actions"
                    },
                    icon = { Icon(Icons.Filled.Casino, contentDescription = "Acciones") },
                    label = { Text("Acciones") }
                )
                NavigationBarItem(
                    selected = selectedGameScreen == "market",
                    onClick = {
                        selectedGameScreen = "market"
                    },
                    icon = { Icon(Icons.Filled.Storefront, contentDescription = "Mercado") },
                    label = { Text("Mercado") }
                )
                NavigationBarItem(
                    selected = selectedGameScreen == "objectives",
                    onClick = {
                        selectedGameScreen = "objectives"
                    },
                    icon = { Icon(Icons.Filled.WorkspacePremium, contentDescription = "Objetivos") },
                    label = { Text("Objetivos") }
                )
                NavigationBarItem(
                    selected = selectedGameScreen == "players",
                    onClick = {
                        selectedGameScreen = "players"
                    },
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

