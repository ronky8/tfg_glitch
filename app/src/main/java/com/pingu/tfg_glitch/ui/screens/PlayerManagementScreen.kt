package com.pingu.tfg_glitch.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// --- Instancias de servicios ---
private val firestoreService = FirestoreService()
private val gameService = GameService()


// ========================================================================
// --- Pantalla Principal de Gestión de Jugadores ---
// ========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerManagementScreen(
    gameId: String,
    currentPlayerId: String,
    onGameEnded: () -> Unit
) {
    // --- Estados ---
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val allPlayers by firestoreService.getPlayersInGame(gameId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEndGameDialog by remember { mutableStateOf(false) }
    // [CORREGIDO] Estado para mostrar el diálogo de gestión de inventario. Guarda el ID del jugador a editar.
    var playerToManageInventoryId by remember { mutableStateOf<String?>(null) }


    // --- Estados Derivados ---
    val isHost by remember(game, currentPlayerId) {
        derivedStateOf { game?.hostPlayerId == currentPlayerId }
    }
    val allPlayersFinishedMarket by remember(game, allPlayers) {
        derivedStateOf { game != null && allPlayers.isNotEmpty() && game!!.playersFinishedMarket.size == allPlayers.size }
    }

    // --- UI Principal ---
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Contenido de la pantalla
            if (game == null || allPlayers.isEmpty()) {
                // Estado de carga
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Contenido principal
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        StatusHeader(game = game, allPlayers = allPlayers)
                    }
                    items(allPlayers, key = { it.id }) { player ->
                        PlayerCard(
                            player = player,
                            isHost = isHost,
                            isCurrentPlayer = player.id == currentPlayerId,
                            onAdjustPV = { pvDelta ->
                                coroutineScope.launch {
                                    val success = gameService.adjustPlayerManualBonusPV(player.id, pvDelta)
                                    if (success) snackbarHostState.showSnackbar("PV de ${player.name} ajustados.")
                                }
                            },
                            onAdjustResources = { moneyDelta, energyDelta ->
                                coroutineScope.launch {
                                    val success = gameService.adjustPlayerResourcesManually(player.id, moneyDelta, energyDelta)
                                    if (success) snackbarHostState.showSnackbar("Recursos de ${player.name} ajustados.")
                                }
                            },
                            onManageInventory = { playerToManageInventoryId = player.id },
                            onDeletePlayer = {
                                coroutineScope.launch {
                                    gameService.deletePlayerFromGame(gameId, player.id)
                                    snackbarHostState.showSnackbar("${player.name} ha sido eliminado.")
                                }
                            }
                        )
                    }
                }

                // Controles del Anfitrión
                if (isHost) {
                    HostControls(
                        game = game!!,
                        allPlayersFinishedMarket = allPlayersFinishedMarket,
                        onAdvanceRound = {
                            coroutineScope.launch {
                                val success = gameService.advanceRound(gameId)
                                snackbarHostState.showSnackbar(
                                    if (success) "¡Ronda avanzada!" else "Aún no todos han terminado en el mercado."
                                )
                            }
                        },
                        onEndGame = { showEndGameDialog = true }
                    )
                }
            }
        }
    }

    // [CORREGIDO] Diálogo para gestionar el inventario de un jugador.
    // Se busca la versión más reciente del jugador desde 'allPlayers' para asegurar que la UI se recompone.
    playerToManageInventoryId?.let { playerId ->
        val freshPlayer = allPlayers.find { it.id == playerId }
        if (freshPlayer != null) {
            InventoryManagementDialog(
                player = freshPlayer,
                onDismiss = { playerToManageInventoryId = null },
                onAdjustCrop = { cropId, delta ->
                    coroutineScope.launch {
                        gameService.adjustPlayerInventory(freshPlayer.id, cropId, delta)
                    }
                }
            )
        }
    }


    // Diálogo de fin de partida
    if (showEndGameDialog) {
        EndGameDialog(
            onDismiss = { showEndGameDialog = false },
            onEndByPoints = {
                coroutineScope.launch {
                    gameService.endGameByPoints(gameId)
                    showEndGameDialog = false
                }
            }
        )
    }
}


// ========================================================================
// --- Sub-componentes de la pantalla ---
// ========================================================================

/**
 * Muestra el estado actual de la partida (fase y turno).
 */
