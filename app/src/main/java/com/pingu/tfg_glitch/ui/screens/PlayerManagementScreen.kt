package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingu.tfg_glitch.data.FirestoreService
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.Player
import com.pingu.tfg_glitch.ui.theme.AccentPurple
import com.pingu.tfg_glitch.ui.theme.AccentYellow
import com.pingu.tfg_glitch.ui.theme.DarkCard
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.TextLight
import com.pingu.tfg_glitch.ui.theme.TextWhite
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RemoveCircleOutline
import com.pingu.tfg_glitch.data.allCrops
import com.pingu.tfg_glitch.data.allObjectives
import kotlin.math.roundToInt
import com.pingu.tfg_glitch.ui.theme.AccentGreen
import com.pingu.tfg_glitch.ui.theme.DarkBackground
import com.pingu.tfg_glitch.ui.theme.GlitchRed


// Instancias de servicios
private val firestoreService = FirestoreService()
private val gameService = GameService()

// Composable para la pantalla de gestión de jugadores
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerManagementScreen(
    gameId: String,
    currentPlayerId: String,
    onGameEnded: () -> Unit
) {
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val allPlayers by firestoreService.getPlayersInGame(gameId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showEndGameDialog by remember { mutableStateOf(false) }

    val isHost = remember(game, currentPlayerId) {
        game?.hostPlayerId == currentPlayerId
    }

    val allPlayersFinishedMarket = remember(game, allPlayers) {
        game?.playersFinishedMarket?.size == allPlayers.size && allPlayers.isNotEmpty()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Jugadores en Partida",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AccentPurple,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (game == null || allPlayers.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp), color = AccentPurple)
                Text(text = "Cargando jugadores...", color = TextLight, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.weight(1f))
            } else {
                val currentTurnPlayer = allPlayers.find { it.id == game?.currentPlayerTurnId }
                val phaseText = when (game?.roundPhase) {
                    "PLAYER_ACTIONS" -> {
                        if (currentTurnPlayer != null) "Turno de: ${currentTurnPlayer.name}" else "Fase de Acciones (Cargando turno...)"
                    }
                    "MARKET_PHASE" -> "Fase Actual: Mercado Glitch"
                    else -> "Cargando fase..."
                }
                val phaseColor = when (game?.roundPhase) {
                    "PLAYER_ACTIONS" -> AccentYellow
                    "MARKET_PHASE" -> AccentGreen
                    else -> TextLight
                }

                Text(
                    text = phaseText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(allPlayers) { player ->
                        PlayerCard(
                            player = player,
                            isHost = isHost,
                            currentPlayerId = currentPlayerId,
                            currentTurnPlayerId = game?.currentPlayerTurnId,
                            onAdjustResources = { targetPlayerId, moneyDelta, energyDelta ->
                                coroutineScope.launch {
                                    val success = gameService.adjustPlayerResourcesManually(targetPlayerId, moneyDelta, energyDelta)
                                    snackbarHostState.showSnackbar(
                                        if (success) "Recursos de ${player.name} ajustados." else "Error al ajustar recursos."
                                    )
                                }
                            },
                            onAdjustPV = { targetPlayerId, pvDelta ->
                                coroutineScope.launch {
                                    val success = gameService.adjustPlayerManualBonusPV(targetPlayerId, pvDelta)
                                    snackbarHostState.showSnackbar(
                                        if (success) "PV de ${player.name} ajustados." else "Error al ajustar PV."
                                    )
                                }
                            },
                            onRemoveCrop = { targetPlayerId, cropId, quantity ->
                                coroutineScope.launch {
                                    val success = gameService.removeCropFromInventory(targetPlayerId, cropId, quantity)
                                    snackbarHostState.showSnackbar(
                                        if (success) "Cultivo eliminado del inventario." else "Error al eliminar cultivo."
                                    )
                                }
                            },
                            onForceTurn = {
                                coroutineScope.launch {
                                    gameService.forceAdvanceTurn(gameId)
                                    snackbarHostState.showSnackbar(
                                        message = "Turno de ${currentTurnPlayer?.name ?: "jugador"} forzado a pasar.",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        )
                    }
                }

                if (isHost && game?.roundPhase == "MARKET_PHASE") {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val success = gameService.advanceRound(gameId)
                                snackbarHostState.showSnackbar(
                                    if (success) "¡Ronda avanzada!" else "No se pudo avanzar la ronda. ¿Han terminado todos?"
                                )
                            }
                        },
                        enabled = allPlayersFinishedMarket,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (allPlayersFinishedMarket) AccentPurple else Color.Gray),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(text = "Siguiente Ronda (Solo Host)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isHost) {
                    Button(
                        onClick = { showEndGameDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlitchRed),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Text(text = "Terminar Partida (Solo Host)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
                    }
                }
            }
        }
    }

    if (showEndGameDialog) {
        AlertDialog(
            onDismissRequest = { showEndGameDialog = false },
            title = { Text("¿Cómo quieres terminar la partida?") },
            text = { Text("Puedes terminar la partida calculando los puntos finales o de forma repentina sin puntuación.") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                showEndGameDialog = false
                                gameService.endGameByPoints(gameId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text("Terminar por Puntos")
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                showEndGameDialog = false
                                gameService.markGameAsEnded(gameId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GlitchRed)
                    ) {
                        Text("Terminar Repentinamente")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndGameDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = DarkCard,
            titleContentColor = AccentPurple,
            textContentColor = TextLight,
            tonalElevation = 8.dp,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun PlayerCard(
    player: Player,
    isHost: Boolean,
    currentPlayerId: String,
    currentTurnPlayerId: String?,
    onAdjustResources: (String, Int, Int) -> Unit,
    onAdjustPV: (String, Int) -> Unit,
    onRemoveCrop: (playerId: String, cropId: String, quantity: Int) -> Unit,
    onForceTurn: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // CORRECCIÓN: Usar el scope de compose

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Nombre: ${player.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
                if (player.id == currentPlayerId) {
                    Text(text = " (Tú)", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextLight)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "ID: ${player.id.take(8)}...", fontSize = 14.sp, color = TextLight)
                IconButton(
                    onClick = {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("Player ID", player.id)
                        clipboardManager.setPrimaryClip(clipData)
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar ID de jugador", tint = TextLight)
                }
            }
            Text(text = "Monedas: ${player.money} 💰", fontSize = 16.sp, color = TextLight)
            Text(text = "Energía Glitch: ${player.glitchEnergy} ⚡", fontSize = 16.sp, color = TextLight)

            val moneyPV = player.money / 5
            val totalUnsoldValue = player.inventario.sumOf { it.valorVentaBase * it.cantidad }
            val unsoldCropsPV = (totalUnsoldValue.toDouble() / 3.0).roundToInt()
            val objectivesPV = player.objectivesClaimed.sumOf { objId ->
                allObjectives.find { it.id == objId }?.rewardPV ?: 0
            }
            val totalPV = moneyPV + unsoldCropsPV + objectivesPV + player.manualBonusPV

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "PV Total: $totalPV PV", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AccentYellow)
            if (player.manualBonusPV != 0) {
                Text(text = "PV Ajuste Manual: ${player.manualBonusPV} PV", fontSize = 14.sp, color = TextLight)
            }

            if (isHost) {
                if (player.inventario.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ajustar Inventario (Manual):",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPurple
                    )
                    player.inventario.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${item.nombre} (x${item.cantidad})",
                                color = TextLight,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onRemoveCrop(player.id, item.id, 1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Eliminar 1 ${item.nombre}",
                                    tint = GlitchRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ajustar PV (Manual):",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPurple
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { onAdjustPV(player.id, -1) },
                        colors = ButtonDefaults.buttonColors(containerColor = GlitchRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Restar 1 PV", tint = TextWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-1 PV", fontSize = 14.sp, color = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdjustPV(player.id, 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir 1 PV", tint = TextWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1 PV", fontSize = 14.sp, color = TextWhite)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ajustar Monedas/Energía (Manual):",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentPurple
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { onAdjustResources(player.id, -1, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = GlitchRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Restar 1 Moneda", tint = TextWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-1 💰", fontSize = 14.sp, color = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdjustResources(player.id, 1, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir 1 Moneda", tint = TextWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1 💰", fontSize = 14.sp, color = TextWhite)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { onAdjustResources(player.id, 0, -1) },
                        colors = ButtonDefaults.buttonColors(containerColor = GlitchRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Restar 1 Energía", tint = TextWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-1 ⚡", fontSize = 14.sp, color = TextWhite)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onAdjustResources(player.id, 0, 1) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.weight(0.5f)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Añadir 1 Energía", tint = TextWhite)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1 ⚡", fontSize = 14.sp, color = TextWhite)
                    }
                }
            }

            if (isHost && player.id == currentTurnPlayerId && player.id != currentPlayerId) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onForceTurn,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Forzar Pasar Turno (Host)", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }

            if (isHost && player.id != currentPlayerId) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            gameService.deletePlayer(player.id)
                            Log.d("PlayerManagementScreen", "Player ${player.name} (${player.id}) deleted by host.")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlitchRed),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Eliminar Jugador", fontSize = 14.sp, color = TextWhite)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlayerManagementScreenPreview() {
    GranjaGlitchAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PlayerManagementScreen(
                gameId = "sampleGame123",
                currentPlayerId = "samplePlayer456",
                onGameEnded = {}
            )
        }
    }
}
