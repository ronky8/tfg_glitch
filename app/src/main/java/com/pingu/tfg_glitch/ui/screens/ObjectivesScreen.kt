package com.pingu.tfg_glitch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.FirestoreService
import com.pingu.tfg_glitch.data.GameService
import com.pingu.tfg_glitch.data.Objective
import com.pingu.tfg_glitch.ui.components.PlayerInfoCard
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import kotlinx.coroutines.launch
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.PaddingValues
import com.pingu.tfg_glitch.ui.theme.getIconForCoin
import com.pingu.tfg_glitch.ui.theme.getIconForEnergy

// --- Instancias de servicios ---
private val firestoreService = FirestoreService()
private val gameService = GameService()


// ========================================================================
// --- Pantalla Principal de Objetivos ---
// ========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectivesScreen(gameId: String, currentPlayerId: String) {
    // --- Estados ---
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val currentPlayer by firestoreService.getPlayer(currentPlayerId).collectAsState(initial = null)
    val allPlayers by firestoreService.getPlayersInGame(gameId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
            if (game == null || currentPlayer == null) {
                // Estado de carga
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Contenido principal
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            text = "Objetivos de la Partida",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PlayerInfoCard(player = currentPlayer!!)
                    }

                    // Seccion de objetivos de ronda
                    if (game!!.roundObjective != null) {
                        item {
                            Text(
                                text = "Objetivo de Ronda",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val isClaimed = game!!.claimedObjectivesByPlayer.containsKey(game!!.roundObjective!!.id)
                            val claimedByPlayer = game!!.claimedObjectivesByPlayer[game!!.roundObjective!!.id]?.let { playerId ->
                                allPlayers.find { it.id == playerId }
                            }
                            ObjectiveCard(
                                objective = game!!.roundObjective!!,
                                isClaimed = isClaimed,
                                claimedByPlayerName = claimedByPlayer?.name,
                                onClaim = {
                                    coroutineScope.launch {
                                        val result = gameService.claimObjective(gameId, currentPlayerId, game!!.roundObjective!!.id)
                                        snackbarHostState.showSnackbar(result)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Seccion de objetivos de partida
                    if (game!!.activeObjectives.isNotEmpty()) {
                        item {
                            Text(
                                text = "Objetivos de Partida Permanentes",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(game!!.activeObjectives, key = { it.id }) { objective ->
                            val isClaimed = game!!.claimedObjectivesByPlayer.containsKey(objective.id)
                            val claimedByPlayer = game!!.claimedObjectivesByPlayer[objective.id]?.let { playerId ->
                                allPlayers.find { it.id == playerId }
                            }
                            ObjectiveCard(
                                objective = objective,
                                isClaimed = isClaimed,
                                claimedByPlayerName = claimedByPlayer?.name,
                                onClaim = {
                                    coroutineScope.launch {
                                        val result = gameService.claimObjective(gameId, currentPlayerId, objective.id)
                                        snackbarHostState.showSnackbar(result)
                                    }
                                }
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "No hay objetivos activos en esta partida.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}


// ========================================================================
// --- Sub-componentes de la pantalla ---
// ========================================================================

@Composable
private fun ObjectiveCard(
    objective: Objective,
    isClaimed: Boolean,
    claimedByPlayerName: String? = null,
    onClaim: () -> Unit
) {
    val cardColors = if (isClaimed) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.elevatedCardColors()
    }

    val currentClaimedByPlayerName = remember(claimedByPlayerName) { claimedByPlayerName }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = cardColors
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = objective.description,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.WorkspacePremium,
                    contentDescription = "Recompensa",
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Fila para la recompensa con iconos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val rewardTextStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (objective.reward.moneyChange > 0) {
                        Text(text = "+${objective.reward.moneyChange}", style = rewardTextStyle)
                        Icon(
                            painter = getIconForCoin(),
                            contentDescription = "Monedas",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (objective.reward.moneyChange > 0 && objective.reward.energyChange > 0) {
                        Text(text = "/", style = rewardTextStyle)
                    }
                    if (objective.reward.energyChange > 0) {
                        Text(text = "+${objective.reward.energyChange}", style = rewardTextStyle)
                        Icon(
                            painter = getIconForEnergy(),
                            contentDescription = "Energía",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isClaimed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Reclamado")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¡Objetivo Reclamado por ${currentClaimedByPlayerName ?: "otro jugador"}!", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onClaim,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reclamar Objetivo")
                }
            }
        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun ObjectivesScreenPreview() {
    GranjaGlitchAppTheme {
        Surface {
            ObjectivesScreen(gameId = "sampleGame123", currentPlayerId = "samplePlayer456")
        }
    }
}
