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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pingu.tfg_glitch.data.*
import com.pingu.tfg_glitch.ui.theme.GranjaGlitchAppTheme
import com.pingu.tfg_glitch.ui.theme.getIconForCoin
import com.pingu.tfg_glitch.ui.theme.getIconForEnergy
import com.pingu.tfg_glitch.ui.theme.getIconForPV
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
                        val totalUnsoldValue = player.inventario.sumOf { it.valorVentaBase.toLong() * it.cantidad.toLong() }
                        val unsoldCropsMoney = (totalUnsoldValue.toDouble() / 2.0).roundToInt()
                        val finalMoney = player.money + unsoldCropsMoney

                        val moneyPV = finalMoney / 3
                        val energyPV = player.glitchEnergy
                        val objectivesPV = 0
                        val totalPV = moneyPV + energyPV + objectivesPV + player.manualBonusPV

                        PlayerScore(
                            player = player,
                            moneyPV = moneyPV,
                            energyPV = energyPV,
                            objectivesPV = objectivesPV,
                            totalPV = totalPV,
                            unsoldCropsMoney = unsoldCropsMoney,
                            finalMoney = finalMoney
                        )
                    }
                    .sortedByDescending { it.totalPV }
            }
        }
    }

    // --- UI Principal ---
    Scaffold(
        bottomBar = {
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

private data class PlayerScore(
    val player: Player,
    val moneyPV: Int,
    val energyPV: Int,
    val objectivesPV: Int,
    val totalPV: Int,
    val unsoldCropsMoney: Int,
    val finalMoney: Int
)

@Composable
private fun PlayerScoreCard(score: PlayerScore, rank: Int) {
    val isWinner = rank == 1
    var revealed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isWinner && revealed) 1.05f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "WinnerCardScale"
    )

    LaunchedEffect(Unit) {
        delay(300L * rank)
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

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${score.totalPV}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    painter = getIconForPV(),
                    contentDescription = "Puntos de Victoria",
                    modifier = Modifier.size(32.dp).padding(start = 4.dp)
                )
            }


            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Desglose de puntos
            ScoreDetailRow(label = "Monedas") {
                ValuePointsPair(value = score.finalMoney, valueIcon = getIconForCoin(), points = score.moneyPV, pointsIcon = getIconForPV())
            }
            if (score.unsoldCropsMoney > 0) {
                ScoreDetailRow(label = "  ↳ Venta de cultivos") {
                    ValuePointsPair(value = score.unsoldCropsMoney, valueIcon = getIconForCoin(), isBonus = true)
                }
            }
            ScoreDetailRow(label = "Energía Glitch") {
                ValuePointsPair(value = score.player.glitchEnergy, valueIcon = getIconForEnergy(), points = score.energyPV, pointsIcon = getIconForPV())
            }
            ScoreDetailRow(label = "Objetivos Reclamados") {
                Text(text = "${score.player.objectivesClaimed.size}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            if (score.player.manualBonusPV != 0) {
                ScoreDetailRow(label = "Ajuste Manual") {
                    ValuePointsPair(points = score.player.manualBonusPV, pointsIcon = getIconForPV(), isBonus = score.player.manualBonusPV > 0)
                }
            }
        }
    }
}

@Composable
private fun ScoreDetailRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            content()
        }
    }
}

@Composable
private fun ValuePointsPair(
    value: Int? = null,
    valueIcon: Painter? = null,
    points: Int? = null,
    pointsIcon: Painter? = null,
    isBonus: Boolean = false
) {
    val textStyle = MaterialTheme.typography.bodyLarge
    val semiBoldStyle = textStyle.copy(fontWeight = FontWeight.SemiBold)

    if (value != null && valueIcon != null) {
        Text(
            text = if (isBonus) "+$value" else "$value",
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Icon(
            painter = valueIcon,
            contentDescription = null,
            modifier = Modifier.size(18.dp).padding(horizontal = 4.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }

    if (points != null && pointsIcon != null) {
        if(value != null) Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = if (isBonus) "+$points" else "$points",
            style = semiBoldStyle
        )
        Icon(
            painter = pointsIcon,
            contentDescription = "PV",
            modifier = Modifier.size(18.dp).padding(start = 4.dp)
        )
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

