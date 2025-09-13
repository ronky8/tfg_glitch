package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.getIconForCoin
import com.pingu.tfg_glitch.ui.theme.getIconForCrop
import com.pingu.tfg_glitch.ui.theme.getIconForEnergy
import com.pingu.tfg_glitch.ui.theme.getIconForPV
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
    currentPlayerId: String
) {
    // --- Estados ---
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val allPlayers by firestoreService.getPlayersInGame(gameId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEndGameDialog by remember { mutableStateOf(false) }
    var showInventoryDialog by remember { mutableStateOf(false) }
    var selectedPlayerForInventory by remember { mutableStateOf<Player?>(null) }


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
                            onManageInventory = {
                                selectedPlayerForInventory = player
                                showInventoryDialog = true
                            },
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

    // Diálogos
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

    if (showInventoryDialog && selectedPlayerForInventory != null) {
        val player = allPlayers.find { it.id == selectedPlayerForInventory!!.id }
        if (player != null) {
            InventoryManagementDialog(
                player = player,
                onDismiss = { showInventoryDialog = false },
                onAdjustCrop = { cropId, delta ->
                    coroutineScope.launch {
                        gameService.adjustPlayerInventory(player.id, cropId, delta)
                    }
                }
            )
        }
    }
}


// ========================================================================
// --- Sub-componentes de la pantalla ---
// ========================================================================

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
    var expanded by remember { mutableStateOf(false) }

    val totalPV = remember(player) {
        val unsoldCropsValue = player.inventario.sumOf { it.valorVentaBase.toLong() * it.cantidad.toLong() }
        val unsoldCropsMoney = (unsoldCropsValue.toDouble() / 2.0).roundToInt()
        val finalMoney = player.money + unsoldCropsMoney
        val moneyPV = finalMoney / 3
        val energyPV = player.glitchEnergy
        val objectivesPV = 0
        moneyPV + energyPV + objectivesPV + player.manualBonusPV
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                    Text(text = "$totalPV", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Icon(painter = getIconForPV(), contentDescription = "Puntos de Victoria", modifier = Modifier.size(24.dp).padding(start = 4.dp))
                    if (isHost) {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, "Expandir")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = getIconForCoin(), contentDescription = "Monedas", modifier = Modifier.size(18.dp))
                    Text(" ${player.money}", style = MaterialTheme.typography.bodyLarge)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painter = getIconForEnergy(), contentDescription = "Energía", modifier = Modifier.size(18.dp))
                    Text(" ${player.glitchEnergy}", style = MaterialTheme.typography.bodyLarge)
                }
            }

            if (expanded && isHost) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                AdjustmentRow(label = "Monedas", onAdd = { onAdjustResources(1, 0) }, onRemove = { onAdjustResources(-1, 0) }, icon = getIconForCoin())
                AdjustmentRow(label = "Energía", onAdd = { onAdjustResources(0, 1) }, onRemove = { onAdjustResources(0, -1) }, icon = getIconForEnergy())
                AdjustmentRow(label = "Puntos Victoria", onAdd = { onAdjustPV(1) }, onRemove = { onAdjustPV(-1) }, icon = getIconForPV())

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onManageInventory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = "Inventario")
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Gestionar Inventario")
                }

                if (!isCurrentPlayer) {
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

@Composable
private fun AdjustmentRow(label: String, onAdd: () -> Unit, onRemove: () -> Unit, icon: Painter) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painter = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onRemove) {
                Icon(Icons.Default.Remove, contentDescription = "Restar")
            }
            Spacer(modifier = Modifier.width(4.dp))
            FilledTonalIconButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Añadir")
            }
        }
    }
}

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

/**
 *Diálogo para gestionar el inventario de un jugador.
 */
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
                items(allCrops, key = { it.id }) { crop ->
                    val currentQuantity = player.inventario.find { it.id == crop.id }?.cantidad ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = getIconForCrop(crop.id),
                                contentDescription = crop.nombre,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${crop.nombre} (x$currentQuantity)")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = { onAdjustCrop(crop.id, -1) },
                                enabled = currentQuantity > 0
                            ) {
                                Icon(Icons.Default.Remove, "Quitar")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            FilledTonalIconButton(onClick = { onAdjustCrop(crop.id, 1) }) {
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

@Preview(showBackground = true)
@Composable
fun PlayerManagementScreenPreview() {
    GranjaGlitchAppTheme {
        Surface {
            PlayerManagementScreen(gameId = "sample", currentPlayerId = "host")
        }
    }
}