@Composable
private fun StatusHeader(game: Game?, allPlayers: List<Player>) {
    val currentTurnPlayer = allPlayers.find { it.id == game?.currentPlayerTurnId }
    val phaseText = when (game?.roundPhase) {
        "PLAYER_ACTIONS" -> if (currentTurnPlayer != null) "Turno de: ${currentTurnPlayer.name}" else "Fase de Acciones"
        "MARKET_PHASE" -> "Fase de Mercado"
        else -> "Cargando..."
    }
    val phaseColor = when (game?.roundPhase) {
        "PLAYER_ACTIONS" -> MaterialTheme.colorScheme.secondary
        "MARKET_PHASE" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Gestión de Partida", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(text = phaseText, style = MaterialTheme.typography.titleMedium, color = phaseColor)
        game?.roundNumber?.let { round ->
            Text(text = "Ronda $round", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/**
 * Tarjeta que muestra la información detallada de un jugador y los controles del anfitrión.
 */
@Composable
private fun PlayerCard(
    player: Player,
    isHost: Boolean,
    isCurrentPlayer: Boolean,
    onAdjustPV: (Int) -> Unit,
    onAdjustResources: (Int, Int) -> Unit,
    onManageInventory: () -> Unit,
    onDeletePlayer: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    // Calcular PV
    val totalPV = remember(player) {
        val unsoldCropsValue = player.inventario.sumOf { it.valorVentaBase.toLong() * it.cantidad.toLong() }
        val unsoldCropsMoney = (unsoldCropsValue.toDouble() / 2.0).roundToInt()
        val finalMoney = player.money + unsoldCropsMoney

        val moneyPV = finalMoney / 3
        val energyPV = player.glitchEnergy
        val objectivesPV = 0 // Los objetivos ya no dan PV directo

        moneyPV + energyPV + objectivesPV + player.manualBonusPV
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            // Fila principal con nombre, PV y botón de expandir
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrentPlayer) {
                        Icon(Icons.Default.Person, contentDescription = "Tú", modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(text = player.name, style = MaterialTheme.typography.titleLarge)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "$totalPV PV", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (isHost) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Expandir")
                        }
                    }
                }
            }

            // Información de recursos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("💰 ${player.money}", style = MaterialTheme.typography.bodyLarge)
                Text("⚡ ${player.glitchEnergy}", style = MaterialTheme.typography.bodyLarge)
            }

            // Controles expandibles del anfitrión
            if (expanded && isHost) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                // Ajuste de Monedas y Energía
                AdjustmentRow(
                    label = "Recursos",
                    onAdd = { onAdjustResources(1, 0) },
                    onRemove = { onAdjustResources(-1, 0) },
                    icon = Icons.Default.Paid
                )
                AdjustmentRow(
                    label = "",
                    onAdd = { onAdjustResources(0, 1) },
                    onRemove = { onAdjustResources(0, -1) },
                    icon = Icons.Default.Bolt
                )
                // Ajuste de PV
                AdjustmentRow(
                    label = "Puntos Victoria",
                    onAdd = { onAdjustPV(1) },
                    onRemove = { onAdjustPV(-1) },
                    icon = Icons.Default.Star
                )

                // Botón para gestionar inventario
                Button(
                    onClick = onManageInventory,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = "Inventario")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Gestionar Inventario", color = MaterialTheme.colorScheme.onTertiaryContainer)
                }


                // Botón de eliminar
                if (!isCurrentPlayer) { // El anfitrión no se puede eliminar a sí mismo
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDeletePlayer,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Eliminar Jugador")
                    }
                }
            }
        }
    }
}

/**
 * Fila de botones para ajustar valores (+/-).
 */
@Composable
private fun AdjustmentRow(label: String, onAdd: () -> Unit, onRemove: () -> Unit, icon: ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(onClick = onRemove, contentPadding = PaddingValues(8.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Restar")
            }
            Spacer(modifier = Modifier.width(4.dp))
            FilledTonalButton(onClick = onAdd, contentPadding = PaddingValues(8.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
    }
}

/**
 * Botones de acción principales para el anfitrión en la parte inferior de la pantalla.
 */
@Composable
private fun HostControls(
    game: Game,
    allPlayersFinishedMarket: Boolean,
    onAdvanceRound: () -> Unit,
    onEndGame: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (game.roundPhase == "MARKET_PHASE") {
            Button(
                onClick = onAdvanceRound,
                enabled = allPlayersFinishedMarket,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Siguiente Ronda")
            }
        }
        Button(
            onClick = onEndGame,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Terminar Partida")
        }
    }
}

/**
 * Diálogo de confirmación para terminar la partida.
 */
@Composable
private fun EndGameDialog(
    onDismiss: () -> Unit,
    onEndByPoints: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Terminar la partida?") },
        text = { Text("Finalizar la partida ahora y ver la puntuación final.") },
        confirmButton = {
            Button(onClick = onEndByPoints) {
                Text("Terminar y Ver Puntuación")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Diálogo para gestionar el inventario
@Composable
private fun InventoryManagementDialog(
    player: Player,
    onDismiss: () -> Unit,
    onAdjustCrop: (cropId: String, delta: Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Inventario de ${player.name}") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCrops) { crop ->
                    val currentAmount = player.inventario.find { it.id == crop.id }?.cantidad ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${crop.nombre}: $currentAmount", modifier = Modifier.weight(1f))
                        Row {
                            IconButton(onClick = { onAdjustCrop(crop.id, -1) }, enabled = currentAmount > 0) {
                                Icon(Icons.Default.Remove, "Quitar")
                            }
                            IconButton(onClick = { onAdjustCrop(crop.id, 1) }) {
                                Icon(Icons.Default.Add, "Añadir")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}


// ========================================================================
// --- Preview ---
// ========================================================================

@Preview(showBackground = true)
@Composable
fun PlayerManagementScreenPreview() {
    GranjaGlitchAppTheme {
        Surface {
            PlayerManagementScreen(gameId = "sample", currentPlayerId = "host", onGameEnded = {})
        }
    }
}

