package com.pingu.tfg_glitch

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

// Composable principal que maneja la navegación entre pantallas de alto nivel
@Composable
fun AppNavigation() {
    // Usamos 'remember' para que estos estados persistan a través de recomposiciones
    val currentScreenState = remember { mutableStateOf("mainMenu") }
    var currentScreen by currentScreenState

    val currentGameIdState = remember { mutableStateOf<String?>(null) }
    var currentGameId by currentGameIdState

    val currentPlayerIdState = remember { mutableStateOf<String?>(null) }
    var currentPlayerId by currentPlayerIdState


    when (currentScreen) {
        "mainMenu" -> MainMenuScreen(
            onStartGame = { currentScreen = "multiplayerMenu" },
            onViewRules = { currentScreen = "rules" }, // Navegar a la pantalla de reglas
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
                currentGameId = gameId
                currentPlayerId = hostPlayerId // El host es el jugador actual
                currentScreen = "game"
            }
        )
        "joinGame" -> JoinGameScreen(
            onGameJoined = { gameId, playerId ->
                Log.d("AppNavigation", "Joined game: $gameId, Current Player: $playerId")
                currentGameId = gameId
                currentPlayerId = playerId // Guarda el ID del jugador que se acaba de unir
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
                        // Cuando la partida termina, navega a la pantalla de puntuación final
                        currentScreen = "finalScore"
                    }
                )
            } else {
                // Si gameId o playerId son nulos, volvemos al menú principal.
                LaunchedEffect(Unit) {
                    Log.w("AppNavigation", "gameId or playerId is null, returning to mainMenu.")
                    currentScreenState.value = "mainMenu" // Acceder con .value
                }
            }
        }
        "oneMobileMode" -> {
            OneMobileScreen(
                onBackToMainMenu = {
                    currentGameIdState.value = null // Acceder con .value
                    currentPlayerIdState.value = null // Acceder con .value
                    currentScreenState.value = "mainMenu" // Acceder con .value
                }
            )
        }
        "rules" -> { // Nueva ruta de navegación para las reglas
            RulesScreen(onBack = { currentScreen = "mainMenu" })
        }
        "finalScore" -> { // Nueva ruta de navegación para la puntuación final
            val gameId = currentGameId // Necesitamos el gameId para la pantalla de puntuación
            if (gameId != null) {
                FinalScoreScreen(
                    gameId = gameId,
                    onBackToMainMenu = {
                        currentGameIdState.value = null // Acceder con .value
                        currentPlayerIdState.value = null // Acceder con .value
                        currentScreenState.value = "mainMenu" // Acceder con .value
                    }
                )
            } else {
                // Si no hay gameId al llegar aquí, volvemos al menú principal.
                LaunchedEffect(Unit) {
                    Log.w("AppNavigation", "gameId is null in finalScore, returning to mainMenu.")
                    currentScreenState.value = "mainMenu" // Acceder con .value
                }
            }
        }
    }
}

// Composable que contiene las pantallas de juego (Mercado y Jugadores)
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

    // Nuevo estado para rastrear si los datos del juego se han cargado inicialmente
    var isGameLoading by remember { mutableStateOf(true) } // Inicia en true

    // Determina si el jugador actual es el anfitrión.
    // Se recalcula cada vez que 'game' o 'currentPlayerId' cambian.
    val isHost = remember(game, currentPlayerId) {
        val hostCheck = game?.hostPlayerId == currentPlayerId
        Log.d("GameScreen", "isHost calculated: $hostCheck (gameId: $gameId, currentPlayerId: $currentPlayerId, game?.hostPlayerId: ${game?.hostPlayerId})")
        hostCheck
    }

    // Observa el estado de la partida
    LaunchedEffect(game) {
        Log.d("GameScreen", "LaunchedEffect(game) triggered. Current game: $game")
        // Crear una copia local no nula de 'game' si no es null
        val gameData = game // Copia local para smart cast
        if (gameData != null) {
            if (isGameLoading) {
                Log.d("GameScreen", "Game data loaded for the first time.")
                isGameLoading = false // Los datos del juego se han cargado
            }
            // Si la partida existe y está marcada como terminada, navega a la pantalla de puntuación final.
            if (gameData.hasGameEnded) { // Usar gameData para el smart cast
                Log.d("GameScreen", "Game hasGameEnded detected. Calling onGameEnded to navigate to FinalScoreScreen.")
                onGameEnded()
            }
        }
        // Si el objeto 'game' se vuelve nulo (lo que significa que el documento de la partida
        // fue eliminado de Firestore) Y ya habíamos cargado datos previamente (no estamos en carga inicial),
        // entonces asumimos que la partida ha terminado y navegamos al menú principal.
        // Este caso solo debería ocurrir si la partida es eliminada por el host desde la FinalScoreScreen
        // o si hay un error externo. No debería causar navegación prematura a FinalScoreScreen.
        else if (game == null && !isGameLoading) { // Eliminado !showGameEndedDialog para este flujo
            Log.w("GameScreen", "Game object is null after initial load. Assuming external deletion or error. Returning to main menu.")
            onGameEnded() // Llama al callback para que el padre maneje la navegación y reseteo
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
                NavigationBarItem( // Nueva pestaña para Objetivos
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
            // Muestra un indicador de carga si los datos del juego aún no se han cargado
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
                // Muestra las pantallas de juego una vez que los datos se han cargado
                when (selectedGameScreen) {
                    "actions" -> PlayerActionsScreen(gameId = gameId, currentPlayerId = currentPlayerId)
                    "market" -> MarketScreen(gameId = gameId, currentPlayerId = currentPlayerId)
                    "objectives" -> ObjectivesScreen(gameId = gameId, currentPlayerId = currentPlayerId) // Nueva pantalla de Objetivos
                    "players" -> PlayerManagementScreen(
                        gameId = gameId,
                        currentPlayerId = currentPlayerId,
                        onGameEnded = onGameEnded
                    )
                }
            }
        }
    }

    // Diálogo para notificar que la partida ha terminado abruptamente
    // Este diálogo se muestra SOLO si el host elige "Terminar Repentinamente"
    // y no si la partida termina por puntos (que navega directamente).
    if (showGameEndedDialog) {
        AlertDialog(
            onDismissRequest = { /* No dismissable by user */ },
            title = { Text("Partida Terminada") },
            text = { Text("La partida ha sido terminada por el anfitrión de forma repentina.") }, // Mensaje ajustado
            confirmButton = {
                Button(onClick = {
                    Log.d("GameScreen", "AlertDialog Confirm Button clicked. showGameEndedDialog = false")
                    showGameEndedDialog = false
                    // Al aceptar el diálogo de fin abrupto, navegamos a la pantalla de puntuación final
                    // y la limpieza se hará desde allí.
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
