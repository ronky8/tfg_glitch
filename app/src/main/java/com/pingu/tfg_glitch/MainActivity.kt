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
import androidx.compose.material.icons.filled.Casino // Nuevo icono para acciones/dados
import androidx.compose.material.icons.filled.Star // Nuevo icono para objetivos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.ui.screens.MarketScreen
import com.pingu.tfg_glitch.ui.screens.PlayerManagementScreen
import com.pingu.tfg_glitch.ui.screens.PlayerActionsScreen // Importar la nueva pantalla
import com.pingu.tfg_glitch.ui.screens.MainMenuScreen
import com.pingu.tfg_glitch.ui.screens.MultiplayerMenuScreen
import com.pingu.tfg_glitch.ui.screens.CreateGameScreen
import com.pingu.tfg_glitch.ui.screens.JoinGameScreen
import com.pingu.tfg_glitch.ui.screens.OneMobileScreen // Importar la nueva pantalla OneMobileScreen
import com.pingu.tfg_glitch.ui.screens.RulesScreen // Importar la nueva pantalla RulesScreen
import com.pingu.tfg_glitch.ui.screens.FinalScoreScreen // Importar la nueva pantalla FinalScoreScreen
import com.pingu.tfg_glitch.ui.screens.ObjectivesScreen // Importar la nueva pantalla ObjectivesScreen
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.GlitchRed
import com.pingu.tfg_glitch.ui.theme.DarkCard
import com.pingu.tfg_glitch.ui.theme.AccentPurple
import com.pingu.tfg_glitch.ui.theme.TextLight
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import android.util.Log // Importar Log para depuración
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

// Instancia del servicio para la gestión de partidas.
private val gameService = GameService()

// NUEVO: Objeto para gestionar la sesión del jugador (reconexión)
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

    // Leer la sesión guardada solo una vez al iniciar
    val (savedGameId, savedPlayerId) = remember { SessionManager.getSession(context) }

    // Si hay una sesión guardada, empezamos en la pantalla de juego, si no, en el menú
    var currentScreen by rememberSaveable { mutableStateOf(if (savedGameId != null && savedPlayerId != null) "game" else "mainMenu") }
    var currentGameId by rememberSaveable { mutableStateOf(savedGameId) }
    var currentPlayerId by rememberSaveable { mutableStateOf(savedPlayerId) }


    when (currentScreen) {
        "mainMenu" -> MainMenuScreen(
            onStartGame = { currentScreen = "multiplayerMenu" },
            onViewRules = { currentScreen = "rules" },
            onOneMobileMode = {
                currentScreen = "oneMobileMode"
            }
        )
        "multiplayerMenu" -> MultiplayerMenuScreen(
            onBack = { currentScreen = "mainMenu" },
            onStartMultiplayerGame = { currentScreen = "createGame" },
            onJoinMultiplayerGame = { currentScreen = "joinGame" }
        )
        "createGame" -> CreateGameScreen(
            onGameCreated = { gameId, hostPlayerId ->
                Log.d("AppNavigation", "Game created: $gameId, Host Player: $hostPlayerId")
                SessionManager.saveSession(context, gameId, hostPlayerId) // Guardar sesión
                currentGameId = gameId
                currentPlayerId = hostPlayerId
                currentScreen = "game"
            }
        )
        "joinGame" -> JoinGameScreen(
            onGameJoined = { gameId, playerId ->
                Log.d("AppNavigation", "Joined game: $gameId, Current Player: $playerId")
                SessionManager.saveSession(context, gameId, playerId) // Guardar sesión
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
                    onGameEnded = {
                        Log.d("AppNavigation", "onGameEnded called. Navigating to FinalScoreScreen.")
                        currentScreen = "finalScore"
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    Log.w("AppNavigation", "gameId or playerId is null, returning to mainMenu.")
                    SessionManager.clearSession(context) // Limpiar por si acaso
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
                        SessionManager.clearSession(context) // Limpiar sesión al volver al menú
                        currentGameId = null
                        currentPlayerId = null
                        currentScreen = "mainMenu"
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    Log.w("AppNavigation", "gameId is null in finalScore, returning to mainMenu.")
                    SessionManager.clearSession(context) // Limpiar por si acaso
                    currentScreen = "mainMenu"
                }
            }
        }
    }
}

// ... (El resto de GameScreen no necesita cambios) ...
@Composable
fun GameScreen(
    gameId: String,
    currentPlayerId: String,
    onGameEnded: () -> Unit
) {
    var selectedGameScreen by remember { mutableStateOf("actions") } // Inicia en la pantalla de acciones
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    var showGameEndedDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var isGameLoading by remember { mutableStateOf(true) }

    val isHost = remember(game, currentPlayerId) {
        val hostCheck = game?.hostPlayerId == currentPlayerId
        Log.d("GameScreen", "isHost calculated: $hostCheck (gameId: $gameId, currentPlayerId: $currentPlayerId, game?.hostPlayerId: ${game?.hostPlayerId})")
        hostCheck
    }

    LaunchedEffect(game) {
        Log.d("GameScreen", "LaunchedEffect(game) triggered. Current game: $game")
        val gameData = game
        if (gameData != null) {
            if (isGameLoading) {
                Log.d("GameScreen", "Game data loaded for the first time.")
                isGameLoading = false
            }
            if (gameData.hasGameEnded) {
                Log.d("GameScreen", "Game hasGameEnded detected. Calling onGameEnded to navigate to FinalScoreScreen.")
                onGameEnded()
            }
        }
        else if (game == null && !isGameLoading) {
            Log.w("GameScreen", "Game object is null after initial load. Assuming external deletion or error. Returning to main menu.")
            onGameEnded()
        }
    }

    Scaffold(
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

    if (showGameEndedDialog) {
        AlertDialog(
            onDismissRequest = { /* No dismissable by user */ },
            title = { Text("Partida Terminada") },
            text = { Text("La partida ha sido terminada por el anfitrión de forma repentina.") },
            confirmButton = {
                Button(onClick = {
                    Log.d("GameScreen", "AlertDialog Confirm Button clicked. showGameEndedDialog = false")
                    showGameEndedDialog = false
                    onGameEnded()
                }) {
                    Text("Aceptar")
                }
            },
            containerColor = DarkCard,
            titleContentColor = AccentPurple,
            textContentColor = TextLight,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(16.dp),
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        )
    }
}
