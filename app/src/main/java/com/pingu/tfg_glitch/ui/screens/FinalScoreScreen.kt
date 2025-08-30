package com.pingu.tfg_glitch.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// --- Instancias de servicios ---
private val firestoreService = FirestoreService()
private val gameService = GameService()


// ========================================================================
// --- Pantalla Principal de Puntuación Final ---
// ========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalScoreScreen(
    gameId: String,
    onBackToMainMenu: () -> Unit
) {
    // --- Estados ---
    val game by gameService.getGame(gameId).collectAsState(initial = null)
    val allPlayers by firestoreService.getPlayersInGame(gameId).collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    // --- Estados Derivados ---
    val finalScores by remember(allPlayers) {
        derivedStateOf {
            if (allPlayers.isEmpty()) {
                emptyList()
            } else {
                allPlayers
                    .map { player ->
                        val moneyPV = player.money / 5
                        val totalUnsoldValue = player.inventario.sumOf { it.valorVentaBase * it.cantidad }
                        val unsoldCropsPV = (totalUnsoldValue.toDouble() / 3.0).roundToInt()
                        val objectivesPV = player.objectivesClaimed.sumOf { objId ->
                            allObjectives.find { it.id == objId }?.rewardPV ?: 0
                        }
                        val totalPV = moneyPV + unsoldCropsPV + objectivesPV + player.manualBonusPV

                        PlayerScore(
                            player = player,
                            moneyPV = moneyPV,
                            unsoldCropsPV = unsoldCropsPV,
                            objectivesPV = objectivesPV,
                            totalPV = totalPV
                        )
                    }
                    .sortedByDescending { it.totalPV } // Ordenar de mayor a menor puntuación
            }
        }
    }

    // --- UI Principal ---
    Scaffold(
        bottomBar = {
            // Botón fijo en la parte inferior
            Button(
                onClick = {
                    coroutineScope.launch {
                        gameService.cleanUpGameData(gameId)
                        onBackToMainMenu()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
            ) {
                Text("Volver al Menú Principal")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (finalScores.isEmpty()) {
                // Estado de carga
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                // Contenido principal
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Text(
                            "¡Partida Terminada!",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Resultados Finales",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    itemsIndexed(finalScores, key = { _, score -> score.player.id }) { index, score ->
                        PlayerScoreCard(
                            score = score,
                            rank = index + 1
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}


// ========================================================================
// --- Sub-componentes de la pantalla ---
// ========================================================================

/**
 * Data class para encapsular los resultados de un jugador.
 */
private data class PlayerScore(
    val player: Player,
    val moneyPV: Int,
    val unsoldCropsPV: Int,
    val objectivesPV: Int,
    val totalPV: Int
)

/**
 * Tarjeta que muestra la puntuación detallada de un jugador.
 */
@Composable
private fun PlayerScoreCard(score: PlayerScore, rank: Int) {
    val isWinner = rank == 1
    var revealed by remember { mutableStateOf(false) }

    // Animación para la tarjeta del ganador
    val scale by animateFloatAsState(
        targetValue = if (isWinner && revealed) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "WinnerCardScale"
    )

    LaunchedEffect(Unit) {
        delay(300L * rank) // Retraso escalonado para revelar las tarjetas
        revealed = true
    }

    val cardColors = if (isWinner) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    } else {
        CardDefaults.elevatedCardColors()
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = cardColors
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila del ranking y nombre
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                    Text(
                        text = score.player.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (isWinner) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Ganador",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Puntuación total
            Text(
                text = "${score.totalPV} PV",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End)
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Desglose de puntos
            ScoreDetailRow("Monedas", "${score.player.money} 💰", "${score.moneyPV} PV")
            ScoreDetailRow("Cultivos no vendidos", "${score.player.inventario.sumOf { it.cantidad }}", "${score.unsoldCropsPV} PV")
            ScoreDetailRow("Objetivos", "${score.player.objectivesClaimed.size}", "${score.objectivesPV} PV")
            if (score.player.manualBonusPV != 0) {
                ScoreDetailRow("Ajuste Manual", "", "${score.player.manualBonusPV} PV")
            }
        }
    }
}

/**
 * Fila para mostrar un detalle de la puntuación.
 */
@Composable
private fun ScoreDetailRow(label: String, value: String, points: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            Text(
                text = points,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}


// ========================================================================
// --- Preview ---
// ========================================================================

@Preview(showBackground = true)
@Composable
fun FinalScoreScreenPreview() {
    GranjaGlitchAppTheme {
        Surface {
            FinalScoreScreen(gameId = "sampleGame123", onBackToMainMenu = {})
        }
    }
}

