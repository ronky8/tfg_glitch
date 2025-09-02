package com.pingu.tfg_glitch.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pingu.tfg_glitch.data.FirestoreService
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.Player
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward

// Instancias de servicios.
private val firestoreService = FirestoreService()
private val gameService = GameService()


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LobbyScreen(
    gameId: String,
    currentPlayerId: String,
    onGameStarted: () -> Unit,
    onBack: () -> Unit
) {
    // Estados.
    val allPlayers by firestoreService.getPlayersInGame(gameId).collectAsState(initial = emptyList())
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Estado local para el orden de los jugadores en el lobby.
    val playerList = remember { mutableStateListOf<Player>() }

    // Sincronizar la lista local con los datos de Firestore.
    LaunchedEffect(allPlayers) {
        if (allPlayers.isNotEmpty()) {
            val currentIds = playerList.map { it.id }.toSet()
            val incomingIds = allPlayers.map { it.id }.toSet()

            // Eliminar jugadores que ya no están.
            playerList.removeIf { it.id !in incomingIds }
            // Añadir nuevos jugadores.
            allPlayers.filter { it.id !in currentIds }.forEach { newPlayer ->
                playerList.add(newPlayer)
            }
        }
    }

    // Efecto para navegar cuando la partida empieza.
    LaunchedEffect(game?.isStarted) {
        if (game?.isStarted == true) {
            onGameStarted()
        }
    }

    // Comprobar si el jugador actual es el anfitrión.
    val isHost = remember(game, currentPlayerId) {
        game?.hostPlayerId == currentPlayerId
    }

    // Función para mover un jugador en la lista local
    val onMovePlayer: (Int, Int) -> Unit = { fromIndex, toIndex ->
        if (fromIndex in playerList.indices && toIndex in playerList.indices) {
            val playerToMove = playerList.removeAt(fromIndex)
            playerList.add(toIndex, playerToMove)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Sala de Espera") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Código de la partida.
            game?.let { currentGame ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Código de Partida:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = currentGame.id,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = {
                                val clipboard =
                                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Game Code", currentGame.id)
                                clipboard.setPrimaryClip(clip)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("¡Código copiado!")
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar Código")
                        }
                    }
                }
            } ?: run {
                CircularProgressIndicator()
                Text("Cargando partida...", modifier = Modifier.padding(top = 16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lista de jugadores con botones para reordenar
            if (allPlayers.isNotEmpty()) {
                Text(
                    text = "Jugadores (${allPlayers.size}/4)",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                PlayerList(
                    players = playerList,
                    hostPlayerId = game?.hostPlayerId,
                    isHost = isHost,
                    onMovePlayer = onMovePlayer
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón de iniciar partida solo para el anfitrión.
            if (isHost) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                gameService.updatePlayerOrderAndStartGame(gameId, playerList.map { it.id })
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(e.message ?: "Error al iniciar.")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = playerList.size >= 2
                ) {
                    Text("Empezar Partida", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            } else {
                CircularProgressIndicator()
                Text("Esperando a que el anfitrión inicie...", textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun PlayerList(
    players: List<Player>,
    hostPlayerId: String?,
    isHost: Boolean,
    onMovePlayer: (fromIndex: Int, toIndex: Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(players, key = { _, player -> player.id }) { index, player ->
            PlayerCardInLobby(
                player = player,
                rank = index + 1,
                hostPlayerId = hostPlayerId,
                isHost = isHost,
                onMoveUp = { onMovePlayer(index, index - 1) },
                onMoveDown = { onMovePlayer(index, index + 1) }
            )
        }
    }
}

@Composable
fun PlayerCardInLobby(
    player: Player,
    rank: Int,
    hostPlayerId: String?,
    isHost: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 16.dp),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (player.id == hostPlayerId) {
                Icon(Icons.Default.Star, contentDescription = "Anfitrión", tint = MaterialTheme.colorScheme.primary)
            }
            if (isHost && player.id != hostPlayerId) {
                Row {
                    IconButton(onClick = onMoveUp, enabled = rank > 1) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Subir")
                    }
                    IconButton(onClick = onMoveDown, enabled = rank < 4) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Bajar")
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun LobbyScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LobbyScreen(gameId = "ABCDEF", currentPlayerId = "host123", onGameStarted = {}, onBack = {})
        }
    }
}
